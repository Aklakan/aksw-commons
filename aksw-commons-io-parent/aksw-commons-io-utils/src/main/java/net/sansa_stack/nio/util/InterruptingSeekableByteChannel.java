package net.sansa_stack.nio.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import com.google.common.primitives.Ints;

public class InterruptingSeekableByteChannel
    extends SeekableByteChannelDecoratorBase<SeekableByteChannel>
{
    protected long interruptPos;

    public InterruptingSeekableByteChannel(SeekableByteChannel decoratee, long interruptPos) {
        super(decoratee);
        this.interruptPos = interruptPos;
    }

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        long pos = position();

        int remainingUntilInterrupt = pos < interruptPos
                ? Ints.saturatedCast(interruptPos - pos)
                : Integer.MAX_VALUE;

        int capacity = byteBuffer.remaining();

        int toRead = remainingUntilInterrupt == 0
            ? capacity
            : Math.min(capacity, remainingUntilInterrupt);

        if (toRead != capacity) {
            byteBuffer = byteBuffer.duplicate();
            byteBuffer.limit(byteBuffer.position() + toRead);
        }

        int result = super.read(byteBuffer);
        return result;
    }
}
