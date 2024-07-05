package dev.optimistic.petdeeds.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

public final class InteractionUtil {
  private InteractionUtil() {
  }

  public static TypedActionResult<ItemStack> mapActionResultToTyped(ItemStack stack, ActionResult result) {
    return switch (result) {
      case SUCCESS -> TypedActionResult.success(stack);
      case CONSUME, CONSUME_PARTIAL -> TypedActionResult.consume(stack);
      case PASS -> TypedActionResult.pass(stack);
      case FAIL -> TypedActionResult.fail(stack);
    };
  }
}
