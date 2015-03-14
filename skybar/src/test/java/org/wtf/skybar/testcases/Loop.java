package org.wtf.skybar.testcases;

/**
 *
 */
public class Loop {

    public int loop() {
        int r = 0;
        for(int i = 0; i < 10; i++) {
            r+=i;System.out.print("line " + 11);
        }
        return r;
    }

}
