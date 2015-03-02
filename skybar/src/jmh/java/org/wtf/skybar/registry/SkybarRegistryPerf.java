package org.wtf.skybar.registry;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class SkybarRegistryPerf {

    @Benchmark
    public int benchFirst() {
        return 1;
    }
}
