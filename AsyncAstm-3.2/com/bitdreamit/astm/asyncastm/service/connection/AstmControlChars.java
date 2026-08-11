package com.bitdreamit.astm.asyncastm.service.connection;

/**
 * Human-readable ASCII control character formatter for ASTM debug logging.
 */
public final class AstmControlChars {
    private static final String[] CONTROL_NAMES = new String[]{
        "[NUL]", "[SOH]", "[STX]", "[ETX]", "[EOT]", "[ENQ]", "[ACK]", "[BEL]",
        "[BS]",  "[HT]",  "[LF]",  "[VT]",  "[FF]",  "[CR]",  "[SO]",  "[SI]",
        "[DLE]", "[DC1]", "[DC2]", "[DC3]", "[DC4]", "[NAK]", "[SYN]", "[ETB]",
        "[CAN]", "[EM]",  "[SUB]", "[ESC]", "[FS]",  "[GS]",  "[RS]",  "[US]"
    };

    public static String name(int b) {
        if (b >= 0 && b < CONTROL_NAMES.length) return CONTROL_NAMES[b];
        if (b > 126) return "[" + b + "]";
        return Character.toString((char) b);
    }

    public static String format(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) sb.append(name(c));
        return sb.toString();
    }

    /* Backward-compatible aliases for internal migration */
    static String a(int b) { return name(b); }
    static String a(String s) { return format(s); }
}
