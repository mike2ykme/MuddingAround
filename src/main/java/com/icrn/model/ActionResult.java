package com.icrn.model;

import lombok.Data;
import lombok.val;

@Data
public class ActionResult {
    private String message;
    private Boolean status;
    private MudUser user;

    private ActionResult(){}

//    private ActionResult(String msg){
//        this.setMessage(msg);
//    }

    private ActionResult(String msg, MudUser user){
        this.setMessage(msg);
        this.setUser(user);
    }

//    public static ActionResult success(String s) {
//        val actionResult = new ActionResult(s);
//        actionResult.setStatus(true);
//        return actionResult;
//    }

    public static ActionResult success(String msg, MudUser user) {
        val ar = new ActionResult(msg,user);
        ar.setStatus(true);
        return ar;
    }

//    public static ActionResult failure(String s) {
//        val actionResult = new ActionResult();
//        actionResult.setStatus(false);
//        actionResult.setMessage(s);
//        return actionResult;
//    }

    public static ActionResult failure(String msg, MudUser user) {
        val ar = new ActionResult(msg,user);
        ar.setStatus(false);
        return ar;
    }
}
