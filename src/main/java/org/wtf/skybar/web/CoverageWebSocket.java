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
        String json = JSON.toString(snapshot);
        outbound.getRemote().sendStringByFuture(json);
    }
}
