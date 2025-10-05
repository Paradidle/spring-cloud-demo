package com.paradidle.nettymessagecommon.code;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import com.paradidle.nettymessagecommon.NettyMessage;

/**
 * <p>
 *
 * </p>
 *
 * <p>
 * Copyright: 2025 . All rights reserved.
 * </p>
 * <p>
 * Company: Zsoft
 * </p>
 * <p>
 * CreateDate:2025/10/2
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2025/10/2；
 */
public class NettyMessageEncoder extends MessageToByteEncoder<NettyMessage> {
    @Override
    public void encode(ChannelHandlerContext ctx, NettyMessage msg, ByteBuf out) throws Exception {
        if (msg == null || msg.getHeader() == null) {
            throw new Exception("The encode message is null");
        }

        ByteBuffer byteBuffer = encodeHeader(msg);
        out.writeBytes(byteBuffer);

        byte[] body = serializable(msg.getBody());
        if(Objects.nonNull(body)){
            out.writeBytes(body);
        }

    }


    private ByteBuffer encodeHeader(NettyMessage msg) {

        int length = 4;

        byte[] header = serializable(msg.getHeader());
        int headerLength = header.length;
        length += headerLength;

        byte[] body = serializable(msg.getBody());
        int bodyLength = 0;
        if(Objects.nonNull(body)){
            bodyLength = body.length;
            length += bodyLength;
        }

        // 总长度 = header长度(4字节) + header数据 + body数据
        ByteBuffer buffer = ByteBuffer.allocate(4 + length - bodyLength);
        buffer.putInt(length);
        buffer.putInt(headerLength);  // 写入header长度
        buffer.put(header);           // 写入header数据

        buffer.flip();

        return buffer;
    }


    private static <T> byte[] serializable(T object) {
        if (object == null) {
            return null;
        }
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(object);
            oos.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}
