package org.wtf.skybar.transform.testcases;

public final class Conditional { // 0
    public static int foo() {
        int x = 0;  // 1
        if (x > 0) { // 1
            return x; // 0
        }

        return x + 1; // 1
    }
}
