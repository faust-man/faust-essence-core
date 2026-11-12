package com.faust.core.runtime;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.*;
public final class PlacedLogs extends SavedData {
 private final java.util.Set<Long> logs=new java.util.HashSet<>();
 public static PlacedLogs get(ServerLevel level){return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(PlacedLogs::new,PlacedLogs::load,null),"faustcore_placed_logs");}
 private static PlacedLogs load(CompoundTag tag,HolderLookup.Provider registries){var data=new PlacedLogs();for(long pos:tag.getLongArray("logs"))data.logs.add(pos);return data;}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){tag.putLongArray("logs",logs.stream().mapToLong(Long::longValue).toArray());return tag;}
 public boolean contains(BlockPos pos){return logs.contains(pos.asLong());}
 public void add(BlockPos pos){if(logs.add(pos.asLong()))setDirty();}
 public void remove(BlockPos pos){if(logs.remove(pos.asLong()))setDirty();}
}
