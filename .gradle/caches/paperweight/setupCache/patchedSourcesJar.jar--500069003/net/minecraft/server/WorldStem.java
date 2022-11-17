package net.minecraft.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.level.storage.WorldData;

public record WorldStem(CloseableResourceManager resourceManager, ReloadableServerResources dataPackResources, RegistryAccess.Frozen registryAccess, WorldData worldData) implements AutoCloseable {
    public static CompletableFuture<WorldStem> load(WorldLoader.InitConfig serverConfig, WorldLoader.WorldDataSupplier<WorldData> savePropertiesSupplier, Executor prepareExecutor, Executor applyExecutor) {
        return WorldLoader.load(serverConfig, savePropertiesSupplier, WorldStem::new, prepareExecutor, applyExecutor);
    }

    @Override
    public void close() {
        this.resourceManager.close();
    }
}
