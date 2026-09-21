# Bidirectional Support (Host Query / Order Download) — v3.1.0

This release makes the ASTM Listener + ASTM Sender connector pair fully
bidirectional. Verified against four analyzer manuals and byte-compared with
their example frames (Pentra 400 checksums 6A/7C/06/47/14/38/4F/A1/09,
i-800 TSDWN style, Erba XL packed style).

## The five fixes

| # | Defect | Fix |
|---|--------|-----|
| A1 | `sendFrame()` emitted `<STX>text<CR><LF>` — no frame number, no ETX/ETB, no checksum → every analyzer NAKed/dropped host frames | New `E1394MessageIterator` builds wire-exact frames: FN 1..7,0 + CR + ETX/ETB + Add-Mod-256 checksum |
| A2 | Received frames never validated; NAK path was dead code | `FrameBuffer` validates checksum + frame-number sequence → NAK + resend per E1381 |
| A3 | `IdleState` NAKed the instrument's ENQ when an outgoing message was pending | Line yield: ACK + receive the instrument's query, then send the pending order immediately after |
| A4 | Source and destination each created their own connection (port conflict in TCP Server mode; query unanswerable on the instrument's socket) | Shared per-channel driver registry — destination reuses the source's driver: ONE socket, both directions |
| A5 | `useChecksum` / `maxFrameSize` channel settings never reached the driver | Propagated via `setFrameConfig()` into outbound framing + receive validation |

## Protocol settings per analyzer

| Analyzer | astmProtocol value | Max Frame Size | Framing produced |
|---|---|---|---|
| Bio-Rad D-10 | `ELECSYS`, `ASTM_E1394`, `D10` | 240 | one record per frame |
| Pentra 400 | `ELECSYS`, `ASTM_E1394`, `PENTRA` | 240 | one record per frame |
| i-800 | `COBAS`, `ASTM_E1394_PACKED`, `I800` | 240 | packed multi-record frames |
| Erba XL | `ERBAXL`, `ASTM_E1394_PACKED` | **1024** | packed multi-record frames |

`Protocol.parse()` is alias-tolerant and falls back to the historical default
(`ELECSYS`) for unknown values, so existing channel configurations keep
loading unchanged.

## Bidirectional channel recipe

1. **Source**: ASTM Listener, TCP Server (analyzers connect in).
2. **Destination**: ASTM Sender on the SAME channel — it automatically reuses
   the source's connection (see log line "AstmDispatcher REUSING the channel's
   shared ASTM connection").
3. Transformer flow:
   - Message contains `R|` records → parse results, upsert into LIS DB.
   - Message contains `Q|` record (Host Query) → look up the order for the
     barcode (`status='NEW'`), reply with `H|` + `P|` + `O|` (+ `C|`) + `L|1|N`
     through the ASTM Sender destination, mark the order `ACCEPTED`.
4. Manual re-send: inject the same order payload through any trigger (HTTP
   listener, channel reprocessing) — the sender state machine performs the
   ENQ → frames → EOT turn on the shared connection.

## Files touched

- `astm-async/.../connection/file/E1394MessageIterator.java` (NEW)
- `astm-async/.../connection/file/FrameBuffer.java` (validation)
- `astm-async/.../service/states/IdleState.java` (line yield)
- `astm-async/.../service/states/TransferReceiverState.java` (checksum flag)
- `astm-async/.../service/states/bundle/AstmContext.java` (frame config + iterator routing)
- `astm-async/.../service/connection/Protocol.java` (new values + aliases)
- `astm-async/.../AsyncAstmTcpDriver.java`, `AsyncAstmSerialDriver.java` (frame config)
- `astm-server/.../AstmService.java` (shared registry + config propagation)
- `astm-server/.../AstmReceiver.java` (register/unregister shared driver)
- `astm-server/.../AstmDispatcher.java` (reuse shared driver)
