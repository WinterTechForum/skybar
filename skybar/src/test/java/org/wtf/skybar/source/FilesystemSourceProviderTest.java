package org.wtf.skybar.source;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class FilesystemSourceProviderTest {

    // just to have a convenient way of making sure we get the right thing
    private static final String TOP_LEVEL_CLASS_SRC_MD5 = "70e823ac0cb5220e6ad8e5fe3548910a";

    SourceProvider sp;

    @Before
    public void setUp() throws Exception {
        Path gradlePath = Paths.get("src", "test", "java");
        Path intellijPath = Paths.get("skybar").resolve(gradlePath);
        // hackily detect if we're running from intellij or gradle
        if (intellijPath.toFile().exists()) {
            sp = new FilesystemSourceProvider(intellijPath);
        } else if (gradlePath.toFile().exists()) {
            sp = new FilesystemSourceProvider(gradlePath);
        } else {
            throw new RuntimeException("Can't find source, current dir is " + new File(".").getCanonicalPath());
        }
    }

    @Test
    public void testCantFindMissingClass() throws IOException {
        assertNull(sp.getSource("no/such/class"));
    }

    @Test
    public void testCantFindDirectory() throws IOException {
        assertNull(sp.getSource("org/wtf/skybar"));
    }

    @Test
    public void testFindsTopLevelClass() throws IOException, NoSuchAlgorithmException {
        assertEquals(TOP_LEVEL_CLASS_SRC_MD5, md5Hex(sp.getSource("org/wtf/skybar/source/TopLevelClass.java")));
    }

    static String md5Hex(String input) throws NoSuchAlgorithmException {
        assertNotNull(input);
        byte[] md5bytes = MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder accum = new StringBuilder();
        for (byte b : md5bytes) {
            accum.append(String.format("%02X", b));
        }

        return accum.toString().toLowerCase(Locale.US);
    }
}
