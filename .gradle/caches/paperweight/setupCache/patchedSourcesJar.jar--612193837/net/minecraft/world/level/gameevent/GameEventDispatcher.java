package net.minecraft.world.level.gameevent;

import java.util.function.BiConsumer;
import net.minecraft.world.phys.Vec3;

public interface GameEventDispatcher {
    GameEventDispatcher NOOP = new GameEventDispatcher() {
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void register(GameEventListener listener) {
        }

        @Override
        public void unregister(GameEventListener listener) {
        }

        @Override
        public boolean walkListeners(GameEvent event, Vec3 pos, GameEvent.Context emitter, BiConsumer<GameEventListener, Vec3> onListenerAccept) {
            return false;
        }
    };

    boolean isEmpty();

    void register(GameEventListener listener);

    void unregister(GameEventListener listener);

    boolean walkListeners(GameEvent event, Vec3 pos, GameEvent.Context emitter, BiConsumer<GameEventListener, Vec3> onListenerAccept);
}
