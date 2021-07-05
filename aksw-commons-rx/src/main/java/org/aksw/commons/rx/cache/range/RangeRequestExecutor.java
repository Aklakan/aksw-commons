package org.aksw.commons.rx.cache.range;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.aksw.commons.rx.range.RangedSupplier;
import org.aksw.commons.util.range.RangeBufferImpl;
import org.aksw.commons.util.ref.Ref;
import org.aksw.commons.util.sink.BulkingSink;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Range;
import com.google.common.math.LongMath;
import com.google.common.primitives.Ints;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * A producer task: Takes items from an iterator and writes them them to pages
 *
 * @author raven
 *
 * @param <T>
 */
public class RangeRequestExecutor<T> {
    // After processing a page, an executor can claim another one

    /**
     * Reference to the manager - if there is a failure in processing the request the executor
     * notifies it
     */
    protected SmartRangeCacheImpl<T> manager;



    /** The data supplying iterator */
    protected Iterator<T> iterator;

    /** The disposable of the data supplier */
    protected Disposable disposable;

    /** Whether processing is aborted */
    protected boolean isAborted = false;


    protected ReentrantReadWriteLock pauseLock = new ReentrantReadWriteLock(true);
    protected volatile boolean isPaused = false;

    /** The pages claimed by the executor */
    // protected Set<Ref<Page<T>>> claimedPages;

    /** The page the executor is currently writing to */
    protected Ref<RangeBufferImpl<T>> currentPageRef;

    protected long requestOffset;


    // The endpoints of the requests being served by this executor
    protected Map<Object, Long> contextToEndpoint;

    // The effective endpoint; the maximum value in contextToEndpoint
    protected long effectiveEndpoint;



    /** The requestLimit must take result-set-limit on the backend into account! */
    protected long requestLimit;


    // protected Map<Long, >

    protected RangedSupplier<Long, T> backend;


    protected long numItemsRead;

    /** The executor synchronizes on itself ('this') and advertises the next offset upon which
     *  it re-checks its conditions */
    // protected long nextInterceptableOffset;


    /** Report read items in chunks preferably and at most this size.
     *  Prevents synchronization on every single item. */
    protected int reportingInterval = 10;



    protected long offset;


    protected long currentLimit;


    protected boolean terminateIfNoObserver;

    // Task termination may be delayed in order to allow it to recover should another observer register
    // in the delay phase
    protected long terminationDelay;




    /*
     * Statistics
     */
    /** Time it took to retrieve the first item */
    protected Duration firstItemTime = null;

    /** Throughput in items / second */
    protected long numItemsProcessed = 0;
    protected long processingTimeInNanos = 0;


    protected ReentrantReadWriteLock executorCreationLock = new ReentrantReadWriteLock();


    /** Time in seconds it took to obtain the first item */
    public Duration getFirstItemTime() {
        return firstItemTime;
    }

    /** Throughput measured in items per second */
    public float getThroughput() {
        return numItemsProcessed / (float)(processingTimeInNanos / 1e9);
    }



    /**
     * A call to this method acquires a lock that causes the executor to pause
     * at the next checkpoint. The client must eventually call .unlock() on the returned lock.
     *
     * The method only returns when the executor has reached that checkpoint.
     *
     * Note that the returned lock is a read lock from a ReentrantReadWriteLock.
     * These locks do not support testing for ownership, so the client code must take care
     * of properly releasing any acquired locks.
     *
     * https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6207928
     */
    public CompletableFuture<Runnable> pause() throws InterruptedException {
        // RefImpl.create(null, backend, currentPageRef)
        Lock result = pauseLock.readLock();
        result.lock();
        return CompletableFuture.completedFuture(() -> result.unlock());
    }



    /**
     * Estimated time of arrival at the given index in seconds
     * Index must be greater or equal to offset
     *
     * Call {@link #pause()} before calling the method.
     */
    public float etaAtIndex(long index) {
        long distance = index - offset;

        float throughput = getThroughput();
        float result = distance * throughput;
        return result;
    }

    /**
     * Stops processing of this executor and
     * also disposes the undelying data supplier (which is expected to terminate)
     */
    public void abort() {
        synchronized (this) {
            if (!isAborted && disposable != null) {
                disposable.dispose();
                isAborted = true;
            }
        }
    }


    public void close() {
        // Free claimed resources
        synchronized (this) {
            if (currentPageRef != null) {
                currentPageRef.close();
            }
        }
    }


    protected void init() {

        // Synchronize because abort may be called concurrently
        synchronized (this) {
            if (!isAborted) {
                Flowable<T> backendFlow = backend.apply(Range.atLeast(offset));
                iterator = backendFlow.blockingIterable().iterator();
                disposable = (Disposable)iterator;
            } else {
                return; // Exit immediately due to abort
            }
        }
    }

