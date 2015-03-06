package org.wtf.skybar.source;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class ClasspathSourceProviderTest {

    private static final String TOP_LEVEL_CLASS_SRC_MD5 = "5296f276851170006a2acfcce0ae5ba2";

    SourceProvider sp;
    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        sp = new ClasspathSourceProvider();
        classLoader = getClass().getClassLoader();
    }

    @Test
    public void testFindsTopLevelClass() throws IOException, NoSuchAlgorithmException {
        assertEquals(TOP_LEVEL_CLASS_SRC_MD5,
            FilesystemSourceProviderTest
                .md5Hex(sp.getSource(classLoader, "org/wtf/skybar/source/TopLevelClassInResources")));
    }

    @Test
    public void testFindsInnerClass() throws IOException, NoSuchAlgorithmException {
        assertEquals(TOP_LEVEL_CLASS_SRC_MD5,
            FilesystemSourceProviderTest
                .md5Hex(sp.getSource(classLoader, "org/wtf/skybar/source/TopLevelClassInResources$InnerClass")));
    }
}