package com.icrn.io;

import com.icrn.controller.FrontController;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
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
    private final FrontController controller;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public TelnetServer(ExecutorService executor, int numThreadsBoss, int numThreadsWorker, int PORT, FrontController controller){
        this.executor = executor;
        this.numThreadsBoss = numThreadsBoss;
        this.numThreadsWorker = numThreadsWorker;
        this.PORT = PORT;
        this.controller = controller;
    }

    public Single<Channel> startNetworking() {

        return Single.create(singleEmitter -> {
            this.bossGroup = new NioEventLoopGroup(numThreadsBoss,executor);
            this.workerGroup = new NioEventLoopGroup(numThreadsWorker,executor);

                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup,workerGroup)
                        .channel(NioServerSocketChannel.class)
//                        .handler(new LoggingHandler(LogLevel.DEBUG))
                        .childHandler(new TelnetServerInitializer(this.controller));
                final Channel channel = bootstrap.bind(PORT).sync().channel();

            singleEmitter.onSuccess(channel);
        });

    }
}
