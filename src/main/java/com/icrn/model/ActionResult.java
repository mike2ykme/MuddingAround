package com.icrn.model;

import lombok.Data;
import lombok.val;

@Data
public class ActionResult {
    private String message;
    private Boolean status;

    private ActionResult(){}

    public static ActionResult success(String s) {
        val actionResult = new ActionResult();
        actionResult.setStatus(true);
        actionResult.setMessage(s);
        return actionResult;
    }

    public static ActionResult failure(String s) {
        val actionResult = new ActionResult();
        actionResult.setStatus(false);
        actionResult.setMessage(s);
        return actionResult;
    }
}
