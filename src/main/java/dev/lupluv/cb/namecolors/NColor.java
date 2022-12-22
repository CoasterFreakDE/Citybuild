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

}
