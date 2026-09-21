package com.bitdreamit.astm.asyncastm.service.connection;

/**
 * ASTM protocol variants understood by the async driver.
 *
 * BIDIRECTIONAL FIX (A1): added explicit generic-ASTM protocol values so the
 * outbound framing (frame number 1..7,0 + ETX/ETB + Add-Mod-256 checksum)
 * can be selected per analyzer family:
 *
 *   ELECSYS           legacy mapping (Roche Elecsys); SEND direction now uses
 *                     the standards-correct E1394MessageIterator, one record
 *                     per frame (D-10 / Pentra 400 host style). RECEIVE path
 *                     unchanged.
 *   COBAS             legacy mapping (Roche Cobas / Maccura i-800 style);
 *                     SEND direction now uses E1394MessageIterator with
 *                     records PACKED into frames, matching the i-800
 *                     TSDWN^REAL example. RECEIVE path unchanged.
 *   ASTM_E1394        explicit generic ASTM E1394, one record per frame.
 *                     Use for Bio-Rad D-10, Horiba Pentra 400, generic LIS.
 *   ASTM_E1394_PACKED explicit generic ASTM E1394 with packed multi-record
 *                     frames. Use for Erba Lachema XL (1024-char frames) and
 *                     any analyzer whose host manual shows packed frames.
 */
public enum Protocol {
    ELECSYS,
    COBAS,
    ASTM_E1394,
    ASTM_E1394_PACKED;

    public static Protocol[] all() {
        return Protocol.values();
    }

    /**
     * BIDIRECTIONAL FIX (A1): tolerant protocol parsing with analyzer-family
     * aliases. Unknown or blank values fall back to ELECSYS (the historical
     * default) so existing channel configurations keep loading.
     *
     * Recognized aliases (case-insensitive, spaces/underscores/dashes ignored):
     *   E1394, ASTM, GENERIC, D10, BIORADD10, BIO-RAD D-10, PENTRA, PENTRA400,
     *   PENTRA C400        -> ASTM_E1394 (one record per frame)
     *   E1394PACKED, ERBA, ERBAXL, XL, ERBA LACHEMA -> ASTM_E1394_PACKED
     */
    public static Protocol parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ELECSYS;
        }
        String normalized = value.trim().toUpperCase()
                .replaceAll("[\\s_\\-]", "");
        // exact enum names first
        for (Protocol p : values()) {
            if (p.name().replaceAll("[\\s_\\-]", "").equals(normalized)) {
                return p;
            }
        }
        if (normalized.equals("E1394") || normalized.equals("ASTM")
                || normalized.equals("GENERIC") || normalized.equals("D10")
                || normalized.equals("BIORADD10") || normalized.equals("PENTRA")
                || normalized.equals("PENTRA400") || normalized.equals("PENTRAC400")
                || normalized.equals("HORIBA")) {
            return ASTM_E1394;
        }
        if (normalized.equals("E1394PACKED") || normalized.equals("ERBA")
                || normalized.equals("ERBAXL") || normalized.equals("ERALICHEMA")
                || normalized.equals("ERBALACHEMA") || normalized.equals("XL")) {
            return ASTM_E1394_PACKED;
        }
        return ELECSYS;
    }
}
