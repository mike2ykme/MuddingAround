package com.icrn.service;

import com.icrn.model.AttackResult;
import com.icrn.model.Entity;
import com.icrn.model.MudUser;
import com.icrn.model.StatsBasedEntity;
import io.reactivex.Maybe;
import io.reactivex.Single;

import java.util.Map;

public interface AttackHandler {
    Single<AttackResult> processAttack(StatsBasedEntity attacker, StatsBasedEntity defender);

//    Single<AttackResult> userAttacks(StatsBasedEntity attacker, StatsBasedEntity defender);
}
