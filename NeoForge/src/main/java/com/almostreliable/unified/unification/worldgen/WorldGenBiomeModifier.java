package com.almostreliable.unified.unification.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import com.almostreliable.unified.AlmostUnifiedCommon;
import com.almostreliable.unified.utils.Utils;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WorldGenBiomeModifier implements BiomeModifier {

    public static final MapCodec<BiomeModifier> CODEC = MapCodec.unit(WorldGenBiomeModifier::new);
    public static final ResourceLocation UNKNOWN_BIOME_ID = Utils.getRL("unknown_biome_id");

    public static void bindUnifier(WorldGenBiomeModifier modifier, RegistryAccess registryAccess) {
        if (AlmostUnifiedCommon.STARTUP_CONFIG.allowWorldGenUnification()) {
            WorldGenUnifier unifier = new WorldGenUnifier(registryAccess);
            unifier.process();
            modifier.unifier = unifier;
        }
    }

    @Nullable
    private WorldGenUnifier unifier;

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.AFTER_EVERYTHING) {
            return;
        }

        if (unifier == null) {
            return;
        }

        Map<GenerationStep.Decoration, List<Holder<PlacedFeature>>> removedFeatures = new LinkedHashMap<>();

        for (GenerationStep.Decoration dec : GenerationStep.Decoration.values()) {
            var features = builder.getGenerationSettings().getFeatures(dec);
            features.removeIf(feature -> {
                if (unifier.shouldRemovePlacedFeature(feature)) {
                    removedFeatures.computeIfAbsent(dec, $ -> new ArrayList<>()).add(feature);
                    return true;
                }

                return false;
            });
        }

        if (!removedFeatures.isEmpty()) {
            AlmostUnifiedCommon.LOGGER.info("[WorldGen] Removed features from Biome {}:",
                biome.unwrapKey().map(ResourceKey::location).orElse(UNKNOWN_BIOME_ID));
            removedFeatures.forEach((decoration, features) -> {
                String ids = features
                    .stream()
                    .flatMap(f -> f.unwrapKey().map(ResourceKey::location).stream())
                    .map(ResourceLocation::toString)
                    .collect(Collectors.joining(", "));

                AlmostUnifiedCommon.LOGGER.info("[WorldGen]\t{}: {}", decoration.getName(), ids);
            });
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
