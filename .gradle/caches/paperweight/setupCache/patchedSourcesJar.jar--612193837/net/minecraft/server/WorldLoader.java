package net.minecraft.server;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.DataPackConfig;

public class WorldLoader {
    public static <D, R> CompletableFuture<R> load(WorldLoader.InitConfig serverConfig, WorldLoader.WorldDataSupplier<D> loadContextSupplier, WorldLoader.ResultFactory<D, R> saveApplierFactory, Executor prepareExecutor, Executor applyExecutor) {
        try {
            Pair<DataPackConfig, CloseableResourceManager> pair = serverConfig.packConfig.createResourceManager();
            CloseableResourceManager closeableResourceManager = pair.getSecond();
            Pair<D, RegistryAccess.Frozen> pair2 = loadContextSupplier.get(closeableResourceManager, pair.getFirst());
            D object = pair2.getFirst();
            RegistryAccess.Frozen frozen = pair2.getSecond();
            return ReloadableServerResources.loadResources(closeableResourceManager, frozen, serverConfig.commandSelection(), serverConfig.functionCompilationLevel(), prepareExecutor, applyExecutor).whenComplete((dataPackContents, throwable) -> {
                if (throwable != null) {
                    closeableResourceManager.close();
                }

            }).thenApplyAsync((dataPackContents) -> {
                dataPackContents.updateRegistryTags(frozen);
                return saveApplierFactory.create(closeableResourceManager, dataPackContents, frozen, object);
            }, applyExecutor);
        } catch (Exception var10) {
            return CompletableFuture.failedFuture(var10);
        }
    }

    public static record InitConfig(WorldLoader.PackConfig packConfig, Commands.CommandSelection commandSelection, int functionCompilationLevel) {
    }

    public static record PackConfig(PackRepository packRepository, DataPackConfig initialDataPacks, boolean safeMode) {
        public Pair<DataPackConfig, CloseableResourceManager> createResourceManager() {
            DataPackConfig dataPackConfig = MinecraftServer.configurePackRepository(this.packRepository, this.initialDataPacks, this.safeMode);
            List<PackResources> list = this.packRepository.openAllSelected();
            CloseableResourceManager closeableResourceManager = new MultiPackResourceManager(PackType.SERVER_DATA, list);
            return Pair.of(dataPackConfig, closeableResourceManager);
        }
    }

    @FunctionalInterface
    public interface ResultFactory<D, R> {
        R create(CloseableResourceManager resourceManager, ReloadableServerResources dataPackContents, RegistryAccess.Frozen dynamicRegistryManager, D loadContext);
    }

    @FunctionalInterface
    public interface WorldDataSupplier<D> {
        Pair<D, RegistryAccess.Frozen> get(ResourceManager resourceManager, DataPackConfig dataPackSettings);
    }
}
