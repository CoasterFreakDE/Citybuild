package net.minecraft.world.level.gameevent;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class EuclideanGameEventDispatcher implements GameEventDispatcher {
    private final List<GameEventListener> listeners = Lists.newArrayList();
    private final Set<GameEventListener> listenersToRemove = Sets.newHashSet();
    private final List<GameEventListener> listenersToAdd = Lists.newArrayList();
    private boolean processing;
    private final ServerLevel level;

    public EuclideanGameEventDispatcher(ServerLevel world) {
        this.level = world;
    }

    @Override
    public boolean isEmpty() {
        return this.listeners.isEmpty();
    }

    @Override
    public void register(GameEventListener listener) {
        if (this.processing) {
            this.listenersToAdd.add(listener);
        } else {
            this.listeners.add(listener);
        }

        DebugPackets.sendGameEventListenerInfo(this.level, listener);
    }

    @Override
    public void unregister(GameEventListener listener) {
        if (this.processing) {
            this.listenersToRemove.add(listener);
        } else {
            this.listeners.remove(listener);
        }

    }

    @Override
    public boolean walkListeners(GameEvent event, Vec3 pos, GameEvent.Context emitter, BiConsumer<GameEventListener, Vec3> onListenerAccept) {
        this.processing = true;
        boolean bl = false;

        try {
            Iterator<GameEventListener> iterator = this.listeners.iterator();

            while(iterator.hasNext()) {
                GameEventListener gameEventListener = iterator.next();
                if (this.listenersToRemove.remove(gameEventListener)) {
                    iterator.remove();
                } else {
                    Optional<Vec3> optional = getPostableListenerPosition(this.level, pos, gameEventListener);
                    if (optional.isPresent()) {
                        onListenerAccept.accept(gameEventListener, optional.get());
                        bl = true;
                    }
                }
            }
        } finally {
            this.processing = false;
        }

        if (!this.listenersToAdd.isEmpty()) {
            this.listeners.addAll(this.listenersToAdd);
            this.listenersToAdd.clear();
        }

        if (!this.listenersToRemove.isEmpty()) {
            this.listeners.removeAll(this.listenersToRemove);
            this.listenersToRemove.clear();
        }

        return bl;
    }

    private static Optional<Vec3> getPostableListenerPosition(ServerLevel world, Vec3 listenerPos, GameEventListener listener) {
        Optional<Vec3> optional = listener.getListenerSource().getPosition(world);
        if (optional.isEmpty()) {
            return Optional.empty();
        } else {
            double d = optional.get().distanceToSqr(listenerPos);
            int i = listener.getListenerRadius() * listener.getListenerRadius();
            return d > (double)i ? Optional.empty() : optional;
        }
    }
}
