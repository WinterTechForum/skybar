package org.wtf.skybar.time;

import java.util.HashMap;
import java.util.Map;
import net.openhft.koloboke.collect.map.IntLongMap;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.wtf.skybar.registry.SkybarRegistry;

/**
 * Send a message to all listeners every second.
 */
public class RegistryUpdateListeners extends Thread implements Listener {
    private static final Logger LOG = Log.getLogger(RegistryUpdateListeners.class);
    public static final long INITIAL_DELAY = 200L;
    public static final long PULSE_PERIOD = 200L;
    private final SkybarRegistry registry;

    public RegistryUpdateListeners(SkybarRegistry registry) {
        super("RegistryUpdateListeners");
        super.setDaemon(true);
        this.registry = registry;
    }

    @Override
    public void run() {
        Map<String, IntLongMap> deltaBuffer = new HashMap<>();
        try {
            Thread.sleep(INITIAL_DELAY);
            while (!this.isInterrupted()) {
                this.registry.updateListeners(deltaBuffer);
                Thread.sleep(PULSE_PERIOD);
            }
        } catch (InterruptedException e) {
            LOG.warn("Interrupted");
        }
    }

    @Override
    public void lifeCycleStarting(LifeCycle event) {
    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
    }

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause) {
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
        this.interrupt();
    }

    @Override
    public void lifeCycleStopped(LifeCycle event) {
    }
}
