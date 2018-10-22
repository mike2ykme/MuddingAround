package com.icrn.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TelnetClient {

    static final int PORT = 8080;
    static final String HOST = "localhost";
    public static void main(String... args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new TelnetClientInitializer());

            Channel ch = b.connect(HOST, PORT).sync().channel();

            ChannelFuture lastWriteFuture = null;

            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String line = in.readLine();

                if (line == null)
                    break;

                lastWriteFuture = ch.writeAndFlush(line + "\r\n");

                if ("bye".equalsIgnoreCase(line) || "shutdown".equalsIgnoreCase(line)) {
                    ch.closeFuture().sync();
                    break;
                }
            }

            if (lastWriteFuture != null)
                lastWriteFuture.sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
