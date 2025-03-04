package org.aksw.commons.util.ref;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import org.aksw.commons.util.stack_trace.StackTraceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a {@link Ref}
 *
 * TODO Avoid needless synchronization; a ConcurrentHashMap may help;
 *   ref.equals could do only reference comparison
 *
 * @author raven
 *
 * @param <T>
 */
public class RefImpl<T>
    implements Ref<T>
{
    private static final Logger logger = LoggerFactory.getLogger(RefImpl.class);

    protected boolean traceAcquisitions = true;


    // private static final Logger logger = LoggerFactory.getLogger(ReferenceImpl.class);

    protected T value;

    /**
     * The release action is run once immediately when the isAlive() state changes to false.
     * The release action cannot 'revive' a reference as the reference is already 'dead'.
     *
     * The release action differs depending on how a reference was created:
     * On the root reference, the releaseAction releases the wrapped resource
     * On a child reference, the releaseAction releases itself (the child) from the parent one.
     *
     */
    protected AutoCloseable releaseAction;

    // TODO Would it be worthwhile to add a pre-release action that is run immediately before
    //      a ref would become dead?
    // protected AutoCloseable preReleaseAction;

    /**
     * Object on which to synchronize on before any change of state of this reference.
     * This allows for e.g. synchronizing on a {@code Map<K, Reference<V>}, such that
     * closing a reference removes the map entry before it can be accessed and conversely,
     * synchronizing on the map prevents the reference from becoming released.
     *
     * TODO Point to the ClaimingLoadingCache implementation
     */
    protected Object synchronizer;

    protected Object comment; // An attribute which can be used for debugging reference chains
    protected RefImpl<T> parent;
    protected volatile boolean isClosed = false;

    protected StackTraceElement[] acquisitionStackTrace;


    protected StackTraceElement[] closeStackTrace;

    protected StackTraceElement[] closeTriggerStackTrace;

    //protected Map<Reference<T>, Object> childRefs = new IdentityHashMap<Reference<T>, Object>();

    // A child ref is active as long as its close() method has not been called
    // The WeakHashMap nature may 'hide' entries whose key is about to be GC'd.
    // This can lead to the situation that childRefs.isEmpty() may true even
    // if there are active child refs (whose close method has not yet been called)

    // TODO The map is only for debugging / reporting - remove?
    protected Map<Ref<T>, Object> childRefs = new WeakHashMap<Ref<T>, Object>();


    protected volatile int activeChildRefs = 0; // new AtomicInteger(0);

    public RefImpl(
            RefImpl<T> parent,
            T value,
            Object synchronizer,
            AutoCloseable releaseAction,
            Object comment) {
        super();

        // logger.debug("Acquired reference " + comment + " from " + parent);

        this.parent = parent;
        this.value = value;
        this.releaseAction = releaseAction;
        this.synchronizer = synchronizer == null ? this : synchronizer;
        this.comment = comment;

//        if (Objects.toString(synchronizer).contains("Ref")) {
//        	System.out.println("DEBUG POINT");
//        }

        if (traceAcquisitions) {
            acquisitionStackTrace = StackTraceUtils.getStackTraceIfEnabled();
        }
    }

    /**
     * TODO Switch to Java 9 Cleaner once we upgrade
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            if (!isClosed) {
                synchronized (synchronizer) {
                    if (!isClosed) {
                        String msg = "Ref released by GC rather than user logic - indicates resource leak."
                                + "Acquired at " + StackTraceUtils.toString(acquisitionStackTrace);
                        logger.warn(msg);

                        close();
                    }
                }
            }
        } finally {
            super.finalize();
        }
    }


    public Object getComment() {
        return comment;
    }

    @Override
    public Object getSynchronizer() {
        return synchronizer;
    }

    @Override
    public T get() {
        if (isClosed) {

            String msg = "Cannot get value of a closed reference:\n"
                    + "Acquired at " + StackTraceUtils.toString(acquisitionStackTrace) + "\n"
                    + "Closed at " + StackTraceUtils.toString(closeStackTrace) + "\n"
                    + "Close Triggered at " + StackTraceUtils.toString(closeTriggerStackTrace);
            logger.warn(msg);

            throw new RuntimeException("Cannot get value of a closed reference");
        }

        return value;
    }

    @Override
    public Ref<T> acquire(Object comment) {
        synchronized (synchronizer) {
            if (!isAlive()) {
                String msg = "Cannot acquire from a reference with status 'isAlive=false'"
                        + "\nClose triggered at: " + StackTraceUtils.toString(closeTriggerStackTrace);
                throw new RuntimeException(msg);
            }

            // A bit of ugliness to allow the reference to release itself
            @SuppressWarnings("rawtypes")
            Ref[] tmp = new Ref[1];
            tmp[0] = new RefImpl<T>(this, value, synchronizer, () -> release(tmp[0]), comment);

            @SuppressWarnings("unchecked")
            Ref<T> result = (Ref<T>)tmp[0];
            childRefs.put(result, comment);
            ++activeChildRefs;
            //activeChildRefs.incrementAndGet();
            return result;
        }
    }

//	void release(Reference<T> childRef) {
    protected void release(Object childRef) {
//        synchronized (synchronizer) {
        boolean isContained = childRefs.containsKey(childRef);
        if (!isContained) {
            throw new RuntimeException("An unknown reference requested to release itself. Should not happen");
        } else {
            childRefs.remove(childRef);
            // activeChildRefs.decrementAndGet();
            --activeChildRefs;
        }

        checkRelease();
//        }
    }

    @Override
    public boolean isAlive() {
        boolean result;
//        synchronized (synchronizer) {
//            result = !isReleased || !childRefs.isEmpty();

        result = !isClosed || activeChildRefs != 0;

//        }
        return result;
    }

    @Override
    public void close() {
        synchronized (synchronizer) {

//        	System.out.println("Closing with synchronizer: " + synchronizer);

            if (isClosed) {
                String msg = "Reference was already closed." +
                        "\nReleased at: " + StackTraceUtils.toString(closeStackTrace) +
                        "\nAcquired at: " + StackTraceUtils.toString(acquisitionStackTrace) +
                        "\nTriggered at " + StackTraceUtils.toString(closeTriggerStackTrace);

                throw new RuntimeException(msg);
            }

            if (traceAcquisitions) {
                closeStackTrace = StackTraceUtils.getStackTraceIfEnabled();
            }

            // logger.debug("Released reference " + comment + " to " + parent);

            isClosed = true;

            checkRelease();
        }
    }

    protected void checkRelease() {

        if (!isAlive()) {
            if (traceAcquisitions) {
                closeTriggerStackTrace = StackTraceUtils.getStackTraceIfEnabled();
            }

            if (releaseAction != null) {
                try {
                    releaseAction.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
//			if(parent != null) {
//				parent.release(this);
//			}
        }
    }

    public static <T extends AutoCloseable> Ref<T> fromCloseable(T value, Object synchronizer) {
        return create(value, synchronizer, value);
    }

//    public static <T> Ref<T> create(T value, AutoCloseable releaseAction, Object comment) {
//        // return new ReferenceImpl<T>(null, value, releaseAction, comment);
//        return create(value, null, releaseAction, comment);
//    }

    /** Create method where the close action is created from a provided lambda that accepts the value */
    public static <T> Ref<T> create2(T value, Object synchronizer, Consumer<? super T> closer) {
        // return new ReferenceImpl<T>(null, value, releaseAction, comment);
        return create(value, synchronizer, () -> closer.accept(value), null);
    }

    public static <T> Ref<T> create(T value, Object synchronizer, AutoCloseable releaseAction) {
        return create(value, synchronizer, releaseAction, null);
    }

    public static <T> Ref<T> create(T value, Object synchronizer, AutoCloseable releaseAction, Object comment) {
        return new RefImpl<T>(null, value, synchronizer, releaseAction, comment);
    }


    public static <T> Ref<T> createClosed() {
        RefImpl<T> result = new RefImpl<T>(null, null, null, null, null);
        result.isClosed = true;
        return result;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @SuppressWarnings("resource")
    @Override
    public Ref<T> getRootRef() {
        RefImpl<T> result = this;
        while (result.parent != null) {
            result = result.parent;
        }
        return result;
    }

    @Override
    public StackTraceElement[] getAcquisitionStackTrace() {
        return acquisitionStackTrace;
    }

    @Override
    public StackTraceElement[] getCloseStackTrace() {
        return closeStackTrace;
    }

    @Override
    public StackTraceElement[] getCloseTriggerStackTrace() {
        return closeTriggerStackTrace;
    }

    @Override
    public String toString() {
        String result = String.format("Ref %s, active(self, #children)=(%b, %d), aquired at %s",
                comment, !isClosed, activeChildRefs, StackTraceUtils.toString(acquisitionStackTrace));
        return result;
    }
}
