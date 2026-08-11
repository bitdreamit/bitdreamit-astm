package com.bitdreamit.astm.asyncastm.service.connection.file;

import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles received ASTM frames into a complete message.
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
        // Remove sequence number (first char) if present
        if (frame.length() > 0 && Character.isDigit(frame.charAt(0))) {
            frame = frame.substring(1);
        }
        this.frames.add(frame);
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
