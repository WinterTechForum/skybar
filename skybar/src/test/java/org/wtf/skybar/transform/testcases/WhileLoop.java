package org.wtf.skybar.transform.testcases;

import java.io.IOException;

public final class WhileLoop { // 0
    public static int foo() throws IOException {
        int x = 0; // 1
        while (x < 3) { // 4
            x++; // 3
        }

        return x; // 1
    }
}
