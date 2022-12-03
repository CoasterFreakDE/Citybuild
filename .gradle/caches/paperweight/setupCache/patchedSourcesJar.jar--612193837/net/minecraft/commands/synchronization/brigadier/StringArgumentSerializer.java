package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

public class StringArgumentSerializer implements ArgumentTypeInfo<StringArgumentType, StringArgumentSerializer.Template> {
    @Override
    public void serializeToNetwork(StringArgumentSerializer.Template properties, FriendlyByteBuf buf) {
        buf.writeEnum(properties.type);
    }

    @Override
    public StringArgumentSerializer.Template deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
        StringArgumentType.StringType stringType = friendlyByteBuf.readEnum(StringArgumentType.StringType.class);
        return new StringArgumentSerializer.Template(stringType);
    }

    @Override
    public void serializeToJson(StringArgumentSerializer.Template properties, JsonObject json) {
        String var10002;
        switch (properties.type) {
            case SINGLE_WORD:
                var10002 = "word";
                break;
            case QUOTABLE_PHRASE:
                var10002 = "phrase";
                break;
            case GREEDY_PHRASE:
                var10002 = "greedy";
                break;
            default:
                throw new IncompatibleClassChangeError();
        }

        json.addProperty("type", var10002);
    }

    @Override
    public StringArgumentSerializer.Template unpack(StringArgumentType argumentType) {
        return new StringArgumentSerializer.Template(argumentType.getType());
    }

    public final class Template implements ArgumentTypeInfo.Template<StringArgumentType> {
        final StringArgumentType.StringType type;

        public Template(StringArgumentType.StringType type) {
            this.type = type;
        }

        @Override
        public StringArgumentType instantiate(CommandBuildContext commandBuildContext) {
            StringArgumentType var10000;
            switch (this.type) {
                case SINGLE_WORD:
                    var10000 = StringArgumentType.word();
                    break;
                case QUOTABLE_PHRASE:
                    var10000 = StringArgumentType.string();
                    break;
                case GREEDY_PHRASE:
                    var10000 = StringArgumentType.greedyString();
                    break;
                default:
                    throw new IncompatibleClassChangeError();
            }

            return var10000;
        }

        @Override
        public ArgumentTypeInfo<StringArgumentType, ?> type() {
            return StringArgumentSerializer.this;
        }
    }
}
