package com.faust.core.progression;
import java.util.*;
/** Effect IDs accepted by configuration. External handlers register before config loading/reloading. */
public final class BonusEffects {
    private static final Set<String> IDS=new LinkedHashSet<>(Set.of("exhaustion_threshold_reduction","bow_cost_reduction","crossbow_cost_reduction","mace_cost_reduction","stat_unlock","shield_cost_reduction","damage","second_wind","regeneration","stamina","speed","capacity","shield_durability","shield_recovery","breaking"));
    public static synchronized void register(String id) {
        if(!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||!IDS.add(id))throw new IllegalArgumentException("Invalid/duplicate bonus "+id);
    }
    public static synchronized boolean known(String id){return IDS.contains(id);}
}
