package com.icrn.model;

import lombok.Data;

@Data
public class MobInfo {
    public long mobCount;
    public String getMobName;
    public StatsBasedEntityTemplate statsBasedEntityTemplate;

    public StatsBasedEntityTemplate getTemplate() {
        return statsBasedEntityTemplate;
    }
}
