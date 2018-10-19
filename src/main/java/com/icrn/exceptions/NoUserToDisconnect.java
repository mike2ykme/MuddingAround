package com.icrn.exceptions;

public class NoUserToDisconnect extends RuntimeException {
    public NoUserToDisconnect(){
        super("Found no user to disconnect");
    }
//    public NoUserToDisconnect(String msg){ super(msg);}
    public static NoUserToDisconnect foundNone(){
        return new NoUserToDisconnect();
    }
}