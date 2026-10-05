package com.exemplo.tearexclusivo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Config em config/tear_exclusivo.json */
public class Config {
    public List<String> autorizados = new ArrayList<>();
    public List<Roupa> roupas = new ArrayList<>();

    public static class Roupa {
        public String nome;                      // nome mostrado na tela
        public String item;                      // item entregue (o que o pack de roupas usa), ex: "minecraft:red_wool"
        public List<Custo> custo = new ArrayList<>();
    }

    public static class Custo {
        public String item;                      // ex: "minecraft:white_wool"
        public int quantidade;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean autorizado(UUID uuid) {
        return autorizados.stream().anyMatch(s -> s.equalsIgnoreCase(uuid.toString()));
    }

    public static Config carregar(Path arquivo) {
        try {
            if (!Files.exists(arquivo)) {
                Config padrao = new Config();
                Roupa exemplo = new Roupa();
                exemplo.nome = "Roupa de exemplo";
                exemplo.item = "minecraft:red_wool";
                Custo c1 = new Custo();
                c1.item = "minecraft:white_wool";
                c1.quantidade = 2;
                Custo c2 = new Custo();
                c2.item = "minecraft:red_dye";
                c2.quantidade = 1;
                exemplo.custo.add(c1);
                exemplo.custo.add(c2);
                padrao.roupas.add(exemplo);
                Files.writeString(arquivo, GSON.toJson(padrao));
                return padrao;
            }
            Config cfg = GSON.fromJson(Files.readString(arquivo), Config.class);
            return cfg != null ? cfg : new Config();
        } catch (Exception e) {
            System.err.println("[TearExclusivo] Erro ao ler config: " + e.getMessage());
            return new Config();
        }
    }
}
