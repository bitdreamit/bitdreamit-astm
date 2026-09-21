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
 *
 * BIDIRECTIONAL FIX (A2) — receive-side integrity enforcement:
 * The old appendFrame() never validated anything and never threw, so the
 * NAK branch in TransferReceiverState was dead code and malformed frames
 * were ACKed and stored. appendFrame() now validates, per ASTM E1381:
 *
 *   - CHECKSUM: the two characters after ETX/ETB must equal
 *     (sum of ASCII values from FN through ETX/ETB inclusive) mod 256,
 *     rendered as two hex digits (case-insensitive compare). All four
 *     audited manuals (D-10, Pentra 400, i-800, Erba XL) use this
 *     Add-Mod-256 algorithm.
 *   - FRAME NUMBER SEQUENCE: 1,2,...,7,0,1,... across the transfer phase,
 *     starting at 1 (the FrameBuffer is recreated per transfer by
 *     TransferReceiverState.init(), which naturally resets the cycle).
 *
 * A violation throws IllegalArgumentException, which TransferReceiverState
 * already catches to send NAK and let the analyzer resend — exactly the
 * E1381 receiver behavior. Validation can be disabled (checksumEnabled=false)
 * for analyzers configured without checksums; frame-number order is still
 * checked whenever a digit is present because it costs nothing and every
 * audited analyzer numbers its frames.
 */
public class FrameBuffer {
    private Protocol protocol;
    private List<String> frames;
    private boolean complete;
    private final boolean checksumEnabled;
    private int expectedFrameNumber = 1; // ASTM: receiver numbers frames 1,2,...,7,0,1,...

    public FrameBuffer(Protocol protocol) {
        this(protocol, true);
    }

    public FrameBuffer(Protocol protocol, boolean checksumEnabled) {
        this.protocol = protocol;
        this.checksumEnabled = checksumEnabled;
        this.frames = new ArrayList<>();
        this.complete = false;
    }

    public void appendFrame(String frame) {
        if (frame == null || frame.isEmpty()) {
            return;
        }

        // Step 1: Strip frame number (first char — a digit 0-7)
        String frameNumberStr = null;
        if (frame.length() > 1 && Character.isDigit(frame.charAt(0))) {
            frameNumberStr = frame.substring(0, 1);
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

        // BIDIRECTIONAL FIX (A2): validate frame number sequence + checksum
        // BEFORE accepting the frame. Throws IllegalArgumentException on any
        // violation, which TransferReceiverState maps to NAK + resend.
        validateFrame(frameNumberStr, endIndex, frame);

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

        // advance the receiver cycle 1,2,...,7,0,1,...
        if (frameNumberStr != null) {
            expectedFrameNumber = (expectedFrameNumber + 1) % 8;
        }
    }

    /**
     * BIDIRECTIONAL FIX (A2): frame-number + checksum validation.
     *
     * @param frameNumberStr the FN digit stripped from the frame (or null)
     * @param endIndex       index of ETX/ETB inside the FN-stripped frame
     *                       (-1 when absent)
     * @param fnStripped     the frame WITHOUT the leading FN digit
     */
    private void validateFrame(String frameNumberStr, int endIndex, String fnStripped) {
        // --- frame number sequence ---
        if (frameNumberStr != null) {
            int received = frameNumberStr.charAt(0) - '0';
            if (received < 0 || received > 7) {
                throw new IllegalArgumentException(
                    "Invalid ASTM frame number: " + received + " (must be 0-7)");
            }
            if (received != expectedFrameNumber) {
                throw new IllegalArgumentException(
                    "ASTM frame number mismatch: expected " + expectedFrameNumber
                    + ", received " + received);
            }
        }

        // --- checksum (Add-Mod-256 over FN..ETX/ETB) ---
        if (!checksumEnabled || endIndex < 0 || frameNumberStr == null) {
            return; // checksum disabled or frame malformed without FN: skip
        }
        if (endIndex + 3 > fnStripped.length()) {
            // fewer than 2 chars after ETX/ETB — no checksum present
            return;
        }
        String receivedChecksum = fnStripped.substring(endIndex + 1, endIndex + 3);
        if (receivedChecksum.isEmpty()) {
            return;
        }
        char endChar = fnStripped.charAt(endIndex);
        int sum = frameNumberStr.charAt(0);
        for (int i = 0; i < endIndex; i++) {
            sum += fnStripped.charAt(i) & 0xFF;
        }
        sum += endChar & 0xFF;
        sum &= 0xFF;
        String calculated = String.format("%02X", sum);
        if (!calculated.equalsIgnoreCase(receivedChecksum)) {
            throw new IllegalArgumentException(
                "ASTM checksum mismatch: calculated " + calculated
                + ", received " + receivedChecksum);
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
