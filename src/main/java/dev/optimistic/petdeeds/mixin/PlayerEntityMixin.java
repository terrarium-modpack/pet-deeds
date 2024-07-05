package dev.optimistic.petdeeds.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.optimistic.petdeeds.DeedItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
  @WrapOperation(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;interact(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"))
  private ActionResult interact(Entity instance, PlayerEntity player,
                                Hand hand, Operation<ActionResult> original,
                                @Local(ordinal = 0) ItemStack stack) {
    return player instanceof ServerPlayerEntity serverPlayer &&
      instance instanceof TameableEntity tameableEntity
      && stack.getItem() == DeedItem.INSTANCE
      ?
      DeedItem.INSTANCE.useOnEntityVerified(stack, serverPlayer, tameableEntity, serverPlayer.getServerWorld())
      :
      original.call(instance, player, hand);
  }
}