    public void run() {
        init();

        // Measuring the time for the first item may be meaningless if an underlying cache is used
        // It may be better to measure on e.g. the HTTP level using interceptors on HTTP client
        Stopwatch firstItemTimer = Stopwatch.createStarted();
        iterator.hasNext();
        Duration firstItemTime = firstItemTimer.elapsed();

        Lock writeLock = pauseLock.writeLock();
        writeLock.lock();

        try {

            // pauseLock.writeLock().newCondition();
            while (true) {
                while (pauseLock.hasQueuedThreads()) {
                    isPaused = true;
                    writeLock.unlock();
                    writeLock.lock();
                }
                isPaused = false;

                process(reportingInterval);


                if (iterator.hasNext()) {

                    // Shut down if there is no pending request for further data
                    try {
                        Thread.sleep(terminationDelay);
                    } catch (InterruptedException e) {

                    }

                    if (currentLimit < numItemsRead) {
                        break;
                    }
                } else {
                    break;
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    public long getCurrentOffset() {
        return offset;
    }


    public long getEndOffset() {
        return 	LongMath.saturatedAdd(requestOffset, requestLimit);
    }

    public Range<Long> getWorkingRange() {
        return Range.closedOpen(offset, getEndOffset());
    }


    /**
     *
     */
    public void process(int n) {
        int bulkSize = 16;
        // BulkConsumer<T>

        // Make sure we don't acquire a page while close is invoked
        // FIXME Only acquire a page if it is necessary
        synchronized (this) {
            if (currentPageRef != null) {
                currentPageRef.close();
            }

            currentPageRef = manager.getPageForOffset(offset);
        }


        int offsetInPage = manager.getIndexInPageForOffset(offset);
        RangeBufferImpl<T> rangeBuffer = currentPageRef.get();

        BulkingSink<T> sink = new BulkingSink<>(bulkSize,
                (arr, start, len) -> rangeBuffer.putAll(offsetInPage, arr, start, len));


        // TODO Fix limit computation
        int limit = Math.min(Ints.saturatedCast(currentLimit), rangeBuffer.getCapacity());
        limit = Math.min(limit, reportingInterval);

        int i = 0;
        boolean hasNext;
        while ((hasNext = iterator.hasNext()) && i < limit && !isAborted && !Thread.interrupted()) {
            T item = iterator.next();
            ++i;
            sink.accept(item);
        }
        sink.flush();
        sink.close();

        numItemsRead += i;
        offset += numItemsRead;

        // If there is no further item although the request range has not been covered
        // then we have detected the end

        // Note: We may have also just hit the backend's result-set-limit
        // This is the case if there is
        // (1) a known result-set-limit value on the smart cache
        // (2) a known higher offset in the smart cache or
        // (3) another request with the same offset that yield results
        if (!hasNext && numItemsRead < requestLimit) {
            // manager.setKnownSize(offset);
        }


        // throughputTimer.start()

        // Note: The SmartRangeCache assigns (range-retrieval) tasks to running executors
        //

        // Check if we are still serving any tasks - possible outcomes:
        // (1) no more tasks are being served:
        //     Possibly finish reading a certain chunk, then go into standby
        //     If no further request arrives in time then terminate
        // (2) there are still open tasks
        //     Check whether to trigger read-ahead: This means that this executor is known having to
        //     terminate (e.g. reached the backend-max-result-size limit) but more data is requested
        //


        // (x) there are requests but they cannot be served
        //     (this can only happen if the backend raises an exception)
        // If there is any request served up to the read-ahead trigger point
        // then schedule a new task at the ExecutorManager

        // computeNextCheckpoint()



    }


    protected void updateEffectiveEndpoint() {
        effectiveEndpoint = contextToEndpoint.values().stream()
                .mapToLong(x -> x).reduce(-1l,Math::max);
    }

    /**
     * Add or update the endpoint for the given context object.
     *
     *
     * @param context
     * @param newLimit
     * @return A runnable that unregisters the endpoint for this executor. Unregistering the last endpoint
     *         may pause or cancel the executor.
     */
    public Runnable requestEndpoint(Object context, long endpoint) {
        synchronized (this) {
            Range<Long> workingRange = getWorkingRange();
            if (!workingRange.contains(endpoint)) {
                throw new IllegalArgumentException(String.format("Request for endpoint %d is outside of working range %s", endpoint, workingRange));
            }
            contextToEndpoint.put(context, endpoint);
            updateEffectiveEndpoint();
        }

        return () -> {
            synchronized (this) {
                contextToEndpoint.remove(context);
                updateEffectiveEndpoint();
            }
        };
    }

}

