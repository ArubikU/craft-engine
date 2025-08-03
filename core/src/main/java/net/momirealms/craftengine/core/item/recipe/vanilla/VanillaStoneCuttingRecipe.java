package net.momirealms.craftengine.core.item.recipe.vanilla;

import net.momirealms.craftengine.core.item.recipe.DatapackRecipeResult;
import net.momirealms.craftengine.core.item.recipe.RecipeSerializers;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public class VanillaStoneCuttingRecipe extends VanillaGroupedRecipe {
    private final List<String> ingredient;

    public VanillaStoneCuttingRecipe(String group, DatapackRecipeResult result, List<String> ingredient) {
        super(group, result);
        this.ingredient = ingredient;
    }

    public List<String> ingredient() {
        return ingredient;
    }

    @Override
    public Key type() {
        return RecipeSerializers.STONECUTTING;
    }
}
