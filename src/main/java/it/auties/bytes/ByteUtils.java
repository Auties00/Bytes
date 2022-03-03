package it.auties.bytes;

import java.math.BigInteger;

class ByteUtils {
    // From java.lang.Long#toUnsignedBigInteger, has private access
    protected static BigInteger toUnsignedBigInteger(long i) {
        if (i >= 0L) {
            return BigInteger.valueOf(i);
        }

        var upper = (int) (i >>> 32);
        var lower = (int) i;
        return (BigInteger.valueOf(Integer.toUnsignedLong(upper)))
                .shiftLeft(32)
                .add(BigInteger.valueOf(Integer.toUnsignedLong(lower)));
    }

    protected static byte[] toBytes(int[] array) {
        var bytes = new byte[array.length];
        for(var i = 0; i < bytes.length; i++){
            bytes[i] = (byte) array[i];
        }
        return bytes;
    }
}
