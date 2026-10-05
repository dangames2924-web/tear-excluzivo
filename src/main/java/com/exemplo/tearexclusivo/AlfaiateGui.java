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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AlfaiateGui {
    private static final int POR_PAGINA = 45;   // 5 linhas de roupas
    private static final int TOTAL_SLOTS = 54;  // 6 linhas (a ultima e a barra de navegacao)

    private AlfaiateGui() {}

    public static void abrir(ServerPlayerEntity jogador, Config cfg) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, jogador, false);
        gui.setTitle(Text.literal("Alfaiate dos Pokémon"));

        List<String> categorias = categorias(cfg);
        if (categorias.size() > 1) {
            menuCategorias(gui, jogador, cfg);
        } else {
            String unica = categorias.isEmpty() ? "Geral" : categorias.get(0);
            mostrarCategoria(gui, jogador, cfg, unica, 0, false);
        }
        gui.open();
    }

    /** Tela inicial: uma pasta por categoria. */
    private static void menuCategorias(SimpleGui gui, ServerPlayerEntity jogador, Config cfg) {
        limpar(gui);
        int slot = 0;
        for (String categoria : categorias(cfg)) {
            if (slot >= TOTAL_SLOTS) break;
            List<Config.Roupa> lista = daCategoria(cfg, categoria);

            Item icone = Items.CHEST;
            for (Config.Roupa r : lista) {
                Item it = item(r.item);
                if (it != Items.AIR) { icone = it; break; }
            }

            GuiElementBuilder botao = new GuiElementBuilder(icone)
                    .setName(Text.literal(categoria).formatted(Formatting.YELLOW))
                    .addLoreLine(Text.literal(lista.size() + " roupas").formatted(Formatting.GRAY))
                    .setCallback((i, tipo, acao) -> mostrarCategoria(gui, jogador, cfg, categoria, 0, true));
            gui.setSlot(slot++, botao);
        }
    }

    /** Lista as roupas de uma categoria, com paginas. */
    private static void mostrarCategoria(SimpleGui gui, ServerPlayerEntity jogador, Config cfg,
                                         String categoria, int pagina, boolean temMenu) {
        limpar(gui);
        List<Config.Roupa> lista = daCategoria(cfg, categoria);
        int paginas = Math.max(1, (lista.size() + POR_PAGINA - 1) / POR_PAGINA);
        final int p = Math.max(0, Math.min(pagina, paginas - 1));
        int inicio = p * POR_PAGINA;

        for (int i = 0; i < POR_PAGINA && inicio + i < lista.size(); i++) {
            Config.Roupa roupa = lista.get(inicio + i);
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
            botao.setCallback((x, tipo, acao) -> criar(jogador, roupa, resultado));
            gui.setSlot(i, botao);
        }

        // Barra de navegacao (ultima linha)
        if (temMenu) {
            gui.setSlot(45, new GuiElementBuilder(Items.BARRIER)
                    .setName(Text.literal("Voltar às categorias").formatted(Formatting.RED))
                    .setCallback((x, tipo, acao) -> menuCategorias(gui, jogador, cfg)));
        }
        if (p > 0) {
            gui.setSlot(48, new GuiElementBuilder(Items.ARROW)
                    .setName(Text.literal("Página anterior").formatted(Formatting.AQUA))
                    .setCallback((x, tipo, acao) -> mostrarCategoria(gui, jogador, cfg, categoria, p - 1, temMenu)));
        }
        gui.setSlot(49, new GuiElementBuilder(Items.PAPER)
                .setName(Text.literal(categoria + " - página " + (p + 1) + "/" + paginas).formatted(Formatting.WHITE)));
        if (p < paginas - 1) {
            gui.setSlot(50, new GuiElementBuilder(Items.ARROW)
                    .setName(Text.literal("Próxima página").formatted(Formatting.AQUA))
                    .setCallback((x, tipo, acao) -> mostrarCategoria(gui, jogador, cfg, categoria, p + 1, temMenu)));
        }
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

    // ---------- auxiliares ----------

    private static void limpar(SimpleGui gui) {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            gui.clearSlot(i);
        }
    }

    private static String categoriaDe(Config.Roupa r) {
        return (r.categoria == null || r.categoria.isBlank()) ? "Geral" : r.categoria;
    }

    private static List<String> categorias(Config cfg) {
        Set<String> nomes = new LinkedHashSet<>();
        for (Config.Roupa r : cfg.roupas) nomes.add(categoriaDe(r));
        return new ArrayList<>(nomes);
    }

    private static List<Config.Roupa> daCategoria(Config cfg, String categoria) {
        List<Config.Roupa> lista = new ArrayList<>();
        for (Config.Roupa r : cfg.roupas) {
            if (categoriaDe(r).equals(categoria)) lista.add(r);
        }
        return lista;
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
