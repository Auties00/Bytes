package it.auties.buffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.System.arraycopy;
import static java.nio.ByteBuffer.wrap;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

public class ByteBuffer {
    private static final String NULLABLE_MESSAGE = "Cannot create buffer from string: only non-null values are allowed inside named constructor %s(%s). " +
            "Use %s(%s) instead if you want to accept nullable values";
    private static final HexFormat HEX_CODEC = HexFormat.of();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int DEFAULT_EXPAND_SIZE = 32;
    private static final int DEFAULT_EXPAND_MULTIPLIER = 4;

    private byte[] buffer;
    private int readerIndex;
    private int writerIndex;

    private ByteBuffer(byte[] buffer, boolean moveToEnd){
        this.buffer = buffer;
        this.readerIndex = 0;
        this.writerIndex = moveToEnd ? buffer.length : 0;
    }

    public static ByteBuffer newBuffer() {
        return allocate(DEFAULT_EXPAND_SIZE * DEFAULT_EXPAND_MULTIPLIER);
    }

    public static ByteBuffer empty() {
        return of(0);
    }

    public static ByteBuffer of(int size) {
        return of(new byte[size]);
    }

    public static ByteBuffer of(byte... input) {
        return new ByteBuffer(input, true);
    }

    public static ByteBuffer of(byte[]... input) {
        return Arrays.stream(input)
                .map(ByteBuffer::of)
                .reduce(empty(), ByteBuffer::append);
    }

    public static ByteBuffer of(String input) {
        Objects.requireNonNull(input, NULLABLE_MESSAGE.formatted("of", "String", "ofNullable", "String"));
        return ofNullable(input);
    }

    public static ByteBuffer ofNullable(String input) {
        return of(input.getBytes(StandardCharsets.UTF_8));
    }

    public static ByteBuffer ofBase64(String input) {
        Objects.requireNonNull(input, NULLABLE_MESSAGE.formatted("ofBase64", "String", "ofNullable", "ofBase64"));
        return ofNullableBase64(input);
    }

    public static ByteBuffer ofNullableBase64(String input) {
        return of(getDecoder().decode(input));
    }

    public static ByteBuffer ofHex(String input) {
        Objects.requireNonNull(input, NULLABLE_MESSAGE.formatted("ofHex", "String", "ofNullable", "ofHex"));
        return ofNullableHex(input);
    }

    public static ByteBuffer ofNullableHex(String input) {
        return of(HEX_CODEC.parseHex(input));
    }

    public static ByteBuffer random(int length) {
        var bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return of(bytes);
    }

    public static ByteBuffer allocate(int length) {
        return new ByteBuffer(new byte[length], false);
    }

    public ByteBuffer prepend(ByteBuffer array) {
        Objects.requireNonNull(array, NULLABLE_MESSAGE.formatted("prepend", "ByteBuffer", "prependNullable", "ByteBuffer"));
        return prependNullable(array);
    }

    public ByteBuffer prependNullable(ByteBuffer array) {
        return array == null ? copy() :
                prependNullable(array.toByteArray());
    }

    public ByteBuffer prepend(byte... array) {
        Objects.requireNonNull(array, NULLABLE_MESSAGE.formatted("prepend", "byte...", "prependNullable", "byte..."));
        return prependNullable(array);
    }

    public ByteBuffer prependNullable(byte... array) {
        return of(array).appendNullable(this);
    }

    public ByteBuffer append(ByteBuffer array) {
        Objects.requireNonNull(array, NULLABLE_MESSAGE.formatted("append", "ByteBuffer", "appendNullable", "ByteBuffer"));
        return appendNullable(array);
    }

    public ByteBuffer appendNullable(ByteBuffer array) {
        return array == null ? copy()
                : appendNullable(array.toByteArray());
    }

    public ByteBuffer append(byte... array) {
        Objects.requireNonNull(array, NULLABLE_MESSAGE.formatted("append", "byte...", "appendNullable", "byte..."));
        return appendNullable(array);
    }

    public ByteBuffer appendNullable(byte... array) {
        if(array == null){
            return copy();
        }

        var result = copyOf(toByteArray(), size() + array.length);
        arraycopy(array, 0, result, size(), array.length);
        return of(result);
    }

    public ByteBuffer cut(int end) {
        return slice(0, end);
    }

    public ByteBuffer slice(int start) {
        return slice(start, size());
    }

    public ByteBuffer slice(int start, int end) {
        return of(copyOfRange(toByteArray(), start >= 0 ? start : size() + start, end >= 0 ? end : size() + end));
    }

    public ByteBuffer fill(char value) {
        return fill((int) value);
    }

    public ByteBuffer fill(Number value) {
        return fill(value, size());
    }

    public ByteBuffer fill(char value, int length) {
        return fill((int) value, length);
    }

