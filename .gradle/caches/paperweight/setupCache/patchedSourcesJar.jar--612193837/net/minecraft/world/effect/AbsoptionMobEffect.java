package net.minecraft.world.effect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class AbsoptionMobEffect extends MobEffect {
    protected AbsoptionMobEffect(MobEffectCategory mobEffectCategory, int color) {
        super(mobEffectCategory, color);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        entity.setAbsorptionAmount(entity.getAbsorptionAmount() - (float)(4 * (amplifier + 1)));
        super.removeAttributeModifiers(entity, attributes, amplifier);
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        entity.setAbsorptionAmount(entity.getAbsorptionAmount() + (float)(4 * (amplifier + 1)));
        super.addAttributeModifiers(entity, attributes, amplifier);
    }
}
