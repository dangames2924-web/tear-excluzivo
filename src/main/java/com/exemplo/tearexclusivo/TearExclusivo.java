package com.exemplo.tearexclusivo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.nio.file.Path;

public class TearExclusivo implements ModInitializer {
    private static final Path CONFIG =
            FabricLoader.getInstance().getConfigDir().resolve("tear_exclusivo.json");
    private static Config cfg;

    @Override
    public void onInitialize() {
        cfg = Config.carregar(CONFIG);

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (player.isSneaking() && !player.getStackInHand(hand).isEmpty()) return ActionResult.PASS;
            if (!world.getBlockState(hit.getBlockPos()).isOf(Blocks.LOOM)) return ActionResult.PASS;

            if (!cfg.autorizado(player.getUuid())) {
                player.sendMessage(Text.literal("Apenas o alfaiate oficial pode usar o tear!"), true);
                return ActionResult.FAIL;
            }
            if (player instanceof ServerPlayerEntity sp) {
                AlfaiateGui.abrir(sp, cfg);
            }
            return ActionResult.SUCCESS;
        });

        // /alfaiate reload  (somente operadores)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
                dispatcher.register(CommandManager.literal("alfaiate")
                        .requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.literal("reload").executes(ctx -> {
                            cfg = Config.carregar(CONFIG);
                            ctx.getSource().sendFeedback(() -> Text.literal("Config do alfaiate recarregada."), false);
                            return 1;
                        }))));
    }
}
