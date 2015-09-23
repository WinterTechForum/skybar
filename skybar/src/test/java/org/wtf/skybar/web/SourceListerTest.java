package org.wtf.skybar.web;

import org.eclipse.jetty.util.resource.Resource;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class SourceListerTest {
    @Test
    public void testGetExistingResourceSinglePath() throws IOException {
        // Set up
        final SourceLister instance = new SourceLister(
            Resource.newResource("src/test/resources/source_dir")
        );

        // Test
        final Resource actualResource = instance.getResource("/file");

        // Verify
        assertNotNull(actualResource);
        assertTrue(actualResource.exists());
    }

    @Test
    public void testGetExistingResourceMultiplePaths() throws IOException {
        // Set up
        final SourceLister instance = new SourceLister(
                Resource.newResource("src/test/resources/empty"),
                Resource.newResource(System.getProperty("path.separator")),
                Resource.newResource("src/test/resources/source_dir")
        );

        // Test
        final Resource actualResource = instance.getResource("/file");

        // Verify
        assertNotNull(actualResource);
        assertTrue(actualResource.exists());
    }

    @Test
    public void testGetMissingResource() throws IOException {
        // Set up
        final SourceLister instance = new SourceLister(
                Resource.newResource("src/test/resources/empty"),
                        Resource.newResource(System.getProperty("path.separator")),
                                Resource.newResource("src/test/resources/source_dir")
        );

        // Test
        final Resource actualResource = instance.getResource("/non-existent");

        // Verify
        assertNotNull(actualResource);
        assertFalse(actualResource.exists());
    }
}
