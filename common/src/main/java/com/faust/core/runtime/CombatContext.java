package com.faust.core.runtime;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import java.util.ArrayDeque;
public final class CombatContext {
    public record Attack(Entity target,float charge) {}
    public static final ThreadLocal<ArrayDeque<DamageSource>> DAMAGE=ThreadLocal.withInitial(ArrayDeque::new);
    public static final ThreadLocal<ArrayDeque<Attack>> ATTACK=ThreadLocal.withInitial(ArrayDeque::new);
}
