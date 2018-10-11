package com.icrn.io;

import com.icrn.model.Message;
import com.icrn.model.RxBus;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Date;

@Slf4j
public class TelnetServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.write("Welcome to " + InetAddress.getLocalHost().getHostName() + "!\r\n");
        ctx.write("It is " + new Date() + " now.\r\n");
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelInactive() called");
        System.out.println(ctx.channel().metadata().toString());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
        String response;
        log.debug( request);
        log.info("channelRead0");
        boolean close = false;
        if (request.isEmpty()) {
            response = "Please type something.\r\n";
        } else if ("bye".equals(request.toLowerCase())) {
            response = "Have a good day!\r\n";
            close = true;
        } else if ("shutdown".equalsIgnoreCase(request)){
            response = "Have a good day!\r\n";
            close = true;
//            ctx.channel().close();
//            ctx.channel().parent().close();
        } else {
            response = "Did you say '" + request + "'?\r\n";
        }
        // We do not need to write a ChannelBuffer here.
        // We know the encoder inserted at TelnetPipelineFactory will do the conversion.
        ChannelFuture future = ctx.write(response);
        System.out.println("IN Server Handler");
        System.out.println(Thread.currentThread().toString());
        //Need to have something that takes all these and then converts them into CMD objects
        RxBus.send(new Message("ABC","DEF",request));
        // Close the connection after sending 'Have a good day!'
        // if the client has sent 'bye'.
        if (close) {
            future.addListener(ChannelFutureListener.CLOSE);
            ChannelFuture channelFuture = ctx.channel().parent().closeFuture();
//            channelFuture.sync();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx){
        log.debug("CALLING CHANNEL READ COMPLETE FROM ServerHandler");
        log.info("CALLING CHANNEL READ COMPLETE FROM ServerHandler");
        System.out.println("CALLING CHANNEL READ COMPLETE FROM ServerHandler");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        System.out.println("exceptionCaught() called");
        cause.printStackTrace();
        ctx.close();
    }
}
