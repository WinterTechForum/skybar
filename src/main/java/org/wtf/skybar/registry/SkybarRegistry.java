package org.wtf.skybar.registry;

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
import net.openhft.koloboke.collect.map.IntLongMap;
import net.openhft.koloboke.collect.map.hash.HashIntLongMaps;
import org.HdrHistogram.WriterReaderPhaser;

@ThreadSafe
public class SkybarRegistry {

    public static final SkybarRegistry registry = new SkybarRegistry();

    @GuardedBy("phaser")
    private volatile ConcurrentHashMap<Long, LongAdder> activeCounts = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, LongAdder> inactiveCounts = new ConcurrentHashMap<>();

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
    private final Map<String, IntLongMap> accumulatedCounts = new HashMap<>();

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
            activeCounts.compute(index, (count, adder) -> {
                if (adder == null) {
                    LongAdder longAdder = new LongAdder();
                    longAdder.increment();
                    return longAdder;
                }

                adder.increment();

                return adder;
            });
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
        } finally {
            phaser.writerCriticalSectionExit(l);
        }

        return index;
    }

    /**
     * @param deltaBuffer map to use to write delta into
     */
    public void updateListeners(Map<String, IntLongMap> deltaBuffer) {
        deltaBuffer.forEach((s, counts) -> counts.clear());

        synchronized (this) {
            flipPhase();

            inactiveCounts.forEach((index, adder) -> {

            /*
             this is accessed outside the registry's phaser read lock, but all keys we will access here for the
             delta must already have been added at registration time
            */
                SourceLocation location = indexToSourceLoc.get(index);

                BiFunction<String, IntLongMap, IntLongMap> mapUpdater
                        = getMapUpdater(location.lineNum, adder);
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
    public synchronized Map<String, IntLongMap> getCurrentSnapshot(DeltaListener deltaListener) {
        listeners.add(deltaListener);

        // deep copy
        HashMap<String, IntLongMap> copy = new HashMap<>();
        accumulatedCounts.forEach((source, counts) -> copy.put(source, HashIntLongMaps.newImmutableMap(counts)));
        return copy;
    }

    public synchronized void unregisterListener(DeltaListener listener) {
        listeners.remove(listener);
    }

    static BiFunction<String, IntLongMap, IntLongMap> getMapUpdater(int lineNum, LongAdder adder) {
        return (source, counts) -> {
            if (counts == null) {
                // no count map; create a new map with just the one count set
                IntLongMap newCounts = HashIntLongMaps.newMutableMap();
                newCounts.put(lineNum, adder.longValue());
                return newCounts;
            }

            counts.addValue(lineNum, adder.longValue());

            return counts;
        };
    }

    /**
     * Swap the inactive and active data structures
     */
    private void flipPhase() {

        clearAdders(inactiveCounts);

        phaser.readerLock();
        try {
            // swap with write to active last since that's volatile
            ConcurrentHashMap<Long, LongAdder> tmpCounts = inactiveCounts;
            inactiveCounts = activeCounts;
            activeCounts = tmpCounts;

            phaser.flipPhase();
        } finally {
            phaser.readerUnlock();
        }
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

    /**
     * Implementations should not hold on to the map that's passed in. It will be re-used, so copy data out of it if you
     * need to keep the contents.
     */
    @FunctionalInterface
    public interface DeltaListener extends Consumer<Map<String, IntLongMap>> {}
}
