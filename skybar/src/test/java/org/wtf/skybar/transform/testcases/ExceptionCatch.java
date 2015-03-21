package org.wtf.skybar.transform.testcases;

public final class ExceptionCatch {
    public static int foo() {
        int x = 1;
        try {
            x = x / 0;
            x++;
        } catch (ArithmeticException e) {
            x = 1;
        }

        return x;
    }
}
