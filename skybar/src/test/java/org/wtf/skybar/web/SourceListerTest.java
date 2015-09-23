package org.wtf.skybar.web;

import org.junit.Test;
import org.wtf.skybar.source.FileSystemSourceProvider;
import org.wtf.skybar.source.Source;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SourceListerTest {
    @Test
    public void testGetExistingResourceSinglePath() throws IOException {
        // Set up
        final SourceLister instance = new SourceLister( null,
                new FileSystemSourceProvider(new File("src/test/resources/source_dir"))
        );

        // Test
        final Source actualResource = instance.getSource("/file", null);

        // Verify
        assertNotNull(actualResource);
    }

    @Test
    public void testGetExistingResourceMultiplePaths() throws IOException {
        // Set up
        final SourceLister instance = new SourceLister(null,
                new FileSystemSourceProvider(new File("src/test/resources/empty")),
                        new FileSystemSourceProvider(new File("path.separator")),
                        new FileSystemSourceProvider(new File("src/test/resources/source_dir"))
                        );

        // Test
        final Source actualResource = instance.getSource("/file", null);

        // Verify
        assertNotNull(actualResource);
    }

    @Test
    public void testGetMissingResource() throws IOException {
        // Set up
        final SourceLister instance = new SourceLister(  null,
                new FileSystemSourceProvider(new File("src/test/resources/empty")),
                new FileSystemSourceProvider(new File("path.separator")),
                new FileSystemSourceProvider(new File("src/test/resources/source_dir"))
        );


        // Test
        final Source actualResource = instance.getSource("/non-existent", null);

        // Verify
        assertNull(actualResource);
    }
}
