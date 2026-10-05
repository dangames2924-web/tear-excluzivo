package com.exemplo.tearexclusivo;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class AlfaiateGui {
    private AlfaiateGui() {}

    public static void abrir(ServerPlayerEntity jogador, Config cfg) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, jogador, false);
        gui.setTitle(Text.literal("Alfaiate dos Pokémon"));

        int slot = 0;
        for (Config.Roupa roupa : cfg.roupas) {
            if (slot >= 54) break; // até 54 roupas (tela grande)
            Item resultado = item(roupa.item);
            if (resultado == Items.AIR) continue;

            GuiElementBuilder botao = new GuiElementBuilder(resultado)
                    .setName(Text.literal(roupa.nome).formatted(Formatting.YELLOW))
                    .addLoreLine(Text.literal("Custo:").formatted(Formatting.GRAY));
            for (Config.Custo c : roupa.custo) {
                botao.addLoreLine(Text.literal(" • " + c.quantidade + "x " + item(c.item).getName().getString())
                        .formatted(Formatting.WHITE));
            }
            botao.addLoreLine(Text.literal("Clique para criar").formatted(Formatting.GREEN));
            botao.setCallback((i, tipo, acao) -> criar(jogador, roupa, resultado));

            gui.setSlot(slot++, botao);
        }
        gui.open();
    }

    private static void criar(ServerPlayerEntity jogador, Config.Roupa roupa, Item resultado) {
        PlayerInventory inv = jogador.getInventory();

        for (Config.Custo c : roupa.custo) {
            if (contar(inv, item(c.item)) < c.quantidade) {
                jogador.sendMessage(Text.literal("Faltam materiais!").formatted(Formatting.RED), true);
                return;
            }
        }
        for (Config.Custo c : roupa.custo) {
            remover(inv, item(c.item), c.quantidade);
        }
        inv.offerOrDrop(new ItemStack(resultado));
        jogador.getServerWorld().playSound(null, jogador.getBlockPos(),
                SoundEvents.UI_LOOM_TAKE_RESULT, SoundCategory.BLOCKS, 1f, 1f);
        jogador.sendMessage(Text.literal("Criado: " + roupa.nome).formatted(Formatting.GREEN), true);
    }

    private static Item item(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null ? Items.AIR : Registries.ITEM.get(identifier);
    }

    private static int contar(PlayerInventory inv, Item item) {
        int total = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) total += s.getCount();
        }
        return total;
    }

    private static void remover(PlayerInventory inv, Item item, int quantidade) {
        int restante = quantidade;
        for (int i = 0; i < inv.size() && restante > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) {
                int tirar = Math.min(restante, s.getCount());
                s.decrement(tirar);
                restante -= tirar;
            }
        }
    }
}
