package org.aksw.commons.rx.io;

import org.aksw.commons.io.buffer.array.ArrayOps;
import org.aksw.commons.io.input.DataStream;
import org.aksw.commons.io.input.DataStreamOverStream;
import org.aksw.commons.io.input.DataStreamSource;
import org.aksw.commons.rx.lookup.ListPaginator;

import com.google.common.collect.Range;

public class SequentialReaderSourceRx<T>
    implements DataStreamSource<T[]>
{
    protected ArrayOps<T[]> arrayOps;
    protected ListPaginator<T> listPaginator;

    public SequentialReaderSourceRx(ArrayOps<T[]> arrayOps, ListPaginator<T> listPaginator) {
        super();
        this.arrayOps = arrayOps;
        this.listPaginator = listPaginator;
    }

    @Override
    public ArrayOps<T[]> getArrayOps() {
        return arrayOps;
    }

    public static <T> DataStreamSource<T[]> create(ArrayOps<T[]> arrayOps, ListPaginator<T> listPaginator) {
        return new SequentialReaderSourceRx<>(arrayOps, listPaginator);
    }

    @Override
    public DataStream<T[]> newDataStream(Range<Long> range) {
        return new DataStreamOverStream<T>(arrayOps, listPaginator.apply(range).blockingStream());
    }
}
