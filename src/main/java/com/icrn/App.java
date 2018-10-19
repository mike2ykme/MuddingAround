package com.icrn;

import com.icrn.controller.Mudder;
import com.icrn.io.TelnetServer;
import com.icrn.model.MudUser;
import com.icrn.model.WorkQueue;
import com.icrn.service.StateHandler;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class App 
{

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main( String[] args ) throws Exception
    {
        val stater = new StateHandler(new HashMap<>());
        stater.saveEntityState(MudUser.makeJoe()).subscribe((entity, throwable) -> {});
        val mudder = new Mudder(stater);

        TelnetServer server = new TelnetServer(executor,1,1,8080,mudder);
        Channel channel = server.startNetworking().blockingGet();
        log.info("Server has been started");
        System.out.println("STARTED");

        WorkQueue.getInstance().toObservable()
            .subscribe(mudCommand -> {
                System.out.println("Thread on ToObservable of subscribe from WorkQueue: " + Thread.currentThread());
                System.out.println(mudCommand);
            });
        System.out.println("AFTER subscribe");

        channel.closeFuture().sync();
        server.getBossGroup().shutdownGracefully();
        server.getWorkerGroup().shutdownGracefully();
        executor.shutdownNow();

        log.info("SHUTTING DOWN & exiting main()");
        System.out.println("shutdown");

    }
}
