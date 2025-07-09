package net.momirealms.craftengine.core.item.recipe.network.display;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class RecipeDisplays {
    public static final Key CRAFTING_SHAPELESS = Key.of("crafting_shapeless");
    public static final Key CRAFTING_SHAPED = Key.of("crafting_shaped");
    public static final Key FURNACE = Key.of("furnace");
    public static final Key STONECUTTER = Key.of("stonecutter");
    public static final Key SMITHING = Key.of("smithing");

    public static void register() {
        register(CRAFTING_SHAPELESS, new RecipeDisplay.Type(ShapelessCraftingRecipeDisplay::read));
        register(CRAFTING_SHAPED, new RecipeDisplay.Type(ShapedCraftingRecipeDisplay::read));
        register(FURNACE, new RecipeDisplay.Type(FurnaceRecipeDisplay::read));
        register(STONECUTTER, new RecipeDisplay.Type(SmithingRecipeDisplay::read));
        register(SMITHING, new RecipeDisplay.Type(SmithingRecipeDisplay::read));
    }

    public static void register(Key key, RecipeDisplay.Type type) {
        Holder.Reference<RecipeDisplay.Type> holder = ((WritableRegistry<RecipeDisplay.Type>) BuiltInRegistries.RECIPE_DISPLAY_TYPE)
                .registerForHolder(new ResourceKey<>(Registries.RECIPE_DISPLAY_TYPE.location(), key));
        holder.bindValue(type);
    }
}
