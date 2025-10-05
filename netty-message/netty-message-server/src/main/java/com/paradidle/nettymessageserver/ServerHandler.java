package com.paradidle.nettymessageserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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
public class ServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage nettyMessage = (NettyMessage) msg;
        System.out.println("收到客户端信息:" + nettyMessage.toString());

        super.channelRead(ctx, msg);
    }

}
