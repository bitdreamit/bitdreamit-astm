package com.bitdreamit.astm.asyncastm.service.connection.file;

import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles received ASTM frames into a complete message.
 *
 * FIX (Bug #20 — Checksum bytes appearing as separate lines):
 *
 * D-10 frame format (from Bio-Rad Technical Bulletin L20017702):
 *   <STX> FN record_data <CR> <ETX> <CHK1> <CHK2> <CR> <LF>
 *
 * NOTE: The frame number is DIRECTLY ATTACHED to the record (no <CR> between them).
 * The <CR> comes BEFORE <ETX>, not before the checksum.
 *
 * readLine() returns (after <STX> is consumed):
 *   FN record_data <CR> <ETX> checksum
 *
 * Example: "1H|\^&...<CR><ETX>03"
 *
 * To extract the record:
 *   1. Strip the frame number (first char — a digit 0-7)
 *   2. Take everything BEFORE the first <CR>
 *   3. That's the record — everything after <CR> is <ETX>+checksum (discard)
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
        // D-10 format: "1H|\^&..." — frame number is directly attached to record
        if (frame.length() > 1 && Character.isDigit(frame.charAt(0))) {
            frame = frame.substring(1);
        }

        // Step 2: Take everything BEFORE the first <CR>
        // After stripping frame number: "H|\^&...<CR><ETX>03"
        // The <CR> separates the record from <ETX>+checksum
        int crIndex = frame.indexOf('\r');
        if (crIndex >= 0) {
            frame = frame.substring(0, crIndex);
        }

        // Step 3: Strip any trailing ETX/ETB just in case
        while (frame.length() > 0) {
            char last = frame.charAt(frame.length() - 1);
            if (last == 0x03 || last == 0x17) {
                frame = frame.substring(0, frame.length() - 1);
            } else {
                break;
            }
        }

        // Add the clean record
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
