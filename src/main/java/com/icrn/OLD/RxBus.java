package com.icrn.OLD;

import com.icrn.model.Message;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class RxBus {
    static private final Subject<Message> MSG_BUS = PublishSubject.create();

    public static void send(Message msg){

        System.out.println("IN RxBus");
        System.out.println(Thread.currentThread().toString());
        MSG_BUS.onNext(msg);
    }

    public static Observable<Message> toObservable(){
        return MSG_BUS;
    }

}
