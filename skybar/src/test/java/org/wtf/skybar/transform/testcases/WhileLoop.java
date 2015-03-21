package org.wtf.skybar.transform.testcases;

import java.io.IOException;

public final class WhileLoop {
    public static int foo() throws IOException {
        int x = 0;
        while (x < 3) {
            x++;
        }

        return x;
    }
}
