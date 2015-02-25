package org.wtf.skybar.web;

import java.util.Map;
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
        LOG.debug("Ignoring webSocketText payload message="+message);
    }

    /**
     * Called by SkybarRegistry once per time period to push new differential
     * snapshot to clients.
     * @param diffSnapshot differential snapshot
     */
    @Override
    public void accept(Map<String, Map<Integer, Long>> diffSnapshot) {
        sendSnapshot(diffSnapshot);
    }

    private void sendSnapshot(Map<String,Map<Integer,Long>> snapshot) {
        StringBuilder sb = new StringBuilder(1024);
        Map<Integer,Long> lineNumToCount;
        String sourcePath;
        int cnt = 0;
        sb.append('{');
        for (Map.Entry<String,Map<Integer,Long>> ent: snapshot.entrySet()) {
            sourcePath = ent.getKey();
            lineNumToCount = ent.getValue();
            if (lineNumToCount == null || lineNumToCount.isEmpty()) {
                continue;
            }
            if (cnt > 0) {
                sb.append(',');
            }
            sb.append("\"").append(sourcePath).append("\":{");
            appendLineNumToCount(sb, lineNumToCount);
            ++cnt;
        }
        sb.append('}');
    }

    private void appendLineNumToCount(StringBuilder sb, Map<Integer, Long> lineNumToCount) {
        int cnt = 0;
        lineNumToCount.entrySet().stream().forEach((ent) -> {
            if (cnt > 0) {
                sb.append(',');
            }
            sb.append(ent.getKey()).append(':').append(ent.getValue());
        });
    }
}
