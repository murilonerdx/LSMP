-- ============================================================================
-- Liberthia Admin — schema completo + dados iniciais
-- Compatível com PostgreSQL 14+
-- Idempotente: pode rodar várias vezes sem erro.
--
-- Uso:
--   psql "$DATABASE_URL" -f init.sql
--   OU monte em /docker-entrypoint-initdb.d/ do container postgres.
-- ============================================================================

-- ============================================================================
-- AUTH / SISTEMA
-- ============================================================================

CREATE TABLE IF NOT EXISTS forbidden_rules (
    id                  VARCHAR(64)  PRIMARY KEY,
    emoji               VARCHAR(16),
    name                VARCHAR(128),
    pattern             VARCHAR(256),
    match_mode          VARCHAR(16),
    case_sensitive      BOOLEAN      NOT NULL DEFAULT FALSE,
    target              VARCHAR(16),
    consequences_json   TEXT,
    cooldown_sec        INTEGER      NOT NULL DEFAULT 30,
    enabled             BOOLEAN      NOT NULL DEFAULT FALSE,
    triggered           BIGINT       NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP
);

CREATE TABLE IF NOT EXISTS forbidden_invocations (
    id              BIGSERIAL    PRIMARY KEY,
    ts              TIMESTAMP,
    speaker_uuid    VARCHAR(64),
    speaker_name    VARCHAR(64),
    rule_id         VARCHAR(64),
    rule_name       VARCHAR(128),
    word            VARCHAR(256)
);
CREATE INDEX IF NOT EXISTS idx_inv_ts   ON forbidden_invocations (ts DESC);
CREATE INDEX IF NOT EXISTS idx_inv_rule ON forbidden_invocations (rule_id);

-- ============================================================================
-- MADNESS METER
-- ============================================================================

