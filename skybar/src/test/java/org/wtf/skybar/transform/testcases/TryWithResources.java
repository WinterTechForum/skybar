package org.wtf.skybar.transform.testcases;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class TryWithResources { // 0
    public static int foo() throws IOException {
        int x = 0; // 1
        try (InputStream is = new FileInputStream("/dev/null")) { // 1
            x = is.read(); // 1
        } // 1

        return x; // 1
    }
}
