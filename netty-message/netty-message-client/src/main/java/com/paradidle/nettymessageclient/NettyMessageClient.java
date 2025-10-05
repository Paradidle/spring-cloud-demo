package com.paradidle.nettymessageclient;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import com.paradidle.nettymessagecommon.code.NettyMessageEncoder;

public class NettyMessageClient {

    public static void main(String[] args) {

        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        ChannelPipeline channelPipeline = socketChannel.pipeline();
                        channelPipeline
                                .addLast(new NettyMessageEncoder())
                                .addLast(new ClientHandler());
                    }
                });

        try {
            ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress("localhost", 20007)).sync();

            channelFuture.channel().closeFuture().sync();


        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