    public ByteBuffer fill(Number value, int length) {
        var result = new byte[size()];
        for(var index = 0; index < size(); index++){
            var entry = buffer[index];
            result[index] = index < length && entry == 0 ? (byte) value : entry;
        }

        return of(result);
    }

    public Optional<Byte> at(int index){
        return size() <= index ? Optional.empty()
                : Optional.of(buffer[index]);
    }

    public OptionalInt indexOf(char entry) {
        return indexOf((int) entry);
    }

    public OptionalInt indexOf(Number entry) {
        return IntStream.range(0, size())
                .filter(index -> at(index).filter(entry::equals).isPresent())
                .findFirst();
    }

    public ByteBuffer assertSize(int size) {
        if(size != size()) {
            throw new AssertionError("Erroneous bytebuffer size: expected %s, got %s".formatted(size, size()));
        }

        return this;
    }

    public ByteBuffer writeByte(int input){
        return writeByte(input, writerIndex);
    }
    
    public ByteBuffer writeByte(int input, int index){
        return writeBytes(new byte[]{(byte) input}, index);
    }

    public ByteBuffer writeUnsignedByte(int input){
        return writeUnsignedByte(input, writerIndex);
    }

    public ByteBuffer writeUnsignedByte(int input, int index){
        return writeInt(Byte.toUnsignedInt((byte) input), index);
    }

    public ByteBuffer writeShort(int input){
        return writeShort(input, writerIndex);
    }

    public ByteBuffer writeShort(int input, int index){
        var bytes = java.nio.ByteBuffer.allocate(2)
                .putShort((short) input)
                .array();
        writeBytes(bytes, index);
        return this;
    }

    public ByteBuffer writeUnsignedShort(short input){
        return writeUnsignedShort(input, writerIndex);
    }

    public ByteBuffer writeUnsignedShort(short input, int index){
        return writeInt(Short.toUnsignedInt(input), index);
    }

    public ByteBuffer writeInt(int input){
        return writeInt(input, writerIndex);
    }

    public ByteBuffer writeInt(int input, int index){
        var bytes = java.nio.ByteBuffer.allocate(4)
                .putInt(input)
                .array();
        writeBytes(bytes, index);
        return this;
    }

    public ByteBuffer writeUnsignedInt(int input){
        return writeUnsignedInt(input, writerIndex);
    }

    public ByteBuffer writeUnsignedInt(int input, int index){
        return writeLong(Integer.toUnsignedLong(input), index);
    }

    public ByteBuffer writeLong(long input){
        return writeLong(input, writerIndex);
    }

    public ByteBuffer writeLong(long input, int index){
        var bytes = java.nio.ByteBuffer.allocate(8)
                .putLong(input)
                .array();
        writeBytes(bytes, index);
        return this;
    }

    public ByteBuffer writeUnsignedLong(long input){
        return writeUnsignedLong(input, writerIndex);
    }

    public ByteBuffer writeUnsignedLong(long input, int index){
        return writeBigInt(toUnsignedBigInteger(input), index);
    }

    // From java.lang.Long#toUnsignedBigInteger, has private access
    private BigInteger toUnsignedBigInteger(long i) {
        if (i >= 0L) {
            return BigInteger.valueOf(i);
        }
        
        var upper = (int) (i >>> 32);
        var lower = (int) i;
        return (BigInteger.valueOf(Integer.toUnsignedLong(upper)))
                .shiftLeft(32)
                .add(BigInteger.valueOf(Integer.toUnsignedLong(lower)));
    }

    public ByteBuffer writeBigInt(BigInteger input) {
        return writeBigInt(input, writerIndex);
    }

    public ByteBuffer writeBigInt(BigInteger input, int index) {
        Objects.requireNonNull(input, NULLABLE_MESSAGE.formatted("writeBigInt", "BigInteger,int", "writeBigInt", "BigInteger,int"));
        return writeNullableBigInt(input, index);
    }

    public ByteBuffer writeNullableBigInt(BigInteger input) {
        return writeNullableBigInt(input, writerIndex);
    }

    public ByteBuffer writeNullableBigInt(BigInteger input, int index) {
        writeBytes(input.toByteArray(), index);
        return this;
    }

    public ByteBuffer writeBytes(byte[] bytes) {
        return writeBytes(bytes, writerIndex);
    }

    public ByteBuffer writeBytes(byte[] bytes, int index) {
        Objects.requireNonNull(bytes, NULLABLE_MESSAGE.formatted("writeBytes", "byte[],int", "writeNullableBytes", "byte[],int"));
        return writeNullableBytes(bytes, index);
    }

