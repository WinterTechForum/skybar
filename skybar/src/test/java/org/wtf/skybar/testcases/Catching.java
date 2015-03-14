package org.wtf.skybar.testcases;

/**
 *
 */
public class Catching {

    public void catching() {
        int i = 0;
        try {
            i = Integer.parseInt("boo");
            System.out.println("Fail");
        } catch (NumberFormatException e) {
            i = 7;
        }
        System.out.println("after");
        return;
    }
}
