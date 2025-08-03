package net.momirealms.craftengine.core.item.recipe.vanilla;

import net.momirealms.craftengine.core.item.recipe.CookingRecipeCategory;
import net.momirealms.craftengine.core.item.recipe.DatapackRecipeResult;
import net.momirealms.craftengine.core.item.recipe.RecipeSerializers;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public class VanillaCampfireRecipe extends VanillaCookingRecipe {

    public VanillaCampfireRecipe(CookingRecipeCategory category, String group, DatapackRecipeResult result, List<String> ingredient, float experience, int cookingTime) {
        super(category, group, result, ingredient, experience, cookingTime);
    }

    @Override
    public Key type() {
        return RecipeSerializers.CAMPFIRE_COOKING;
    }
}
