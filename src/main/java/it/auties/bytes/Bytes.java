package it.auties.bytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.System.arraycopy;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

public class Bytes {
    private static final String NULLABLE_MESSAGE = "Cannot create buffer from string: only non-null values are allowed inside named constructor %s(%s). " +
            "Use %s(%s) instead if you want to accept nullable values";
    private static final HexFormat HEX_CODEC = HexFormat.of();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private java.nio.ByteBuffer buffer;
    private int readerIndex;

    private Bytes(byte[] buffer){
        this.buffer = wrap(buffer);
        this.readerIndex = 0;
    }

    public static Bytes newBuffer() {
        return of(0);
    }

    public static Bytes of(int size) {
        return of(new byte[size]);
    }

    public static Bytes of(byte... input) {
        return new Bytes(input);
    }

    public static Bytes of(byte[]... input) {
        return Arrays.stream(input)
                .map(Bytes::of)
                .reduce(newBuffer(), Bytes::append);
    }

    public static Bytes of(String input) {
        Objects.requireNonNull(input, NULLABLE_MESSAGE.formatted("of", "String", "ofNullable", "String"));
        return ofNullable(input);
    }

    public static Bytes ofNullable(String input) {
        return of(input.getBytes(StandardCharsets.UTF_8));
    }

    public static Bytes ofBase64(String input) {
        Objects.requireNonNull(input, NULLABLE_MESSAGE.formatted("ofBase64", "String", "ofNullable", "ofBase64"));
        return ofNullableBase64(input);
    }

    public static Bytes ofNullableBase64(String input) {
        return of(getDecoder().decode(input));
    }

    public static Bytes ofHex(String input) {
        Objects.requireNonNull(input, NULLABLE_MESSAGE.formatted("ofHex", "String", "ofNullable", "ofHex"));
        return ofNullableHex(input);
    }

    public static Bytes ofNullableHex(String input) {
        return of(HEX_CODEC.parseHex(input));
    }

    public static Bytes ofRandom(int length) {
        var bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return of(bytes);
    }

    public Bytes prepend(Bytes array) {
        Objects.requireNonNull(array, NULLABLE_MESSAGE.formatted("prepend", "ByteBuffer", "prependNullable", "ByteBuffer"));
        return prependNullable(array);
    }

    public Bytes prependNullable(Bytes array) {
        return array == null ? copy() :
                prependNullable(array.toByteArray());
    }

    public Bytes prepend(byte... array) {
        Objects.requireNonNull(array, NULLABLE_MESSAGE.formatted("prepend", "byte...", "prependNullable", "byte..."));
        return prependNullable(array);
    }

    public Bytes prependNullable(byte... array) {
        return of(array).appendNullable(this);
    }

    public Bytes append(Bytes array) {
        Objects.requireNonNull(array, NULLABLE_MESSAGE.formatted("append", "ByteBuffer", "appendNullable", "ByteBuffer"));
        return appendNullable(array);
    }

    public Bytes appendNullable(Bytes array) {
        return array == null ? copy()
                : appendNullable(array.toByteArray());
    }

    public Bytes append(int... array) {
        return append(ByteUtils.toBytes(array));
    }

    public Bytes append(byte... array) {
        Objects.requireNonNull(array, NULLABLE_MESSAGE.formatted("append", "byte...", "appendNullable", "byte..."));
        return appendNullable(array);
    }

    public Bytes appendNullable(int... array) {
        return appendNullable(ByteUtils.toBytes(array));
    }

    public Bytes appendNullable(byte... array) {
        if(array == null){
            return copy();
        }

        var result = copyOf(toByteArray(), size() + array.length);
        arraycopy(array, 0, result, size(), array.length);
        return of(result);
    }

    public Bytes cut(int end) {
        return slice(0, end);
    }

    public Bytes slice(int start) {
        return slice(start, size());
    }

    public Bytes slice(int start, int end) {
        return of(copyOfRange(toByteArray(), start >= 0 ? start : size() + start, end >= 0 ? end : size() + end));
    }

    public Bytes fill(char value) {
        return fill((int) value);
    }

    public Bytes fill(Number value) {
        return fill(value, size());
    }

    public Bytes fill(char value, int length) {
        return fill((int) value, length);
    }

