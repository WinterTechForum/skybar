package org.wtf.skybar.source;

/**
 *
 */
public interface SourceProvider {
    Source lookup(String path, ClassLoader classLoader);
}
