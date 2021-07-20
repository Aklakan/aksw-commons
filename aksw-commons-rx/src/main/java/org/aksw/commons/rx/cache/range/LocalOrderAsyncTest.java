package org.aksw.commons.rx.cache.range;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.aksw.commons.rx.op.LocalOrderSpec;
import org.aksw.commons.rx.op.LocalOrderSpecImpl;
import org.aksw.commons.rx.op.OperatorLocalOrder;
import org.aksw.commons.rx.range.KeyObjectStore;
import org.aksw.commons.rx.range.KeyObjectStoreImpl;
import org.aksw.commons.rx.range.ObjectFileStoreKyro;
import org.aksw.commons.util.range.RangeBuffer;
import org.aksw.commons.util.range.RangeBufferImpl;
import org.aksw.commons.util.ref.RefFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.minlog.Log;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeMap;
import com.google.common.collect.TreeRangeSet;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subjects.PublishSubject;

class MyPublisher<T, S extends Comparable<S>> {
    protected PublishSubject<T> publishSubject = PublishSubject.create();
    protected LocalOrderSpec<T, S> orderSpec;


    public MyPublisher(LocalOrderSpec<T, S> orderSpec) {
        super();
        this.orderSpec = orderSpec;
    }

    public static <T, S extends Comparable<S>> MyPublisher<T, S> create(LocalOrderSpec<T, S> orderSpec) {
        return new MyPublisher<T, S>(orderSpec);
    }

    public void offer(T item) {
        publishSubject.onNext(item);
    }

    public Flowable<T> createFlow(S start) {
        return publishSubject
            .toFlowable(BackpressureStrategy.ERROR)
            .filter(item -> orderSpec.getDistanceFn().apply(start, orderSpec.getExtractSeqId().apply(item)).longValue() > 0)
            .lift(OperatorLocalOrder.<T, S>create(start, orderSpec));
    }
}


