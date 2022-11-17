package dev.lupluv.cb.stats;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;

public class StatsNPC {

    public NPC getNPC(int i){
        if(i == 1){
            return CitizensAPI.getNPCRegistry().getById(26);
        }else if(i == 2){
            return CitizensAPI.getNPCRegistry().getById(24);
        }else if(i == 3){
            return CitizensAPI.getNPCRegistry().getById(25);
        }
        return null;
    }

    public void updateNpc(int i, String name, String skin){
        NPC npc = getNPC(i);
        npc.getOrAddTrait(net.citizensnpcs.trait.HologramTrait.class).setLine(0, name);
        npc.getOrAddTrait(SkinTrait.class).setSkinName(skin, true);
    }

}
