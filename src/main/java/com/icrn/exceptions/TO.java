package com.icrn.exceptions;

public class TO extends RuntimeException {
    public TO(){
        super("This method is not implemented yet!");
    }
//    public static TO DO(){
//        return new TO();
//    }
    public static void DO(){
        throw new TO();
    }
}
