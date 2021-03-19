/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.housepower.io;

import com.github.housepower.misc.ExceptionUtil.CheckedBiConsumer;
import com.github.housepower.misc.ExceptionUtil.CheckedFunction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteBufHelperTest implements ByteBufHelper {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testBoolean(boolean value) throws Exception {
        testSerde(value,
                CompositeSink::writeBoolean, CompositeSource::readBoolean,
                ByteBuf::writeBoolean, ByteBuf::readBoolean);
    }

    @ParameterizedTest
    @ValueSource(bytes = {0, 1, Byte.MIN_VALUE, Byte.MAX_VALUE})
    void testByte(byte value) throws Exception {
        testSerde(value,
                CompositeSink::writeByte, CompositeSource::readByte,
                ((CheckedBiConsumer<ByteBuf, Byte>) ByteBuf::writeByte), ByteBuf::readByte);
    }

    @ParameterizedTest
    @ValueSource(shorts = {0, 1, Short.MIN_VALUE, Short.MAX_VALUE})
    void testShort(short value) throws Exception {
        testSerde(value,
                CompositeSink::writeShortLE, CompositeSource::readShortLE,
                ((CheckedBiConsumer<ByteBuf, Short>) ByteBuf::writeShortLE), ByteBuf::readShortLE);
    }


    @ParameterizedTest
    @ValueSource(ints = {0, 1, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void testInt(int value) throws Exception {
        testSerde(value,
                CompositeSink::writeIntLE, CompositeSource::readIntLE,
                ByteBuf::writeIntLE, ByteBuf::readIntLE);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, Long.MIN_VALUE, Long.MAX_VALUE})
    void testLong(long value) throws Exception {
        testSerde(value,
                CompositeSink::writeLongLE, CompositeSource::readLongLE,
                ByteBuf::writeLongLE, ByteBuf::readLongLE);
    }

    @ParameterizedTest
    @ValueSource(floats = {0.0F, 1.1F, Float.MIN_VALUE, Float.MAX_VALUE, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY})
    void testFloat(float value) throws Exception {
        testSerde(value,
                CompositeSink::writeFloatLE, CompositeSource::readFloatLE,
                ByteBuf::writeFloatLE, ByteBuf::readFloatLE);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 1.1, Double.MIN_VALUE, Double.MAX_VALUE, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
    void testDouble(double value) throws Exception {
        testSerde(value,
                CompositeSink::writeDoubleLE, CompositeSource::readDoubleLE,
                ByteBuf::writeDoubleLE, ByteBuf::readDoubleLE);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, Integer.MAX_VALUE, Long.MAX_VALUE})
    void testVarInt(long value) throws Exception {
        testSerde(value,
                CompositeSink::writeVarInt, CompositeSource::readVarInt,
                this::writeVarInt, this::readVarInt);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "哈哈", "😝"})
    void testUTF8Binary(String value) throws Exception {
        testSerde(value,
                CompositeSink::writeUTF8Binary, CompositeSource::readUTF8Binary,
                this::writeUTF8Binary, this::readUTF8Binary);
    }

    private <T> void testSerde(T value,
                               CheckedBiConsumer<CompositeSink, T> legacySerialize,
                               CheckedFunction<CompositeSource, T> legacyDeserialize,
                               CheckedBiConsumer<ByteBuf, T> nettySerialize,
                               CheckedFunction<ByteBuf, T> nettyDeserialize) throws Exception {
        ByteBufSink memoryWriter = new ByteBufSink();
        CompositeSink serializer = new CompositeSink(memoryWriter, false, null);
        legacySerialize.accept(serializer, value);
        ByteBuf legacyBuf = memoryWriter.retain();
        ByteBuf nettyBuf = heapBuf();
        nettySerialize.accept(nettyBuf, value);
        assertEquals(legacyBuf, nettyBuf);

        CompositeSource deserializer = new CompositeSource(new ByteBufSource(legacyBuf), false);
        assertEquals(value, legacyDeserialize.apply(deserializer));
        assertEquals(value, nettyDeserialize.apply(nettyBuf));

        ReferenceCountUtil.release(legacyBuf);
        ReferenceCountUtil.release(nettyBuf);
    }

    private ByteBuf heapBuf() {
        return ByteBufAllocator.DEFAULT.heapBuffer();
    }
}