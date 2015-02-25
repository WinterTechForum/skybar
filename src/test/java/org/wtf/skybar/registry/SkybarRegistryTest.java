package org.wtf.skybar.registry;

import java.util.HashMap;
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
        r.updateListeners(new HashMap<>());

        assertSnapshotCount("foo", 1, 33, 0);
    }

    @Test
    public void testRegisterForDifferentLineInSameFile() {
        r.registerLine("foo", 33);
        r.registerLine("foo", 44);
        r.updateListeners(new HashMap<>());

        assertSnapshotCount("foo", 2, 44, 0);
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

        assertSnapshotCount("foo", 2, 33, 1);
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

    /**
     * @param source   source file
     * @param numLines number of lines for the source
     * @param lineNum  line number to check
     * @param count    expected count
     */
    private void assertSnapshotCount(String source, int numLines, int lineNum, int count) {
        Map<String, Map<Integer, Long>> j = r.getCurrentSnapshot((delta) -> { });
        assertEquals(1, j.size());
        assertEquals(source, j.keySet().iterator().next());

        Map<Integer, Long> counts = j.get(source);
        assertEquals(numLines, counts.size());
        assertTrue(counts.containsKey(lineNum));
        assertEquals(count, counts.get(lineNum).intValue());
    }
}
