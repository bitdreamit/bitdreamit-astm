package com.bitdreamit.astm.asyncastm.service.connection.file;

import java.util.ArrayList;
import java.util.List;

/**
 * BIDIRECTIONAL FIX (A1) - standards-correct ASTM E1381/E1394 outbound frame
 * builder.
 *
 * The legacy CobasMessageIterator / ElecsysMessageIterator split the message
 * into plain record lines and AbstractAstmConnection.sendFrame() wrapped them
 * as <STX>text<CR><LF> - with NO frame number, NO ETX/ETB and NO checksum.
 * Every ASTM analyzer (Bio-Rad D-10, Horiba Pentra 400, Maccura i-800,
 * Erba Lachema XL, Roche, ...) validates the frame number and the Add-Mod-256
 * checksum and therefore NAKed or silently dropped EVERY host-sent frame,
 * which made bidirectional order download impossible.
 *
 * This iterator produces fully framed, wire-exact frames. Each element
 * returned by nextFrame() is the COMPLETE frame content WITHOUT the leading
 * STX and WITHOUT the trailing CR LF (AbstractAstmConnection.sendFrame()
 * adds both), i.e.:
 *
 *     FN  <content>  <ETX|ETB>  C1 C2
 *
 * so the emitted wire frame is
 *
 *     <STX> FN <content> <ETX|ETB> C1 C2 <CR><LF>
 *
 * which is byte-for-byte the format in:
 *   - Bio-Rad D-10 LIS Interface Requirements L20017702 (sections 4.5/4.7),
 *   - Horiba Pentra 400 RAA023JEN section 6.2 host answer turn,
 *   - Maccura i-800 LIS Protocol V1.0.00.221210 section 3.6.2 TSDWN^REAL,
 *   - Erba Lachema XL ASTM Host Interface Document v2.0.
 *
 * Rules implemented (all four manuals agree):
 *   - Frame number FN is a single ASCII digit cycling 1,2,...,7,0,1,...
 *     across the whole transfer phase, starting at 1.
 *   - Record content ends with CR before ETX ("H|\^&...\r\x03").
 *     Packed mode additionally separates records by CR INSIDE one frame
 *     (Erba XL: "<STX>1H|...<CR>P|1|...<CR>L|1|N<CR><ETX>6F<CR><LF>",
 *     i-800 TSDWN example: same style within its 240-char budget).
 *   - An over-long record is chunked: intermediate chunks are ETB-terminated
 *     (no CR), the final chunk carries the CR and the ETX.
 *   - Checksum C1C2 = (sum of ASCII values of FN..ETX/ETB inclusive) mod 256,
 *     rendered as TWO UPPERCASE hex digits. STX and the trailing CR LF are
 *     NOT included.
 *   - maxFrameContentLength caps the TEXT area (FN + content), i.e. content
 *     is at most maxFrameContentLength - 1 bytes (240 default -> 247-byte
 *     wire frame incl. STX/ETX/checksum/CRLF; Erba XL uses 1024).
 */
public class E1394MessageIterator implements MessageIterator {

    private final List<String> frames;
    private int index = 0;

    /**
     * @param message               the complete ASTM message, records separated
     *                              by CR, LF or CRLF (as produced by the
     *                              channel transformer)
     * @param maxFrameContentLength maximum TEXT characters per frame
     *                              (FN + content); 240 = ASTM default,
     *                              1024 = Erba XL
     * @param packed                true = pack consecutive records into one
     *                              frame up to the length limit (Erba XL /
     *                              i-800 TSDWN style); false = one record per
     *                              frame (D-10 / Pentra 400 host style)
     */
    public E1394MessageIterator(String message, int maxFrameContentLength, boolean packed) {
        int maxContent = Math.max(8, maxFrameContentLength) - 1; // -1 room for FN
        this.frames = new ArrayList<>();

        List<String> records = splitRecords(message);
        if (records.isEmpty()) {
            return; // nothing to send; TransferSenderState will emit EOT directly
        }

        int frameNumber = 1; // ASTM: transfer phase starts at FN=1, cycles 1..7,0
        if (packed) {
            frameNumber = buildPackedFrames(records, maxContent, frameNumber);
        } else {
            frameNumber = buildRecordFrames(records, maxContent, frameNumber);
        }
    }

