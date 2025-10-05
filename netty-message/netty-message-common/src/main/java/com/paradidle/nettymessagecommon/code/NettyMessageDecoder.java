package com.paradidle.nettymessagecommon.code;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import com.paradidle.nettymessagecommon.NettyMessage;
import com.paradidle.nettymessagecommon.NettyMessageHeader;

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
public class NettyMessageDecoder extends LengthFieldBasedFrameDecoder {

    public NettyMessageDecoder() {
        super(Integer.MAX_VALUE, 0, 4, 0, 4);
    }


    @Override
    protected NettyMessage decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf byteBuf = (ByteBuf) super.decode(ctx, in);
        if (byteBuf == null) {
            return null;
        }

        ByteBuffer byteBuffer = byteBuf.nioBuffer();

        return NettyMessageDecoder.decode(byteBuffer);

    }


    private static NettyMessage decode(ByteBuffer byteBuffer) {

        // byteBuffer长度
        int length = byteBuffer.limit();

        // 头长度
        int headerLength = byteBuffer.getInt();

        // 头数据
        byte[] headerData = new byte[headerLength];
        byteBuffer.get(headerData);

        NettyMessageHeader nettyMessageHeader = deserialize(NettyMessageHeader.class, headerData);

        NettyMessage nettyMessage = new NettyMessage();
        nettyMessage.setHeader(nettyMessageHeader);

        // Body数据
        int bodyLength = length - 4 - headerLength;
        if(bodyLength>0) {
            byte[] bodyData = new byte[bodyLength];
            byteBuffer.get(bodyData);
            nettyMessage.setBody(deserialize(Object.class, bodyData));
        }

        return nettyMessage;

    }

    private static<T> T deserialize(Class<T> clazz, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return (T) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

}