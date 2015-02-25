package org.wtf.skybar.time;

/**
 * Called by TimeGenerator every period.
 */
public interface TimeListener {
    
    void onTime(long timestamp);
}
