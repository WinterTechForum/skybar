package com.example.skybar.test;

import org.wtf.skybar.registry.SkybarRegistry;

/**
 *
 */
public class HelloWorld {

    public static void main(String[] args) {
        SkybarRegistry.registry.visitLine("com/example", 1);
    }
}
