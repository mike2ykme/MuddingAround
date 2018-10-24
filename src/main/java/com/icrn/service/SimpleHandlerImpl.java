package com.icrn.service;

import com.icrn.model.Actions;
import com.icrn.model.AttackResult;
import com.icrn.model.MudUser;
import com.icrn.model.StatsBasedEntity;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
public class SimpleHandlerImpl implements AttackHandler, RestHandler {
    private static BiFunction<StatsBasedEntity,StatsBasedEntity,Integer> attackFunction = (attacker, defender) -> {

        val damageDealt = (-1 *(((defender.getDEX()/5) + (defender.getCON()/3) + getDefenseBonus(defender.getLastCommand()))
                - (attacker.getSTR()/2 + attacker.getDEX()/3 )));

        if (damageDealt < 1){
            return 0;
        }else {
            return damageDealt;
        }
    };

    private static Function<StatsBasedEntity,StatsBasedEntity> restFunction = (entity) ->{
        val newHp = entity.getHP() + (int)(entity.getCON()*.2);
        entity.setHP(newHp);

        return entity;

    };

    private static int getDefenseBonus(Optional<Actions> actions){
        if (actions.isPresent()){
            val action = actions.get();
            if (Actions.DEFEND == action)
                return 3;
            else if (Actions.ATTACK == action)
                return -2;
        }
            return 0;

    }

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

    @Override
    public Single<StatsBasedEntity> restStatsEntity(MudUser user) {
        return Single.create(singleEmitter -> {
            val entity = restFunction.apply(user);
            singleEmitter.onSuccess(entity);

        });
    }
}
