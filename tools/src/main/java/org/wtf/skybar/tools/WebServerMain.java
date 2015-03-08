package org.wtf.skybar.tools;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.source.FilesystemSourceProvider;
import org.wtf.skybar.source.SourceProvider;
import org.wtf.skybar.web.WebServer;

final class WebServerMain {
    public static void main(String[] args) throws InterruptedException {
        List<SourceProvider> sourceProviders = new ArrayList<>();
        sourceProviders.add(new FilesystemSourceProvider(Paths.get("tools/src/main/java")));

        new WebServer(new SkybarRegistry(), 8080, sourceProviders).start();

        while (true) {
            Thread.sleep(1000);
        }
    }
}
