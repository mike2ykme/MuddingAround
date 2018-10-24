package com.icrn;

import com.icrn.controller.FrontController;
import com.icrn.io.TelnetServer;
import com.icrn.model.MudUser;
import com.icrn.model.Room;
import com.icrn.service.SimpleHandlerImpl;
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
        System.out.println(
                stater.saveEntityState(new Room(0L)).blockingGet());
        val joe = MudUser.makeJoe();
        joe.setOnline(false);
        stater.saveEntityState(joe).subscribe((entity, throwable) -> {System.out.println(entity);});
        val mike = MudUser.makeJoe();
        mike.setName("mike");
        mike.setId(2L);
        mike.setPassword("MIKE");
        mike.setOnline(false);
        System.out.println(
                stater.saveEntityState(mike).blockingGet());

        val attackHandler = new SimpleHandlerImpl();
        val controller = new FrontController(stater,attackHandler,attackHandler);
        TelnetServer server = new TelnetServer(executor,1,1,8080,controller);
        Channel channel = server.startNetworking().blockingGet();
        log.info("Server has been started");
        System.out.println("STARTED");

        System.out.println("AFTER subscribe");

        channel.closeFuture().sync();
        server.getBossGroup().shutdownGracefully();
        server.getWorkerGroup().shutdownGracefully();
        executor.shutdownNow();

        log.info("SHUTTING DOWN & exiting main()");
        System.out.println("shutdown");

    }
}
