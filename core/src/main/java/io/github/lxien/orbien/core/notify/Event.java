package io.github.lxien.orbien.core.notify;

import java.io.Serializable;

/**
 * 事件
 */
public abstract class Event implements Serializable {

    private long sequence = -1;
    private boolean endOfBatch = false;
    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public boolean isEndOfBatch() {
        return endOfBatch;
    }

    public void setEndOfBatch(boolean endOfBatch) {
        this.endOfBatch = endOfBatch;
    }

}