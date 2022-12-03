package net.minecraft.world.level.levelgen;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseRouterData {
    public static final float GLOBAL_OFFSET = -0.50375F;
    private static final float ORE_THICKNESS = 0.08F;
    private static final double VEININESS_FREQUENCY = 1.5D;
    private static final double NOODLE_SPACING_AND_STRAIGHTNESS = 1.5D;
    private static final double SURFACE_DENSITY_THRESHOLD = 1.5625D;
    private static final double CHEESE_NOISE_TARGET = -0.703125D;
    public static final int ISLAND_CHUNK_DISTANCE = 64;
    public static final long ISLAND_CHUNK_DISTANCE_SQR = 4096L;
    private static final DensityFunction BLENDING_FACTOR = DensityFunctions.constant(10.0D);
    private static final DensityFunction BLENDING_JAGGEDNESS = DensityFunctions.zero();
    private static final ResourceKey<DensityFunction> ZERO = createKey("zero");
    private static final ResourceKey<DensityFunction> Y = createKey("y");
    private static final ResourceKey<DensityFunction> SHIFT_X = createKey("shift_x");
    private static final ResourceKey<DensityFunction> SHIFT_Z = createKey("shift_z");
    private static final ResourceKey<DensityFunction> BASE_3D_NOISE_OVERWORLD = createKey("overworld/base_3d_noise");
    private static final ResourceKey<DensityFunction> BASE_3D_NOISE_NETHER = createKey("nether/base_3d_noise");
    private static final ResourceKey<DensityFunction> BASE_3D_NOISE_END = createKey("end/base_3d_noise");
    public static final ResourceKey<DensityFunction> CONTINENTS = createKey("overworld/continents");
    public static final ResourceKey<DensityFunction> EROSION = createKey("overworld/erosion");
    public static final ResourceKey<DensityFunction> RIDGES = createKey("overworld/ridges");
    public static final ResourceKey<DensityFunction> RIDGES_FOLDED = createKey("overworld/ridges_folded");
    public static final ResourceKey<DensityFunction> OFFSET = createKey("overworld/offset");
    public static final ResourceKey<DensityFunction> FACTOR = createKey("overworld/factor");
    public static final ResourceKey<DensityFunction> JAGGEDNESS = createKey("overworld/jaggedness");
    public static final ResourceKey<DensityFunction> DEPTH = createKey("overworld/depth");
    private static final ResourceKey<DensityFunction> SLOPED_CHEESE = createKey("overworld/sloped_cheese");
    public static final ResourceKey<DensityFunction> CONTINENTS_LARGE = createKey("overworld_large_biomes/continents");
    public static final ResourceKey<DensityFunction> EROSION_LARGE = createKey("overworld_large_biomes/erosion");
    private static final ResourceKey<DensityFunction> OFFSET_LARGE = createKey("overworld_large_biomes/offset");
    private static final ResourceKey<DensityFunction> FACTOR_LARGE = createKey("overworld_large_biomes/factor");
    private static final ResourceKey<DensityFunction> JAGGEDNESS_LARGE = createKey("overworld_large_biomes/jaggedness");
    private static final ResourceKey<DensityFunction> DEPTH_LARGE = createKey("overworld_large_biomes/depth");
    private static final ResourceKey<DensityFunction> SLOPED_CHEESE_LARGE = createKey("overworld_large_biomes/sloped_cheese");
    private static final ResourceKey<DensityFunction> OFFSET_AMPLIFIED = createKey("overworld_amplified/offset");
    private static final ResourceKey<DensityFunction> FACTOR_AMPLIFIED = createKey("overworld_amplified/factor");
    private static final ResourceKey<DensityFunction> JAGGEDNESS_AMPLIFIED = createKey("overworld_amplified/jaggedness");
    private static final ResourceKey<DensityFunction> DEPTH_AMPLIFIED = createKey("overworld_amplified/depth");
    private static final ResourceKey<DensityFunction> SLOPED_CHEESE_AMPLIFIED = createKey("overworld_amplified/sloped_cheese");
    private static final ResourceKey<DensityFunction> SLOPED_CHEESE_END = createKey("end/sloped_cheese");
    private static final ResourceKey<DensityFunction> SPAGHETTI_ROUGHNESS_FUNCTION = createKey("overworld/caves/spaghetti_roughness_function");
    private static final ResourceKey<DensityFunction> ENTRANCES = createKey("overworld/caves/entrances");
    private static final ResourceKey<DensityFunction> NOODLE = createKey("overworld/caves/noodle");
    private static final ResourceKey<DensityFunction> PILLARS = createKey("overworld/caves/pillars");
    private static final ResourceKey<DensityFunction> SPAGHETTI_2D_THICKNESS_MODULATOR = createKey("overworld/caves/spaghetti_2d_thickness_modulator");
    private static final ResourceKey<DensityFunction> SPAGHETTI_2D = createKey("overworld/caves/spaghetti_2d");

    private static ResourceKey<DensityFunction> createKey(String id) {
        return ResourceKey.create(Registry.DENSITY_FUNCTION_REGISTRY, new ResourceLocation(id));
    }

    public static Holder<? extends DensityFunction> bootstrap(Registry<DensityFunction> registry) {
        register(registry, ZERO, DensityFunctions.zero());
        int i = DimensionType.MIN_Y * 2;
        int j = DimensionType.MAX_Y * 2;
        register(registry, Y, DensityFunctions.yClampedGradient(i, j, (double)i, (double)j));
        DensityFunction densityFunction = registerAndWrap(registry, SHIFT_X, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftA(getNoise(Noises.SHIFT)))));
        DensityFunction densityFunction2 = registerAndWrap(registry, SHIFT_Z, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftB(getNoise(Noises.SHIFT)))));
        register(registry, BASE_3D_NOISE_OVERWORLD, BlendedNoise.createUnseeded(0.25D, 0.125D, 80.0D, 160.0D, 8.0D));
        register(registry, BASE_3D_NOISE_NETHER, BlendedNoise.createUnseeded(0.25D, 0.375D, 80.0D, 60.0D, 8.0D));
        register(registry, BASE_3D_NOISE_END, BlendedNoise.createUnseeded(0.25D, 0.25D, 80.0D, 160.0D, 4.0D));
        Holder<DensityFunction> holder = register(registry, CONTINENTS, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.CONTINENTALNESS))));
        Holder<DensityFunction> holder2 = register(registry, EROSION, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.EROSION))));
        DensityFunction densityFunction3 = registerAndWrap(registry, RIDGES, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.RIDGE))));
        register(registry, RIDGES_FOLDED, peaksAndValleys(densityFunction3));
        DensityFunction densityFunction4 = DensityFunctions.noise(getNoise(Noises.JAGGED), 1500.0D, 0.0D);
        registerTerrainNoises(registry, densityFunction4, holder, holder2, OFFSET, FACTOR, JAGGEDNESS, DEPTH, SLOPED_CHEESE, false);
        Holder<DensityFunction> holder3 = register(registry, CONTINENTS_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.CONTINENTALNESS_LARGE))));
        Holder<DensityFunction> holder4 = register(registry, EROSION_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.EROSION_LARGE))));
        registerTerrainNoises(registry, densityFunction4, holder3, holder4, OFFSET_LARGE, FACTOR_LARGE, JAGGEDNESS_LARGE, DEPTH_LARGE, SLOPED_CHEESE_LARGE, false);
        registerTerrainNoises(registry, densityFunction4, holder, holder2, OFFSET_AMPLIFIED, FACTOR_AMPLIFIED, JAGGEDNESS_AMPLIFIED, DEPTH_AMPLIFIED, SLOPED_CHEESE_AMPLIFIED, true);
        register(registry, SLOPED_CHEESE_END, DensityFunctions.add(DensityFunctions.endIslands(0L), getFunction(registry, BASE_3D_NOISE_END)));
        register(registry, SPAGHETTI_ROUGHNESS_FUNCTION, spaghettiRoughnessFunction());
        register(registry, SPAGHETTI_2D_THICKNESS_MODULATOR, DensityFunctions.cacheOnce(DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_2D_THICKNESS), 2.0D, 1.0D, -0.6D, -1.3D)));
        register(registry, SPAGHETTI_2D, spaghetti2D(registry));
        register(registry, ENTRANCES, entrances(registry));
        register(registry, NOODLE, noodle(registry));
        return register(registry, PILLARS, pillars());
    }

    private static void registerTerrainNoises(Registry<DensityFunction> registry, DensityFunction jaggedNoise, Holder<DensityFunction> continents, Holder<DensityFunction> erosion, ResourceKey<DensityFunction> offsetKey, ResourceKey<DensityFunction> factorKey, ResourceKey<DensityFunction> jaggednessKey, ResourceKey<DensityFunction> depthKey, ResourceKey<DensityFunction> slopedCheeseKey, boolean amplified) {
        DensityFunctions.Spline.Coordinate coordinate = new DensityFunctions.Spline.Coordinate(continents);
        DensityFunctions.Spline.Coordinate coordinate2 = new DensityFunctions.Spline.Coordinate(erosion);
        DensityFunctions.Spline.Coordinate coordinate3 = new DensityFunctions.Spline.Coordinate(registry.getHolderOrThrow(RIDGES));
        DensityFunctions.Spline.Coordinate coordinate4 = new DensityFunctions.Spline.Coordinate(registry.getHolderOrThrow(RIDGES_FOLDED));
        DensityFunction densityFunction = registerAndWrap(registry, offsetKey, splineWithBlending(DensityFunctions.add(DensityFunctions.constant((double)-0.50375F), DensityFunctions.spline(TerrainProvider.overworldOffset(coordinate, coordinate2, coordinate4, amplified))), DensityFunctions.blendOffset()));
        DensityFunction densityFunction2 = registerAndWrap(registry, factorKey, splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldFactor(coordinate, coordinate2, coordinate3, coordinate4, amplified)), BLENDING_FACTOR));
        DensityFunction densityFunction3 = registerAndWrap(registry, depthKey, DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 1.5D, -1.5D), densityFunction));
        DensityFunction densityFunction4 = registerAndWrap(registry, jaggednessKey, splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldJaggedness(coordinate, coordinate2, coordinate3, coordinate4, amplified)), BLENDING_JAGGEDNESS));
        DensityFunction densityFunction5 = DensityFunctions.mul(densityFunction4, jaggedNoise.halfNegative());
        DensityFunction densityFunction6 = noiseGradientDensity(densityFunction2, DensityFunctions.add(densityFunction3, densityFunction5));
        register(registry, slopedCheeseKey, DensityFunctions.add(densityFunction6, getFunction(registry, BASE_3D_NOISE_OVERWORLD)));
    }

    private static DensityFunction registerAndWrap(Registry<DensityFunction> registry, ResourceKey<DensityFunction> key, DensityFunction densityFunction) {
        return new DensityFunctions.HolderHolder(BuiltinRegistries.register(registry, key, densityFunction));
    }

    private static Holder<DensityFunction> register(Registry<DensityFunction> registry, ResourceKey<DensityFunction> key, DensityFunction densityFunction) {
        return BuiltinRegistries.register(registry, key, densityFunction);
    }

    private static Holder<NormalNoise.NoiseParameters> getNoise(ResourceKey<NormalNoise.NoiseParameters> key) {
        return BuiltinRegistries.NOISE.getHolderOrThrow(key);
    }

    private static DensityFunction getFunction(Registry<DensityFunction> registry, ResourceKey<DensityFunction> key) {
        return new DensityFunctions.HolderHolder(registry.getHolderOrThrow(key));
    }

    private static DensityFunction peaksAndValleys(DensityFunction input) {
        return DensityFunctions.mul(DensityFunctions.add(DensityFunctions.add(input.abs(), DensityFunctions.constant(-0.6666666666666666D)).abs(), DensityFunctions.constant(-0.3333333333333333D)), DensityFunctions.constant(-3.0D));
    }

    public static float peaksAndValleys(float weirdness) {
        return -(Math.abs(Math.abs(weirdness) - 0.6666667F) - 0.33333334F) * 3.0F;
    }

    private static DensityFunction spaghettiRoughnessFunction() {
        DensityFunction densityFunction = DensityFunctions.noise(getNoise(Noises.SPAGHETTI_ROUGHNESS));
        DensityFunction densityFunction2 = DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_ROUGHNESS_MODULATOR), 0.0D, -0.1D);
        return DensityFunctions.cacheOnce(DensityFunctions.mul(densityFunction2, DensityFunctions.add(densityFunction.abs(), DensityFunctions.constant(-0.4D))));
    }

    private static DensityFunction entrances(Registry<DensityFunction> registry) {
        DensityFunction densityFunction = DensityFunctions.cacheOnce(DensityFunctions.noise(getNoise(Noises.SPAGHETTI_3D_RARITY), 2.0D, 1.0D));
        DensityFunction densityFunction2 = DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_3D_THICKNESS), -0.065D, -0.088D);
        DensityFunction densityFunction3 = DensityFunctions.weirdScaledSampler(densityFunction, getNoise(Noises.SPAGHETTI_3D_1), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1);
        DensityFunction densityFunction4 = DensityFunctions.weirdScaledSampler(densityFunction, getNoise(Noises.SPAGHETTI_3D_2), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1);
        DensityFunction densityFunction5 = DensityFunctions.add(DensityFunctions.max(densityFunction3, densityFunction4), densityFunction2).clamp(-1.0D, 1.0D);
        DensityFunction densityFunction6 = getFunction(registry, SPAGHETTI_ROUGHNESS_FUNCTION);
        DensityFunction densityFunction7 = DensityFunctions.noise(getNoise(Noises.CAVE_ENTRANCE), 0.75D, 0.5D);
        DensityFunction densityFunction8 = DensityFunctions.add(DensityFunctions.add(densityFunction7, DensityFunctions.constant(0.37D)), DensityFunctions.yClampedGradient(-10, 30, 0.3D, 0.0D));
        return DensityFunctions.cacheOnce(DensityFunctions.min(densityFunction8, DensityFunctions.add(densityFunction6, densityFunction5)));
    }

    private static DensityFunction noodle(Registry<DensityFunction> registry) {
        DensityFunction densityFunction = getFunction(registry, Y);
        int i = -64;
        int j = -60;
        int k = 320;
        DensityFunction densityFunction2 = yLimitedInterpolatable(densityFunction, DensityFunctions.noise(getNoise(Noises.NOODLE), 1.0D, 1.0D), -60, 320, -1);
        DensityFunction densityFunction3 = yLimitedInterpolatable(densityFunction, DensityFunctions.mappedNoise(getNoise(Noises.NOODLE_THICKNESS), 1.0D, 1.0D, -0.05D, -0.1D), -60, 320, 0);
        double d = 2.6666666666666665D;
        DensityFunction densityFunction4 = yLimitedInterpolatable(densityFunction, DensityFunctions.noise(getNoise(Noises.NOODLE_RIDGE_A), 2.6666666666666665D, 2.6666666666666665D), -60, 320, 0);
        DensityFunction densityFunction5 = yLimitedInterpolatable(densityFunction, DensityFunctions.noise(getNoise(Noises.NOODLE_RIDGE_B), 2.6666666666666665D, 2.6666666666666665D), -60, 320, 0);
        DensityFunction densityFunction6 = DensityFunctions.mul(DensityFunctions.constant(1.5D), DensityFunctions.max(densityFunction4.abs(), densityFunction5.abs()));
        return DensityFunctions.rangeChoice(densityFunction2, -1000000.0D, 0.0D, DensityFunctions.constant(64.0D), DensityFunctions.add(densityFunction3, densityFunction6));
    }

    private static DensityFunction pillars() {
        double d = 25.0D;
        double e = 0.3D;
        DensityFunction densityFunction = DensityFunctions.noise(getNoise(Noises.PILLAR), 25.0D, 0.3D);
        DensityFunction densityFunction2 = DensityFunctions.mappedNoise(getNoise(Noises.PILLAR_RARENESS), 0.0D, -2.0D);
        DensityFunction densityFunction3 = DensityFunctions.mappedNoise(getNoise(Noises.PILLAR_THICKNESS), 0.0D, 1.1D);
        DensityFunction densityFunction4 = DensityFunctions.add(DensityFunctions.mul(densityFunction, DensityFunctions.constant(2.0D)), densityFunction2);
        return DensityFunctions.cacheOnce(DensityFunctions.mul(densityFunction4, densityFunction3.cube()));
    }

    private static DensityFunction spaghetti2D(Registry<DensityFunction> registry) {
        DensityFunction densityFunction = DensityFunctions.noise(getNoise(Noises.SPAGHETTI_2D_MODULATOR), 2.0D, 1.0D);
        DensityFunction densityFunction2 = DensityFunctions.weirdScaledSampler(densityFunction, getNoise(Noises.SPAGHETTI_2D), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE2);
        DensityFunction densityFunction3 = DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_2D_ELEVATION), 0.0D, (double)Math.floorDiv(-64, 8), 8.0D);
        DensityFunction densityFunction4 = getFunction(registry, SPAGHETTI_2D_THICKNESS_MODULATOR);
        DensityFunction densityFunction5 = DensityFunctions.add(densityFunction3, DensityFunctions.yClampedGradient(-64, 320, 8.0D, -40.0D)).abs();
        DensityFunction densityFunction6 = DensityFunctions.add(densityFunction5, densityFunction4).cube();
        double d = 0.083D;
        DensityFunction densityFunction7 = DensityFunctions.add(densityFunction2, DensityFunctions.mul(DensityFunctions.constant(0.083D), densityFunction4));
        return DensityFunctions.max(densityFunction7, densityFunction6).clamp(-1.0D, 1.0D);
    }

    private static DensityFunction underground(Registry<DensityFunction> registry, DensityFunction slopedCheese) {
        DensityFunction densityFunction = getFunction(registry, SPAGHETTI_2D);
        DensityFunction densityFunction2 = getFunction(registry, SPAGHETTI_ROUGHNESS_FUNCTION);
        DensityFunction densityFunction3 = DensityFunctions.noise(getNoise(Noises.CAVE_LAYER), 8.0D);
        DensityFunction densityFunction4 = DensityFunctions.mul(DensityFunctions.constant(4.0D), densityFunction3.square());
        DensityFunction densityFunction5 = DensityFunctions.noise(getNoise(Noises.CAVE_CHEESE), 0.6666666666666666D);
        DensityFunction densityFunction6 = DensityFunctions.add(DensityFunctions.add(DensityFunctions.constant(0.27D), densityFunction5).clamp(-1.0D, 1.0D), DensityFunctions.add(DensityFunctions.constant(1.5D), DensityFunctions.mul(DensityFunctions.constant(-0.64D), slopedCheese)).clamp(0.0D, 0.5D));
        DensityFunction densityFunction7 = DensityFunctions.add(densityFunction4, densityFunction6);
        DensityFunction densityFunction8 = DensityFunctions.min(DensityFunctions.min(densityFunction7, getFunction(registry, ENTRANCES)), DensityFunctions.add(densityFunction, densityFunction2));
        DensityFunction densityFunction9 = getFunction(registry, PILLARS);
        DensityFunction densityFunction10 = DensityFunctions.rangeChoice(densityFunction9, -1000000.0D, 0.03D, DensityFunctions.constant(-1000000.0D), densityFunction9);
        return DensityFunctions.max(densityFunction8, densityFunction10);
    }

    private static DensityFunction postProcess(DensityFunction density) {
        DensityFunction densityFunction = DensityFunctions.blendDensity(density);
        return DensityFunctions.mul(DensityFunctions.interpolated(densityFunction), DensityFunctions.constant(0.64D)).squeeze();
    }

    protected static NoiseRouter overworld(Registry<DensityFunction> registry, boolean largeBiomes, boolean amplified) {
        DensityFunction densityFunction = DensityFunctions.noise(getNoise(Noises.AQUIFER_BARRIER), 0.5D);
        DensityFunction densityFunction2 = DensityFunctions.noise(getNoise(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67D);
        DensityFunction densityFunction3 = DensityFunctions.noise(getNoise(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143D);
        DensityFunction densityFunction4 = DensityFunctions.noise(getNoise(Noises.AQUIFER_LAVA));
        DensityFunction densityFunction5 = getFunction(registry, SHIFT_X);
        DensityFunction densityFunction6 = getFunction(registry, SHIFT_Z);
        DensityFunction densityFunction7 = DensityFunctions.shiftedNoise2d(densityFunction5, densityFunction6, 0.25D, getNoise(largeBiomes ? Noises.TEMPERATURE_LARGE : Noises.TEMPERATURE));
        DensityFunction densityFunction8 = DensityFunctions.shiftedNoise2d(densityFunction5, densityFunction6, 0.25D, getNoise(largeBiomes ? Noises.VEGETATION_LARGE : Noises.VEGETATION));
        DensityFunction densityFunction9 = getFunction(registry, largeBiomes ? FACTOR_LARGE : (amplified ? FACTOR_AMPLIFIED : FACTOR));
        DensityFunction densityFunction10 = getFunction(registry, largeBiomes ? DEPTH_LARGE : (amplified ? DEPTH_AMPLIFIED : DEPTH));
        DensityFunction densityFunction11 = noiseGradientDensity(DensityFunctions.cache2d(densityFunction9), densityFunction10);
        DensityFunction densityFunction12 = getFunction(registry, largeBiomes ? SLOPED_CHEESE_LARGE : (amplified ? SLOPED_CHEESE_AMPLIFIED : SLOPED_CHEESE));
        DensityFunction densityFunction13 = DensityFunctions.min(densityFunction12, DensityFunctions.mul(DensityFunctions.constant(5.0D), getFunction(registry, ENTRANCES)));
        DensityFunction densityFunction14 = DensityFunctions.rangeChoice(densityFunction12, -1000000.0D, 1.5625D, densityFunction13, underground(registry, densityFunction12));
        DensityFunction densityFunction15 = DensityFunctions.min(postProcess(slideOverworld(amplified, densityFunction14)), getFunction(registry, NOODLE));
        DensityFunction densityFunction16 = getFunction(registry, Y);
        int i = Stream.of(OreVeinifier.VeinType.values()).mapToInt((veinType) -> {
            return veinType.minY;
        }).min().orElse(-DimensionType.MIN_Y * 2);
        int j = Stream.of(OreVeinifier.VeinType.values()).mapToInt((veinType) -> {
            return veinType.maxY;
        }).max().orElse(-DimensionType.MIN_Y * 2);
        DensityFunction densityFunction17 = yLimitedInterpolatable(densityFunction16, DensityFunctions.noise(getNoise(Noises.ORE_VEININESS), 1.5D, 1.5D), i, j, 0);
        float f = 4.0F;
        DensityFunction densityFunction18 = yLimitedInterpolatable(densityFunction16, DensityFunctions.noise(getNoise(Noises.ORE_VEIN_A), 4.0D, 4.0D), i, j, 0).abs();
        DensityFunction densityFunction19 = yLimitedInterpolatable(densityFunction16, DensityFunctions.noise(getNoise(Noises.ORE_VEIN_B), 4.0D, 4.0D), i, j, 0).abs();
        DensityFunction densityFunction20 = DensityFunctions.add(DensityFunctions.constant((double)-0.08F), DensityFunctions.max(densityFunction18, densityFunction19));
        DensityFunction densityFunction21 = DensityFunctions.noise(getNoise(Noises.ORE_GAP));
        return new NoiseRouter(densityFunction, densityFunction2, densityFunction3, densityFunction4, densityFunction7, densityFunction8, getFunction(registry, largeBiomes ? CONTINENTS_LARGE : CONTINENTS), getFunction(registry, largeBiomes ? EROSION_LARGE : EROSION), densityFunction10, getFunction(registry, RIDGES), slideOverworld(amplified, DensityFunctions.add(densityFunction11, DensityFunctions.constant(-0.703125D)).clamp(-64.0D, 64.0D)), densityFunction15, densityFunction17, densityFunction20, densityFunction21);
    }

    private static NoiseRouter noNewCaves(Registry<DensityFunction> registry, DensityFunction density) {
        DensityFunction densityFunction = getFunction(registry, SHIFT_X);
        DensityFunction densityFunction2 = getFunction(registry, SHIFT_Z);
        DensityFunction densityFunction3 = DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.TEMPERATURE));
        DensityFunction densityFunction4 = DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.VEGETATION));
        DensityFunction densityFunction5 = postProcess(density);
        return new NoiseRouter(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), densityFunction3, densityFunction4, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), densityFunction5, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
    }

    private static DensityFunction slideOverworld(boolean amplified, DensityFunction density) {
        return slide(density, -64, 384, amplified ? 16 : 80, amplified ? 0 : 64, -0.078125D, 0, 24, amplified ? 0.4D : 0.1171875D);
    }

    private static DensityFunction slideNetherLike(Registry<DensityFunction> registry, int minY, int maxY) {
        return slide(getFunction(registry, BASE_3D_NOISE_NETHER), minY, maxY, 24, 0, 0.9375D, -8, 24, 2.5D);
    }

    private static DensityFunction slideEndLike(DensityFunction function, int minY, int maxY) {
        return slide(function, minY, maxY, 72, -184, -23.4375D, 4, 32, -0.234375D);
    }

    protected static NoiseRouter nether(Registry<DensityFunction> registry) {
        return noNewCaves(registry, slideNetherLike(registry, 0, 128));
    }

    protected static NoiseRouter caves(Registry<DensityFunction> registry) {
        return noNewCaves(registry, slideNetherLike(registry, -64, 192));
    }

    protected static NoiseRouter floatingIslands(Registry<DensityFunction> registry) {
        return noNewCaves(registry, slideEndLike(getFunction(registry, BASE_3D_NOISE_END), 0, 256));
    }

    private static DensityFunction slideEnd(DensityFunction slopedCheese) {
        return slideEndLike(slopedCheese, 0, 128);
    }

    protected static NoiseRouter end(Registry<DensityFunction> registry) {
        DensityFunction densityFunction = DensityFunctions.cache2d(DensityFunctions.endIslands(0L));
        DensityFunction densityFunction2 = postProcess(slideEnd(getFunction(registry, SLOPED_CHEESE_END)));
        return new NoiseRouter(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), densityFunction, DensityFunctions.zero(), DensityFunctions.zero(), slideEnd(DensityFunctions.add(densityFunction, DensityFunctions.constant(-0.703125D))), densityFunction2, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
    }

    protected static NoiseRouter none() {
        return new NoiseRouter(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
    }

    private static DensityFunction splineWithBlending(DensityFunction function, DensityFunction blendOffset) {
        DensityFunction densityFunction = DensityFunctions.lerp(DensityFunctions.blendAlpha(), blendOffset, function);
        return DensityFunctions.flatCache(DensityFunctions.cache2d(densityFunction));
    }

    private static DensityFunction noiseGradientDensity(DensityFunction factor, DensityFunction depth) {
        DensityFunction densityFunction = DensityFunctions.mul(depth, factor);
        return DensityFunctions.mul(DensityFunctions.constant(4.0D), densityFunction.quarterNegative());
    }

    private static DensityFunction yLimitedInterpolatable(DensityFunction y, DensityFunction whenInRange, int minInclusive, int maxInclusive, int whenOutOfRange) {
        return DensityFunctions.interpolated(DensityFunctions.rangeChoice(y, (double)minInclusive, (double)(maxInclusive + 1), whenInRange, DensityFunctions.constant((double)whenOutOfRange)));
    }

    private static DensityFunction slide(DensityFunction density, int minY, int maxY, int topRelativeMinY, int topRelativeMaxY, double topDensity, int bottomRelativeMinY, int bottomRelativeMaxY, double bottomDensity) {
        DensityFunction densityFunction2 = DensityFunctions.yClampedGradient(minY + maxY - topRelativeMinY, minY + maxY - topRelativeMaxY, 1.0D, 0.0D);
        DensityFunction densityFunction = DensityFunctions.lerp(densityFunction2, topDensity, density);
        DensityFunction densityFunction3 = DensityFunctions.yClampedGradient(minY + bottomRelativeMinY, minY + bottomRelativeMaxY, 0.0D, 1.0D);
        return DensityFunctions.lerp(densityFunction3, bottomDensity, densityFunction);
    }

    protected static final class QuantizedSpaghettiRarity {
        protected static double getSphaghettiRarity2D(double value) {
            if (value < -0.75D) {
                return 0.5D;
            } else if (value < -0.5D) {
                return 0.75D;
            } else if (value < 0.5D) {
                return 1.0D;
            } else {
                return value < 0.75D ? 2.0D : 3.0D;
            }
        }

        protected static double getSpaghettiRarity3D(double value) {
            if (value < -0.5D) {
                return 0.75D;
            } else if (value < 0.0D) {
                return 1.0D;
            } else {
                return value < 0.5D ? 1.5D : 2.0D;
            }
        }
    }
}
