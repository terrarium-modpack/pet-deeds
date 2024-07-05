package dev.optimistic.petdeeds;

import dev.optimistic.petdeeds.mixin.accessor.PlayerEntityAccessor;
import dev.optimistic.petdeeds.util.InteractionUtil;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class DeedItem extends Item {
  public static final DeedItem INSTANCE = new DeedItem();

  private static final String PET_ID_KEY = "pet-id";
  private static final String OWNER_ID_KEY = "owner-id";
  private static final String UUID_KEY = "UUID";

  private static final Text INITIALIZE_DEED = generateTooltip("pet_deeds.item.deed.tooltip.initialize");
  private static final Text UTILIZE_DEED = generateTooltip("pet_deeds.item.deed.tooltip.utilize");

  private DeedItem() {
    super(new FabricItemSettings());
  }

  private static Text generateTooltip(String translationKey) {
    return Text.translatable(translationKey)
      .formatted(Formatting.GRAY);
  }

  private @Nullable Entity ejectPetFromShoulder(ServerPlayerEntity serverPlayerEntity,
                                                PlayerEntityAccessor accessor,
                                                NbtCompound shoulderEntity, Consumer<NbtCompound> shoulderSetter,
                                                UUID petId) {
    if (!shoulderEntity.containsUuid(UUID_KEY)) return null;
    if (!shoulderEntity.getUuid(UUID_KEY).equals(petId)) return null;
    accessor.invokeDropShoulderEntity(shoulderEntity);
    shoulderSetter.accept(new NbtCompound());
    return serverPlayerEntity.getServerWorld().getEntity(petId);
  }

  private ActionResult reset(ItemStack stack) {
    stack.removeCustomName();
    stack.setNbt(null);
    return ActionResult.SUCCESS;
  }

  private ActionResult utilizeDeed(UUID userId, ItemStack stack, ServerWorld serverWorld) {
    NbtCompound nbt = stack.getNbt();
    if (nbt == null || nbt.isEmpty()) return ActionResult.PASS;

    if (!nbt.containsUuid(PET_ID_KEY) || !nbt.containsUuid(OWNER_ID_KEY)) return ActionResult.FAIL;
    // we don't care about this
    // if (!nbt.contains(PET_NAME_KEY, NbtElement.STRING_TYPE)) return ActionResult.FAIL;

    UUID petId = nbt.getUuid(PET_ID_KEY);
    UUID storedOwnerId = nbt.getUuid(OWNER_ID_KEY);
    Entity entity = null;

    // eject parrots who changed owner from shoulders
    ServerPlayerEntity ownerEntity = serverWorld.getServer().getPlayerManager().getPlayer(storedOwnerId);
    if (ownerEntity != null) {
      PlayerEntityAccessor accessor = (PlayerEntityAccessor) ownerEntity;

      if (
        (entity =
          ejectPetFromShoulder(
            ownerEntity,
            accessor,
            ownerEntity.getShoulderEntityLeft(),
            accessor::invokeSetShoulderEntityLeft, petId
          )
        ) == null) {
        entity = ejectPetFromShoulder(ownerEntity,
          accessor,
          ownerEntity.getShoulderEntityRight(),
          accessor::invokeSetShoulderEntityRight,
          petId
        );
      }
    }

    if (entity == null) entity = serverWorld.getEntity(petId);
    if (!(entity instanceof TameableEntity pet)) return reset(stack);
    UUID ownerId = pet.getOwnerUuid();


    if (
      // we check if the current owner id matches the owner id the deed was written by
      // to prevent scams whereby a pet owner would create two deeds, sell one, keep one
      // and then after the sale was completed use the one they kept, regaining ownership
      // of the pet
      !storedOwnerId.equals(ownerId)
        ||
        // they probably intend to reset it, right?
        Objects.equals(ownerId, userId)
    ) return reset(stack);

    pet.setOwnerUuid(userId);
    stack.decrement(1);
    return ActionResult.SUCCESS;
  }

  @Override
  public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
    ItemStack stack = user.getStackInHand(hand);
    if (!(user instanceof ServerPlayerEntity serverPlayer)) return TypedActionResult.pass(stack);
    return InteractionUtil.mapActionResultToTyped(stack, utilizeDeed(user.getUuid(), stack, serverPlayer.getServerWorld()));
  }

  public ActionResult useOnEntityVerified(ItemStack stack, PlayerEntity user,
                                          TameableEntity tameableEntity, ServerWorld serverWorld) {
    UUID userId = user.getUuid();

    ActionResult utilization = utilizeDeed(userId, stack, serverWorld);
    if (utilization != ActionResult.PASS) return utilization;

    UUID ownerId = tameableEntity.getOwnerUuid();
    if (!Objects.equals(ownerId, userId)) return ActionResult.FAIL;
    NbtCompound stackNbt = stack.getNbt() != null ? stack.getNbt().copy() : new NbtCompound();

    stackNbt.putUuid(PET_ID_KEY, tameableEntity.getUuid());
    stackNbt.putUuid(OWNER_ID_KEY, ownerId);

    Text entityDefaultName = tameableEntity.getType().getName();
    Text customName = Text.empty()
      .append(entityDefaultName)
      .append(ScreenTexts.SPACE)
      .append(getName())
      .styled(style -> style.withItalic(false));

    if (stack.getCount() > 1) {
      stack.decrement(1);

      ItemStack newStack = stack.copyWithCount(1);
      newStack.setNbt(stackNbt);
      newStack.setCustomName(customName);

      user.giveItemStack(newStack);
    } else {
      stack.setNbt(stackNbt);
      stack.setCustomName(customName);
    }

    return ActionResult.SUCCESS;
  }

  // this method is provided to satisfy the deed
  @Override
  public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
    if (!(user.getWorld() instanceof ServerWorld serverWorld)) return ActionResult.PASS;
    if (!(entity instanceof TameableEntity tameableEntity)) return ActionResult.PASS;

    return useOnEntityVerified(stack, user, tameableEntity, serverWorld);
  }


  @Override
  public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
    NbtCompound nbt = stack.getNbt();

    tooltip.add(nbt != null && nbt.containsUuid(PET_ID_KEY) ?
      UTILIZE_DEED
      :
      INITIALIZE_DEED
    );
  }
}
