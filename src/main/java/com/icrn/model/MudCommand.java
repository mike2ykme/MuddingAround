package com.icrn.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.Optional;

@Data
@AllArgsConstructor
public class MudCommand {
    private Actions type;
    @NonNull private Optional<String> target;
    private MudUser requester;

    public static MudCommand of(Actions type, String target, MudUser requester){
        Optional<String> opt = null;

        if(target == null)
            opt = Optional.empty();

        else
            opt = Optional.of(target);

        return new MudCommand(type,opt,requester);
    }
}
