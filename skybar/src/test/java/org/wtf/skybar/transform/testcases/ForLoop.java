package org.wtf.skybar.transform.testcases;

import java.io.IOException;

public final class ForLoop {
    public static int foo() throws IOException {
        int x = 0;
        for (int i = 0; i < 3; i++) {
            x++;
        }

        return x;
    }
}
