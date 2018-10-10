package com.icrn.io;

import com.icrn.model.Message;
import com.icrn.model.RxBus;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelnetServer {

    private final Executor executor;
    private final int numThreadsBoss;
    private final int numThreadsWorker;
    private final int PORT;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
//    static private final Subject<Message> MSG_BUS = PublishSubject.create();

    public TelnetServer(Executor executor, int numThreadsBoss, int numThreadsWorker,int PORT){
        this.executor = executor;
        this.numThreadsBoss = numThreadsBoss;
        this.numThreadsWorker = numThreadsWorker;
        this.PORT = PORT;
    }

    public static void main(String... args)throws Exception{
        Executor executor = Executors.newCachedThreadPool();
        TelnetServer server = new TelnetServer(executor,1,1,8080);

        Channel channel = server.startNetworking().blockingGet();
        RxBus.toObservable()
                .subscribeOn(Schedulers.io())
                .subscribe(message -> {
                    System.out.println("IN TelnetServer subscribe");
                    System.out.println(Thread.currentThread().toString());
                    System.out.println("message.getMessage(): " + message.getMessage());
                    System.out.println("message.getRecipient(): " + message.getRecipient());
                    System.out.println("message.getSender(): " + message.getSender());
                });
        System.out.println("TEST");

        channel.closeFuture().sync();
        server.bossGroup.shutdownGracefully();
        server.workerGroup.shutdownGracefully();
        ((ExecutorService) executor).shutdownNow();

        System.out.println("shutdown");
    }

    public Single<Channel> startNetworking() {

        return Single.create(singleEmitter -> {
            this.bossGroup = new NioEventLoopGroup(numThreadsBoss,executor);
            this.workerGroup = new NioEventLoopGroup(numThreadsWorker,executor);

                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup,workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.DEBUG))
                        .childHandler(new TelnetServerInitializer());
                final Channel channel = bootstrap.bind(PORT).sync().channel();

//            observableEmitter.onNext(channel);
            singleEmitter.onSuccess(channel);
        });

    }
}
