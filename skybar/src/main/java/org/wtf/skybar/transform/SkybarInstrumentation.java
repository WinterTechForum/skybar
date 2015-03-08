package org.wtf.skybar.transform;

/**
 *
 */
public interface SkybarInstrumentation {
    void onLineNumber(int lineNumber);


    void visitMaxs(int maxStack, int maxLocals);

}
