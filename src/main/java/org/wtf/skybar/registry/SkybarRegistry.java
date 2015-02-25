package org.wtf.skybar.registry;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class SkybarRegistry {

    public static final SkybarRegistry registry = new SkybarRegistry();

    // Holds the count of each line of each source file
    private final ConcurrentHashMap<Long, LongAdder> visits = new ConcurrentHashMap<>();
    private final AtomicLong nextIndex = new AtomicLong();

    private final ConcurrentHashMap<Long, SourceLocation> indexToSourceLoc = new ConcurrentHashMap<>();

    @Nonnull
    public Map<String, Map<Integer, Integer>> getSnapshot() {

        HashMap<String, Map<Integer, Integer>> snapshot = new HashMap<>();

        visits.forEach((index, adder) -> {

            SourceLocation sl = indexToSourceLoc.get(index);

            snapshot.compute(sl.source, (source, counts) -> {
                if (counts == null) {
                    HashMap<Integer, Integer> snapshotCounts = new HashMap<>();
                    snapshotCounts.put(sl.line, adder.intValue());
                    return snapshotCounts;
                }
                counts.put(sl.line, adder.intValue());
                return counts;
            });
        });

        return snapshot;
    }

    /**
     * Called each time a line is visited
     */
    public void visitLine(long index) {
        visits.get(index).increment();
    }

    /**
     * Can only be called once for a given source / line pair
     *
     * @param sourceName path to source file
     * @param lineNumber line number
     * @return index to be used with visitLine
     */
    public long registerLine(String sourceName, int lineNumber) {
        long index = nextIndex.incrementAndGet();

        indexToSourceLoc.put(index, new SourceLocation(sourceName, lineNumber));
        visits.put(index, new LongAdder());

        return index;
    }

    public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type, String sourceName,
            int lineNumber) throws NoSuchMethodException, IllegalAccessException {
        // TODO: (Advanced!)
        // Create a MethodHandle for the counter and return a ConstantCallSite for that
        return null;
    }

    private static class SourceLocation {
        final String source;
        final int line;

        private SourceLocation(String source, int line) {
            this.source = source;
            this.line = line;
        }
    }
}
