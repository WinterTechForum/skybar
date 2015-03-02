package org.wtf.skybar.web;

import java.util.HashMap;
import net.openhft.koloboke.collect.map.IntLongMap;
import org.junit.Test;

import static net.openhft.koloboke.collect.map.hash.HashIntLongMaps.newMutableMap;
import static org.junit.Assert.assertEquals;
import static org.wtf.skybar.web.CoverageWebSocket.toJson;

public class CoverageWebSocketTest {

    @Test
    public void testToJsonRemovesLinesWithZeroCountsAndSubsequentlyEmptySources() {
        HashMap<String, IntLongMap> data = new HashMap<>();
        IntLongMap counts = newMutableMap();
        counts.put(2, 0L);
        data.put("foo", counts);
        assertEquals("{}", toJson(data));
    }

    @Test
    public void testToJsonRemovesLinesWithZeroCounts() {
        HashMap<String, IntLongMap> data = new HashMap<>();
        IntLongMap counts = newMutableMap();
        counts.put(1, 2L);
        counts.put(2, 0L);
        data.put("foo", counts);
        assertEquals("{\"foo\":{\"1\":2}}", toJson(data));
    }

    @Test
    public void testToJsonRemovesSourcesWithoutCounts() {
        HashMap<String, IntLongMap> data = new HashMap<>();
        IntLongMap counts = newMutableMap();
        counts.put(1, 2L);
        data.put("foo", counts);
        data.put("bar", newMutableMap());
        assertEquals("{\"foo\":{\"1\":2}}", toJson(data));
    }
}
