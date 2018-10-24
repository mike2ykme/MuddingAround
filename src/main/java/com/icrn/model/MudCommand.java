package com.icrn.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
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
        log.debug(request);
        List<String> cmds = Arrays.asList(request.split("\\s+"));

        if (!cmds.get(0).toLowerCase().contains("defend") && (mudUser == null || request == null)) {
            throw new IllegalArgumentException("arguments are null");
        }

        try {
            val action = Actions.valueOf(cmds.get(0).toUpperCase());
            log.info("found action: " + action.toString());

            if (action == Actions.TALK) {
                System.out.println("MATCHES MOVE");
                StringBuilder builder = new StringBuilder();

                for (int i = 1; i < cmds.size(); i++) {
                    builder.append(cmds.get(i) + " ");

                }
                return MudCommand.of(Actions.TALK, builder.toString().trim(), mudUser);

            } else if (action == Actions.WHISPER) {
                StringBuilder builder = new StringBuilder();

                for (int i = 1; i < cmds.size(); i++) {
                    builder.append(cmds.get(i) + " ");

                }
                return MudCommand.of(action, builder.toString().trim(), mudUser);
            } else if (action == Actions.DEFEND) {

                return MudCommand.of(action, mudUser.getName(), mudUser);
            } else if (action == Actions.REST) {
                return MudCommand.of(Actions.REST, mudUser.getName(), mudUser);
            } else if (action == Actions.STATUS){
                return MudCommand.of(Actions.STATUS,mudUser.getName(),mudUser);
            }else {
                val size = cmds.size();
                log.debug("size of cmds: " + size);
                if (size >1){
                    return MudCommand.of(action,cmds.get(1),mudUser);
                }else {
                    log.info("We're creating a BADCOMMAND");
                    return MudCommand.of(Actions.BADCOMMAND,null,mudUser);
                }

            }

        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            System.out.println("Caught an illegal argument exception with MudCommand parse(), defaulting to Actions.TALK");
            return MudCommand.of(Actions.TALK,request,mudUser);

        }
    }
}
