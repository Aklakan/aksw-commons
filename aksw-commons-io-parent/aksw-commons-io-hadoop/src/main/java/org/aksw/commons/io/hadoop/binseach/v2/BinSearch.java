package org.aksw.commons.io.hadoop.binseach.v2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.aksw.commons.io.binseach.BinarySearcher;
import org.apache.hadoop.io.compress.BZip2Codec;

import com.google.common.base.Stopwatch;


public class BinSearch {


    public static void main(String[] args) throws IOException {
        // Path path = Path.of("/home/raven/tmp/2018-04-04-Amenity.node.sorted.nt");

        Path plainPath = Path.of("/media/raven/T9/raven/datasets/wikidata/2024-08-24_wikidata-truthy.sorted.nt");
        Path bz2Path = Path.of("/media/raven/T9/raven/datasets/wikidata/2024-08-24_wikidata-truthy.sorted.nt.bz2");

        byte[] prefix = "<http://linkedgeodata.org/geometry/node1202810066>".getBytes();

        byte[] first = "<http://linkedgeodata.org/geometry/node1000036734>".getBytes();
        byte[] last = "<http://linkedgeodata.org/triplify/node999596437>".getBytes();

        byte[] wd = "<http://www.wikidata.org/entity/Q24075>".getBytes();

        byte[] lookup = wd;

        // BinarySearcher bs = BinarySearcherOverPlainSource.of(path, BinSearchLevelCache.dftCache());

        BinarySearcher bs = BinarySearchBuilder.newBuilder()
                .setSource(bz2Path)
                .setCodec(new BZip2Codec())
                .setBinSearchCache(BinSearchLevelCache.dftCache())
                .build();
//        BinarySearcher bs = BinarySearchBuilder.newBuilder()
//                .setSource(plainPath)
//                .setBinSearchCache(BinSearchLevelCache.dftCache())
//                .build();

        List<String> tasks = Files.lines(Path.of("/media/raven/T9/raven/datasets/wikidata/movies.nt")).map(line -> {
            String parts[] = line.split(" ", 2);
            return parts[0];
        }).toList();


        Stopwatch sw = Stopwatch.createStarted();

        int[] i = new int[]{0};
        tasks.parallelStream().forEach(str -> {
//            System.out.println("Processing: " + str);

            if (str.contains("<http://www.wikidata.org/entity/Q1001777>")) {
//                System.out.println("here");
            }

            try {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(bs.search(str.getBytes()), StandardCharsets.UTF_8))) {
                    boolean foundMatch = br.lines().limit(1).count() > 0;
                    if (!foundMatch) {
                        System.err.println("NO MATCH FOR: " + str);
                    }
//                	br.lines().limit(1).forEach(x -> {
//                        // System.out.println(x);
//                    });
                }
            } catch (Exception e) {
                throw new RuntimeException("Error while processing " + str, e);
            }

            int x = ++i[0];
            if (x % 100 == 0) {
                float elapsed = sw.elapsed(TimeUnit.MILLISECONDS) * 0.001f;
                System.out.println(String.format("Item %d - elapsed: %f, throughput %.2f", x, elapsed, (x / elapsed)));
            }
        });


        System.out.println(sw.elapsed(TimeUnit.SECONDS));
    }

