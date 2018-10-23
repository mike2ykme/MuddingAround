package com.icrn.service;

import com.icrn.model.AttackResult;
import com.icrn.model.StatsBasedEntity;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@Slf4j
public class SimpleAttackHandler implements AttackHandler {
    private static BiFunction<StatsBasedEntity,StatsBasedEntity,Integer> attackFunction = (attacker, defender) -> {

        val damageDealt = (-1 *((defender.getDEX()/5 + defender.getCON()/3) - (attacker.getSTR()/2 + attacker.getDEX()/3 )));

        if (damageDealt < 1){
            return 0;
        }else {
            return damageDealt;
        }
    };

    @Override
    public Single<AttackResult> processAttack(StatsBasedEntity attacker, StatsBasedEntity defender) {

        return Single.create(singleEmitter -> {
            defender.setLastAttackedById(attacker.getId());
            List<String> combatLog = new ArrayList<>();
//            combatLog.add(attacker.getName() + " attacks " + defender.getName());

            val defenderHP = defender.getHP();
            val damageDealt = attackFunction.apply(attacker,defender);
            defender.setHP(defenderHP - damageDealt);

            combatLog.add(attacker.getName() + " attacks " + defender.getName() + " for " + damageDealt + " damage");
            log.info(attacker.getName() + " attacks " + defender.getName() + " for " + damageDealt + " damage");

            singleEmitter.onSuccess(AttackResult.of(attacker,defender,combatLog));

        });
    }
}
