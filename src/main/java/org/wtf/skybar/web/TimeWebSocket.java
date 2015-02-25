package org.wtf.skybar.web;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.wtf.skybar.time.TimeGenerator;
import org.wtf.skybar.time.TimeListener;
 
/**
 * Example EchoSocket using Listener.
 */
public class TimeWebSocket implements WebSocketListener, TimeListener {
    private static final Logger LOG = Log.getLogger(TimeWebSocket.class);
    private Session outbound;
 
    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        LOG.debug("Ignoring webSocketBinary payload len="+len);
    }
 
    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        TimeGenerator.getInstance().unregister(this);
        this.outbound = null;
    }
 
    @Override
    public void onWebSocketConnect(Session session) {
        this.outbound = session;
        TimeGenerator.getInstance().register(this);
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
     * Called by TimeGenerator every period.
     * @param timestamp to send to the client.
     */
    @Override
    public void onTime(long timestamp) {
        String json = "{\"time\":"+timestamp+"}";
        if (outbound != null && outbound.isOpen()) {
            outbound.getRemote().sendStringByFuture(json);
        }
    }
}