package com.icrn.io;

import com.icrn.Controller.Mudder;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TelnetServerLoginHandler extends SimpleChannelInboundHandler<String> {
    private String username = null;
    private String password = null; // DON'T DO THIS IN ANYTHING REAL BECAUSE OF HOW Java Stores Strings. You could see the PW if everything crashes or if someone is examining a crash dump
    private boolean close = false;
    private AtomicInteger loginCount = new AtomicInteger(0);
    private final Mudder mudder;

    public TelnetServerLoginHandler(Mudder mudder) {
        this.mudder = mudder;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.write("Welcome to " + InetAddress.getLocalHost().getHostName() + "!\r\n");
        ctx.write("It is " + new Date() + " now.\r\n");
        ctx.write("Please type in your username or 'bye' to quit.\r\n");
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String input) throws Exception {
        log.info("IN CHANNEL READ 0");
        System.out.println("IN CHANNEL READ 0");
        String response = null;
        if (this.username == null){
            if (input.isEmpty()){
                   response = "Please type in your username or 'bye' to quit.\r\n";

            }else
                if ("bye".equalsIgnoreCase(input)){
                response = "Have a good day!\r\n";
                close = true;

            }else {
                response = "Please type in your password or 'bye' to quit.\r\n";
                this.username = input;

            }
        }else {
            if (input.isEmpty()){
                response = "Please type in your password or 'bye' to quit.\r\n";

            }else if ("bye".equalsIgnoreCase(input)){
                response = "Have a good day!\r\n";
                close = true;

            }else {
                this.password = input;
            }
        }

        if (this.username != null && this.password != null){
            log.info("trying to get user: " + this.username + " from login handler");
            this.mudder.maybeGetUser(this.username,this.password)
                    .subscribe(mudUser -> {
                        System.out.println(mudUser.toString());
                                this.mudder.maybeRegisterUser(mudUser,(String msg)->{
                                    ctx.writeAndFlush(msg);
                                })
                                        .subscribe(() -> {
                                            ctx.pipeline().addLast(new TelnetServerHandler(mudUser));
                                            ctx.pipeline().remove(TelnetServerLoginHandler.class);

                                            ctx.fireChannelActive();

                                        },throwable ->{
                                            System.out.println(throwable);
                                            log.info("Unable to register a user as online");
                                            throw new RuntimeException(throwable);
                                        });
                            },
                            throwable -> {
                                log.info(throwable.toString());
                                int loginNum = this.loginCount.incrementAndGet();
                                if (loginNum >2) {
                                    ctx.channel().close();
                                    log.info("Unable to login for: " + this.username);
                                    throw new RuntimeException("Unable to login for user");
                                }else {
                                    this.username = null;
                                    this.password = null;
                                    ChannelFuture channelFuture = ctx.writeAndFlush("\r\nInvalid password and or username.\r\n\r\nPlease type in your username or 'bye' to quit.\r\n");
                                    channelFuture.sync();
                                }
                            });
        }else {
            ChannelFuture future = ctx.write(response);
            System.out.println("\t\tIN Server Handler");
            System.out.println(Thread.currentThread().toString());
            if (close) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx){
        ctx.flush();
    }
}
