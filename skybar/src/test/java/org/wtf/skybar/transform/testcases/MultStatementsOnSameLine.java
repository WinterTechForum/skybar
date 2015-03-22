package org.wtf.skybar.transform.testcases;

import java.io.IOException;

public final class MultStatementsOnSameLine { // 0
    public static int foo() throws IOException {
        int x = 0; // 1
        int y = 0; // 1

        y += x; x += y; return y; // 1
    }
}
