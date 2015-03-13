package com.example.skybar.test;

import org.wtf.skybar.registry.SkybarRegistry;

/**
 *
 */
public class HelloWorld {

    public static void main(String[] args) {
        SkybarRegistry.registry.registerLine("foo", 1);
    }
}
