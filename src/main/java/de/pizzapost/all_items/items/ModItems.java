package de.pizzapost.all_items.items;

import de.pizzapost.all_items.AllItems;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {
    public static final Item ALL_ITEMS = registerItem("all_items", Item::new);

    private static Item registerItem(String name, Function<Item.Settings, Item> function) {
        return Registry.register(Registries.ITEM, Identifier.of(AllItems.MOD_ID, name), function.apply(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(AllItems.MOD_ID, name)))));
    }

    public static void registerModItems() {
        AllItems.LOGGER.info("Registering Mod Items for " + AllItems.MOD_ID);
    }
}
