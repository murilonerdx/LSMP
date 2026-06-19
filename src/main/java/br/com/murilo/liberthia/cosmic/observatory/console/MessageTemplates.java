package br.com.murilo.liberthia.cosmic.observatory.console;

import java.util.ArrayList;
import java.util.List;

/**
 * v0.1.22 r50: Templates de mensagens predefinidas pro Caretaker Console.
 *
 * <h2>Categorias</h2>
 * <ul>
 *   <li>JOIN/LEAVE — fake events de outros players entrando/saindo</li>
 *   <li>DEATH — fake death messages</li>
 *   <li>SYSTEM — fake server/admin warnings</li>
 *   <li>COMMAND_FAIL — fake "comando falhou" messages</li>
 *   <li>WHISPER — fake whispers narrativos</li>
 *   <li>OBSERVATION — fake "alguém te observa" hints</li>
 * </ul>
 *
 * <p>Cada template tem um nome curto (label) e o template do texto
 * (já com §-codes). Placeholders:
 * <ul>
 *   <li>{TARGET}: nome do player target</li>
 *   <li>{RANDOM_NAME}: nome random do pool de WrongPlayer</li>
 * </ul>
 */
public final class MessageTemplates {

    public static class Template {
        public final String id;
        public final String label;
        public final String text;
        public final String category;

        public Template(String id, String label, String category, String text) {
            this.id = id;
            this.label = label;
            this.category = category;
            this.text = text;
        }
    }

    /** Catálogo completo (35 templates). */
    public static final List<Template> ALL = new ArrayList<>();

    static {
        // JOIN/LEAVE
        ALL.add(new Template("join_random", "Random join",
                "JOIN_LEAVE", "§e§l{RANDOM_NAME} §ejoined the game"));
        ALL.add(new Template("leave_random", "Random leave",
                "JOIN_LEAVE", "§e§l{RANDOM_NAME} §eleft the game"));
        ALL.add(new Template("rejoin_self", "Você 'rejoined'",
                "JOIN_LEAVE", "§e§l{TARGET} §ejoined the game"));
        ALL.add(new Template("leave_self", "Você saiu (fake)",
                "JOIN_LEAVE", "§e§l{TARGET} §eleft the game"));

        // DEATH
        ALL.add(new Template("death_fall", "Death: caiu",
                "DEATH", "§7§l{RANDOM_NAME} §7fell from a high place"));
        ALL.add(new Template("death_zombie", "Death: zombie",
                "DEATH", "§7§l{RANDOM_NAME} §7was slain by Zombie"));
        ALL.add(new Template("death_skeleton", "Death: skeleton",
                "DEATH", "§7§l{RANDOM_NAME} §7was shot by Skeleton"));
        ALL.add(new Template("death_burn", "Death: queimou",
                "DEATH", "§7§l{RANDOM_NAME} §7burned to death"));
        ALL.add(new Template("death_drown", "Death: afogou",
                "DEATH", "§7§l{RANDOM_NAME} §7drowned"));
        ALL.add(new Template("death_void", "Death: void",
                "DEATH", "§7§l{RANDOM_NAME} §7fell out of the world"));
        ALL.add(new Template("death_self_zombie", "Você morreu (fake)",
                "DEATH", "§7§l{TARGET} §7was slain by Zombie"));
        ALL.add(new Template("death_unknown", "Death misterioso",
                "DEATH", "§7§l{RANDOM_NAME} §7died"));

        // SYSTEM
        ALL.add(new Template("sys_conn_unstable", "Conexão instável",
                "SYSTEM", "§7[§cSistema§7] §cConnection unstable... reconnecting"));
        ALL.add(new Template("sys_saving", "Saving world",
                "SYSTEM", "§7[§cSistema§7] §7Saving world..."));
        ALL.add(new Template("sys_chunk_corrupted", "Chunk corrompido",
                "SYSTEM", "§7[§4Sistema§7] §cChunk corruption detected"));
        ALL.add(new Template("sys_ban_warning", "Ban warning",
                "SYSTEM", "§7[§cAdmin§7] {TARGET} banned for cheating"));
        ALL.add(new Template("sys_kick_warning", "Kick warning",
                "SYSTEM", "§7[§cAdmin§7] {TARGET} will be kicked in 60s"));
        ALL.add(new Template("sys_lagback", "Lag-back",
                "SYSTEM", "§7[§cSistema§7] §cMovement desync detected"));
        ALL.add(new Template("sys_admin_watching", "Admin watching",
                "SYSTEM", "§7[§4Admin§7] §4§oI see you"));

        // COMMAND_FAIL
        ALL.add(new Template("cmd_spawn_fail", "/spawn fail",
                "COMMAND_FAIL", "§c/spawn is unavailable in this dimension"));
        ALL.add(new Template("cmd_home_fail", "/home fail",
                "COMMAND_FAIL", "§cYou do not have a home set"));
        ALL.add(new Template("cmd_tpa_fail", "/tpa fail",
                "COMMAND_FAIL", "§cTeleport request expired"));
        ALL.add(new Template("cmd_unknown", "Unknown command",
                "COMMAND_FAIL", "§cUnknown command. Type /help for help."));

        // WHISPER
        ALL.add(new Template("whisper_name", "Sussurro: seu nome",
                "WHISPER", "§8§o*você ouve seu nome em algum lugar*"));
        ALL.add(new Template("whisper_behind", "Sussurro: atrás",
                "WHISPER", "§8§o*há respiração atrás de você*"));
        ALL.add(new Template("whisper_familiar", "Sussurro: voz familiar",
                "WHISPER", "§8§o*você ouve uma voz familiar*"));
        ALL.add(new Template("whisper_step", "Sussurro: passo",
                "WHISPER", "§8§o*um passo onde não há ninguém*"));
        ALL.add(new Template("whisper_door", "Sussurro: porta",
                "WHISPER", "§8§o*uma porta se fecha distante*"));

        // OBSERVATION
        ALL.add(new Template("obs_watching", "Alguém observa",
                "OBSERVATION", "§8§o*alguém te observa*"));
        ALL.add(new Template("obs_remember", "Memória plantada",
                "OBSERVATION", "§8§o*você se lembra de algo que não fez*"));
        ALL.add(new Template("obs_familiar", "Lugar familiar",
                "OBSERVATION", "§8§o*esse lugar te parece familiar*"));
        ALL.add(new Template("obs_chunk", "Chunk corrompido",
                "OBSERVATION", "§8§o*o chunk treme um instante*"));
        ALL.add(new Template("obs_eyes", "Olhos no céu",
                "OBSERVATION", "§8§o*olhos no céu, em silêncio*"));
        ALL.add(new Template("obs_wrong_pos", "Posição errada",
                "OBSERVATION", "§8§o*sua posição parece estar errada*"));
    }

    public static List<Template> getByCategory(String cat) {
        List<Template> out = new ArrayList<>();
        for (Template t : ALL) if (t.category.equals(cat)) out.add(t);
        return out;
    }

    public static Template byId(String id) {
        for (Template t : ALL) if (t.id.equals(id)) return t;
        return null;
    }

    /** Substitui placeholders. */
    public static String resolve(Template t, String targetName, String randomName) {
        return t.text
                .replace("{TARGET}", targetName)
                .replace("{RANDOM_NAME}", randomName);
    }

    private MessageTemplates() {}
}
