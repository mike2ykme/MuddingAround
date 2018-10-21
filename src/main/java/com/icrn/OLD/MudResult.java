package com.icrn.OLD;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class MudResult {
    private boolean completed;
    private String msg;

    public static MudResult noActionSuccess(String msg){
        return new MudResult(true,msg);
    }
    public static MudResult noActionFailure(String msg){
        return new MudResult(false,msg);
    }
}
