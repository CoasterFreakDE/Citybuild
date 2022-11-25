package dev.lupluv.cb.utils;

import org.bukkit.Material;

public enum CbItem {

    DIAMOND,
    GOLD_INGOT,
    RAW_IRON,
    COPPER_INGOT,
    REDSTONE_WIRE,
    LAPIS_LAZULI,
    COAL,
    DARK_OAK_LOG,
    JUNGLE_LOG,
    ACACIA_LOG,
    MANGROVE_LOG,
    BIRCH_LOG,
    OAK_LOG,
    SPRUCE_LOG,
    CRIMSON_STEM,
    WARPED_STEM,
    ANCIENT_DEBRIS,
    GLOWSTONE,
    BRICKS,
    BEDROCK,
    AMETHYST_BLOCK,
    SPAWNER,
    BARRIER,
    BEACON,
    LIGHT,
    END_PORTAL_FRAME,
    DRAGON_EGG,
    DRAGON_HEAD;

    public Material getMaterial(){
        Material mat;
        mat = Material.getMaterial(this.toString());
        return mat;
    }

}
