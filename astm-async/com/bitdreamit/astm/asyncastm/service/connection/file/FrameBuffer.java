package com.bitdreamit.astm.asyncastm.service.connection.file;

import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles received ASTM frames into a complete message.
 *
 * FIX (Bug #20 — Checksum bytes appearing as separate lines in decoded message):
 *
 * ASTM E-1381 frame format on the wire:
 *   <STX> FN <CR> record_data <CR> checksum <ETX|ETB> <CR> <LF>
 *
 * The TransferReceiverState reads the STX byte, then calls readLine() which
 * reads until <CR><LF>. So the frame string passed to appendFrame() contains:
 *   FN <CR> record_data <CR> checksum <ETX|ETB>
 *
 * (The final <CR><LF> is consumed by readLine but not included in the return.)
 *
 * Previous code only stripped the frame number (first char), leaving:
 *   <CR> record_data <CR> checksum <ETX>
 *
 * When this was later split by <CR>, the checksum appeared as a separate
 * "record" line (e.g., "07", "3F", "DB"). This caused Mirth's ASTM parser
 * and the user's JavaScript transformer to see extra garbage lines.
 *
 * This fix properly parses the frame to extract ONLY the record_data:
 *   1. Strip frame number (first char, a digit 1-7)
 *   2. Strip leading <CR> (if present after frame number)
 *   3. Find the LAST <CR> in the frame — everything after it is "checksum<ETX/ETB>"
 *   4. Keep only the record_data between the leading <CR> and the last <CR>
 *
 * For multi-frame messages (ETB-terminated middle frames), each frame's
 * record_data is appended. The final ETX-terminated frame signals completion.
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

        // ============================================================
        // FIX (Bug #20): Properly parse the ASTM frame to extract
        // only the record_data, stripping frame number, checksum,
        // and ETX/ETB terminator.
        // ============================================================

        // Step 1: Strip frame number (first char, should be a digit 1-7)
        if (Character.isDigit(frame.charAt(0))) {
            frame = frame.substring(1);
        }

        // Step 2: Strip leading <CR> (0x0D) if present
        // After removing the frame number, the next char should be <CR>
        // which separates the frame number from the record data.
        while (!frame.isEmpty() && (frame.charAt(0) == '\r' || frame.charAt(0) == '\n')) {
            frame = frame.substring(1);
        }

        // Step 3: Strip trailing <ETX> (0x03) or <ETB> (0x17) terminator
        // and the 2-character checksum before it.
        //
        // The end of the frame looks like: ...record_data<CR>XX<ETX>
        // where XX is the 2-char hex checksum and <ETX> is the terminator.
        //
        // We need to find the LAST <CR> in the frame — everything after
        // it is "checksum + terminator" which must be stripped.
        if (!frame.isEmpty()) {
            // Strip trailing ETX/ETB and any trailing CR/LF
            // Work from the end of the string
            int end = frame.length();
            while (end > 0) {
                char c = frame.charAt(end - 1);
                if (c == 0x03 || c == 0x17 || c == '\r' || c == '\n') {
                    end--;
                } else {
                    break;
                }
            }

            // Now find the LAST <CR> in the remaining content — this <CR>
            // separates the record_data from the checksum.
            // The checksum is exactly 2 hex characters, so we look for
            // the pattern: <CR> XX  where XX is 2 hex chars at the end.
            if (end > 2) {
                // Check if the last 2 chars before the terminator are hex digits
                // (the checksum). If so, strip them AND the <CR> before them.
                int checkStart = end - 2;
                if (checkStart > 0 && frame.charAt(checkStart - 1) == '\r'
                        && isHexChar(frame.charAt(checkStart))
                        && isHexChar(frame.charAt(checkStart + 1))) {
                    // Found: ...record_data<CR>XX<terminator>
                    // Strip the <CR>XX (checksum)
                    end = checkStart - 1;
                }
            }

            frame = frame.substring(0, end);
        }

        // Step 4: Strip any trailing <CR> that might remain after checksum removal
        while (frame.endsWith("\r") || frame.endsWith("\n")) {
            frame = frame.substring(0, frame.length() - 1);
        }

        // Only add non-empty frames
        if (!frame.isEmpty()) {
            this.frames.add(frame);
        }
    }

    /**
     * Check if a character is a valid hex digit (0-9, A-F, a-f).
     */
    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
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
