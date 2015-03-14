package org.wtf.skybar.testcases;

/**
 *
 */
public class Exceptional {

    public void exceptional() {
        System.out.println("before");
        int i = 1/0;
        System.out.println("after");
    }
}
