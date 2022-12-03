package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class ClientboundUpdateAttributesPacket implements Packet<ClientGamePacketListener> {
    private final int entityId;
    private final List<ClientboundUpdateAttributesPacket.AttributeSnapshot> attributes;

    public ClientboundUpdateAttributesPacket(int entityId, Collection<AttributeInstance> attributes) {
        this.entityId = entityId;
        this.attributes = Lists.newArrayList();

        for(AttributeInstance attributeInstance : attributes) {
            this.attributes.add(new ClientboundUpdateAttributesPacket.AttributeSnapshot(attributeInstance.getAttribute(), attributeInstance.getBaseValue(), attributeInstance.getModifiers()));
        }

    }

    public ClientboundUpdateAttributesPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.attributes = buf.readList((buf2) -> {
            ResourceLocation resourceLocation = buf2.readResourceLocation();
            Attribute attribute = Registry.ATTRIBUTE.get(resourceLocation);
            double d = buf2.readDouble();
            List<AttributeModifier> list = buf2.readList((modifiers) -> {
                return new AttributeModifier(modifiers.readUUID(), "Unknown synced attribute modifier", modifiers.readDouble(), AttributeModifier.Operation.fromValue(modifiers.readByte()));
            });
            return new ClientboundUpdateAttributesPacket.AttributeSnapshot(attribute, d, list);
        });
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeCollection(this.attributes, (buf2, attribute) -> {
            buf2.writeResourceLocation(Registry.ATTRIBUTE.getKey(attribute.getAttribute()));
            buf2.writeDouble(attribute.getBase());
            buf2.writeCollection(attribute.getModifiers(), (buf3, modifier) -> {
                buf3.writeUUID(modifier.getId());
                buf3.writeDouble(modifier.getAmount());
                buf3.writeByte(modifier.getOperation().toValue());
            });
        });
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleUpdateAttributes(this);
    }

    public int getEntityId() {
        return this.entityId;
    }

    public List<ClientboundUpdateAttributesPacket.AttributeSnapshot> getValues() {
        return this.attributes;
    }

    public static class AttributeSnapshot {
        private final Attribute attribute;
        private final double base;
        private final Collection<AttributeModifier> modifiers;

        public AttributeSnapshot(Attribute attribute, double baseValue, Collection<AttributeModifier> modifiers) {
            this.attribute = attribute;
            this.base = baseValue;
            this.modifiers = modifiers;
        }

        public Attribute getAttribute() {
            return this.attribute;
        }

        public double getBase() {
            return this.base;
        }

        public Collection<AttributeModifier> getModifiers() {
            return this.modifiers;
        }
    }
}
