package org.wtf.skybar.web;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.wtf.skybar.web.CoverageWebSocket.toJson;

public class CoverageWebSocketTest {

    @Test
    public void testToJsonRemovesLinesWithZeroCountsAndSubsequentlyEmptySources() {
        HashMap<String, Map<Integer, Long>> data = new HashMap<>();
        HashMap<Integer, Long> counts = new HashMap<>();
        counts.put(2, 0L);
        data.put("foo", counts);
        assertEquals("{}", toJson(data));
    }

    @Test
    public void testToJsonRemovesLinesWithZeroCounts() {
        HashMap<String, Map<Integer, Long>> data = new HashMap<>();
        HashMap<Integer, Long> counts = new HashMap<>();
        counts.put(1, 2L);
        counts.put(2, 0L);
        data.put("foo", counts);
        assertEquals("{\"foo\":{\"1\":2}}", toJson(data));
    }

    @Test
    public void testToJsonRemovesSourcesWithoutCounts() {
        HashMap<String, Map<Integer, Long>> data = new HashMap<>();
        HashMap<Integer, Long> counts = new HashMap<>();
        counts.put(1, 2L);
        data.put("foo", counts);
        data.put("bar", new HashMap<>());
        assertEquals("{\"foo\":{\"1\":2}}", toJson(data));
    }
}
