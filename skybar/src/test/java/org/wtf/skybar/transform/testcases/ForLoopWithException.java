package org.wtf.skybar.transform.testcases;

import java.io.IOException;

public final class ForLoopWithException {
    public static int foo() throws IOException {
        int x = 0;
        try {
            for (int i = 0; i < 3; i++) {
                if (x == 2) {
                    throw new ArithmeticException("foo");
                }
                x++;
            }
        } catch (ArithmeticException e) {
            return -1;
        }

        return x;
    }
}
