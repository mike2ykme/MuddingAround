package com.icrn.model;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Data
public class WorkQueue {
    private final LinkedBlockingQueue<MudCommand> WORK_QUEUE = new LinkedBlockingQueue<>();

    private WorkQueue(){}

    public Completable offerCommand(MudCommand command) {
        return Completable.create(completableEmitter -> {
            System.out.println("Thread inside of offerCommand " + Thread.currentThread());
            if(WorkQueue.getInstance().WORK_QUEUE.offer(command,1,TimeUnit.SECONDS)){
                completableEmitter.onComplete();

            } else {
                completableEmitter.onError(new RuntimeException("Unable to add element to WorkQueue"));
            }
        });
    }

    private static class LazyHolder{
        static final WorkQueue INSTANCE = new WorkQueue();
    }

    public static WorkQueue getInstance(){
        return LazyHolder.INSTANCE;
    }

    public Observable<MudCommand> toObservable(){

        return Observable.<MudCommand>create(observableEmitter -> {
            MudCommand cmd;

            while (true){
                if (observableEmitter.isDisposed())
                    break;
                cmd = WorkQueue.getInstance().WORK_QUEUE.poll(1,TimeUnit.SECONDS);
                if (cmd != null)
                    observableEmitter.onNext(cmd);
            }
            observableEmitter.onComplete();
        }).subscribeOn(Schedulers.io());
    }
}
