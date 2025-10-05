package com.paradidle.nettymessageclient;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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
public class ClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        NettyMessage nettyMessage = new NettyMessage();
        nettyMessage.setHeader(new NettyMessageHeader().setVersion("1.0").setLang("zh")).setBody("Hello World");

        ctx.writeAndFlush(nettyMessage);

        System.out.println("ClientHandler.channelActive");
    }

}
