package net.momirealms.craftengine.core.item.recipe.vanilla;

import net.momirealms.craftengine.core.item.recipe.DatapackRecipeResult;
import net.momirealms.craftengine.core.item.recipe.RecipeSerializers;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public class VanillaSmithingTransformRecipe implements VanillaRecipe {
    private final DatapackRecipeResult result;
    private final List<String> base;
    private final List<String> template;
    private final List<String> addition;

    public VanillaSmithingTransformRecipe(List<String> base, List<String> template, List<String> addition, DatapackRecipeResult result) {
        this.result = result;
        this.base = base;
        this.template = template;
        this.addition = addition;
    }

    @Override
    public Key type() {
        return RecipeSerializers.SMITHING_TRANSFORM;
    }

    public DatapackRecipeResult result() {
        return result;
    }

    public List<String> base() {
        return base;
    }

    public List<String> template() {
        return template;
    }

    public List<String> addition() {
        return addition;
    }
}
