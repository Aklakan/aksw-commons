package org.aksw.commons.rx.cache.range;

import java.util.concurrent.locks.ReadWriteLock;

public interface BufferView<A> {
    RangeBuffer<A> getRangeBuffer();
    // ReadWriteLock getReadWriteLock();
    long getGeneration();

    long getCapacity();

    ReadWriteLock getReadWriteLock();
//	protected RangeBuffer rangeBuffer;
//	protected ReadWriteLock readWriteLock;
//	protected long generation;
}
