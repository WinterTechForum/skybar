package org.wtf.skybar.transform.testcases;

public final class ExceptionCatch { // 0
    public static int foo() {
        int x = 1; // 1
        try {
            x = x / 0; // 1
            x++; // 0
        } catch (ArithmeticException e) { // 1
            x = 1; // 1
        } // 0

        return x; //1
    }
}
