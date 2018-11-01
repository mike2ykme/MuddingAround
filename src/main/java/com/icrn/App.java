package com.icrn;

import com.icrn.controller.FrontController;
import com.icrn.controller.NpcController;
import com.icrn.io.TelnetServer;
import com.icrn.model.MudUser;
import com.icrn.model.Room;
import com.icrn.service.SimpleHandlerImpl;
import com.icrn.service.StateHandler;
import io.netty.channel.Channel;
import io.reactivex.Observable;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class App 
{

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main( String[] args ) throws Exception
    {
        val room0 = new Room(0L);
        room0.setSafeZone(true);
//
//        val room1 = new Room(100L);
//        room1.setSafeZone(false);

        val stater = new StateHandler(new ConcurrentHashMap<>());
        System.out.println(
                stater.saveEntityState(room0).blockingGet());

        System.out.println(
                stater.saveEntityState(Room.makeTrapRoom()).blockingGet());

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
        val npcController = new NpcController(stater,attackHandler);

        TelnetServer server = new TelnetServer(executor,1,1,8080,controller);
        Channel channel = server.startNetworking().blockingGet();
        log.info("Server has been started");
        System.out.println("STARTED");

//        System.out.println("AFTER subscribe");

        System.out.println("STARING NPC TICK");
        Observable.interval(3, TimeUnit.SECONDS)
                .subscribe(aLong -> {
                    log.info("STARTING TICK");
                    npcController.processTick()
                            .subscribe(() -> {
                                log.info("FINISHED PROCESSING TICK");
                            },throwable -> log.error(throwable.getMessage()));
                },throwable -> log.error(throwable.getMessage()));
        channel.closeFuture().sync();
        server.getBossGroup().shutdownGracefully();
        server.getWorkerGroup().shutdownGracefully();
        executor.shutdownNow();

        log.info("SHUTTING DOWN & exiting main()");
        System.out.println("shutdown");

    }
}
