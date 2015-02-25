package org.wtf.skybar.registry;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SkybarRegistryTest {

    private SkybarRegistry r;

    @Before
    public void setUp() throws Exception {
        r = new SkybarRegistry();
    }

    @Test
    public void testRegisterInitializesMapWithEmptyCount() {
        r.registerLine("foo", 33);

        assertCount("foo", 1, 33, 0);
    }

    @Test
    public void testRegisterForDifferentLineInSameFile() {
        r.registerLine("foo", 33);
        r.registerLine("foo", 44);

        assertCount("foo", 2, 44, 0);
    }

    @Test
    public void testVisitLineIncrementsCount() {
        long index = r.registerLine("foo", 33);
        r.visitLine(index);

        assertCount("foo", 1, 33, 1);
    }

    @Test
    public void testVisitLineIncrementsCountForOnlyTheCorrectLine() {
        long index = r.registerLine("foo", 33);
        r.registerLine("foo", 44);
        r.visitLine(index);

        assertCount("foo", 2, 33, 1);
    }

    /**
     * @param source   source file
     * @param numLines number of lines for the source
     * @param lineNum  line number to check
     * @param count    expected count
     */
    private void assertCount(String source, int numLines, int lineNum, int count) {
        Map<String, Map<Integer, Integer>> j = r.getSnapshot();
        assertEquals(1, j.size());
        assertEquals(source, j.keySet().iterator().next());

        Map<Integer, Integer> counts = j.get(source);
        assertEquals(numLines, counts.size());
        assertTrue(counts.containsKey(lineNum));
        assertEquals(count, counts.get(lineNum).intValue());
    }
}
