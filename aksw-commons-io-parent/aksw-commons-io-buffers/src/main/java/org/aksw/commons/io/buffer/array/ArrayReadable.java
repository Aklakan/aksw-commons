package org.aksw.commons.io.buffer.array;

import java.io.IOException;

import org.aksw.commons.io.input.ReadableChannel;
import org.aksw.commons.io.input.ReadableChannels;

public interface ArrayReadable<A>
    extends HasArrayOps<A>
{
    int readInto(A tgt, int tgtOffset, long srcOffset, int length) throws IOException;

    default ReadableChannel<A> newReadChannel() {
        return newReadChannel(0l);
    }

    default ReadableChannel<A> newReadChannel(long offset) {
        return ReadableChannels.newChannel(this, offset);
    }

    @SuppressWarnings("unchecked")
    default int readIntoRaw(Object tgt, int tgtOffset, long srcOffset, int length) throws IOException {
        return readInto((A)tgt, tgtOffset, srcOffset, length);
    }

    default Object get(long index) throws IOException {
        ArrayOps<A> arrayOps = getArrayOps();
        A singleton = arrayOps.create(1);
        readInto(singleton, 0, index, 1);
        Object result = arrayOps.get(singleton, 0);
        return result;
    }

    // T get(long index);
    // Iterator<T> iterator(long offset);
}
