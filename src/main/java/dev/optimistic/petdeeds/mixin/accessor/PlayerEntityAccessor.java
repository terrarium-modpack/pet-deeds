package dev.optimistic.petdeeds.mixin.accessor;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {
  @Invoker
  void invokeDropShoulderEntity(NbtCompound shoulderEntity);

  @Invoker
  void invokeSetShoulderEntityLeft(NbtCompound shoulderEntity);

  @Invoker
  void invokeSetShoulderEntityRight(NbtCompound shoulderEntity);
}
