package com.faust.core.runtime;

import com.faust.core.api.CoreApi;
import com.faust.core.progression.*;
import com.faust.core.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Path;
import java.util.*;

public final class CoreRuntime {
    public static Balance balance=new Balance();
    private static Path configDirectory;
    private static final Set<UUID> GRANTING=new HashSet<>();
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final class Session {int lastSpent=-100000,attackTick=-100000, lastRead=-100000, shieldTick=-100000, inputTick=-100000,ranTick=-100000,movedTick=-100000,budgetWindow,swimTicks;double run,swim,shield,blocks;boolean airVisible,campfire,soulCampfire,forward,paidSwing,wasSprinting;Balance sentBalance;Vec3 lastPosition;Map<ResourceLocation,net.minecraft.core.Holder<Attribute>> statModifiers=new HashMap<>();}
    public static void input(ServerPlayer p,boolean forward,boolean airVisible) {Session s=session(p);s.forward=forward;s.airVisible=airVisible;s.inputTick=p.tickCount;}
    public static void initialize(Path directory) {configDirectory=directory;reload();}
    public static ConfigResult editConfig(ServerPlayer player,ConfigEdit edit) {
        CoreApi.requireServerThread(player);
        boolean allowed=player.hasPermissions(2)||player.getServer().isSingleplayerOwner(player.getGameProfile());
        if(!allowed)return new ConfigResult(false,false,"core.config.denied");
        if(edit.section().isEmpty()){session(player).sentBalance=null;sync(player);return new ConfigResult(true,true,"core.config.ready");}
        try {
            // Validate against the live full configuration before touching any file.
            balance.edited(edit.section(),edit.json());
            Balance.saveGroup(configDirectory,edit.section(),edit.json());reload(player.getServer());
            return new ConfigResult(true,true,"core.config.saved");
        } catch(Exception e){return new ConfigResult(true,false,"core.config.invalid");}
    }
    public static void refresh(ServerPlayer player){data(player).reconcile(balance);attributes(player);sync(player);}
    public static void reload() {
        try {Balance candidate=Balance.load(configDirectory);balance=candidate;}
        catch(java.io.IOException ex) {throw new IllegalStateException("Cannot load core config: "+configDirectory,ex);}
    }
    public static void reload(net.minecraft.server.MinecraftServer server) {
        Balance previous=balance;reload();
        for(var player:server.getPlayerList().getPlayers()) {
            Progress p=data(player);p.reconcileAbilities(balance);
            for(var entry:p.tracks.entrySet()) {
                double fraction=Math.min(1,entry.getValue().xp/p.needed(entry.getKey(),previous));
                entry.getValue().xp=fraction*p.needed(entry.getKey(),balance);entry.getValue().threshold=p.needed(entry.getKey(),balance);
            }
            p.fatigue=Math.min(p.fatigue,p.maxFatigue(balance));p.stamina=Math.min(p.stamina,p.maxStamina(balance));p.revision++;
            attributes(player);sync(player);
        }
    }
    public static Progress data(ServerPlayer player) {return ((PlayerData)player).coreData();}
    public static boolean active(ServerPlayer player) {return !player.isCreative()&&!player.isSpectator()&&!player.isDeadOrDying();}
    private static Session session(ServerPlayer p) {
        Session s=SESSIONS.computeIfAbsent(p.getUUID(),k->new Session());
        if(p.tickCount<s.budgetWindow||p.tickCount-s.budgetWindow>=1200) {s.budgetWindow=p.tickCount;s.run=s.swim=s.shield=s.blocks=0;}
        return s;
    }
    public static void disconnected(ServerPlayer p) {SESSIONS.remove(p.getUUID());}
    public static boolean grant(ServerPlayer player,String action,double units,double count) {
        CoreApi.requireServerThread(player);
        if(player.isCreative()||player.isSpectator()||(!action.equals("damage")&&player.isDeadOrDying())||!GRANTING.add(player.getUUID())) return false;
        try {
            Progress p=data(player);String before=p.json();double scale=CoreApi.before(new CoreApi.Grant(player,action,units,count));
            if(scale==0) return false;
            if(!p.event(action,units*scale,count,balance)) return false;
            CoreApi.after(new CoreApi.Change(player.getUUID(),action,before,p.json()));return true;
        } finally {GRANTING.remove(player.getUUID());}
    }
    public static void stat(ServerPlayer player,Stat<?> stat,int amount) {
        if(!active(player)||amount<=0)return;
        if(foodStat(data(player),stat,amount,balance))return;
        Session s=session(player);
        if(stat.getType()==Stats.CUSTOM) {
            Object key=stat.getValue();
            if(key.equals(Stats.SPRINT_ONE_CM)&&player.onGround()&&!player.isPassenger()) {
                s.ranTick=player.tickCount;
                double metres=Math.min(amount/100.0,Math.max(0,balance.skills.get("runMetresPerMinute")-s.run));s.run+=metres;
                grant(player,"run",metres,metres);
                grant(player,"constitution_run",metres,metres);
                if(weight(player)>0) grant(player,"armor_run",metres,metres);
            } else if(key.equals(Stats.SWIM_ONE_CM)&&player.isSwimming()&&!player.isPassenger()) {
                double metres=Math.min(amount/100.0,Math.max(0,balance.skills.get("swimMetresPerMinute")-s.swim));s.swim+=metres;
                grant(player,"constitution_swim",metres*balance.skills.get("constitutionSwimMultiplier"),metres);
            } else if(key.equals(Stats.TRADED_WITH_VILLAGER)) grant(player,"trade",amount,amount);
            else if(key.equals(Stats.ENCHANT_ITEM)) grant(player,"enchant",amount,amount);
        } else if(stat.getType()==Stats.ITEM_CRAFTED) {
            double units=Math.min(amount,Math.max(0,balance.skills.get("blockMaximumPerMinute")-s.blocks));s.blocks+=units;
            grant(player,"craft",units,units);
        } else if(stat.getType()==Stats.ITEM_USED && (stat.getValue() instanceof BowItem||stat.getValue() instanceof CrossbowItem)) {
            grant(player,stat.getValue() instanceof BowItem?"bow":"crossbow",amount,amount);
        } else if(stat.getType()==Stats.ITEM_USED && stat.getValue() instanceof BlockItem) {
            double units=Math.min(amount,Math.max(0,balance.skills.get("blockMaximumPerMinute")-s.blocks));s.blocks+=units;
            grant(player,"place",units,units);
        }
    }
    static boolean foodStat(Progress progress,Stat<?> stat,int amount,Balance b){
        if(amount<=0||stat.getType()!=Stats.CUSTOM||!stat.getValue().equals(Stats.EAT_CAKE_SLICE))return false;
        progress.sweetFood("minecraft:cake",amount,b);return true;
    }
    private static long worldAge(ServerPlayer player){return player.serverLevel().getServer().overworld().getGameTime();}
    public static boolean attack(ServerPlayer player,boolean air) {
        if(player.isCreative()||player.isSpectator()||balance.stamina.get("enabled")==0)return true;
        Session s=session(player);if(air&&s.paidSwing&&player.tickCount-s.attackTick<=5){s.paidSwing=false;return true;}
        var data=data(player);double cost=data.actionCost(player.getMainHandItem().is(Items.MACE)?"mace":"attack",balance);
        if(data.actionsBlocked(balance))return false;
        data.spend(cost,balance,worldAge(player));s.lastSpent=player.tickCount;s.attackTick=player.tickCount;s.paidSwing=!air;return true;
    }
    public static boolean pay(ServerPlayer player,double cost){if(player.isCreative()||player.isSpectator()||balance.stamina.get("enabled")==0)return true;var p=data(player);if(p.actionsBlocked(balance))return false;p.spend(cost,balance,worldAge(player));if(cost>0)session(player).lastSpent=player.tickCount;return true;}
    public static boolean shieldPayment(ServerPlayer player){return pay(player,Math.max(0,1-Math.min(0.5,data(player).effect("shield_cost_reduction",balance))));}
    public static boolean jump(ServerPlayer player){if(!active(player)||balance.stamina.get("enabled")==0)return true;if(data(player).actionsBlocked(balance)){player.setSprinting(false);return true;}return !player.isSprinting()||pay(player,balance.stamina.get("sprintJumpCost")+weight(player)*balance.stamina.get("jumpArmorCost"));}
    public static boolean canAct(ServerPlayer player){return !active(player)||!data(player).actionsBlocked(balance);}
    public static boolean ranged(ServerPlayer player,String weapon){return pay(player,data(player).actionCost(weapon,balance));}
    public static void sleptNight(net.minecraft.server.level.ServerLevel level){for(var player:level.players())if(player.isSleeping())data(player).sleptNight(balance);}
    public static void food(ServerPlayer player,net.minecraft.world.item.ItemStack stack){if(!active(player))return;data(player).sweetFood(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),balance);}
    public static void read(ServerPlayer p,ItemStack stack) {
        if(!(stack.is(Items.WRITTEN_BOOK)||stack.is(Items.WRITABLE_BOOK)))return;
        Session s=session(p);
        if(p.tickCount-s.lastRead<balance.skills.get("readCooldownSeconds")*20) return;
        s.lastRead=p.tickCount;grant(p,"read",1,1);
    }
    public static double weight(ServerPlayer player) {
        double weight=0;
        for(ItemStack stack:player.getArmorSlots()) {
            if(stack.isEmpty())continue;
            String key=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            Double configured=balance.armor.get(key);
            if(configured!=null){weight+=configured;continue;}
            double itemWeight=balance.armor.get("fallbackWeight");
            for(String material:List.of("leather","chainmail","iron","golden","diamond","netherite")) if(key.startsWith("minecraft:"+material+"_")) itemWeight=balance.armor.get(material);
            weight+=itemWeight;
        }
        return weight;
    }
    public static void shield(ServerPlayer p,double prevented) {
        Session s=session(p);
        if(p.tickCount-s.shieldTick<balance.skills.get("shieldIntervalTicks"))return;
        double q=Math.min(Math.min(prevented,balance.skills.get("shieldMaximum")),Math.max(0,balance.skills.get("shieldBudgetPerMinute")-s.shield));
        if(q<=0)return;s.shieldTick=p.tickCount;s.shield+=q;grant(p,"shield",q,1);
    }
    public static void tick(ServerPlayer player) {
        if(!active(player)) {if(player.tickCount%20==0)sync(player);return;}
        Progress p=data(player); Session s=session(player); Balance b=balance;p.reconcile(b);
        if(b.stamina.get("enabled")>0&&p.actionsBlocked(b))player.setSprinting(false);
        boolean moving=s.lastPosition!=null&&s.lastPosition.distanceToSqr(player.position())>0.0001;
        if(moving)s.movedTick=player.tickCount;
        s.lastPosition=player.position();
        double armorRate=player.isSprinting()&&player.tickCount-s.ranTick<=1?b.stamina.get("armorCost")*Math.max(0,weight(player)-p.capacity(b)):0;
        Vec3 flow=player.level().getFluidState(player.blockPosition()).getFlow(player.level(),player.blockPosition());
        Vec3 look=Vec3.directionFromRotation(0,player.getYRot());
        // Server movement input plus facing direction; passive water movement alone does not qualify.
        boolean upstream=player.isSwimming()&&!player.isPassenger()&&s.forward&&player.tickCount-s.inputTick<=20&&look.dot(flow)<-b.stamina.get("flowThreshold");
        double waterRate=upstream?b.stamina.get("waterCost")*Math.max(0,-look.dot(flow)):0;
        double runRate=player.isSprinting()&&player.tickCount-s.ranTick<=1?b.stamina.get("runCost"):0;
        if(player.isSwimming()&&player.isSprinting()&&moving)s.swimTicks++;else s.swimTicks=0;
        double swimRate=s.swimTicks>b.stamina.get("swimDelaySeconds")*20?b.stamina.get("swimCost"):0;
        double rate=runRate+armorRate+waterRate+swimRate;
        if(p.stamina>0&&rate>0&&b.stamina.get("enabled")>0) {p.spend(rate/20,b,worldAge(player));s.lastSpent=player.tickCount;}
        if(player.tickCount%20==0) {
            s.campfire=false;s.soulCampfire=false;
            {
                int r=(int)Math.ceil(b.fatigue.get("campfireRadius"));
                BlockPos origin=player.blockPosition();
                for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-r,-r,-r),origin.offset(r,r,r))) {
                    if(!player.level().hasChunkAt(pos)||pos.distSqr(origin)>r*r)continue;
                    var state=player.level().getBlockState(pos);
                    if(state.getBlock() instanceof CampfireBlock&&state.getValue(CampfireBlock.LIT)){if(state.is(Blocks.SOUL_CAMPFIRE))s.soulCampfire=true;else s.campfire=true;}
                }
            }
        }
        if(!player.isOnFire())p.soulFireBurn=false;else if(player.level().getBlockState(player.blockPosition()).is(Blocks.SOUL_FIRE)||player.level().getBlockState(player.blockPosition().below()).is(Blocks.SOUL_FIRE))p.soulFireBurn=true;
        p.environmentalFatigue(0.05,s.soulCampfire,p.soulFireBurn&&player.isOnFire(),b,worldAge(player));
        boolean shielding=player.isUsingItem()&&player.getUseItem().is(Items.SHIELD);
        boolean airVisible=com.faust.core.progression.Breathing.visible(player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER),player.getAirSupply(),player.getMaxAirSupply())||(s.airVisible&&player.tickCount-s.inputTick<=20);
        p.recover(0.05,player.isSleeping(),s.campfire&&player.tickCount-s.movedTick>5,player.tickCount-s.lastSpent>=b.stamina.get("delaySeconds")*20,shielding,player.isFallFlying(),airVisible,player.isSwimming()&&player.isSprinting(),b);
        if(p.actionsBlocked(b)&&shielding)player.stopUsingItem();
        if(b.stamina.get("enabled")>0&&p.actionsBlocked(b))player.setSprinting(false);
        if(b.stamina.get("enabled")>0&&p.actionsBlocked(b)&&upstream)player.setSprinting(false);
        if(player.tickCount%20==0||s.wasSprinting!=player.isSprinting()){s.wasSprinting=player.isSprinting();attributes(player);}
        if(player.tickCount%5==0)sync(player);
    }
    public static void rebuildAttributes(ServerPlayer player){attributes(player);}
    private static void attributes(ServerPlayer player) {
        Progress p=data(player);Balance b=balance;
        // Remove pre-1.2 hard-coded modifiers once; configurable rules own the base bonuses.
        for(var attr:List.of(Attributes.ATTACK_DAMAGE,Attributes.MOVEMENT_SPEED,Attributes.MAX_HEALTH,Attributes.LUCK,Attributes.BLOCK_INTERACTION_RANGE,Attributes.ENTITY_INTERACTION_RANGE)) {
            var instance=player.getAttribute(attr);if(instance!=null)for(String id:List.of("strength","dexterity","constitution_health","intelligence_luck","building_block_reach","building_entity_reach"))instance.removeModifier(ResourceLocation.fromNamespaceAndPath("faustcore",id));
        }
        Session session=session(player);
        for(var old:session.statModifiers.entrySet()){var instance=player.getAttribute(old.getValue());if(instance!=null)instance.removeModifier(old.getKey());}session.statModifiers.clear();
        Map<String,Double> buildingReachUsed=new HashMap<>();
        for(var rule:b.statBonuses){
            if(!rule.requiresAbility.isEmpty()&&!p.enabled(rule.requiresAbility,b))continue;
            var key=ResourceLocation.tryParse(rule.attribute);if(key==null)continue;
            var attribute=BuiltInRegistries.ATTRIBUTE.getHolder(key);if(attribute.isEmpty())continue;
            var instance=player.getAttribute(attribute.get());if(instance==null)continue;
            double amount=rule.sprintOnly&&!player.isSprinting()?0:rule.amount(p.track(rule.skill).level);if(amount==0)continue;
            if(rule.skill==Skill.building&&(rule.attribute.equals("minecraft:player.block_interaction_range")||rule.attribute.equals("minecraft:player.entity_interaction_range"))){amount=Math.min(amount,Math.max(0,b.skills.get("buildingReachMaximum")-buildingReachUsed.getOrDefault(rule.attribute,0.0)));buildingReachUsed.merge(rule.attribute,amount,Double::sum);}
            var id=ResourceLocation.fromNamespaceAndPath("faustcore","stat/"+rule.id);
            var operation=switch(rule.operation){case "multiply_base"->AttributeModifier.Operation.ADD_MULTIPLIED_BASE;case "multiply_total"->AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;default->AttributeModifier.Operation.ADD_VALUE;};
            instance.addTransientModifier(new AttributeModifier(id,amount,operation));session.statModifiers.put(id,attribute.get());
        }
        setAttribute(player,Attributes.ATTACK_DAMAGE,"tree_damage",p.effect("damage",b));
        setAttribute(player,Attributes.MOVEMENT_SPEED,"tree_sprint",player.isSprinting()?p.effect("speed",b):0);
        if(player.getHealth()>player.getMaxHealth())player.setHealth(player.getMaxHealth());
        CoreApi.applyBonuses(player);
    }
    private static void setAttribute(ServerPlayer p,net.minecraft.core.Holder<Attribute> attribute,String name,double value) {
        AttributeInstance instance=p.getAttribute(attribute);if(instance==null)return;
        ResourceLocation id=ResourceLocation.fromNamespaceAndPath("faustcore",name);instance.removeModifier(id);
        if(value!=0)instance.addTransientModifier(new AttributeModifier(id,value,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }
    private static void setFlatAttribute(ServerPlayer p,net.minecraft.core.Holder<Attribute> attribute,String name,double value) {
        var instance=p.getAttribute(attribute);if(instance==null)return;
        var id=ResourceLocation.fromNamespaceAndPath("faustcore",name);instance.removeModifier(id);
        if(value!=0)instance.addTransientModifier(new AttributeModifier(id,value,AttributeModifier.Operation.ADD_VALUE));
    }
    public static double breaking(ServerPlayer player) {return SkillEffects.breaking(data(player),balance);}
    public static void sync(ServerPlayer player) {
        Session session=session(player);
        var publicData=Balance.JSON.toJsonTree(data(player)).getAsJsonObject();publicData.remove("discoveries");
        var snapshot=new Snapshot(publicData.toString(),session.sentBalance==balance?"":Balance.JSON.toJson(balance),player.experienceLevel,weight(player));
        Services.PLATFORM.send(player,snapshot);
        session.sentBalance=balance;
    }
    public static int xp(ServerPlayer p) {
        int l=p.experienceLevel;long base=l<=16?(long)l*l+6L*l:l<=31?(5L*l*l-81L*l+720)/2:(9L*l*l-325L*l+4440)/2;
        return (int)Math.min(Integer.MAX_VALUE,base+(int)(p.experienceProgress*p.getXpNeededForNextLevel()));
    }
    public static boolean buy(ServerPlayer player,Skill skill) {
        if(!active(player))return false;
        long price=data(player).price(skill,balance);
        if(!data(player).buy(skill,player.experienceLevel,balance))return false;
        player.giveExperienceLevels(-(int)price);sync(player);return true;
    }
}
