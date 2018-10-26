package com.icrn.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttackResult {
    private StatsBasedEntity attacker;
    private StatsBasedEntity defender;
    private List<String> messageLog;

    public static AttackResult of(StatsBasedEntity attacker, StatsBasedEntity defender, List<String> log){
        return new AttackResult(attacker,defender,log);
    }
    public String getMessageLogString(){
        StringBuilder builder = new StringBuilder();

        for (String line : messageLog){
            builder.append(line);
        }
        return builder.toString();
    }
}
