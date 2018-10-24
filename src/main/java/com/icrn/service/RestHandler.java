package com.icrn.service;

import com.icrn.model.MudUser;
import com.icrn.model.StatsBasedEntity;
import io.reactivex.Single;

public interface RestHandler {
    Single<StatsBasedEntity> restStatsEntity(MudUser user);
}
