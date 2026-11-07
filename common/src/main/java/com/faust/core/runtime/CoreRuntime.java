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
    private static final class Session {int lastSpent=-100000, lastRead=-100000, shieldTick=-100000, inputTick=-100000,ranTick=-100000,movedTick=-100000,budgetWindow;double run,shield,blocks;boolean campfire,forward;Balance sentBalance;Vec3 lastPosition;}
    public static void input(ServerPlayer p,boolean forward) {Session s=session(p);s.forward=forward;s.inputTick=p.tickCount;}
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
            Progress p=data(player);
            for(var entry:p.tracks.entrySet()) {
                double fraction=Math.min(1,entry.getValue().xp/p.needed(entry.getKey(),previous));
                entry.getValue().xp=fraction*p.needed(entry.getKey(),balance);entry.getValue().threshold=p.needed(entry.getKey(),balance);
            }
            p.fatigue=Math.min(p.fatigue,balance.fatigue.get("maximum"));p.stamina=Math.min(p.stamina,p.maxStamina(balance));p.revision++;
            attributes(player);sync(player);
        }
    }
    public static Progress data(ServerPlayer player) {return ((PlayerData)player).coreData();}
    public static boolean active(ServerPlayer player) {return !player.isCreative()&&!player.isSpectator()&&!player.isDeadOrDying();}
    private static Session session(ServerPlayer p) {
        Session s=SESSIONS.computeIfAbsent(p.getUUID(),k->new Session());
        if(p.tickCount<s.budgetWindow||p.tickCount-s.budgetWindow>=1200) {s.budgetWindow=p.tickCount;s.run=s.shield=s.blocks=0;}
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
        Session s=session(player);
        if(stat.getType()==Stats.CUSTOM) {
            Object key=stat.getValue();
            if(key.equals(Stats.SPRINT_ONE_CM)&&player.onGround()&&!player.isPassenger()) {
                s.ranTick=player.tickCount;
                double metres=Math.min(amount/100.0,Math.max(0,balance.skills.get("runMetresPerMinute")-s.run));s.run+=metres;
                grant(player,"run",metres,metres);
                if(weight(player)>0) grant(player,"armor_run",metres,metres);
            } else if(key.equals(Stats.TRADED_WITH_VILLAGER)) grant(player,"trade",amount,amount);
            else if(key.equals(Stats.ENCHANT_ITEM)) grant(player,"enchant",amount,amount);
        } else if(stat.getType()==Stats.ITEM_CRAFTED) {
            double units=Math.min(amount,Math.max(0,balance.skills.get("blockMaximumPerMinute")-s.blocks));s.blocks+=units;
            grant(player,"craft",units,units);
        } else if(stat.getType()==Stats.ITEM_USED && stat.getValue() instanceof BlockItem) {
            double units=Math.min(amount,Math.max(0,balance.skills.get("blockMaximumPerMinute")-s.blocks));s.blocks+=units;
            grant(player,"place",units,units);
        }
    }
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
        if(b.stamina.get("enabled")>0&&p.exhausted)player.setSprinting(false);
        if(s.lastPosition!=null&&s.lastPosition.distanceToSqr(player.position())>0.0001)s.movedTick=player.tickCount;
        s.lastPosition=player.position();
        double armorRate=player.isSprinting()&&player.tickCount-s.ranTick<=1?b.stamina.get("armorCost")*Math.max(0,weight(player)-p.capacity(b)):0;
        Vec3 flow=player.level().getFluidState(player.blockPosition()).getFlow(player.level(),player.blockPosition());
        Vec3 look=Vec3.directionFromRotation(0,player.getYRot());
        // Server movement input plus facing direction; passive water movement alone does not qualify.
        boolean upstream=player.isSwimming()&&!player.isPassenger()&&s.forward&&player.tickCount-s.inputTick<=20&&look.dot(flow)<-b.stamina.get("flowThreshold");
        double waterRate=upstream?b.stamina.get("waterCost")*Math.max(0,-look.dot(flow)):0;
        double rate=armorRate+waterRate;
        if(!p.exhausted&&rate>0&&b.stamina.get("enabled")>0) {p.spend(rate/20,b);s.lastSpent=player.tickCount;}
        if(player.tickCount%20==0) {
            s.campfire=false;
            if(player.tickCount-s.movedTick>5) {
                int r=(int)Math.ceil(b.fatigue.get("campfireRadius"));
                BlockPos origin=player.blockPosition();
                for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-r,-r,-r),origin.offset(r,r,r))) {
                    if(!player.level().hasChunkAt(pos)||pos.distSqr(origin)>r*r)continue;
                    var state=player.level().getBlockState(pos);
                    if(state.getBlock() instanceof CampfireBlock&&state.getValue(CampfireBlock.LIT)){s.campfire=true;break;}
                }
            }
        }
        p.rest(0.05,player.isSleeping(),s.campfire&&player.tickCount-s.movedTick>5,player.tickCount-s.lastSpent>=b.stamina.get("delaySeconds")*20,b);
        if(b.stamina.get("enabled")>0&&p.exhausted)player.setSprinting(false);
        if(b.stamina.get("enabled")>0&&p.exhausted&&upstream)player.setSprinting(false);
        if(player.tickCount%20==0) attributes(player);
        int interval=Math.max(1,b.skills.get("structureIntervalTicks").intValue());
        if(player.tickCount%interval==0) {
            var structures=player.serverLevel().structureManager();
            for(var structure:structures.getAllStructuresAt(player.blockPosition()).keySet()) {
                var start=structures.getStructureWithPieceAt(player.blockPosition(),structure);
                if(!start.isValid())continue;
                var id=player.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
                if(id==null||!id.getNamespace().equals("minecraft"))continue;
                String key=player.level().dimension().location()+"/"+id+"/"+start.getChunkPos();
                if(p.discoveries.add(key)) grant(player,"structure",1,1);
            }
        }
        if(player.tickCount%5==0)sync(player);
    }
    public static void rebuildAttributes(ServerPlayer player){attributes(player);}
    private static void attributes(ServerPlayer player) {
        Progress p=data(player);Balance b=balance;
        setAttribute(player,Attributes.ATTACK_DAMAGE,"strength",Math.min(b.skills.get("damageMaximum"),p.track(Skill.strength).level*b.skills.get("damagePerLevel"))+p.effect("damage",b));
        double speed=player.isSprinting()?Math.min(b.skills.get("speedMaximum"),p.track(Skill.dexterity).level*b.skills.get("speedPerLevel"))+p.effect("speed",b):0;
        setAttribute(player,Attributes.MOVEMENT_SPEED,"dexterity",speed);
        setFlatAttribute(player,Attributes.MAX_HEALTH,"constitution_health",SkillEffects.health(p,b));
        setFlatAttribute(player,Attributes.LUCK,"intelligence_luck",SkillEffects.luck(p,b));
        setFlatAttribute(player,Attributes.BLOCK_INTERACTION_RANGE,"building_block_reach",SkillEffects.reach(p,b));
        setFlatAttribute(player,Attributes.ENTITY_INTERACTION_RANGE,"building_entity_reach",SkillEffects.reach(p,b));
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
