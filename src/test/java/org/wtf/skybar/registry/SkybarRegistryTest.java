package org.wtf.skybar.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SkybarRegistryTest {

    private SkybarRegistry r;

    private ExecutorService ex = Executors.newCachedThreadPool();
    CompletionService<?> completionService = new ExecutorCompletionService<>(ex);

    @Before
    public void setUp() throws Exception {
        r = new SkybarRegistry();
    }

    @After
    public void tearDown() throws Exception {
        ex.shutdownNow();
    }

    @Test
    public void testRegisterInitializesEmptyMap() {
        r.registerLine("foo", 33);
        r.updateListeners(new HashMap<>());

        Map<String, Map<Integer, Long>> data = r.getCurrentSnapshot((delta) -> { });
        assertTrue(data.isEmpty());
    }

    @Test
    public void testVisitLineIncrementsCount() {
        long index = r.registerLine("foo", 33);
        r.visitLine(index);
        r.updateListeners(new HashMap<>());

        assertSnapshotCount("foo", 1, 33, 1);
    }

    @Test
    public void testVisitLineIncrementsCountForOnlyTheCorrectLine() {
        long index = r.registerLine("foo", 33);
        r.registerLine("foo", 44);
        r.visitLine(index);
        r.updateListeners(new HashMap<>());

        assertSnapshotCount("foo", 1, 33, 1);
    }

    @Test
    public void testVisitAgainAfterFirstUpdateUpdatesSnapshot() {
        long index33 = r.registerLine("foo", 33);
        long index44 = r.registerLine("foo", 44);
        r.visitLine(index33);
        r.updateListeners(new HashMap<>());

        // update existing line
        r.visitLine(index33);

        // update a never before visited line
        r.visitLine(index44);
        r.visitLine(index44);
        r.updateListeners(new HashMap<>());

        assertSnapshotCount("foo", 2, 33, 2);
        assertSnapshotCount("foo", 2, 44, 2);
    }

    @Test
    public void testUpdateListenersCallsListenerWithDelta() {
        long index33 = r.registerLine("foo", 33);
        long index44 = r.registerLine("foo", 44);
        r.visitLine(index33);
        r.updateListeners(new HashMap<>());

        HashMap<String, Map<Integer, Long>> data = new HashMap<>();

        r.getCurrentSnapshot(data::putAll);

        // update existing line
        r.visitLine(index33);

        // update a never before visited line
        r.visitLine(index44);
        r.visitLine(index44);

        r.updateListeners(new HashMap<>());

        assertSnapshotCount(data, "foo", 2, 33, 1);
        assertSnapshotCount(data, "foo", 2, 44, 2);
    }

    @Test
    public void testUnregisteredListenerDoesntGetUpdates() {
        long index33 = r.registerLine("foo", 33);

        HashMap<String, Map<Integer, Long>> data = new HashMap<>();
        SkybarRegistry.DeltaListener listener = data::putAll;
        r.getCurrentSnapshot(listener);

        r.visitLine(index33);
        r.updateListeners(new HashMap<>());

        assertSnapshotCount(data, "foo", 1, 33, 1);

        r.unregisterListener(listener);

        data.clear();
        // data should not be updated

        r.visitLine(index33);
        r.updateListeners(new HashMap<>());

        assertTrue(data.isEmpty());
    }

    @Test
    public void testReUseBufferForMultipleUpdates() {
        long index33 = r.registerLine("foo", 33);
        long index44 = r.registerLine("foo", 44);

        HashMap<String, Map<Integer, Long>> data = new HashMap<>();
        // ignore return value; will be all zeroes
        r.getCurrentSnapshot(data::putAll);

        r.visitLine(index33);

        HashMap<String, Map<Integer, Long>> buffer = new HashMap<>();
        r.updateListeners(buffer);
        assertSnapshotCount(data, "foo", 1, 33, 1);

        // update a never before visited line
        r.visitLine(index44);
        r.visitLine(index44);

        data.clear();
        r.updateListeners(buffer);

        assertSnapshotCount(data, "foo", 1, 44, 2);
    }

    @Test
    public void testMapUpdaterAddsWhenSourceNotAlreadyPresent() {
        Map<String, Map<Integer, Long>> map = new HashMap<>();

        map.compute("foo", new SkybarRegistry.MapUpdater(1, new LongAdder()));

        assertSnapshotCount(map, "foo", 1, 1, 0);
    }

    @Test
    public void testMapUpdaterAddsNonZeroCountWhenSourceAlreadyPresent() {
        Map<String, Map<Integer, Long>> map = new HashMap<>();
        map.put("foo", new HashMap<>());

        LongAdder adder = new LongAdder();
        adder.increment();
        map.compute("foo", new SkybarRegistry.MapUpdater(1, adder));

        assertSnapshotCount(map, "foo", 1, 1, 1);
    }

    @Test
    public void testMapUpdaterAddsWhenSourceAndCountAlreadyPresent() {
        Map<String, Map<Integer, Long>> map = new HashMap<>();
        HashMap<Integer, Long> origCounts = new HashMap<>();
        origCounts.put(1, 4L);
        map.put("foo", origCounts);

        LongAdder adder = new LongAdder();
        adder.increment();
        map.compute("foo", new SkybarRegistry.MapUpdater(1, adder));

        assertSnapshotCount(map, "foo", 1, 1, 5);
    }

    @Test
    public void testWriteFromManyThreads() throws ExecutionException, InterruptedException {
        long seed = new Random().nextLong();
        System.out.println("Seed: " + seed);
        SplittableRandom rand = new SplittableRandom(seed);

        int numLinesToRegister = 2000;
        AtomicLongArray indexes = new AtomicLongArray(numLinesToRegister);
        AtomicInteger lastWrittenIndexPosition = new AtomicInteger();

        Map<Long, Long> expectedCounts = new ConcurrentHashMap<>();
        Map<Long, SourceLocation> indexToSourceLoc = new ConcurrentHashMap<>();

        // fill up half the indexes already
        for (int i = 0; i < numLinesToRegister / 2; i++) {
            String sourceName = "initial file";
            long index = r.registerLine(sourceName, i);
            indexes.set(lastWrittenIndexPosition.getAndIncrement(), index);
            indexToSourceLoc.put(index, new SourceLocation(sourceName, i));
        }

        int numVisitThreads = 2;

        AtomicBoolean doneWithNewIndexes = new AtomicBoolean(false);
        AtomicInteger visitThreadsDone = new AtomicInteger(0);

        AtomicInteger numFutures = new AtomicInteger();

        completionService.submit(() -> {
            numFutures.incrementAndGet();
            long fileId = 0;
            for (int lineNum = 0; lineNum < numLinesToRegister / 2; lineNum++) {
                String source = "Source " + fileId;
                long index = r.registerLine(source, lineNum);
                indexes.set(lastWrittenIndexPosition.getAndIncrement(), index);
                indexToSourceLoc.put(index, new SourceLocation(source, lineNum));
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }

                // bump the file id now and then
                if (lineNum % 10 == 0) {
                    fileId++;
                }
            }

            doneWithNewIndexes.set(true);
        }, null);

        for (int thread = 0; thread < numVisitThreads; thread++) {
            completionService.submit(() -> {
                numFutures.incrementAndGet();
                SplittableRandom visitRand = rand.split();
                while (true) {
                    // get a random entry. Increment after get when writing, so need to decrement here
                    long index = indexes.get(visitRand.nextInt(lastWrittenIndexPosition.get() - 1));

                    r.visitLine(index);
                    expectedCounts.compute(index, (ignoredIndex, count) -> count == null ? 1 : count + 1);

                    if (doneWithNewIndexes.get()) {
                        visitThreadsDone.incrementAndGet();
                        return;
                    }
                }
            }, null);
        }

        List<Map<String, Map<Integer, Long>>> chunks = Collections.synchronizedList(new ArrayList<>());

        completionService.submit(() -> {
            numFutures.incrementAndGet();
            boolean keepGoing = true;

            chunks.add(r.getCurrentSnapshot((delta) -> {
                HashMap<String, Map<Integer, Long>> copy = new HashMap<>();
                delta.forEach((source, counts) -> copy.put(source, new HashMap<>(counts)));
                chunks.add(copy);
            }));

            HashMap<String, Map<Integer, Long>> deltaBuffer = new HashMap<>();
            while (keepGoing) {
                if (visitThreadsDone.get() == numVisitThreads) {
                    keepGoing = false;
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }

                r.updateListeners(deltaBuffer);
            }
        }, null);

        for (int i = 0; i < numFutures.get(); i++) {
            completionService.take().get();
        }

        System.out.println("Got " + chunks.size() + " chunks");

        Map<String, Map<Integer, Long>> totals = new HashMap<>();

        for (Map<String, Map<Integer, Long>> chunk : chunks) {
            for (Map.Entry<String, Map<Integer, Long>> sourceCounts : chunk.entrySet()) {
                String sourceFile = sourceCounts.getKey();
                totals.putIfAbsent(sourceFile, new HashMap<>());

                Map<Integer, Long> totalSourceCounts = totals.get(sourceFile);
                for (Map.Entry<Integer, Long> lineCountPair : sourceCounts.getValue().entrySet()) {
                    Integer lineNum = lineCountPair.getKey();
                    Long count = lineCountPair.getValue();

                    Long existingCount = totalSourceCounts.get(lineNum);
                    long updatedCount = existingCount == null ? count : count + existingCount;
                    totalSourceCounts.put(lineNum, updatedCount);
                }
            }
        }

        expectedCounts.forEach((index, count) -> {
            SourceLocation sourceLocation = indexToSourceLoc.get(index);
            assertNotNull(sourceLocation);
            Map<Integer, Long> countsForSource = totals.get(sourceLocation.source);
            assertNotNull(countsForSource);
            Long totalCount = countsForSource.get(sourceLocation.lineNum);
            assertNotNull(totalCount);

            assertEquals("index " + index + " source " + sourceLocation.source + " line " + sourceLocation.lineNum,
                    count, totalCount);
        });

        System.out.println("foo");
    }

    private void assertSnapshotCount(String source, int numLines, int lineNum, int count) {
        assertSnapshotCount(r.getCurrentSnapshot((delta) -> { }), source, numLines, lineNum, count);
    }

    /**
     * @param data     count data
     * @param source   source file
     * @param numLines number of lines for the source
     * @param lineNum  line number to check
     * @param count    expected count
     */
    private void assertSnapshotCount(Map<String, Map<Integer, Long>> data, String source, int numLines, int lineNum,
            int count) {
        assertEquals(1, data.size());
        assertEquals(source, data.keySet().iterator().next());

        Map<Integer, Long> counts = data.get(source);
        assertEquals("num lines", numLines, counts.size());
        assertTrue(counts.containsKey(lineNum));
        assertEquals(count, counts.get(lineNum).intValue());
    }

    private static class SourceLocation {
        final String source;
        final int lineNum;

        private SourceLocation(String source, int lineNum) {
            this.source = source;
            this.lineNum = lineNum;
        }
    }
}
