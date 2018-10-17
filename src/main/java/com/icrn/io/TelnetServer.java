package com.icrn.io;

import com.icrn.Controller.Mudder;
import com.icrn.model.RxBus;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.reactivex.Single;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

@Slf4j
@Data
public class TelnetServer {

    private final ExecutorService executor;
    private final int numThreadsBoss;
    private final int numThreadsWorker;
    private final int PORT;
    private final Mudder mudder;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
//    static private final Subject<Message> MSG_BUS = PublishSubject.create();

    public TelnetServer(ExecutorService executor, int numThreadsBoss, int numThreadsWorker,int PORT,Mudder mudder){
        this.executor = executor;
        this.numThreadsBoss = numThreadsBoss;
        this.numThreadsWorker = numThreadsWorker;
        this.PORT = PORT;
        this.mudder = mudder;
    }

    public static void main(String... args)throws Exception{
//        Executor executor = Executors.newCachedThreadPool();
//        TelnetServer server = new TelnetServer(executor,1,1,8080,null);
//        Channel channel = server.startNetworking().blockingGet();
//        log.info("Server has been started");
//        System.out.println("STARTED");
//        RxBus.toObservable()
//                .subscribeOn(Schedulers.io())
//                .subscribe(message -> {
//                    log.info("NEW MESSAGE ARRIVED");
//                    System.out.println("IN TelnetServer subscribe");
//                    System.out.println(Thread.currentThread().toString());
//                    System.out.println("message.getMessage(): " + message.getMessage());
//                    System.out.println("message.getRecipient(): " + message.getRecipient());
//                    System.out.println("message.getSender(): " + message.getSender());
//                });
//
//        channel.closeFuture().sync();
//        server.bossGroup.shutdownGracefully();
//        server.workerGroup.shutdownGracefully();
//        ((ExecutorService) executor).shutdownNow();
//
//        System.out.println("shutdown");
    }

    public Single<Channel> startNetworking() {

        return Single.create(singleEmitter -> {
            this.bossGroup = new NioEventLoopGroup(numThreadsBoss,executor);
            this.workerGroup = new NioEventLoopGroup(numThreadsWorker,executor);

                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup,workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.DEBUG))
                        .childHandler(new TelnetServerInitializer(this.mudder));
                final Channel channel = bootstrap.bind(PORT).sync().channel();

            singleEmitter.onSuccess(channel);
        });

    }
}
