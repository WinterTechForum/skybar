package org.wtf.skybar.transform.testcases;

import java.io.IOException;

public final class ForLoopWithException { // 0
    public static int foo() throws IOException {
        int x = 0; // 1
        try {
            for (int i = 0; i < 3; i++) { // 3
                if (x == 2) { // 3
                    throw new ArithmeticException("foo"); // 1
                }
                x++; // 2
            }
        } catch (ArithmeticException e) { // 1
            return -1; // 1
        } // 0

        return x; // 0
    }
}