    public ByteBuffer writeNullableBytes(byte[] bytes, int index) {
        checkEOS(index, bytes.length);
        for(int inputIndex = 0, bufferIndex = index; inputIndex < bytes.length; inputIndex++, bufferIndex++){
            buffer[bufferIndex] = bytes[inputIndex];
        }

        step(bytes.length, false);
        return this;
    }

    public byte readByte(){
        return readByte(readerIndex);
    }

    public byte readByte(int index){
        return readBytes(index, 1)[0];
    }

    public int readUnsignedByte(){
        return readUnsignedByte(readerIndex);
    }

    public int readUnsignedByte(int index){
        return Byte.toUnsignedInt(readByte(index));
    }

    public short readShort(){
        return readShort(readerIndex);
    }

    public short readShort(int index){
        return readBigInt(index, 2).shortValueExact();
    }

    public int readUnsignedShort(){
        return readUnsignedShort(readerIndex);
    }

    public int readUnsignedShort(int index){
        return Short.toUnsignedInt(readShort(index));
    }

    public int readInt(){
        return readInt(readerIndex);
    }

    public int readInt(int index){
        return readBigInt(index, 4).intValueExact();
    }

    public long readUnsignedInt(){
        return readUnsignedInt(readerIndex);
    }

    public long readUnsignedInt(int index){
        return Integer.toUnsignedLong(readInt(index));
    }

    public long readLong(){
        return readLong(readerIndex);
    }

    public long readLong(int index){
        return readBigInt(index, 8).longValueExact();
    }

    public BigInteger readUnsignedLong(){
        return readUnsignedLong(readerIndex);
    }

    public BigInteger readUnsignedLong(int index){
        return new BigInteger(1, readBigInt(index, 8).toByteArray());
    }

    public BigInteger readBigInt(int length) {
        return readBigInt(readerIndex, length);
    }

    public BigInteger readBigInt(int index, int length) {
        return new BigInteger(readBytes(index, length));
    }

    public ByteBuffer readBuffer(int length) {
        return of(readBytes(readerIndex, length));
    }

    public ByteBuffer readBuffer(int index, int length) {
        return of(readBytes(index, length));
    }

    public byte[] readBytes(int length) {
        return readBytes(readerIndex, length);
    }

    public byte[] readBytes(int index, int length) {
        var result = new byte[length];
        System.arraycopy(buffer, index, result, 0, length);
        step(length, true);
        return result;
    }

    private void checkEOS(int index, int delta){
        if (index + delta < size()) {
            return;
        }

        var reservedSpace = Math.max(delta, DEFAULT_EXPAND_SIZE) + (DEFAULT_EXPAND_SIZE * DEFAULT_EXPAND_MULTIPLIER);
        this.buffer = copyOf(buffer, size() + reservedSpace);
    }

    private int step(int delta, boolean read){
        var oldCounter = read ? readerIndex : writerIndex;
        if(read){
            this.readerIndex = oldCounter + delta;
        }else {
            this.writerIndex = oldCounter + delta;
        }

        return oldCounter;
    }

    public int size(){
        return buffer.length;
    }

    public int readableBytes(){
        return Math.max(size() - readerIndex, 0);
    }

    public int writableBytes(){
        return Math.max(size() - writerIndex, 0);
    }

    public boolean isReadable(){
        return size() - readerIndex > 0;
    }

    public boolean isWritable(){
        return size() - writerIndex > 0;
    }

    public byte[] toByteArray(){
        return Arrays.copyOfRange(buffer, 0, writerIndex);
    }

    public java.nio.ByteBuffer toNioBuffer(){
        return wrap(toByteArray());
    }

    public ByteBuffer copy(){
        var bytes = toByteArray();
        return of(Arrays.copyOf(bytes, bytes.length));
    }

    public ByteBuffer clear(){
        for(var index = 0; index < size(); index++){
            buffer[index] = 0;
        }

        this.readerIndex = 0;
        this.writerIndex = 0;
        return this;
    }

    public ByteBuffer remaining(){
        return of(readBytes(readerIndex, size() - readerIndex));
    }

    @Override
    public String toString() {
        return new String(toByteArray(), StandardCharsets.UTF_8);
    }

    public ByteArrayOutputStream toOutputStream() throws IOException {
        var stream = new ByteArrayOutputStream(size());
        stream.write(toByteArray());
        return stream;
    }

    public String toHex() {
        return HEX_CODEC.formatHex(toByteArray());
    }

    public String toBase64(){
        return getEncoder().encodeToString(toByteArray());
    }
    
    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof ByteBuffer that && Arrays.equals(toByteArray(), that.toByteArray()));
    }

    public boolean contentEquals(Object other){
        return equals(other)
                || (other instanceof java.nio.ByteBuffer thatNioBuffer && thatNioBuffer.equals(toNioBuffer()))
                || (other instanceof byte[] thoseBytes && Arrays.equals(thoseBytes, toByteArray()));
    }
}
