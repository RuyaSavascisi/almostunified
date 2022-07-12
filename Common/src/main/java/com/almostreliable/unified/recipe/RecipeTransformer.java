package com.almostreliable.unified.recipe;

import com.almostreliable.unified.AlmostUnified;
import com.almostreliable.unified.recipe.handler.RecipeHandlerFactory;
import com.almostreliable.unified.utils.ReplacementMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class RecipeTransformer {

    private final RecipeHandlerFactory factory;
    private final ReplacementMap replacementMap;

    public RecipeTransformer(RecipeHandlerFactory factory, ReplacementMap replacementMap) {
        this.factory = factory;
        this.replacementMap = replacementMap;
    }

    public boolean hasValidType(JsonObject json) {
        if (json.get("type") instanceof JsonPrimitive type) {
            return ResourceLocation.tryParse(type.getAsString()) != null;
        }
        return false;
    }

    public RecipeTransformationResult transformRecipes(Map<ResourceLocation, JsonElement> recipes) {
        ConcurrentMap<ResourceLocation, List<RawRecipe>> rawRecipesByType = recipes
                .entrySet()
                .parallelStream()
                .filter(entry -> entry.getValue().isJsonObject() && hasValidType(entry.getValue().getAsJsonObject()))
                .map(entry -> new RawRecipe(entry.getKey(), entry.getValue().getAsJsonObject()))
                .collect(Collectors.groupingByConcurrent(RawRecipe::getType));

        RecipeTransformationResult recipeTransformationResult = new RecipeTransformationResult();

        rawRecipesByType.forEach((type, rawRecipes) -> {
            for (int curIndex = 0; curIndex < rawRecipes.size(); curIndex++) {
                RawRecipe curRecipe = rawRecipes.get(curIndex);
                JsonObject result = transformRecipe(curRecipe.getId(), curRecipe.getOriginal());
                if (result != null) {
                    recipeTransformationResult.track(curRecipe.getId(), curRecipe.getOriginal(), result); // TODO remove

                    curRecipe.setTransformed(result);

                    for (int compareIndex = 0; compareIndex < curIndex; compareIndex++) { // TODO extract
                        RawRecipe toCompare = rawRecipes.get(compareIndex);
                        if(toCompare.isDuplicate()) continue;
                        JsonObject json = toCompare.isTransformed() ? toCompare.getTransformed() : toCompare.getOriginal();
                        if(result.equals(json)) { // TODO replace with cool equal which has additional compare rules
                            toCompare.markAsDuplicate();
                        }
                    }
                }
            }
        });

        Map<ResourceLocation, List<RawRecipe>> duplicates = new HashMap<>();
        rawRecipesByType.forEach((type, rawRecipes) -> {
            List<RawRecipe> duplicatesForType = rawRecipes.stream().filter(RawRecipe::isDuplicate).collect(Collectors.toList());
            if(!duplicatesForType.isEmpty()) {
                duplicates.put(type, duplicatesForType);
            }
        });

        recipeTransformationResult.end();

//        for (var entry : recipes.entrySet()) {
//            if (!hasValidType(entry.getValue().getAsJsonObject())) {
//                continue;
//            }
//
//            if (entry.getValue() instanceof JsonObject json) {
//                JsonObject result = transformRecipe(entry.getKey(), json);
//                recipeTransformationResult.track(entry.getKey(), json, result);
//                if (result != null) {
//                    entry.setValue(result);
//                }
//            }
//        }
        return recipeTransformationResult;
    }

    @Nullable
    public JsonObject transformRecipe(ResourceLocation recipeId, JsonObject json) {
        try {
            RecipeContextImpl ctx = new RecipeContextImpl(json, replacementMap);
            RecipeTransformationBuilderImpl builder = new RecipeTransformationBuilderImpl();
            factory.fillTransformations(builder, ctx);
            JsonObject result = builder.transform(json, ctx);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            AlmostUnified.LOG.warn("Error transforming recipe '{}': {}",
                    recipeId,
                    e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
