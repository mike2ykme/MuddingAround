package com.icrn.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Data
@AllArgsConstructor
public class MudCommand {
    private Actions type;
    @NonNull private Optional<String> target;
    private MudUser requester;

    public static MudCommand of(Actions type, String target, MudUser requester){
        if (type == null)
            type = Actions.BADCOMMAND;

        Optional<String> opt = null;

        if(target == null)
            opt = Optional.empty();

        else
            opt = Optional.of(target);

        return new MudCommand(type,opt,requester);
    }

    public static MudCommand parse(String request, MudUser mudUser) {
        List<String> cmds = Arrays.asList(request.split("\\s+"));

        try {
            return MudCommand.of(Actions.valueOf(cmds.get(0).toUpperCase()),cmds.get(1),mudUser);
        } catch (IllegalArgumentException e) {
            return MudCommand.of(Actions.BADCOMMAND,null,null);
        }
    }
}
