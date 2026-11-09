package com.faust.core.progression;

import java.util.ServiceLoader;

/** Optional provider, discovered without loading client or loader classes. */
public final class StaminaServices {
    private static final StaminaMechanics PROVIDER=ServiceLoader.load(StaminaMechanics.class,StaminaServices.class.getClassLoader()).findFirst().orElse(null);
    private static final StaminaMechanics NONE=new StaminaMechanics(){};
    private StaminaServices(){}
    public static boolean present(){return PROVIDER!=null;}
    public static StaminaMechanics mechanics(){return present()?PROVIDER:NONE;}
}