//    public static void main(String[] args) throws IOException {
//
//        // Path path = Paths.get("/home/raven/Datasets/2024-08-24_wikidata-truthy.sorted.nt.zst");
//        Path path = Paths.get("/media/raven/T9/raven/datasets/wikidata/2024-08-24_wikidata-truthy.sorted.nt");
//
//        int cpuCount = Runtime.getRuntime().availableProcessors();
//        System.out.println("cpus: " + cpuCount);
//        // cpuCount = 1;
//
//        long totalSize = Files.size(path);
//
//        // int splitCount = 10000; //cpuCount;
//        // long splitSize = totalSize / splitCount;
//
//
//        long splitSize = 50_000_000;
//        int evenSplitCount = (int)(totalSize / splitSize);
//        int splitCount = evenSplitCount + 1;
//
//        ReadableChannelSource<byte[]> source = ReadableChannelSources.of(path);
//
//        // long lastSplitSize = totalSize % splitSize;
//
//        // long lastSplitSize = totalSize - (evenSplitCount * splitSize);
//
//
//
//        System.out.println("Processing " + splitCount + " splits");
//
//
//        List<Integer> splitIds = IntStream.range(0, splitCount).boxed().toList();
//         // splitIds = splitIds.subList(10, 11);
///*
//        Transition to next block: 389394
//        Transition to next block: 389421
//  Line: <http://www.wikidata.org/entity/P10432> <http://wikiba.se/ontology#reference> <http://www.wikidata.org/prop/reference/P10432> .
//Line: <http://www.wikidata.org/entity/P10432> <http://wikiba.se/ontology#referenceValue> <http://www.wikidata.org/prop/reference/value/P10432> .
//Line: <http://www.wikidata.org/entity/P10432> <http://wikiba.se/ontology#referenceValueNormalized> <http://www.wikidata.org/prop/reference/value-normalized/P10432> .
//Line: <http://www.wikidata.org/entity/P10432> <http://wikiba.se/ontology#statementProperty> <http://www.wikidata.org/prop/statement/P10432> .
//Line: <http://www.wikidata.org/entity/P10432> <http://wikiba.se/ontology#statementValue> <http://www.wikidata.org/prop/statement/value/P10432> .
//
//        */
//        try (Stream<String> lines = splitIds.parallelStream().flatMap(splitId -> {
//            long start = splitId * splitSize;
//
//            boolean isLastSplit = splitId == splitCount - 1;
//            long end = isLastSplit ? totalSize : start + splitSize;
//
//            SeekableReadableChannel<byte[]> coreChannel;
//            try {
//                coreChannel = SeekableReadableChannels.wrapForwardSeekable(source.newReadableChannel(start), start);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//
//            ReadableChannel<byte[]> channel = new ReadableChannelWithLimitByDelimiter<>(coreChannel, coreChannel::position, (byte)'\n', end);
//            BufferedReader br = new BufferedReader(new InputStreamReader(ReadableChannels.newInputStream(channel), StandardCharsets.UTF_8));
//            int skipCount = splitId == 0 ? 0 : 1; // Skip the first line on all splits but the first
//
//            return br.lines().skip(skipCount).onClose(() -> {
//                try {
//                    br.close();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//        })) {
//            // lines.limit(10000).forEach(x -> System.out.println("Line: " + x));
//
//            System.out.println("Count: " + lines.count());
//        }
//
//        if (true) {
//            return;
//        }
//
//        /*
//         * index structure: keep track of the first record (e.g. triple/quad/line) in each block.
//         *
//         * search:
//         * - obtain a position p1
//         * - set the channel to it
//         * - read 1 byte, then take the position p2, unread the byte (should be a helper function)
//         *   now we know at which position the channel is positioned.
//         *   sanity check: p1 should differ from p2 unless we hit a block offset
//         *
//         * - skip over the first record (newline), then start a parser on the remaining stream.
//         * - take the first item from the parser, add it to the cache metadata.
//         * - use it to decide whether to search in the block left or right.
//         *   if the id matches, we still need to find the starting point, which is left.
//         *
//         * searching within blocks:
//         *   for decoded data we need to lead the whole block
//         *
//         *
//         */
//
//        //pos = 20133431795l;
//        // pos = 20133431796l;
////        SeekableByteChannel coreChannel = blockSource.newChannel();
////        coreChannel.position(pos);
////
////
////        long finalPos = pos;
////
////        try (ReadableByteChannel channel = new ReadableByteChannelWithLimitByNewline<>(
////                coreChannel, pos + 100000)) {
////            System.out.println(coreChannel.position());
////
////            try (BufferedReader br = new BufferedReader(new InputStreamReader(Channels.newInputStream(channel), StandardCharsets.UTF_8))) {
////                String line;
////                while ((line = br.readLine()) != null) {
////                    System.out.println(line);
////                    System.out.println("next line will be read from pos: " + coreChannel.position());
////                }
////                System.out.println(coreChannel.position());
////            }
////        }
//    }
}
