package org.wtf.skybar.registry;

import net.openhft.koloboke.collect.map.IntLongMap;
import net.openhft.koloboke.collect.map.hash.HashIntLongMap;
import net.openhft.koloboke.collect.map.hash.HashIntLongMaps;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.invoke.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 * Handles accumulating line visits and distributing deltas to any registered listeners.
 */
@ThreadSafe
public class SkybarRegistry {

    public static final SkybarRegistry registry = new SkybarRegistry();

    private final ConcurrentMap<String, Map<Integer, LongAdder>> visits = new ConcurrentHashMap<>();

    private final List<DeltaListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Can only be called once for a given source / line pair
     *
     * @param sourceName path to source file
     * @param lineNumber line number
     */
    public void registerLine(String sourceName, int lineNumber) {
        Map<Integer, LongAdder> lines = visits.get(sourceName);
        if(lines == null) {
            Map<Integer, LongAdder> newLines = new HashMap<>();
            Map<Integer, LongAdder> existingLines = visits.putIfAbsent(sourceName, newLines);
            lines = existingLines != null ? existingLines : newLines;
        }
        lines.putIfAbsent(lineNumber, new LongAdder());
    }

    /**
     * Can only be called after registerLine has been called for the same line
     * @param sourceName name of the source file
     * @param lineNumber the line number to get the LongAdder for
     * @return the LongAdder used to accumulate visit counts for the line
     */
    public LongAdder getAdderForLine(String sourceName, int lineNumber) {
        return visits.get(sourceName).get(lineNumber);
    }

    /**
     * @param prev map containing the previously accumulated line visits
     */
    public void updateListeners(Map<String, IntLongMap> prev) {
        Map<String, IntLongMap> delta = new HashMap<>();
        for (Map.Entry<String, Map<Integer, LongAdder>> sourceLines : visits.entrySet()) {

            String sourceName = sourceLines.getKey();
            Map<Integer, LongAdder> currentLines = sourceLines.getValue();

            Map<Integer, Long> prevLines = prev.get(sourceName);


            if(prevLines == null) {
                IntLongMap map = HashIntLongMaps.newMutableMap();
                delta.put(sourceName, map);
                prev.put(sourceName, map);
                currentLines.forEach((lnum, adder) -> map.put(lnum.intValue(), adder.longValue()));
            } else {
                currentLines.forEach((lnum, adder) -> {
                    long count = adder.longValue();
                    Long prevCount = prevLines.get(lnum);
                    long diff = count - (prevCount == null ? 0 : prevCount);
                    if(diff > 0) {
                        IntLongMap deltaForSource = delta.get(sourceName);
                        if(deltaForSource == null) {
                            delta.put(sourceName, deltaForSource = HashIntLongMaps.newMutableMap());
                        }
                        deltaForSource.put(lnum.intValue(), diff);
                        prev.get(sourceName).put(lnum.intValue(), count);
                    }
                });
            }

        }

        if(!delta.isEmpty()) {
            for (DeltaListener listener : listeners) {
                listener.accept(delta);
            }
        }
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
        Map<String, IntLongMap> snapshot = new HashMap<>();
        visits.forEach((source, lines) -> {
            HashIntLongMap map = HashIntLongMaps.newMutableMap();
            snapshot.put(source, map);
            lines.forEach((line, adder) -> map.put(line.intValue(), adder.longValue()));
        });
        return snapshot;
    }

    public synchronized void unregisterListener(DeltaListener listener) {
        listeners.remove(listener);
    }

    /**
     * Invoke Dynamic bootstrap method called once per line callsite. Takes the source name and line number as "extra" bootstrap parameters
     * and returns a CallSite with a method handle for the add method of the LongAdder for that line
     * @param lookup factory for creating MethodHandles
     * @param name name of the method (unused)
     * @param type signature of the indy method
     * @param sourceName source file name
     * @param lineNumber line number
     * @return the cal site
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unused")
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String sourceName, int lineNumber) throws NoSuchMethodException, IllegalAccessException {
        LongAdder adder = registry.getAdderForLine(sourceName, lineNumber);
        MethodHandle add = lookup
                .findVirtual(LongAdder.class, "add", MethodType.methodType(void.class, new Class[]{long.class}))
                .bindTo(adder);

        return new ConstantCallSite(MethodHandles.insertArguments(add, 0, 1l).asType(type));
    }

    /**
     * Implementations should not hold on to the map that's passed in. It will be re-used, so copy data out of it if you
     * need to keep the contents.
     */
    @FunctionalInterface
    public interface DeltaListener extends Consumer<Map<String, IntLongMap>> {}
}
