package org.wtf.skybar.web;

import java.util.LinkedHashMap;
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
        LOG.debug("Ignoring webSocketBinary payload len=" + len);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        SkybarRegistry.registry.unregisterListener(this);
        this.outbound = null;
    }

    @Override
    public void onWebSocketConnect(Session session) {
        this.outbound = session;
        Map<String, Map<Integer, Long>> snapshot = SkybarRegistry.registry.getCurrentSnapshot(this);
        sendSnapshot(snapshot);
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
        LOG.debug("Ignoring webSocketText payload message=" + message);
    }

    /**
     * Called by SkybarRegistry once per time period to push new differential snapshot to clients.
     *
     * @param diffSnapshot differential snapshot
     */
    @Override
    public void accept(Map<String, Map<Integer, Long>> diffSnapshot) {
        sendSnapshot(diffSnapshot);
    }

    /**
     * Send snapshot to this client.  Will send an empty map {} if nothing new happened.
     *
     * @param snapshot snapshot coverage count.
     */
    private void sendSnapshot(Map<String, Map<Integer, Long>> snapshot) {
        outbound.getRemote().sendStringByFuture(toJson(snapshot));
    }

    static String toJson(Map<String, Map<Integer, Long>> data) {
        return JSON.toString(filterSnap(data));
    }

    private static Map<String, Map<Integer, Long>> filterSnap(Map<String, Map<Integer, Long>> orig) {
        Map<String, Map<Integer, Long>> res = new LinkedHashMap<>();
        Map<Integer, Long> counts;
        for (Map.Entry<String, Map<Integer, Long>> ent : orig.entrySet()) {
            counts = filterCounts(ent.getValue());
            if (counts != null && counts.size() > 0) {
                res.put(ent.getKey(), counts);
            }
        }
        return res;
    }

    private static Map<Integer, Long> filterCounts(Map<Integer, Long> orig) {
        if (orig == null || orig.isEmpty()) {
            return null;
        }
        Map<Integer, Long> res = new LinkedHashMap<>();
        for (Map.Entry<Integer, Long> ent : orig.entrySet()) {
            if (ent.getValue() > 0L) {
                res.put(ent.getKey(), ent.getValue());
            }
        }
        return res;
    }
}
