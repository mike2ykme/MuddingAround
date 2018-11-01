package com.icrn.model;

import lombok.Data;
import lombok.val;

@Data
public class MobInfo {
    public long mobCount;
    public String mobName;
    public StatsBasedEntityTemplate statsBasedEntityTemplate;


    public static MobInfo of(long mobCount, String mobName, StatsBasedEntityTemplate template){
        val a = new MobInfo();

        a.setMobCount(mobCount);
        a.setMobName(mobName);
        a.setStatsBasedEntityTemplate(template);

        return a;
    }
}
