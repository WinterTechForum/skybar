package org.wtf.skybar.transform.testcases;

public final class Conditional {
    public static int foo() {
        int x = 0;
        if (x > 0) {
            return x;
        }

        return x + 1;
    }
}
