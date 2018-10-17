package com.icrn;

import com.icrn.Controller.Mudder;
import com.icrn.dao.EntityDaoImpl;
import com.icrn.io.TelnetServer;
import com.icrn.model.RxBus;
import com.icrn.model.WorkQueue;
import com.icrn.service.StateHandler;
import io.netty.channel.Channel;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class App 
{

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main( String[] args ) throws Exception
    {
//        val dao = EntityDaoImpl.getInstance();
//        val stater = new StateHandler(dao);
        val stater = new StateHandler(new HashMap<>());
        val mudder = new Mudder(stater);

        TelnetServer server = new TelnetServer(executor,1,1,8080,null);
        Channel channel = server.startNetworking().blockingGet();
        log.info("Server has been started");
        System.out.println("STARTED");
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
        WorkQueue.getInstance().toObservable()
//                .observeOn(Schedulers.io())
//                .subscribeOn(Schedulers.io())
                .subscribe(mudCommand -> {
                    System.out.println("Thread on ToObservable of subscribe from WorkQueu: " + Thread.currentThread());
                    System.out.println(mudCommand);
                });
//        executor.execute(() -> {
//            while (true){
////                val size = WorkQueue.getInstance().getWORK_QUEUE().size();
////                System.out.println(size);
//                try {
//                    Thread.sleep(5_000);
//                    System.out.println("AFTER SNOOZE");
//                    WorkQueue.getInstance().toObservable()
//                            .subscribe(mudCommand -> {
//                                System.out.println("In SLEEP Thread on ToObservable of subscribe from WorkQueu: " + Thread.currentThread());
//                                System.out.println(mudCommand);
//                            });
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
        channel.closeFuture().sync();
        server.getBossGroup().shutdownGracefully();
//        server.bossGroup.shutdownGracefully();
        server.getWorkerGroup().shutdownGracefully();
//        server.workerGroup.shutdownGracefully();
        ((ExecutorService) executor).shutdownNow();

        System.out.println("shutdown");

    }
}
