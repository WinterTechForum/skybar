package org.wtf.skybar.registry;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.HdrHistogram.WriterReaderPhaser;

@ThreadSafe
public class SkybarRegistry {

    public static final SkybarRegistry registry = new SkybarRegistry();

    @GuardedBy("phaser")
    private volatile ConcurrentHashMap<Long, LongAdder> activeCounts = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, LongAdder> inactiveCounts = new ConcurrentHashMap<>();

    @GuardedBy("phaser")
    private volatile List<Long> activeIndexesSinceLastFlip = new ArrayList<>();
    private List<Long> inactiveIndexesSinceLastFlip = new ArrayList<>();

    private final WriterReaderPhaser phaser = new WriterReaderPhaser();

    private final AtomicLong nextIndex = new AtomicLong();

    /**
     * Only ever inserted (not overwritten) into.
     */
    private final ConcurrentHashMap<Long, SourceLocation> indexToSourceLoc = new ConcurrentHashMap<>();

    /**
     * Stores all accumulated counts to serve as the initial data from which future deltas will apply to
     */
    @GuardedBy("this")
    private final Map<String, Map<Integer, Long>> accumulatedCounts = new HashMap<>();

    @GuardedBy("this")
    private final List<DeltaListener> listeners = new ArrayList<>();

    /**
     * Used to quickly take a snapshot of listeners so we don't hold the lock while actually calling listeners
     */
    @GuardedBy("this")
    private final List<DeltaListener> listenersCopy = new ArrayList<>();

    /**
     * Called each time a line is visited
     */
    public void visitLine(long index) {
        long l = phaser.writerCriticalSectionEnter();
        try {
            activeCounts.get(index).increment();
        } finally {
            phaser.writerCriticalSectionExit(l);
        }
    }

    /**
     * Can only be called once for a given source / line pair
     *
     * @param sourceName path to source file
     * @param lineNumber line number
     * @return index to be used with visitLine
     */
    public long registerLine(String sourceName, int lineNumber) {
        long l = phaser.writerCriticalSectionEnter();
        long index;

        try {
            index = nextIndex.incrementAndGet();
            indexToSourceLoc.put(index, new SourceLocation(sourceName, lineNumber));
            activeCounts.put(index, new LongAdder());
            activeIndexesSinceLastFlip.add(index);
        } finally {
            phaser.writerCriticalSectionExit(l);
        }

        return index;
    }

    /**
     * @param deltaBuffer map to use to write delta into
     */
    public void updateListeners(Map<String, Map<Integer, Long>> deltaBuffer) {
        deltaBuffer.forEach((source, counts) -> counts.clear());

        synchronized (this) {
            flipPhase();

            inactiveCounts.forEach((index, adder) -> {

                /*
                 this is accessed outside the registry's phaser read lock, but all keys we will access here for the
                 delta must already have been added at registration time
                */
                SourceLocation location = indexToSourceLoc.get(index);

                BiFunction<String, Map<Integer, Long>, Map<Integer, Long>> mapUpdater = (source, counts) -> {
                    if (counts == null) {
                        // no count map; create a new map with just the one count set
                        HashMap<Integer, Long> newCounts = new HashMap<>();
                        newCounts.put(location.lineNum, adder.longValue());
                        return newCounts;
                    }

                    // update count in existing counts map
                    counts.compute(location.lineNum, (line, count) -> {
                        if (count == null) {
                            // no count yet, use adder value
                            return adder.longValue();
                        }

                        // already a count, add on the adder value
                        return count + adder.longValue();
                    });

                    return counts;
                };
                deltaBuffer.compute(location.source, mapUpdater);
                accumulatedCounts.compute(location.source, mapUpdater);
            });

            listenersCopy.clear();
            listenersCopy.addAll(listeners);
        }

        // Invoke the listeners outside the lock because we don't need to hold it any more
        listenersCopy.forEach(l -> l.accept(deltaBuffer));
    }

    /**
     * Get the current snapshot (calculated at the time of the last updateListeners()) and register a listener for
     * future deltas
     *
     * @param deltaListener the listener to be called when a delta is available
     * @return the current accumulated state
     */
    public synchronized Map<String, Map<Integer, Long>> getCurrentSnapshot(DeltaListener deltaListener) {
        listeners.add(deltaListener);

        // deep copy
        HashMap<String, Map<Integer, Long>> copy = new HashMap<>();
        accumulatedCounts.forEach((source, counts) -> copy.put(source, new HashMap<>(counts)));
        return copy;
    }

    public synchronized void unregisterListener(DeltaListener listener) {
        listeners.remove(listener);
    }

    /**
     * Swap the inactive and active data structures
     */
    private void flipPhase() {

        clearAdders(inactiveCounts);
        inactiveIndexesSinceLastFlip.clear();

        phaser.readerLock();
        try {
            // need to write into inactiveCounts before it starts getting used so that all indexes have an adder
            // this is allowed because we're only reading from active
            activeIndexesSinceLastFlip.forEach(l -> inactiveCounts.put(l, new LongAdder()));

            // swap with write to active last since that's volatile
            List<Long> tmpIndexes = inactiveIndexesSinceLastFlip;
            inactiveIndexesSinceLastFlip = activeIndexesSinceLastFlip;
            activeIndexesSinceLastFlip = tmpIndexes;

            ConcurrentHashMap<Long, LongAdder> tmpCounts = inactiveCounts;
            inactiveCounts = activeCounts;
            activeCounts = tmpCounts;

            phaser.flipPhase();
        } finally {
            phaser.readerUnlock();
        }
    }

    public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type, String sourceName,
            int lineNumber) throws NoSuchMethodException, IllegalAccessException {
        // TODO: (Advanced!)
        // Create a MethodHandle for the counter and return a ConstantCallSite for that
        return null;
    }

    private static void clearAdders(ConcurrentHashMap<Long, LongAdder> counts) {
        counts.forEachValue(1, LongAdder::reset);
    }

    private static class SourceLocation {
        final String source;
        final int lineNum;

        private SourceLocation(String source, int lineNum) {
            this.source = source;
            this.lineNum = lineNum;
        }
    }

    @FunctionalInterface
    public interface DeltaListener extends Consumer<Map<String, Map<Integer, Long>>> {}
}
