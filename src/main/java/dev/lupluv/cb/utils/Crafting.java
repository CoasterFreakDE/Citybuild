package dev.lupluv.cb.utils;

import dev.lupluv.cb.Citybuild;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

public class Crafting {

    public void loadRecipes(){
        loadPrivateElevator();
        loadPublicElevator();
    }

    public void loadPrivateElevator(){
        Item item = new Item(Material.BLACK_CONCRETE);
        item.setDisplayName("§6§lAufzug");
        item.setLore(Lore.create("§7Nutze diesen Block als Aufzug."));

        // create a NamespacedKey for your recipe
        NamespacedKey key = new NamespacedKey(Citybuild.getPlugin(), "private_elevator");

        // Create our custom recipe variable
        ShapedRecipe recipe = new ShapedRecipe(key, item.build());

        // Here we will set the places. E and S can represent anything, and the letters can be anything. Beware; this is case sensitive.
        recipe.shape("CCC", "CPC", "CCC");

        // Set what the letters represent.
        // E = Emerald, S = Stick
        recipe.setIngredient('C', Material.BLACK_CONCRETE);
        recipe.setIngredient('P', Material.PISTON);

        // Finally, add the recipe to the bukkit recipes
        Bukkit.addRecipe(recipe);
    }

    public void loadPublicElevator(){
        Item item = new Item(Material.WHITE_CONCRETE);
        item.setDisplayName("§6§lAufzug");
        item.setLore(Lore.create("§7Nutze diesen Block als Aufzug."));

        // create a NamespacedKey for your recipe
        NamespacedKey key = new NamespacedKey(Citybuild.getPlugin(), "public_elevator");

        // Create our custom recipe variable
        ShapedRecipe recipe = new ShapedRecipe(key, item.build());

        // Here we will set the places. E and S can represent anything, and the letters can be anything. Beware; this is case sensitive.
        recipe.shape("CCC", "CPC", "CCC");

        // Set what the letters represent.
        // E = Emerald, S = Stick
        recipe.setIngredient('C', Material.WHITE_CONCRETE);
        recipe.setIngredient('P', Material.PISTON);

        // Finally, add the recipe to the bukkit recipes
        Bukkit.addRecipe(recipe);
    }

}
