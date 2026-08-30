package com.bitdreamit.astm.asyncastm.service.connection.file;

import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles received ASTM frames into a complete message.
 *
 * FIX (Bug #20 — Checksum bytes appearing as separate lines):
 * FIX (Bug #23 — i800: multiple records per frame were lost):
 *
 * Two analyzer frame formats:
 *
 * 1. Bio-Rad D-10 (one record per frame):
 *    <STX> FN record <CR> <ETX> checksum <CR> <LF>
 *    Example: <STX>1H|\^&...<CR><ETX>03<CR><LF>
 *    → Extract: "H|\^&..."
 *
 * 2. Maccura i800 (multiple records per frame, COBAS-style):
 *    <STX> FN rec1 <CR> rec2 <CR> rec3 <CR> ... <ETB/ETX> checksum <CR> <LF>
 *    Example: <STX>1H|\^&...<CR>P|1...<CR>O|1...<CR>R|1...<ETB>B8<CR><LF>
 *    → Extract: "H|\^&...<CR>P|1...<CR>O|1...<CR>R|1..."
 *
 * The fix:
 *   1. Strip frame number (first char)
 *   2. Find <ETB> (0x17) or <ETX> (0x03) — these mark the end of record data
 *   3. Take everything between frame number and <ETB>/<ETX>
 *   4. The <CR> characters within are record separators (preserve them)
 *   5. Everything after <ETB>/<ETX> is the checksum (discard)
 *
 * This handles BOTH formats:
 *   - D-10: "1H|\^&...\r\x03" → strip "1" → find \x03 → keep "H|\^&..."
 *   - i800: "1H|\^&...\rP|1...\rO|1...\xETB" → strip "1" → find \xETB → keep "H|\^&...\rP|1...\rO|1..."
 */
public class FrameBuffer {
    private Protocol protocol;
    private List<String> frames;
    private boolean complete;

    public FrameBuffer(Protocol protocol) {
        this.protocol = protocol;
        this.frames = new ArrayList<>();
        this.complete = false;
    }

    public void appendFrame(String frame) {
        if (frame == null || frame.isEmpty()) {
            return;
        }

        // Step 1: Strip frame number (first char — a digit 0-7)
        if (frame.length() > 1 && Character.isDigit(frame.charAt(0))) {
            frame = frame.substring(1);
        }

        // Step 2: Find <ETB> (0x17) or <ETX> (0x03) — marks end of record data
        // Everything after this is the checksum (2 hex chars) which we discard.
        int endIndex = -1;
        for (int i = 0; i < frame.length(); i++) {
            char c = frame.charAt(i);
            if (c == 0x03 || c == 0x17) {  // ETX or ETB
                endIndex = i;
                break;
            }
        }

        // Step 3: Take everything before <ETB>/<ETX>
        // This is the record data — may contain multiple <CR>-separated records
        if (endIndex >= 0) {
            frame = frame.substring(0, endIndex);
        }
        // If no ETB/ETX found, keep the whole frame (shouldn't happen, but be safe)

        // Step 4: Strip any trailing <CR> that might be at the end of the record data
        // (some analyzers put <CR> right before <ETB>/<ETX>)
        while (frame.endsWith("\r") || frame.endsWith("\n")) {
            frame = frame.substring(0, frame.length() - 1);
        }

        // Step 5: Add the clean record data to the frames list
        // The <CR> characters WITHIN the frame are preserved — they separate
        // multiple records (i800/COBAS style).
        if (!frame.isEmpty()) {
            this.frames.add(frame);
        }
    }

    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        for (String frame : frames) {
            sb.append(frame).append("\r\n");
        }
        return sb.toString();
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
