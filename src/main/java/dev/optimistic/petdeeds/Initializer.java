package dev.optimistic.petdeeds;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class Initializer implements ModInitializer {
  @Override
  public void onInitialize() {
    Registry.register(
      Registries.ITEM,
      Identifier.of("pet_deeds", "deed"),
      DeedItem.INSTANCE
    );

    ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> content.add(DeedItem.INSTANCE));
  }
}
