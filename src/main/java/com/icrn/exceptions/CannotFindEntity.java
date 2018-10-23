package com.icrn.exceptions;

public class CannotFindEntity extends RuntimeException {
    public CannotFindEntity(){
        super("Cannot find an Entity matching criteria");
    }
    public CannotFindEntity(String msg){ super(msg);}
    public static CannotFindEntity foundNone(){
        return new CannotFindEntity();
    }
    public static CannotFindEntity foundNone(String user){
        return new CannotFindEntity("Cannot find an Entity matching criteria: " + user);
    }
}
