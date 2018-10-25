package com.icrn.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

//@Builder
@Data
@Builder(builderMethodName = "builder")
public class StatsBasedEntityTemplate {
    @NonNull private Integer maxHP;
    @NonNull private Integer HP;
    @NonNull private Integer STR;
    @NonNull private Integer DEX;
    @NonNull private Integer CON;
    @NonNull private Long room;
}
