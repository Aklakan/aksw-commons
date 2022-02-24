package org.aksw.commons.rx.cache.range;

import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.aksw.commons.util.slot.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;


public class AdvancedRangeCacheNew<T>
	implements SequentialReaderSource<T>
//     implements ListPaginator<T>
{
    private static final Logger logger = LoggerFactory.getLogger(AdvancedRangeCacheNew.class);

    protected SequentialReaderSource<T> dataSource;
    protected SliceWithPages<T> slice;

    protected Set<RangeRequestIterator<T>> activeRequests = Collections.synchronizedSet(Sets.newIdentityHashSet());
    
    protected ReentrantLock workerCreationLock = new ReentrantLock();

    protected Set<RangeRequestWorkerNew<T>> executors = Collections.synchronizedSet(Sets.newIdentityHashSet());
    
    protected long requestLimit;
    protected Duration terminationDelay;


    protected ExecutorService executorService =
            MoreExecutors.getExitingExecutorService((ThreadPoolExecutor)Executors.newCachedThreadPool());

    public AdvancedRangeCacheNew(
    		SequentialReaderSource<T> dataSource,
    		SliceWithPages<T> slice,
            Duration syncDelayDuration,
            long requestLimit,
            Duration terminationDelay) {

    	this.dataSource = dataSource;

        this.slice = slice;
        this.requestLimit = requestLimit;
        this.terminationDelay = terminationDelay;
    }


    public static <A> AdvancedRangeCacheNew<A> create(
    		SequentialReaderSource<A> dataSource,
    		SliceWithPages<A> slice,
            Duration syncDelayDuration,
            long requestLimit,
            Duration terminationDelay) {

    	return new AdvancedRangeCacheNew<>(dataSource, slice, syncDelayDuration, requestLimit, terminationDelay);
    }

    
    public SequentialReaderSource<T> getDataSource() {
		return dataSource;
	}
    
    public SliceWithPages<T> getSlice() {
        return slice;
    }

    public Set<RangeRequestWorkerNew<T>> getExecutors() {
        return executors;
    }

    public Lock getExecutorCreationReadLock() {
    	return workerCreationLock;
    }

    public Runnable register(RangeRequestIterator<T> it) {
        activeRequests.add(it);

        return () -> {
            activeRequests.remove(it);
        };
    }


    /**
     * Creates a new worker and immediately starts it.
     * The executor creation is driven by the RangeRequestIterator which creates executors on demand
     * whenever it detects any gaps in its read ahead range which are not served by any existing executors.
     * 
     * @param offset
     * @param initialLength
     * @return
     */
    public Entry<RangeRequestWorkerNew<T>, Slot<Long>> newExecutor(long offset, long initialLength) {
        RangeRequestWorkerNew<T> worker;
        Slot<Long> slot;
        //executorCreationLock.writeLock().lock();
        try {
            worker = new RangeRequestWorkerNew<>(this, offset, requestLimit, terminationDelay);
            slot = worker.newDemandSlot();
            slot.set(offset + initialLength);

            executors.add(worker);
            logger.debug(String.format("New worker created with initial schedule of offset %1$d and length %2$d", offset, initialLength));
            executorService.submit(worker);
        } finally {
            // executorCreationLock.writeLock().unlock();
        }
        return new SimpleEntry<>(worker, slot);
    }

    
    // Should only be called by the RangeRequestWorker once it terminates
    void removeExecutor(RangeRequestWorkerNew<T> worker) {
    	this.executors.remove(worker);
    }

    /**
     * Create a RequestContext for the given requestRange:
     *
     * (1) Claim cached pages for the start-range of the request range
     * (2) Check the running executors for whether they are suitable for (partially) servinge the request range
     *     If so, assign tasks to those executors
     * (3) If no running executor is suitable then add the request to the 'pending queue'
     *
     * If the executor service
     *
     *
     * @param requestRange
     */
    @Override
    public SequentialReader<T> newInputStream(Range<Long> range) {
    	SequentialReaderFromSliceImpl<T> result = new SequentialReaderFromSliceImpl<>(this, range);
        // RangeRequestIterator<T> result = new RangeRequestIterator<>(this, requestRange);

        return result;
    }

    
    public static class Builder<A> {
        protected SequentialReaderSource<A> dataSource;
        protected SliceWithPages<A> slice;

        protected long requestLimit;
        protected Duration syncDelay;
        protected Duration terminationDelay;
        
        public static <A> Builder<A> create() {
        	return new Builder<A>();
        }
        
		public SequentialReaderSource<A> getDataSource() {
			return dataSource;
		}
		
		public Builder<A> setDataSource(SequentialReaderSource<A> dataSource) {
			this.dataSource = dataSource;
			return this;
		}
		
		public SliceWithPages<A> getSlice() {
			return slice;
		}
		
		public Builder<A> setSlice(SliceWithPages<A> slice) {
			this.slice = slice;
			return this;
		}
		
		public long getRequestLimit() {
			return requestLimit;
		}
		
		public Builder<A> setRequestLimit(long requestLimit) {
			this.requestLimit = requestLimit;
			return this;
		}
		
		public Duration getTerminationDelay() {
			return terminationDelay;
		}
		
		public Builder<A> setTerminationDelay(Duration terminationDelay) {
			this.terminationDelay = terminationDelay;
			return this;
		}
        
		public Duration getSyncDelay() {
			return syncDelay;
		}

		public Builder<A> setSyncDelay(Duration syncDelay) {
			this.syncDelay = syncDelay;
			return this;
		}

		public AdvancedRangeCacheNew<A> build() {
			return AdvancedRangeCacheNew.create(dataSource, slice, syncDelay, requestLimit, terminationDelay);
		}
    }
    
}

