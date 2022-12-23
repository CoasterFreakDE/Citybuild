package dev.lupluv.cb.namecolors;

import org.bukkit.Material;

public enum NColor {

    NONE,
    ORANGE,
    MAGENTA,
    LIGHT_GREEN,
    BLUE,
    RED,
    CYAN,
    YELLOW,
    VIOLET,
    GRAD_FLAME,
    GRAD_GREEN,
    GRAD_ORANGE,
    GRAD_AQUA,
    GRAD_CHRISTMAS;

    public String format(String string){
        switch (this){
            case GRAD_AQUA -> {
                return "<#3776e5>" + string + "</#60c5d3>";
            }
            case ORANGE -> {
                return "§6" + string;
            }
            case BLUE -> {
                return "§9" + string;
            }
            case RED -> {
                return "§c" + string;
            }
            case CYAN -> {
                return "§b" + string;
            }
            case VIOLET -> {
                return "§5" + string;
            }
            case YELLOW -> {
                return "§e" + string;
            }
            case MAGENTA -> {
                return "§d" + string;
            }
            case GRAD_CHRISTMAS -> {
                return "<#197a24>" + string + "</#a42121>";
            }
        }
        return string;
    }

    public Material getMaterial(){
        return Material.ACACIA_CHEST_BOAT;
    }

    public String getValue(){
        switch (this){
            case MAGENTA -> {
                return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjA" +
                        "1YzE3NjUwZTVkNzQ3MDEwZThiNjlhNmYyMzYzZmQxMWViOTNmODFjNmNlOTliZjAzODk1Y2VmYjkyYmFhIn19fQ==";
            }
        }
        return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQ3Y2YwZjN" +
                "iOWVjOWRmMjQ4NWE5Y2Q0Nzk1YjYwYTM5MWM4ZTZlYmFjOTYzNTRkZTA2ZTMzNTdhOWE4ODYwNyJ9fX0=";
    }

    public String getName(){
        switch (this){
            case MAGENTA -> {
                return "Magenta";
            }
        }
        return "Nicht verfügbar.";
    }

}
