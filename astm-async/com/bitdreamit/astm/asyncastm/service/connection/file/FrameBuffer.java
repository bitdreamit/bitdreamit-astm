package com.bitdreamit.astm.asyncastm.service.connection.file;

import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles received ASTM frames into a complete message.
 *
 * FIX (Bug #20 — Checksum bytes appearing as separate lines):
 *
 * ASTM frame format on the wire:
 *   <STX> FN <CR> record_data <CR> checksum <ETX> <CR> <LF>
 *
 * readLine() returns everything between <STX> and <CR><LF>:
 *   FN <CR> record_data <CR> checksum <ETX>
 *
 * SIMPLE FIX: Just split by <CR> and take field [1] (the record data).
 * Field [0] = frame number (discard)
 * Field [1] = record data (KEEP)
 * Field [2] = checksum + ETX (discard)
 *
 * This is simpler and more robust than trying to detect hex checksums.
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
        // SIMPLE FIX (Bug #20): Split by <CR> and take only the record.
        // ============================================================
        // Frame format from readLine():
        //   FN<CR>record<CR>checksum<ETX>
        //
        // Split by <CR> (\r):
        //   parts[0] = "FN"           (frame number — discard)
        //   parts[1] = "record"       (the actual ASTM record — KEEP)
        //   parts[2] = "checksum<ETX>" (checksum + terminator — discard)
        //
        // For multi-record frames (Cobas), parts[1] may contain
        // multiple records separated by <CR>. But for D-10 (one
        // record per frame), parts[1] is the single record.
        // ============================================================

        String[] parts = frame.split("\r");

        // parts[1] is the record data — this is what we want
        if (parts.length >= 2 && !parts[1].isEmpty()) {
            String record = parts[1];

            // Strip any trailing ETX/ETB that might have been included
            // (shouldn't happen with split, but just in case)
            while (record.length() > 0) {
                char last = record.charAt(record.length() - 1);
                if (last == 0x03 || last == 0x17) {
                    record = record.substring(0, record.length() - 1);
                } else {
                    break;
                }
            }

            if (!record.isEmpty()) {
                this.frames.add(record);
            }
        } else {
            // Fallback: if split didn't work, use the old method
            // (strip first char = frame number, strip last 4 chars = CR+checksum+ETX)
            String record = frame;
            if (record.length() > 0 && Character.isDigit(record.charAt(0))) {
                record = record.substring(1);
            }
            // Strip leading CR
            while (record.startsWith("\r") || record.startsWith("\n")) {
                record = record.substring(1);
            }
            // Strip trailing CR + 2-char checksum + ETX (4 chars total)
            if (record.length() > 4) {
                record = record.substring(0, record.length() - 4);
            }
            while (record.endsWith("\r") || record.endsWith("\n")) {
                record = record.substring(0, record.length() - 1);
            }
            if (!record.isEmpty()) {
                this.frames.add(record);
            }
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
