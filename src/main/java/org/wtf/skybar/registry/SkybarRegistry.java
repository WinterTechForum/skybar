package org.wtf.skybar.registry;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class SkybarRegistry {

    public static final SkybarRegistry registry = new SkybarRegistry();

    // Holds the count of each line of each source file
    private final Map<String, Map<Integer, LongAdder>> visits = new ConcurrentHashMap<>();

    @Nonnull
    public Map<String, Map<Integer, Integer>> toJson() {
        //TODO: We need to return a JSON map representing the covered lines for each source file
        return new HashMap<>();
    }

    /**
     * Called each time a line is visted
     */
    public void visitLine(String sourceFileName, int lineNumber) {
        visits.get(sourceFileName).get(lineNumber).increment();
    }

    public void registerLine(String sourceName, int lineNumber) {
        visits.compute(sourceName, (sourceFile, map) -> {
            if (map == null) {
                // no map for the source file; create one with a single entry with a 0 count
                HashMap<Integer, LongAdder> newCounterMap = new HashMap<>();
                newCounterMap.put(lineNumber, new LongAdder());
                return newCounterMap;
            }

            // map already exists for this source name; insert a new adder
            if (map.put(lineNumber, new LongAdder()) != null) {
                throw new IllegalStateException(
                        "Duplicate call for source <" + sourceName + "> and line <" + lineNumber + ">");
            }

            return map;
        });
    }

    public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type, String sourceName,
            int lineNumber) throws NoSuchMethodException, IllegalAccessException {
        // TODO: (Advanced!)
        // Create a MethodHandle for the counter and return a ConstantCallSite for that
        return null;
    }
}
