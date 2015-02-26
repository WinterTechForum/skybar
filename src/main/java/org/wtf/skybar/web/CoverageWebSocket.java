package org.wtf.skybar.web;

import java.util.Map;
import org.eclipse.jetty.util.ajax.JSON;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.registry.SkybarRegistry.DeltaListener;
 
/**
 * Send out Coverage map via JSON on initial connect and every second.
 */
public class CoverageWebSocket implements WebSocketListener, DeltaListener {
    private static final Logger LOG = Log.getLogger(CoverageWebSocket.class);
    private Session outbound;
 
    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        LOG.debug("Ignoring webSocketBinary payload len="+len);
    }
 
    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        SkybarRegistry.registry.unregisterListener(this);
        this.outbound = null;
    }
 
    @Override
    public void onWebSocketConnect(Session session) {
        this.outbound = session;
        Map<String,Map<Integer,Long>> snapshot = SkybarRegistry.registry.getCurrentSnapshot(this);
        sendSnapshot(snapshot, false);
    }
 
    @Override
    public void onWebSocketError(Throwable cause) {
        cause.printStackTrace(System.err);
        if (outbound != null) {
            outbound.close(500, cause.toString());
        }
    }
 
    @Override
    public void onWebSocketText(String message) {
        LOG.debug("Ignoring webSocketText payload message="+message);
    }

    /**
     * Called by SkybarRegistry once per time period to push new differential
     * snapshot to clients.
     * @param diffSnapshot differential snapshot
     */
    @Override
    public void accept(Map<String, Map<Integer, Long>> diffSnapshot) {
        sendSnapshot(diffSnapshot, true);
    }

    /**
     * Send snapshot to this client.  Will send an empty map {} if nothing new happened.
     * @param snapshot snapshot coverage count.
     */
    private void sendSnapshot(Map<String,Map<Integer,Long>> snapshot, boolean doFilter) {
        Map<String,Map<Integer,Long>> filtered = snapshot;
        if (doFilter) {
            filtered = filterSnap(snapshot);
        }
        String json = JSON.toString(filtered);
        outbound.getRemote().sendStringByFuture(json);
    }

    private static Map<String,Map<Integer,Long>> filterSnap(Map<String,Map<Integer,Long>> orig) {
        Map<String,Map<Integer,Long>> res = new java.util.LinkedHashMap<>();
        Map<Integer,Long> counts;
        for (Map.Entry<String,Map<Integer,Long>> ent: orig.entrySet()) {
            counts = filterCounts(ent.getValue());
            if (counts != null && counts.size() > 0) {
                res.put(ent.getKey(), counts);
            }
        }
        return res;
    }

    private static Map<Integer,Long> filterCounts(Map<Integer,Long> orig) {
        if (orig == null || orig.isEmpty()) {
            return null;
        }
        Map<Integer,Long> res = new java.util.LinkedHashMap<>();
        for (Map.Entry<Integer,Long> ent: orig.entrySet()) {
            if (ent.getValue() > 0L) {
                res.put(ent.getKey(), ent.getValue());
            }
        }
        return res;
    }

    /**
     * Test if the coverage counts are non-empty and non-zero.
     * @param lineCounts to check for empty/zero condition.
     * @return true if there are counts, false otherwise.
     */
    private static boolean hasCounts(Map<Integer,Long> lineCounts) {
        if (lineCounts == null || lineCounts.isEmpty()) {
            return false;
        }
        boolean hasCount = false;
        for (Map.Entry<Integer,Long> ent: lineCounts.entrySet()) {
            if (ent.getValue() > 0L) {
                hasCount = true;
                break;
            }
        }
        return hasCount;
    }
}
