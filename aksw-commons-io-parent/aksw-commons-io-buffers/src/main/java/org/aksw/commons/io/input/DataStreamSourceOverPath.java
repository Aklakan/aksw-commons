package org.aksw.commons.io.input;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.aksw.commons.io.buffer.array.ArrayOps;

import com.google.common.base.Preconditions;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;

public class DataStreamSourceOverPath
    implements DataStreamSource<byte[]>
{
    protected Path path;

    public DataStreamSourceOverPath(Path path) {
        super();
        this.path = path;
    }

    @Override
    public ArrayOps<byte[]> getArrayOps() {
        return ArrayOps.BYTE;
    }

    @Override
    public DataStream<byte[]> newDataStream(Range<Long> range) throws IOException {
        DataStream<byte[]> result;

        ContiguousSet<Long> set = ContiguousSet.create(range, DiscreteDomain.longs());
        if (set.isEmpty()) {
            result = DataStreams.empty(ArrayOps.BYTE);
        } else {
            long pos = set.first();
            Preconditions.checkArgument(pos >= 0, "Ranges must start with 0 or greater");

            FileChannel fc = FileChannel.open(path, StandardOpenOption.READ);
            fc.position(pos);
            long len = set.size();
            result = DataStreams.limit(DataStreams.wrap(fc), len);
        }
        return result;
    }
}
