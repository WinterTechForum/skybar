package org.wtf.skybar.transform.testcases;

import java.io.IOException;

public final class ForLoop { // 0
    public static int foo() throws IOException {
        int x = 0; // 1
        for (int i = 0; i < 3; i++) { // 4
            x++; // 3
        }

        return x; // 1
    }
}