    public Bytes fill(Number value, int length) {
        var result = new byte[size()];
        for(var index = 0; index < size(); index++){
            var entry = buffer.get(index);
            result[index] = index < length && entry == 0 ? (byte) value : entry;
        }

        return of(result);
    }

    public Optional<Byte> at(int index){
        return size() <= index ? Optional.empty()
                : Optional.of(buffer.get(index));
    }

    public OptionalInt indexOf(char entry) {
        return indexOf((int) entry);
    }

    public OptionalInt indexOf(Number entry) {
        return IntStream.range(0, size())
                .filter(index -> at(index).filter(entry::equals).isPresent())
                .findFirst();
    }

    public Bytes assertSize(int size) {
        if(size != size()) {
            throw new AssertionError("Erroneous bytebuffer size: expected %s, got %s".formatted(size, size()));
        }

        return this;
    }

    public Bytes appendUnsignedByte(int input){
        return appendInt(Byte.toUnsignedInt((byte) input));
    }

    public Bytes appendShort(int input){
        var bytes = allocate(2)
                .putShort((short) input)
                .array();
        return append(bytes);
    }

    public Bytes appendUnsignedShort(short input){
        return appendInt(Short.toUnsignedInt(input));
    }

    public Bytes appendInt(int input){
        var bytes = allocate(4)
                .putInt(input)
                .array();
        return append(bytes);
    }

    public Bytes appendUnsignedInt(int input){
        return appendLong(Integer.toUnsignedLong(input));
    }

    public Bytes appendLong(long input){
        var bytes = allocate(8)
                .putLong(input)
                .array();
        return append(bytes);
    }

    public Bytes appendUnsignedLong(long input){
        return appendBigInt(ByteUtils.toUnsignedBigInteger(input));
    }

    public Bytes appendBigInt(BigInteger input) {
        Objects.requireNonNull(input, NULLABLE_MESSAGE.formatted("writeBigInt", "BigInteger,int", "writeBigInt", "BigInteger,int"));
        return appendNullableBigInt(input);
    }

    public Bytes appendNullableBigInt(BigInteger input) {
        return append(input.toByteArray());
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

    public Bytes readBuffer(int length) {
        return of(readBytes(readerIndex, length));
    }

    public Bytes readBuffer(int index, int length) {
        return of(readBytes(index, length));
    }

    public byte[] readBytes(int length) {
        return readBytes(readerIndex, length);
    }

    public byte[] readBytes(int index, int length) {
        var result = new byte[length];
        buffer.get(index, result);
        if(index == readerIndex) {
            step(length);
        }

        return result;
    }

    private void step(int delta){
        this.readerIndex = readerIndex + delta;
    }

    public int size(){
        return buffer.capacity();
    }

    public boolean isReadable(){
        return size() - readerIndex > 0;
    }

    public byte[] toByteArray(){
        return buffer.array();
    }

    public java.nio.ByteBuffer toNioBuffer(){
        return wrap(toByteArray());
    }

    public Bytes copy(){
        var bytes = toByteArray();
        return of(Arrays.copyOf(bytes, bytes.length));
    }

    public Bytes setBytes(byte[] input){
        Objects.requireNonNull(input, NULLABLE_MESSAGE.formatted("setBytes", "byte[]", "setNullableBytes", "byte[]"));
        return setNullableBytes(input);
    }

    public Bytes setNullableBytes(byte[] input){
        this.buffer = wrap(input);
        this.readerIndex = 0;
        return this;
    }

    public Bytes clear(){
        for(var index = 0; index < size(); index++){
            buffer.put(index, (byte) 0);
        }

        this.readerIndex = 0;
        return this;
    }

    public Bytes remaining(){
        return of(readBytes(readerIndex, size() - readerIndex));
    }

    public int readableBytes(){
        return Math.max(size() - readerIndex, 0);
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
                || (other instanceof Bytes that && Arrays.equals(toByteArray(), that.toByteArray()));
    }

    public boolean contentEquals(Object other){
        return equals(other)
                || (other instanceof java.nio.ByteBuffer thatNioBuffer && thatNioBuffer.equals(toNioBuffer()))
                || (other instanceof byte[] thoseBytes && Arrays.equals(thoseBytes, toByteArray()));
    }
}
