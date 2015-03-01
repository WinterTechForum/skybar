package org.wtf.skybar.registry;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.wtf.skybar.web.WebServer;

/**
 * Starts up the server, "visits lines" as quickly as possible, and subscribes to updates via WebSocket.
 */
public final class SkybarStressTool {
    private SkybarRegistry r = SkybarRegistry.registry;

    private ExecutorService ex = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Exception {

        int numSeconds = 60;
        if (args.length == 1) {
            numSeconds = Integer.parseInt(args[0]);
        }

        new SkybarStressTool().go(numSeconds);
    }

    public void go(int durationSecs) throws Exception {

        WebServer webServer = new WebServer(r, 0, ".");
        int port = webServer.start();

        long seed = new Random().nextLong();
        System.out.println("Seed: " + seed);
        SplittableRandom rand = new SplittableRandom(seed);

        int numLinesToRegister = 2000;
        long[] indexes = new long[numLinesToRegister];
        int numVisitThreads = 2;

        for (int i = 0; i < numLinesToRegister; i++) {
            String sourceName = "file " + i % 10;
            long index = r.registerLine(sourceName, i);
            indexes[i] = index;
        }

        AtomicInteger numFutures = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();

        for (int thread = 0; thread < numVisitThreads; thread++) {
            futures.add(ex.submit(() -> {
                numFutures.incrementAndGet();
                SplittableRandom visitRand = rand.split();
                while (true) {
                    // get a random entry. Increment after get when writing, so need to decrement here
                    long index = indexes[visitRand.nextInt(numLinesToRegister)];

                    r.visitLine(index);

                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                }
            }));
        }

        WebSocketClient client = new WebSocketClient();
        client.start();
        futures.add(client.connect(new SimpleEchoSocket(),
                new URI(String.format("ws://localhost:%d/livecoverage/", port)),
                new ClientUpgradeRequest()));

        Thread.sleep(durationSecs * 1000);

        ex.shutdownNow();

        client.stop();

        for (Future<?> future : futures) {
            future.get();
        }
    }

    @WebSocket(maxTextMessageSize = 64 * 1024)
    public static class SimpleEchoSocket {

        @OnWebSocketClose
        public void onClose(int statusCode, String reason) {
            System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
        }

        @OnWebSocketConnect
        public void onConnect(Session session) {
            System.out.printf("Got connect: %s%n", session);
        }

        @OnWebSocketMessage
        public void onMessage(String msg) {
            System.out.printf("Got msg: %s%n", msg);
        }
    }
}
