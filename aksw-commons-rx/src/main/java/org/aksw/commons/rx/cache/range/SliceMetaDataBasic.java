package org.aksw.commons.rx.cache.range;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.RangeSet;

public interface SliceMetaDataBasic {
    RangeSet<Long> getLoadedRanges();
    RangeMap<Long, List<Throwable>> getFailedRanges();
	
    long getMinimumKnownSize();
    void setMinimumKnownSize(long size);

    long getMaximumKnownSize();
    void setMaximumKnownSize(long size);

    
    /** Updates the maximum known size iff the argument is less than the current known maximum */
    default SliceMetaDataBasic updateMaximumKnownSize(long size) {
        long current = getMaximumKnownSize();

        if (size < current) {
            setMaximumKnownSize(size);
        }

        return this;
    }

    /** Updates the minimum known size iff the argument is greater than the current known minimum */
    default SliceMetaDataBasic updateMinimumKnownSize(long size) {
        long current = getMinimumKnownSize();

        if (size > current) {
            setMinimumKnownSize(size);
        }

        return this;
    }
    

    default long getKnownSize() {
        long minSize = getMinimumKnownSize();
        long maxSize = getMaximumKnownSize();

        return minSize == maxSize ? minSize : -1;
    }

    default SliceMetaDataBasic setKnownSize(long size) {
        Preconditions.checkArgument(size >= 0, "Negative known size");

        setMinimumKnownSize(size);
        setMaximumKnownSize(size);

        return this;
    }

    RangeSet<Long> getGaps(Range<Long> requestRange);
}