class RangeSetSerializer
    extends Serializer<RangeSet>
{
    public static <T extends Comparable<T>> Set<Range<T>> toSet(RangeSet<T> rangeSet) {
        return new HashSet<Range<T>>(rangeSet.asRanges());
    }

    public static <T extends Comparable<T>> RangeSet<T> fromSet(Set<Range<T>> set) {
        TreeRangeSet<T> result = TreeRangeSet.create();
        result.addAll(set);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(Kryo kryo, Output output, RangeSet object) {
        kryo.writeClassAndObject(output, toSet(object));
    }

    @SuppressWarnings("unchecked")
    @Override
    public RangeSet read(Kryo kryo, Input input, Class<RangeSet> type) {
        Set set = (Set)kryo.readClassAndObject(input);
        RangeSet result = fromSet(set);
        return result;
    }
}
class RangeMapSerializer
    extends Serializer<RangeMap>
{
    public static <K extends Comparable<K>, V> Map<Range<K>, V> toMap(RangeMap<K, V> rangeMap) {
        return new HashMap<Range<K>, V>(rangeMap.asMapOfRanges());
    }

    public static <K extends Comparable<K>, V> RangeMap<K, V> fromMap(Map<Range<K>, V> map) {
        TreeRangeMap<K, V> result = TreeRangeMap.create();
        map.entrySet().forEach(e -> {
            result.put(e.getKey(), e.getValue());
        });
        return result;
    }


    @SuppressWarnings("unchecked")
    @Override
    public void write(Kryo kryo, Output output, RangeMap object) {
        kryo.writeClassAndObject(output, toMap(object));
    }

    @SuppressWarnings("unchecked")
    @Override
    public RangeMap read(Kryo kryo, Input input, Class<RangeMap> type) {
        Map map = (Map)kryo.readClassAndObject(input);
        RangeMap result = fromMap(map);
        return result;
    }
}

public class LocalOrderAsyncTest {
    public static void main(String[] args) throws Exception {
        main1();
    }

    private static final Logger logger = LoggerFactory.getLogger(LocalOrderAsyncTest.class);

    public static <V> AsyncClaimingCache<Long, RangeBuffer<V>> syncedRangeBuffer(KeyObjectStore store, Supplier<RangeBuffer<V>> newValue) {

        AsyncClaimingCache<Long, RangeBuffer<V>> result = AsyncClaimingCache.create(
                AsyncRefCache.<Long, RangeBuffer<V>>create(
                   Caffeine.newBuilder()
                   .scheduler(Scheduler.systemScheduler())
                   .maximumSize(3000).expireAfterWrite(1, TimeUnit.SECONDS),
                   key -> {
                       List<String> internalKey = Arrays.asList(Long.toString(key));
                       RangeBuffer<V> value;
                       try {
                           value = store.get(internalKey);
                       } catch (Exception e) {
                           // throw new RuntimeException(e);
                           value = newValue.get(); //new RangeBufferImpl<V>(1024);
                       }

                       RangeBuffer<V> r = value;
    //                   Ref<V> r = RefImpl.create(v, null, () -> {
    //                       // Sync the page upon closing it
    //                       store.put(internalKey, v);
    //                       logger.info("Synced " + internalKey);
    //                       System.out.println("Synced" + internalKey);
    //                   });
    //
                       return r;

                   },
                   (key, value, cause) -> {}),
                (key, value, cause) -> {
                    List<String> internalKey = Arrays.asList(Long.toString(key));

                    Lock readLock = value.getReadWriteLock().readLock();
                    readLock.lock();
                    try {
                        store.put(internalKey, value);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        readLock.unlock();
                    }
                    logger.info("Synced " + internalKey);
                    System.out.println("Synced" + internalKey);
                }
            );

        return result;
    }

    public static KeyObjectStore createKeyObjectStore() {
        KryoFactory factory = new KryoFactory() {
            public Kryo create() {
                Kryo kryo = new Kryo();

                Serializer<?> javaSerializer = new JavaSerializer();
                Serializer<?> rangeSetSerializer = new RangeSetSerializer();
                Serializer<?> rangeMapSerializer = new RangeMapSerializer();
                kryo.register(TreeRangeSet.class, rangeSetSerializer);
                kryo.register(TreeRangeMap.class, rangeMapSerializer);
                kryo.register(Range.class, javaSerializer);

                return kryo;
            }
        };
        // Build pool with SoftReferences enabled (optional)
        KryoPool kryoPool = new KryoPool.Builder(factory).softReferences().build();

        KeyObjectStore result = KeyObjectStoreImpl.create(Paths.get("/tmp/test/"), new ObjectFileStoreKyro(kryoPool));

        return result;
    }

    public static void main1() throws Exception {
        KeyObjectStore objStore = createKeyObjectStore();

        List<String> key = Arrays.asList("q1", "100");
        RangeBuffer<String> value = new RangeBufferImpl<>(1024);
        value.put(0, "hello");
        objStore.put(key, value);



        RangeBuffer<String> restored = objStore.get(key);
        System.out.println(restored.get(0));
        System.out.println(restored.getKnownSize());



        AsyncClaimingCache<Long, RangeBuffer<String>> cache = syncedRangeBuffer(objStore, () -> new RangeBufferImpl<String>(1024));

        // troll the system: Acquire a page which we want to load in a moment
        // and cancel its request
        // This may potentially sometimes cause troubles with kryo - its not clear whether this is harmless:
        // [kryo] Unable to load class  with kryo's ClassLoader. Retrying with current..
        cache.claim(1024l).close();

        try (RefFuture<RangeBuffer<String>> page1 = cache.claim(1024l)) {
            page1.await().put(10, "hello!!!");
        }

        try (RefFuture<RangeBuffer<String>> page2 = cache.claim(2048l)) {
            page2.await().put(15, "world");
        }

        try (RefFuture<RangeBuffer<String>> page1 = cache.claim(1024l)) {
            System.out.println(page1.await().get(10).next());
        }

        try (RefFuture<RangeBuffer<String>> page2 = cache.claim(2048l)) {
            System.out.println(page2.await().get(15).next());
        }



        cache.invalidateAll();


    }

    public static void main2() {
        LocalOrderSpec<Long, Long> spec = LocalOrderSpecImpl.forLong(x -> x);

        MyPublisher<Long, Long> publisher = MyPublisher.create(spec);

        publisher.createFlow(90l).forEach(x -> System.out.println("GOT A: " + x));
        publisher.createFlow(95l).forEach(x -> System.out.println("GOT B: " + x));


        List<Long> longs = LongStream.range(0l, 100l).boxed().collect(Collectors.toList());
        Collections.shuffle(longs, new Random(0));


        longs.forEach(publisher::offer);


    }
}
