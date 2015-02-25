package org.wtf.skybar.time;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Send a message to all listeners every second.
 */
public class TimeGenerator {
    public static final long INITIAL_DELAY = 1000L;
    public static final long PULSE_PERIOD = 1000L;
    private static final TimeGenerator instance;
    static {
        instance = new TimeGenerator();
        instance.start();
    }
    
    private final Timer timer;
    private final List<TimeListener> listeners;

    public TimeGenerator() {
        this.timer = new Timer(true);
        this.listeners = new java.util.LinkedList<>();
    }

    public static TimeGenerator getInstance() {
        return instance;
    }

    public synchronized void register(TimeListener listener) {
        assert(listener != null);
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void unregister(TimeListener listener) {
        assert(listener != null);
        listeners.remove(listener);
    }
    private void start() {
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                notifyListeners(System.currentTimeMillis());
            }
        }, INITIAL_DELAY, PULSE_PERIOD);
    }

    private synchronized void notifyListeners(long timestamp) {
        listeners.stream().forEach((listener) -> {
            listener.onTime(timestamp);
        });
    }
}
