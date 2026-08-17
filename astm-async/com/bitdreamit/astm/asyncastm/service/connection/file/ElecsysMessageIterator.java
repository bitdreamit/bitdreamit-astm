package com.bitdreamit.astm.asyncastm.service.connection.file;

import java.util.ArrayList;
import java.util.List;

/**
 * ELECSYS protocol frame iterator.
 */
public class ElecsysMessageIterator implements MessageIterator {
    private List<String> frames;
    private int index = 0;

    public ElecsysMessageIterator(String message) {
        this.frames = new ArrayList<>();
        // Split message into frames based on ELECSYS rules
        // Each frame is typically a single ASTM record line
        String[] lines = message.split("\r\n|\r|\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                this.frames.add(line);
            }
        }
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
