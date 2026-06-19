package br.com.murilo.liberthia.item.computer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de dados do {@code Computador}: uma lista de "arquivos" (nome + corpo
 * de texto). Compartilhado entre client (tela) e server (persistência).
 *
 * <p>Persiste no NBT do item (nativo do MC, sobrevive restart) e é exportado
 * em JSON pro disco do servidor (legível/editável pra lore e enigmas).
 */
public final class ComputerData {

    public static final String NBT_FILES = "Files";
    public static final String NBT_ID = "CompId";
    public static final int MAX_FILES = 64;
    public static final int MAX_BODY = 8000;
    public static final int MAX_NAME = 48;

    private ComputerData() {}

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_PHOTO = 1; // body = URL da imagem

    public static final class Entry {
        public String name;
        public String body;
        public int type;

        public Entry(String name, String body) {
            this(name, body, TYPE_TEXT);
        }

        public Entry(String name, String body, int type) {
            this.name = name == null ? "" : name;
            this.body = body == null ? "" : body;
            this.type = type;
        }

        public boolean isPhoto() { return type == TYPE_PHOTO; }
    }

    /** NBT ListTag → lista de arquivos. */
    public static List<Entry> fromList(ListTag list) {
        List<Entry> out = new ArrayList<>();
        if (list == null) return out;
        for (int i = 0; i < list.size() && i < MAX_FILES; i++) {
            CompoundTag c = list.getCompound(i);
            out.add(new Entry(c.getString("n"), c.getString("b"), c.getInt("t")));
        }
        return out;
    }

    /** Lista de arquivos → NBT ListTag (com limites de tamanho). */
    public static ListTag toList(List<Entry> files) {
        ListTag list = new ListTag();
        if (files == null) return list;
        int count = 0;
        for (Entry e : files) {
            if (count++ >= MAX_FILES) break;
            CompoundTag c = new CompoundTag();
            c.putString("n", clamp(e.name, MAX_NAME));
            c.putString("b", clamp(e.body, MAX_BODY));
            c.putInt("t", e.type);
            list.add(c);
        }
        return list;
    }

    /** Wrap numa CompoundTag pra trafegar em packet via writeNbt/readNbt. */
    public static CompoundTag wrap(ListTag list) {
        CompoundTag tag = new CompoundTag();
        tag.put(NBT_FILES, list);
        return tag;
    }

    public static ListTag unwrap(CompoundTag tag) {
        if (tag == null) return new ListTag();
        return tag.getList(NBT_FILES, Tag.TAG_COMPOUND);
    }

    /** Exporta os arquivos pra JSON legível (pra lore/enigmas e backup no disco). */
    public static String toJson(List<Entry> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < files.size(); i++) {
            Entry e = files.get(i);
            sb.append("  {\"name\": \"").append(esc(e.name))
                    .append("\", \"type\": \"").append(e.isPhoto() ? "foto" : "texto")
                    .append("\", \"body\": \"").append(esc(e.body)).append("\"}");
            if (i < files.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    private static String clamp(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> b.append(c);
            }
        }
        return b.toString();
    }
}