CREATE TABLE IF NOT EXISTS madness_state (
    uuid        VARCHAR(64)        PRIMARY KEY,
    name        VARCHAR(64),
    sanity      DOUBLE PRECISION   NOT NULL DEFAULT 100,
    last_tick   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS madness_config (
    id                          INTEGER  PRIMARY KEY,
    enabled                     BOOLEAN  NOT NULL DEFAULT FALSE,
    tick_interval_sec           INTEGER  NOT NULL DEFAULT 12,
    decay_per_minute            DOUBLE PRECISION NOT NULL DEFAULT 1.5,
    whisper_broadcast_mode      VARCHAR(16) NOT NULL DEFAULT 'player',
    world_fx_enabled            BOOLEAN  NOT NULL DEFAULT TRUE,
    world_fx_chance             DOUBLE PRECISION NOT NULL DEFAULT 0.4,
    zone_messages_json          TEXT,
    zone_sounds_json            TEXT,
    world_fx_types_json         TEXT
);

INSERT INTO madness_config (
    id, enabled, tick_interval_sec, decay_per_minute, whisper_broadcast_mode,
    world_fx_enabled, world_fx_chance,
    zone_messages_json, zone_sounds_json, world_fx_types_json
) VALUES (
    1, FALSE, 12, 1.5, 'player', TRUE, 0.4,
    '{"inquieto":["§8§o...alguém te chama?","§8§oa pele formiga.","§8§o...você foi observado.","§8§ohá algo aqui.","§8§oa lâmpada fraqueja.","§8§oo silêncio é denso.","§8§o— olhe pra trás. devagar."],"perturbado":["§5§o...algo respira.","§5§oeles te conhecem agora.","§5§o— a sombra cresce —","§5§oo chão fica frio.","§5§oele sabe seu nome."],"histerico":["§c§oCORRA.","§c§oele tá vindo.","§c§o— a porta abriu —","§c§oeles estão dentro de você.","§c§oolha pra cima §lAGORA§r§c§o."],"consumido":["§4§l— você não está mais aqui —","§4§lo véu se rasgou.","§4§leles te chamam pelo §nverdadeiro§r§4§l nome.","§4§l— o mundo é deles agora —","§4§lresponda."]}',
    '{"inquieto":["minecraft:ambient.cave","minecraft:block.sculk.charge","minecraft:entity.allay.ambient_with_item"],"perturbado":["minecraft:entity.warden.heartbeat","minecraft:ambient.warped_forest.loop"],"histerico":["minecraft:entity.warden.angry","minecraft:entity.ghast.scream","minecraft:ambient.cave"],"consumido":["minecraft:entity.warden.angry","minecraft:entity.wither.spawn","minecraft:entity.ender_dragon.growl"]}',
    '["random_particle","ghost_armor_stand","random_sound","falling_block","random_lightning"]'
) ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- ELDRITCH POSSESSION
-- ============================================================================

CREATE TABLE IF NOT EXISTS possession_entities (
    id                  VARCHAR(64)  PRIMARY KEY,
    emoji               VARCHAR(16),
    display_name        VARCHAR(256),
    particle            VARCHAR(64),
    sound               VARCHAR(128),
    voice_lines_json    TEXT,
    enter_msg           VARCHAR(256),
    exit_msg            VARCHAR(256),
    speak_interval_sec  INTEGER,
    effects_json        TEXT,
    aura_color          VARCHAR(16)
);

CREATE TABLE IF NOT EXISTS possession_active (
    id          BIGSERIAL    PRIMARY KEY,
    player_uuid VARCHAR(64),
    player_name VARCHAR(64),
    entity_id   VARCHAR(64),
    started_at  TIMESTAMP,
    ends_at     TIMESTAMP,
    last_speak  TIMESTAMP
);

INSERT INTO possession_entities (id, emoji, display_name, particle, sound, voice_lines_json, enter_msg, exit_msg, speak_interval_sec, effects_json, aura_color) VALUES
('yhkuath', '👁', '§5§lYh§dk§5uath§r', 'minecraft:sculk_soul', 'minecraft:entity.warden.heartbeat',
 '["eu vi onde você dorme.","a porta nunca fechou.","eles cantam o seu nome agora.","a luz que você acende é minha.","olhe pra cima. devagar.","eu sou o segundo par de olhos.","você nunca esteve sozinho aqui."]',
 '§5§o— Yh''kuath entra através de você —', '§5§o— Yh''kuath se retira, lentamente —', 18,
 '[{"effect":"minecraft:glowing","amplifier":0},{"effect":"minecraft:slowness","amplifier":0}]', '#a78bfa'),
('vorashen', '🩸', '§4§lV§corashen§4§l Pale§r', 'minecraft:dripping_lava', 'minecraft:entity.evoker.prepare_summon',
 '["eu sangro pelo seu eco.","todos vocês são meus filhos.","a primeira mãe não esqueceu.","o útero se lembra.","volte pra mim.","o sangue ainda quente — beba.","ouça o coração que não é seu."]',
 '§4§o— Vorashen Pale toma seu coração —', '§4§o— Vorashen Pale solta seu pulso —', 22,
 '[{"effect":"minecraft:wither","amplifier":0},{"effect":"minecraft:glowing","amplifier":0}]', '#dc2626'),
('morghaur', '🦴', '§8§lM§7or''§8§lGhaur§r', 'minecraft:smoke', 'minecraft:entity.warden.angry',
 '["eu como nomes.","qual era o nome do seu pai?","já comi a palavra ''casa''.","esqueça. é mais leve.","eu ergui dentes onde havia bocas.","um por dia. seu, talvez."]',
 '§8§o— Mor''Ghaur abocanha sua sombra —', '§8§o— Mor''Ghaur volta para o silêncio —', 25,
 '[{"effect":"minecraft:hunger","amplifier":1},{"effect":"minecraft:glowing","amplifier":0}]', '#475569'),
('last_pilgrim', '🕯', '§6§lO §eÚltimo §6§lPeregrino§r', 'minecraft:end_rod', 'minecraft:block.bell.use',
 '["eu caminhei mais que houve estrada.","a lanterna pesa mais que a fé.","já fui devorado três vezes.","a porta no leste — não abra.","tudo que vejo já se foi.","volte, peregrino. volte."]',
 '§6§o— O Último Peregrino vê por seus olhos —', '§6§o— Ele segue em frente, sozinho —', 20,
 '[{"effect":"minecraft:slow_falling","amplifier":0},{"effect":"minecraft:glowing","amplifier":0}]', '#f59e0b'),
('isthar', '🌙', '§3§lIss§9''thar§3§l, Lua Sangrenta§r', 'minecraft:portal', 'minecraft:ambient.cave',
 '["cai a lua azul.","o mar respira sob a pedra.","eu sou a maré.","algo afunda em você.","olhe pra água. eu olho de volta.","a lua nunca foi cheia."]',
 '§3§o— Iss''thar derrama-se em você —', '§3§o— A maré recua —', 24,
 '[{"effect":"minecraft:water_breathing","amplifier":0},{"effect":"minecraft:glowing","amplifier":0}]', '#06b6d4')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- CURSED ITEMS
-- ============================================================================

CREATE TABLE IF NOT EXISTS cursed_items (
    id                  VARCHAR(64)  PRIMARY KEY,
    emoji               VARCHAR(16),
    item_id             VARCHAR(128),
    display_name        VARCHAR(256),
    lore_json           TEXT,
    enchantments_json   TEXT,
    curse_effects_json  TEXT,
    whispers_json       TEXT,
    particle            VARCHAR(64),
    sound               VARCHAR(128),
    tick_interval_sec   INTEGER
);

CREATE TABLE IF NOT EXISTS cursed_bindings (
    id          BIGSERIAL    PRIMARY KEY,
    player_uuid VARCHAR(64),
    player_name VARCHAR(64),
    curse_id    VARCHAR(64),
    started_at  TIMESTAMP,
    last_tick   TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bind_player ON cursed_bindings (player_uuid);

INSERT INTO cursed_items (id, emoji, item_id, display_name, lore_json, enchantments_json, curse_effects_json, whispers_json, particle, sound, tick_interval_sec) VALUES
('eye_of_vorash', '👁', 'minecraft:ender_eye', '§4§lOlho de §c§lVorash§r',
 '["§7§oele olha de volta.","§8§o— nunca está apagado —","§4§oa sede aumenta."]',
 '[{"id":"minecraft:vanishing_curse","level":1},{"id":"minecraft:binding_curse","level":1}]',
 '[{"effect":"minecraft:nausea","durationSec":8,"amplifier":0},{"effect":"minecraft:hunger","durationSec":20,"amplifier":1}]',
 '["§4§oo olho pulsa.","§4§oVorash respira.","§8§o— olhe pra trás.","§4§oele te conhece pelo nome."]',
 'minecraft:smoke', 'minecraft:entity.warden.heartbeat', 35),
('cracked_heart', '🩸', 'minecraft:redstone', '§c§lCoração §4Rachado§r',
 '["§7§obatendo, devagar.","§4§oalguém o segura por dentro.","§8§o— não é seu —"]',
 '[{"id":"minecraft:binding_curse","level":1}]',
 '[{"effect":"minecraft:wither","durationSec":4,"amplifier":0},{"effect":"minecraft:slowness","durationSec":10,"amplifier":0}]',
 '["§4§oseu coração não é seu.","§4§obatida. batida. batida.","§8§oa próxima é a última."]',
 'minecraft:damage_indicator', 'minecraft:entity.warden.heartbeat', 30),
('crown_of_silence', '👑', 'minecraft:netherite_helmet', '§8§lCoroa do §0Silêncio§r',
 '["§7§oas vozes pararam.","§8§oporque elas estão DENTRO agora."]',
 '[{"id":"minecraft:vanishing_curse","level":1},{"id":"minecraft:protection","level":4}]',
 '[{"effect":"minecraft:darkness","durationSec":15,"amplifier":0},{"effect":"minecraft:blindness","durationSec":5,"amplifier":0}]',
 '["§8§oo silêncio cresce.","§8§oo silêncio sussurra.","§8§oo silêncio te chama §lpelo seu nome§r§8§o."]',
 'minecraft:sculk_soul', 'minecraft:ambient.warped_forest.loop', 40),
('bone_chime', '🔔', 'minecraft:bell', '§f§lSino de §7§lOssos§r',
 '["§7§oosso fino, bateria seca.","§e§oo som apaga as estrelas."]',
 '[{"id":"minecraft:soul_speed","level":3}]',
 '[{"effect":"minecraft:slow_falling","durationSec":20,"amplifier":0},{"effect":"minecraft:weakness","durationSec":10,"amplifier":0}]',
 '["§f§oting...","§e§oas estrelas se afastam.","§7§oo sino bate sozinho."]',
 'minecraft:end_rod', 'minecraft:block.bell.use', 28)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- DIMENSIONAL RIFTS
-- ============================================================================

CREATE TABLE IF NOT EXISTS rifts (
    id                  VARCHAR(64)  PRIMARY KEY,
    emoji               VARCHAR(16),
    name                VARCHAR(128),
    pos_x               DOUBLE PRECISION,
    pos_y               DOUBLE PRECISION,
    pos_z               DOUBLE PRECISION,
    pos_dim             VARCHAR(32),
    radius              INTEGER,
    dest_x              DOUBLE PRECISION,
    dest_y              DOUBLE PRECISION,
    dest_z              DOUBLE PRECISION,
    dest_dim            VARCHAR(32),
    random_dest_radius  INTEGER,
    particle            VARCHAR(64),
    particle_count      INTEGER,
    visual_every        INTEGER,
    ambient_sound       VARCHAR(128),
    pre_msg             VARCHAR(256),
    post_msg            VARCHAR(256),
    pre_effects_json    TEXT,
    post_effects_json   TEXT,
    cooldown_sec        INTEGER,
    enabled             BOOLEAN  NOT NULL DEFAULT FALSE,
    visualize           BOOLEAN  NOT NULL DEFAULT TRUE,
    triggers            BIGINT   NOT NULL DEFAULT 0
);

INSERT INTO rifts (id, emoji, name, pos_x, pos_y, pos_z, pos_dim, radius, dest_x, dest_y, dest_z, dest_dim, random_dest_radius,
    particle, particle_count, visual_every, ambient_sound, pre_msg, post_msg, pre_effects_json, post_effects_json,
    cooldown_sec, enabled, visualize, triggers) VALUES
('rift_void', '🌀', 'Fenda do Vazio', 0, 80, 0, 'overworld', 2, 0, 200, 0, 'the_end', 0,
 'minecraft:portal', 80, 3, 'minecraft:block.portal.ambient',
 '§5§o— o tecido se desfaz —', '§5§l— você atravessou —',
 '[{"effect":"minecraft:blindness","durationSec":3,"amplifier":0},{"effect":"minecraft:slowness","durationSec":3,"amplifier":4}]',
 '[{"effect":"minecraft:nausea","durationSec":10,"amplifier":0},{"effect":"minecraft:slow_falling","durationSec":20,"amplifier":0}]',
 30, FALSE, TRUE, 0),
('rift_lost', '🕳', 'Portão dos Perdidos', 100, 70, 100, 'overworld', 3, NULL, NULL, NULL, NULL, 2000,
 'minecraft:sculk_soul', 100, 4, 'minecraft:entity.warden.heartbeat',
 '§8§o— algo te empurra para longe —', '§8§o— você está perdido —',
 '[{"effect":"minecraft:darkness","durationSec":5,"amplifier":0}]',
 '[{"effect":"minecraft:blindness","durationSec":8,"amplifier":0},{"effect":"minecraft:resistance","durationSec":60,"amplifier":0}]',
 60, FALSE, TRUE, 0)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- GLYPH DISCOVERY
-- ============================================================================

CREATE TABLE IF NOT EXISTS glyphs (
    id                  VARCHAR(64)  PRIMARY KEY,
    symbol              VARCHAR(16),
    emoji               VARCHAR(16),
    name                VARCHAR(256),
    pos_x               DOUBLE PRECISION,
    pos_y               DOUBLE PRECISION,
    pos_z               DOUBLE PRECISION,
    pos_dim             VARCHAR(32),
    radius              INTEGER,
    lore_fragment       TEXT,
    rewards_json        TEXT,
    discover_sound      VARCHAR(128),
    discover_particle   VARCHAR(128),
    hint_particle       VARCHAR(128),
    hint_every          INTEGER,
    hint_count          INTEGER,
    discovered_by_json  TEXT,
    enabled             BOOLEAN  NOT NULL DEFAULT FALSE
);

INSERT INTO glyphs (id, symbol, emoji, name, pos_x, pos_y, pos_z, pos_dim, radius, lore_fragment, rewards_json,
    discover_sound, discover_particle, hint_particle, hint_every, hint_count, discovered_by_json, enabled) VALUES
('glyph_eye', '👁', '👁', '§5§lGlifo do §dOlho§r', 0, 64, 0, 'overworld', 3,
 '§7§o"o olho que vê quando o mundo dorme. ele te conhece."§r',
 '[{"type":"effect","effect":"minecraft:night_vision","durationSec":600,"amplifier":0},{"type":"title","title":"§5§l✦","subtitle":"§dOlho desperto","fadeIn":10,"stay":60,"fadeOut":20}]',
 'minecraft:block.bell.use', 'minecraft:end_rod', 'minecraft:soul_fire_flame', 5, 8, '[]', FALSE),
('glyph_void', '🕳', '🕳', '§8§lGlifo do §0§lVazio§r', 100, 64, 100, 'overworld', 2,
 '§8§o"o vazio não é nada. é tudo o que foi esquecido."§r',
 '[{"type":"effect","effect":"minecraft:slow_falling","durationSec":300,"amplifier":0},{"type":"item","itemId":"minecraft:ender_eye","count":1}]',
 'minecraft:block.portal.ambient', 'minecraft:portal', 'minecraft:sculk_soul', 6, 6, '[]', FALSE),
('glyph_blood', '🩸', '🩸', '§4§lGlifo do §c§lSangue§r', -100, 64, -100, 'overworld', 2,
 '§4§o"o sangue dela ainda corre. nem ela sabe pra onde."§r',
 '[{"type":"effect","effect":"minecraft:strength","durationSec":600,"amplifier":1},{"type":"effect","effect":"minecraft:bad_omen","durationSec":600,"amplifier":0}]',
 'minecraft:entity.warden.heartbeat', 'minecraft:dripping_lava', 'minecraft:damage_indicator', 4, 5, '[]', FALSE),
('glyph_silence', '🤫', '🤫', '§7§lGlifo do §8§lSilêncio§r', 0, 80, 200, 'overworld', 2,
 '§7§o"ouça o que não está sendo dito."§r',
 '[{"type":"title","title":"§7§l...","subtitle":"§8§o— você ouviu —","fadeIn":10,"stay":60,"fadeOut":20},{"type":"effect","effect":"minecraft:invisibility","durationSec":120,"amplifier":0}]',
 'minecraft:ambient.warped_forest.loop', 'minecraft:white_ash', 'minecraft:warped_spore', 7, 6, '[]', FALSE)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- TIME-LOCKED BOXES
-- ============================================================================

CREATE TABLE IF NOT EXISTS time_boxes (
    id                      VARCHAR(64)  PRIMARY KEY,
    emoji                   VARCHAR(16),
    name                    VARCHAR(128),
    description             TEXT,
    unlock_at               TIMESTAMP,
    recipient               VARCHAR(64),
    items_json              TEXT,
    effects_json            TEXT,
    unlock_message          TEXT,
    unlock_sound            VARCHAR(128),
    unlock_particle         VARCHAR(128),
    countdown_enabled       BOOLEAN  NOT NULL DEFAULT TRUE,
    countdown_every_min     INTEGER,
    countdown_msg           VARCHAR(256),
    delivered               BOOLEAN  NOT NULL DEFAULT FALSE,
    last_countdown_ts       TIMESTAMP
);

-- Exemplo: caixa que abre amanhã às 6h
INSERT INTO time_boxes (id, emoji, name, description, unlock_at, recipient, items_json, effects_json,
    unlock_message, unlock_sound, unlock_particle, countdown_enabled, countdown_every_min, countdown_msg) VALUES
('box_dawn', '🎁', 'Caixa da Aurora', 'Aberta na próxima manhã — items úteis pro novo dia.',
 NOW() + INTERVAL '1 day', '@a',
 '[{"itemId":"minecraft:bread","count":16},{"itemId":"minecraft:torch","count":32},{"itemId":"minecraft:golden_apple","count":1}]',
 '[{"effect":"minecraft:regeneration","durationSec":30,"amplifier":1}]',
 '§e§l☀ A AURORA CHEGOU§r §6§o— a caixa se abre —', 'minecraft:block.bell.use', 'minecraft:end_rod',
 TRUE, 60, '§e§o[Caixa da Aurora] abre em §l{time}')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- MEMORY ECHOES
-- ============================================================================

CREATE TABLE IF NOT EXISTS memory_echoes (
    id                  VARCHAR(64)  PRIMARY KEY,
    emoji               VARCHAR(16),
    name                VARCHAR(128),
    color               VARCHAR(16),
    pos_x               DOUBLE PRECISION,
    pos_y               DOUBLE PRECISION,
    pos_z               DOUBLE PRECISION,
    pos_dim             VARCHAR(32),
    radius              INTEGER,
    ghosts_json         TEXT,
    whispers_json       TEXT,
    whisper_every_sec   INTEGER,
    ambient_sound       VARCHAR(128),
    ambient_particle    VARCHAR(128),
    play_duration_sec   INTEGER,
    cooldown_sec        INTEGER,
    enabled             BOOLEAN  NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS memory_echoes_active (
    id           BIGSERIAL    PRIMARY KEY,
    echo_id      VARCHAR(64),
    player_uuid  VARCHAR(64),
    started_at   TIMESTAMP,
    ends_at      TIMESTAMP,
    last_whisper TIMESTAMP,
    whisper_idx  INTEGER  NOT NULL DEFAULT 0
);

INSERT INTO memory_echoes (id, emoji, name, color, pos_x, pos_y, pos_z, pos_dim, radius, ghosts_json, whispers_json,
    whisper_every_sec, ambient_sound, ambient_particle, play_duration_sec, cooldown_sec, enabled) VALUES
('echo_lost_circle', '🕯', 'O Círculo dos Esquecidos', '#a78bfa', 0, 64, 0, 'overworld', 6,
 '[{"playerName":"Notch","offset":{"x":2,"y":0,"z":0},"rotation":180,"showArms":true},{"playerName":"Herobrine","offset":{"x":-2,"y":0,"z":0},"rotation":0,"showArms":true},{"playerName":"Steve","offset":{"x":0,"y":0,"z":2},"rotation":270,"showArms":true},{"playerName":"Alex","offset":{"x":0,"y":0,"z":-2},"rotation":90,"showArms":true}]',
 '["§7§o— você não vê eles, mas estão aqui —","§7§o\"...nós ficamos aqui depois\"","§7§o\"...eles vão te esquecer também\"","§5§o— eles olham pra você —"]',
 5, 'minecraft:ambient.cave', 'minecraft:sculk_soul', 30, 120, FALSE),
('echo_battle', '⚔', 'Memória da Batalha', '#dc2626', 100, 64, 100, 'overworld', 8,
 '[{"playerName":"Steve","offset":{"x":3,"y":0,"z":0},"rotation":180,"showArms":true},{"playerName":"Alex","offset":{"x":-3,"y":0,"z":0},"rotation":0,"showArms":true}]',
 '["§4§o— aço bate no aço —","§4§o— eles morreram aqui —","§4§o— o sangue secou faz tempo —"]',
 4, 'minecraft:entity.warden.heartbeat', 'minecraft:dripping_lava', 25, 90, FALSE)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- COSMIC CALENDAR
-- ============================================================================

CREATE TABLE IF NOT EXISTS calendar_config (
    id              INTEGER  PRIMARY KEY,
    year_name       VARCHAR(128),
    day_zero_ts     BIGINT,
    mode            VARCHAR(16),
    months_json     TEXT,
    paused          BOOLEAN  NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS calendar_special_days (
    id                  VARCHAR(64)  PRIMARY KEY,
    day_of_year         INTEGER,
    name                VARCHAR(128),
    emoji               VARCHAR(16),
    color               VARCHAR(16),
    description         TEXT,
    tellraw             TEXT,
    sound               VARCHAR(128),
    weather             VARCHAR(16),
    set_time_to         INTEGER  NOT NULL DEFAULT -1,
    effects_json        TEXT,
    custom_cmd          TEXT,
    last_trigger_ts     BIGINT
);

INSERT INTO calendar_config (id, year_name, day_zero_ts, mode, months_json, paused) VALUES
(1, 'Ano Vorashen I', EXTRACT(EPOCH FROM (NOW() - INTERVAL '12 days')) * 1000, 'real',
 '[{"emoji":"👁","name":"Olho Aberto","days":31,"color":"#a78bfa"},{"emoji":"🌊","name":"Ecos Profundos","days":28,"color":"#06b6d4"},{"emoji":"🌫","name":"Cinzas Pálidas","days":31,"color":"#94a3b8"},{"emoji":"🌙","name":"Lua Sangrenta","days":30,"color":"#dc2626"},{"emoji":"🫀","name":"Coração Mudo","days":31,"color":"#000000"},{"emoji":"🪞","name":"Véu Rachado","days":30,"color":"#7c3aed"},{"emoji":"🕯","name":"Última Vigília","days":28,"color":"#fbbf24"}]',
 TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO calendar_special_days (id, day_of_year, name, emoji, color, description, tellraw, sound, weather, set_time_to, effects_json, custom_cmd) VALUES
('sp_eye_open', 13, 'A Noite que Olha', '👁', '#a78bfa',
 'Em 13 de Olho Aberto, o céu inteiro pisca uma vez. Quem não fechou os olhos viu.',
 '§5§l[§dCosmos§5§l]§r §dA noite olha pra trás esta noite. §5Não erga os olhos.',
 'minecraft:ambient.cave', 'thunder', 18000,
 '[{"effect":"minecraft:blindness","durationSec":8,"amplifier":0},{"effect":"minecraft:darkness","durationSec":30,"amplifier":0}]', ''),
('sp_blood_moon', 95, 'A Lua Sangrenta', '🌙', '#dc2626',
 'Lua vermelha. Hostilidade dobra. Mortos sussurram dos canteiros.',
 '§4§l[§cCosmos§4§l]§r §cA lua sangra. §4Eles caçam esta noite.',
 'minecraft:entity.wither.spawn', 'thunder', 13000,
 '[{"effect":"minecraft:bad_omen","durationSec":600,"amplifier":1}]', 'difficulty hard'),
('sp_silence', 152, 'O Coração Mudo', '🫀', '#000000',
 'Nada faz som por um dia. Apenas a batida do que sobra.',
 '§8§l[§7Cosmos§8§l]§r §7§oTudo se cala. Só o coração resta.',
 'minecraft:entity.warden.heartbeat', '', -1,
 '[{"effect":"minecraft:slowness","durationSec":60,"amplifier":0}]', ''),
('sp_pilgrim', 200, 'O Último Peregrino Chega', '🕯', '#fbbf24',
 'Ele caminhou por tantos anos. Hoje, por algumas horas, está aqui.',
 '§6§l[§eCosmos§6§l]§r §eO Último Peregrino chega. §6Acendam suas lanternas.',
 'minecraft:block.bell.use', 'clear', 0,
 '[{"effect":"minecraft:luck","durationSec":1200,"amplifier":1}]', '')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- SCHEDULED CUTSCENES
-- ============================================================================

CREATE TABLE IF NOT EXISTS cutscenes (
    id                  VARCHAR(64)  PRIMARY KEY,
    emoji               VARCHAR(16),
    name                VARCHAR(128),
    description         TEXT,
    steps_json          TEXT,
    target_selector     VARCHAR(64),
    scheduled_at        TIMESTAMP,
    schedule_mode       VARCHAR(16),
    interval_sec        INTEGER,
    daily_hour          INTEGER,
    daily_minute        INTEGER,
    enabled             BOOLEAN  NOT NULL DEFAULT FALSE,
    last_run_at         TIMESTAMP,
    run_count           BIGINT  NOT NULL DEFAULT 0
);

INSERT INTO cutscenes (id, emoji, name, description, steps_json, target_selector,
    schedule_mode, daily_hour, daily_minute, interval_sec, enabled) VALUES
('cs_midnight_whisper', '🌙', 'Sussurro da Meia-Noite', 'Todo dia à meia-noite, todos os players online ouvem um sussurro.',
 '[{"type":"sound","sound":"minecraft:entity.warden.heartbeat","volume":1,"pitch":0.4},{"type":"chat","message":"§5§o— a noite te observa —"},{"type":"wait","durationSec":3},{"type":"chat","message":"§8§o...alguém respira ao seu lado..."}]',
 '@a', 'daily', 0, 0, NULL, FALSE),
('cs_dawn_blessing', '☀', 'Bênção da Aurora', 'Todo dia às 6h: regeneration + título.',
 '[{"type":"sound","sound":"minecraft:block.bell.use","volume":1,"pitch":1.5},{"type":"title","title":"§e§l☀","subtitle":"§6§oa aurora chegou","fadeIn":20,"stay":60,"fadeOut":20},{"type":"effect","effect":"minecraft:regeneration","durationSec":30,"amplifier":1}]',
 '@a', 'daily', 6, 0, NULL, FALSE),
('cs_random_ghost', '👻', 'Fantasma Aleatório', 'A cada 30min, spawna um zombie invisível próximo aleatório.',
 '[{"type":"spawn_mob","entity":"minecraft:zombie","count":1,"offsetX":8,"offsetZ":8},{"type":"sound","sound":"minecraft:entity.ghast.scream","volume":0.5,"pitch":0.6}]',
 '@first', 'interval', NULL, NULL, 1800, FALSE)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- FORBIDDEN WORDS — exemplos de regras
-- ============================================================================

INSERT INTO forbidden_rules (id, emoji, name, pattern, match_mode, case_sensitive, target,
    consequences_json, cooldown_sec, enabled, triggered, updated_at) VALUES
('fw_vorashen', '🩸', 'Não diga o nome', 'vorashen', 'contains', FALSE, 'both',
 '[{"type":"sound","sound":"minecraft:entity.warden.heartbeat","volume":1.2,"pitch":0.4},{"type":"tellraw","target":"broadcast","message":"§4§o— alguém disse o nome —"},{"type":"effect","effect":"minecraft:nausea","durationSec":12,"amplifier":0},{"type":"effect","effect":"minecraft:wither","durationSec":4,"amplifier":0},{"type":"particle","particle":"minecraft:smoke","count":60,"offsetY":1},{"type":"lightning"}]',
 60, FALSE, 0, NOW()),
('fw_run', '🏃', '"corra"', 'corra', 'contains', FALSE, 'speaker',
 '[{"type":"effect","effect":"minecraft:speed","durationSec":30,"amplifier":2},{"type":"effect","effect":"minecraft:darkness","durationSec":20,"amplifier":0},{"type":"title","title":"§4§lCORRA","subtitle":"§c— eles te ouviram —","fadeIn":5,"stay":40,"fadeOut":10},{"type":"sound","sound":"minecraft:entity.ghast.scream","volume":0.8,"pitch":0.8}]',
 90, FALSE, 0, NOW()),
('fw_help', '🆘', '"socorro" sem ninguém perto', 'socorro', 'contains', FALSE, 'speaker',
 '[{"type":"tellraw","target":"speaker","message":"§8§o— ninguém vem —"},{"type":"sound","sound":"minecraft:ambient.cave","volume":1.5,"pitch":0.3},{"type":"effect","effect":"minecraft:slowness","durationSec":15,"amplifier":1},{"type":"spawn_mob","entity":"minecraft:zombie","count":2,"offsetX":5,"offsetZ":5}]',
 120, FALSE, 0, NOW()),
('fw_god', '✨', 'invoca os deuses', '(deus|god|divino)', 'regex', FALSE, 'speaker',
 '[{"type":"effect","effect":"minecraft:glowing","durationSec":30,"amplifier":0},{"type":"effect","effect":"minecraft:regeneration","durationSec":20,"amplifier":1},{"type":"particle","particle":"minecraft:end_rod","count":40,"offsetY":2},{"type":"title","title":"§e§l✦","subtitle":"§6§o— eles ouviram —","fadeIn":10,"stay":40,"fadeOut":10}]',
 180, FALSE, 0, NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- CHOICE / DECISÕES INTERATIVAS
-- ============================================================================

CREATE TABLE IF NOT EXISTS choice_decisions (
    id                      VARCHAR(64)  PRIMARY KEY,
    title                   VARCHAR(256),
    question                TEXT,
    options_json            TEXT,
    multi_response          BOOLEAN  NOT NULL DEFAULT FALSE,
    active_target           VARCHAR(64),
    active_responses_json   TEXT,
    activated_at            TIMESTAMP
);

CREATE TABLE IF NOT EXISTS choice_history (
    id              BIGSERIAL    PRIMARY KEY,
    ts              TIMESTAMP,
    decision_id     VARCHAR(64),
    decision_title  VARCHAR(256),
    player_uuid     VARCHAR(64),
    player_name     VARCHAR(64),
    option_idx      INTEGER,
    option_text     VARCHAR(256)
);
CREATE INDEX IF NOT EXISTS idx_choice_hist_ts ON choice_history (ts DESC);

INSERT INTO choice_decisions (id, title, question, options_json, multi_response) VALUES
('dec_crossroads', 'O Cruzamento',
 'Você chega numa encruzilhada. A esquerda leva à montanha. A direita, à floresta. Qual caminho?',
 '[{"text":"Esquerda — Montanha","color":"§b","action":"title","payload":"","resultMessage":"§b§l⛰ Você sobe..."},{"text":"Direita — Floresta","color":"§a","action":"title","payload":"","resultMessage":"§a§l🌲 Você entra na mata..."}]',
 FALSE),
('dec_oath', 'O Juramento',
 'Uma voz antiga lhe oferece um pacto. O que você responde?',
 '[{"text":"Aceito","color":"§d","action":"effect","payload":"minecraft:strength 600 1","resultMessage":"§d§l⛧ O pacto está selado..."},{"text":"Recuso","color":"§e","action":"effect","payload":"minecraft:resistance 300 0","resultMessage":"§e§l✨ Você foi poupado."},{"text":"Pergunto o preço","color":"§7","action":"title","payload":"","resultMessage":"§7§o— silêncio —"}]',
 FALSE)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- HISTORY (chat e command) — tabelas só, dados crescem em runtime
-- ============================================================================

CREATE TABLE IF NOT EXISTS chat_log (
    id      BIGSERIAL    PRIMARY KEY,
    ts      TIMESTAMP,
    uuid    VARCHAR(64),
    name    VARCHAR(64),
    message TEXT
);
CREATE INDEX IF NOT EXISTS idx_chat_ts   ON chat_log (ts DESC);
CREATE INDEX IF NOT EXISTS idx_chat_uuid ON chat_log (uuid);

CREATE TABLE IF NOT EXISTS command_log (
    id          BIGSERIAL    PRIMARY KEY,
    ts          TIMESTAMP,
    uuid        VARCHAR(64),
    name        VARCHAR(64),
    command     TEXT,
    is_player   BOOLEAN  NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_cmd_ts   ON command_log (ts DESC);
CREATE INDEX IF NOT EXISTS idx_cmd_uuid ON command_log (uuid);

-- ============================================================================
-- PLAYER SNAPSHOTS — backup automático de inventário/posição
-- ============================================================================

CREATE TABLE IF NOT EXISTS player_snapshots (
    id          BIGSERIAL    PRIMARY KEY,
    uuid        VARCHAR(64),
    name        VARCHAR(64),
    ts          TIMESTAMP,
    data_json   TEXT
);
CREATE INDEX IF NOT EXISTS idx_snap_uuid_ts ON player_snapshots (uuid, ts DESC);

-- ============================================================================
-- SAVE ANCHORS — checkpoints estilo Dark Souls
-- ============================================================================

CREATE TABLE IF NOT EXISTS save_anchors (
    id          VARCHAR(64)  PRIMARY KEY,
    emoji       VARCHAR(16),
    name        VARCHAR(128),
    description TEXT,
    pos_x       DOUBLE PRECISION,
    pos_y       DOUBLE PRECISION,
    pos_z       DOUBLE PRECISION,
    pos_dim     VARCHAR(32),
    radius      INTEGER,
    uses_json   TEXT,
    enabled     BOOLEAN  NOT NULL DEFAULT FALSE
);

-- ============================================================================
-- KV CONFIGS — storage genérico chave→JSON pra páginas sem engine
-- (substitui localStorage: dice_history, wheel, atmospheres_horror, etc.)
-- ============================================================================

CREATE TABLE IF NOT EXISTS kv_configs (
    key         VARCHAR(128)  PRIMARY KEY,
    data_json   TEXT,
    updated_at  TIMESTAMP
);

-- ============================================================================
-- MAP CHUNKS — cache PNG dos chunks renderizados
-- ============================================================================

CREATE TABLE IF NOT EXISTS map_chunks (
    dim         VARCHAR(32),
    cx          INTEGER,
    cz          INTEGER,
    png         BYTEA,
    updated_at  TIMESTAMP,
    PRIMARY KEY (dim, cx, cz)
);

-- ============================================================================
-- FIM
-- ============================================================================
