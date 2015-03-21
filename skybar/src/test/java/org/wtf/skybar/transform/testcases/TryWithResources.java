package org.wtf.skybar.transform.testcases;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class TryWithResources {
    public static int foo() throws IOException {
        int x = 0;
        try (InputStream is = new FileInputStream("/dev/null")) {
            x = is.read();
        }

        return x;
    }
}
