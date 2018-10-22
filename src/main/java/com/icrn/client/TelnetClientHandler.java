package com.icrn.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.val;

public class TelnetClientHandler extends SimpleChannelInboundHandler<String> {
    String SHUTDOWN_CLIENT_SIGNAL = "SHUTDOWN_CLIENT_SIGNAL";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg.toUpperCase().contains(SHUTDOWN_CLIENT_SIGNAL)){
            System.err.println("@@@RECEIVED SHUTDOWN SIGNAL@@@");
            ChannelFuture future = ctx.channel().close();
            future.sync();
            ctx.close().sync();
            System.exit(0);
        }else {
            System.err.println(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        cause.printStackTrace();
        ctx.channel().parent().close();
        ctx.close();
    }
}
