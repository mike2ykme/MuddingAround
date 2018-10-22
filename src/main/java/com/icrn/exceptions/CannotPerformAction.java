package com.icrn.exceptions;

public class CannotPerformAction extends RuntimeException {
    public CannotPerformAction(){super();}
    public CannotPerformAction(String msg){super(msg);}

    public static Throwable of(String msg) {
        return new CannotPerformAction(msg);
    }
}
