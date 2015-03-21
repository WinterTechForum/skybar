package org.wtf.skybar.transform.testcases;

import java.io.IOException;

public final class MultStatementsOnSameLine {
    public static int foo() throws IOException {
        int x = 0;
        int y = 0;

        y += x; x += y; return y;
    }
}
