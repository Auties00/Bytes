package it.auties.bytes;

import io.netty.buffer.ByteBufUtil;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
public class BufferTest {
    private static final int ITERATIONS = 1_000;

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void nettyByteBuffer() {
        var buffer = ByteBufUtil.threadLocalDirectBuffer();
        for(var iteration = 0; iteration < ITERATIONS; iteration++){
            buffer.writeBytes(new byte[iteration]);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void customByteBuffer() {
        var buffer = Bytes.newBuffer();
        for(var iteration = 0; iteration < ITERATIONS; iteration++){
            buffer.append(new byte[iteration]);
        }
    }
}
