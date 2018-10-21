package com.icrn.exceptions;

public class CannotFindUser extends RuntimeException {
    public CannotFindUser(){
        super("Cannot find a user matching criteria");
    }
    public static CannotFindUser foundNone(){
        return new CannotFindUser();
    }
}
