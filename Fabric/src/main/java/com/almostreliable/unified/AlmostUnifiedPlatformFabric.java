package com.almostreliable.unified;

import com.almostreliable.unified.api.ModConstants;
import com.almostreliable.unified.compat.*;
import com.almostreliable.unified.recipe.unifier.RecipeHandlerFactory;
import com.almostreliable.unified.utils.UnifyTag;
import com.google.auto.service.AutoService;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.Item;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@AutoService(AlmostUnifiedPlatform.class)
public class AlmostUnifiedPlatformFabric implements AlmostUnifiedPlatform {

    @Override
    public Platform getPlatform() {
        return Platform.FABRIC;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(BuildConfig.MOD_ID);
    }

    @Override
    public Path getLogPath() {
        return FabricLoader.getInstance().getGameDir().resolve("logs").resolve(BuildConfig.MOD_ID);
    }

    @Override
    public void bindRecipeHandlers(RecipeHandlerFactory factory) {
        factory.registerForMod(ModConstants.AD_ASTRA, new AdAstraRecipeUnifier());
        factory.registerForMod(ModConstants.ALLOY_FORGERY, new AlloyForgeryRecipeUnifier());
        factory.registerForMod(ModConstants.APPLIED_ENERGISTICS, new AppliedEnergisticsUnifier());
        factory.registerForMod(ModConstants.AMETHYST_IMBUEMENT, new AmethystImbuementRecipeUnifier());
        factory.registerForMod(ModConstants.GREGTECH_MODERN, new GregTechModernRecipeUnifier());
        factory.registerForMod(ModConstants.MODERN_INDUSTRIALIZATION, new ModernIndustrializationRecipeUnifier());
    }

    @Override
    public Set<UnifyTag<Item>> getStoneStrataTags(List<String> stoneStrataIds) {
        return Set.of();
    }
}