    /** One record per frame (D-10 4.7 / Pentra 6.2 / i-800 host examples). */
    private int buildRecordFrames(List<String> records, int maxContent, int frameNumber) {
        for (String record : records) {
            String withCr = record + "\r";
            if (withCr.length() <= maxContent) {
                frames.add(buildFrame(frameNumber, withCr, true));
                frameNumber = (frameNumber + 1) % 8;
            } else {
                frameNumber = appendChunkedRecord(record, maxContent, frameNumber);
            }
        }
        return frameNumber;
    }

    /** Records packed into frames up to the limit (Erba XL style). */
    private int buildPackedFrames(List<String> records, int maxContent, int frameNumber) {
        StringBuilder current = new StringBuilder();
        for (String record : records) {
            // records already carry their trailing CR, which IS the record
            // separator inside a packed frame - no extra separator, otherwise
            // a double CR would be emitted and analyzers would reject the frame
            String withCr = record + "\r";
            if (withCr.length() > maxContent) {
                // flush pending packed frame, then chunk the over-long record
                if (current.length() > 0) {
                    frames.add(buildFrame(frameNumber, current.toString(), true));
                    frameNumber = (frameNumber + 1) % 8;
                    current.setLength(0);
                }
                frameNumber = appendChunkedRecord(record, maxContent, frameNumber);
                continue;
            }
            if (current.length() + withCr.length() > maxContent) {
                frames.add(buildFrame(frameNumber, current.toString(), true));
                frameNumber = (frameNumber + 1) % 8;
                current.setLength(0);
            }
            current.append(withCr);
        }
        if (current.length() > 0) {
            frames.add(buildFrame(frameNumber, current.toString(), true));
            frameNumber = (frameNumber + 1) % 8;
        }
        return frameNumber;
    }

    /**
     * Over-long single record: intermediate ETB chunks (no CR), final chunk
     * carries the CR and the ETX (ASTM E1381 continuation rules).
     */
    private int appendChunkedRecord(String record, int maxContent, int frameNumber) {
        int offset = 0;
        int length = record.length();
        while (offset < length) {
            boolean lastChunk = (length - offset) <= (maxContent - 1); // reserve CR
            int take = lastChunk ? (length - offset) : (maxContent - 1);
            String chunk = record.substring(offset, offset + take);
            offset += take;
            if (lastChunk) {
                frames.add(buildFrame(frameNumber, chunk + "\r", true));
            } else {
                frames.add(buildFrame(frameNumber, chunk, false)); // ETB, no CR
            }
            frameNumber = (frameNumber + 1) % 8;
        }
        return frameNumber;
    }

    /**
     * Builds one frame: FN + content + ETX/ETB + 2 checksum chars.
     * (sendFrame() adds STX and the trailing CR LF.)
     */
    static String buildFrame(int frameNumber, String content, boolean finalFrame) {
        char fn = (char) ('0' + frameNumber);
        char end = finalFrame ? (char) 0x03 : (char) 0x17; // ETX : ETB
        // checksum covers FN through ETX/ETB inclusive (NOT STX, NOT CRLF)
        int sum = fn;
        for (int i = 0; i < content.length(); i++) {
            sum += content.charAt(i) & 0xFF;
        }
        sum += end & 0xFF;
        sum &= 0xFF;
        String checksum = String.format("%02X", sum);
        return fn + content + end + checksum;
    }

    /** Splits the message into records on CR / LF / CRLF, dropping empties. */
    static List<String> splitRecords(String message) {
        List<String> records = new ArrayList<>();
        if (message == null || message.isEmpty()) {
            return records;
        }
        String[] lines = message.split("\r\n|\r|\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                records.add(line);
            }
        }
        return records;
    }

    @Override
    public boolean hasNext() {
        return index < frames.size();
    }

    @Override
    public String nextFrame() {
        return frames.get(index++);
    }
}
