package org.wtf.skybar.registry;

import java.lang.invoke.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 */
public class SkybarRegistry {

    public static  SkybarRegistry registry = new SkybarRegistry();

    // Holds the count of each line of each source file
    private Map<String, Map<Integer, LongAdder>> visits = new ConcurrentHashMap<>();

    public Map toJson() {
        //TODO: We need to return a JSON map representing the covered lines for each source file
        return new HashMap();
    }

    /**
     * Called each time a line is visted
     */
    public void visitLine(String sourceFileName, int lineNumber) {
        // TODO: Count the line visits
    }


    public void registerLine(String sourceName, int lineNumber) {
        // TODO: Called at class load time. Initialize each existing line number in the class to count 0
    }

    public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type, String sourceName, int lineNumber) throws NoSuchMethodException, IllegalAccessException {
        // TODO: (Advanced!)
        // Create a MethodHandle for the counter and return a ConstantCallSite for that
        return null;
    }

}
