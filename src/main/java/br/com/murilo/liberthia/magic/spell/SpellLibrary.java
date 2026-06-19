package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import br.com.murilo.liberthia.particle.engine.ConfigurableParticleOptions;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * v0.1.143 r111: <b>SpellLibrary</b> — registro de TODOS os 50+ feitiços
 * portados do Iron's Spells N Spellbooks + Ars Nouveau.
 *
 * <h2>Catálogo</h2>
 * <ul>
 *   <li>Fire (8): fireball, burning_dash, inferno, magma_bomb, sun_beam, phoenix_reborn, cauterize, heat_wave</li>
 *   <li>Ice (8): frostbolt, ice_spike, frost_nova, glacial_storm, frost_step, frozen_ground, ray_of_frost, ice_lance</li>
 *   <li>Lightning (8): lightning_bolt, spark_burst, chain_lightning, shock, thunder_step, storm_cloud, static_field, lightning_lance</li>
 *   <li>Blood (8): blood_step, lifedrain, heartstop, blood_spear, sanguine_bind, crimson_mist, vampiric_touch, blood_pact</li>
 *   <li>Eldritch (6): void_tentacle, mind_spike, eldritch_blast, soul_tear, madness_wave, cosmic_void</li>
 *   <li>Holy (8): greater_heal, smite, divine_light, sun_strike, holy_lance, healing_aura, sacred_ground, judgment</li>
 *   <li>Nature (6): vine_tangle, earth_wall, stone_shard, wisps_heal, roots, bramble_storm</li>
 *   <li>Evocation (4): magic_missile, bone_spear, magic_shield, summon_vex</li>
 * </ul>
 */
public final class SpellLibrary {

    public static final Map<String, SpellDef> ALL = new LinkedHashMap<>();

    static {
        registerFire();
        registerIce();
        registerLightning();
        registerBlood();
        registerEldritch();
        registerHoly();
        registerNature();
        registerEvocation();
        registerMobility();
        registerApolao();
        registerVoid();
        // r168: 50 new spells using Pack-2 VFX framework
        NewSpellsR168.registerAll();
        // r169: +20 spells with carefully matched VFX choreography (no purple boxes!)
        NewSpellsR169.registerAll();
        // r171: REWRITE — 70 spells with EXACTLY 2 sprites each (color-locked, no mixing)
        //  → sobrescreve r168+r169 com versões limpas (mesmo IDs)
        TwoSpriteSpells.registerAll();
        // r172: 15 feitiços ACESSÍVEIS (mobilidade/utilidade, mana/cd baixos, efeito real)
        MobilitySpellsR172.registerAll();
        // r172: 8 scrolls de combate (danos variados)
        CombatScrollsR172.registerAll();
    }

    private SpellLibrary() {}

    private static void reg(SpellDef def) {
        ALL.put(def.id, def);
    }

    /** r148: Pública pra Spell Factory adicionar spells JSON-driven em runtime. */
    public static void registerExternal(SpellDef def) {
        ALL.put(def.id, def);
    }

    public static SpellDef get(String id) { return ALL.get(id); }
    public static java.util.Collection<SpellDef> all() { return ALL.values(); }
    public static boolean exists(String id) { return ALL.containsKey(id); }

    // ════════════════════════════════════════════════════════════════════════
    // FIRE (8 spells)
    // ════════════════════════════════════════════════════════════════════════

    private static void registerFire() {
        reg(SpellDef.builder("fireball")
                .name("Bola de Fogo")
                .school(SpellSchool.FIRE).rarity(Rarity.UNCOMMON)
                .mana(15).cooldown(40).damage(10F).range(24F)
                .lore("Projétil flamejante. Explode em impacto, queima alvos próximos.")
                .cast(ctx -> projectile(ctx, SchoolDamageSource.fire(80), 2.5F, true)).build());

        reg(SpellDef.builder("burning_dash")
                .name("Avanço Flamejante")
                .school(SpellSchool.FIRE).rarity(Rarity.UNCOMMON)
                .mana(20).cooldown(80).damage(6F).range(8F)
                .lore("Avança numa explosão de fogo, ignitando tudo que toca.")
                .cast(SpellLibrary::castBurningDash).build());

        reg(SpellDef.builder("inferno")
                .name("Inferno Cônico")
                .school(SpellSchool.FIRE).rarity(Rarity.RARE)
                .mana(35).cooldown(120).damage(8F).range(10F)
                .lore("Sopra um cone de chamas. Atinge múltiplos alvos, queima 8s.")
                .cast(SpellLibrary::castInferno).build());

        reg(SpellDef.builder("magma_bomb")
                .name("Bomba de Magma")
                .school(SpellSchool.FIRE).rarity(Rarity.RARE)
                .mana(30).cooldown(100).damage(14F).range(20F)
                .lore("Arremessa uma esfera de magma. Cria pool de fogo no impacto.")
                .cast(SpellLibrary::castMagmaBomb).build());

        reg(SpellDef.builder("sun_beam")
                .name("Raio Solar")
                .school(SpellSchool.FIRE).rarity(Rarity.EPIC)
                .mana(50).cooldown(180).damage(20F).range(32F)
                .lore("Concentra a luz solar num feixe destruidor. +50% dmg vs undead.")
                .cast(SpellLibrary::castSunBeam).build());

        reg(SpellDef.builder("phoenix_reborn")
                .name("Renascer da Fênix")
                .school(SpellSchool.FIRE).rarity(Rarity.EPIC)
                .mana(80).cooldown(2400).damage(0F).range(0F)
                .lore("Marca-se com chama. Próxima morte: revive com 50% HP + Strength III.")
                .cast(SpellLibrary::castPhoenixReborn).build());

        reg(SpellDef.builder("cauterize")
                .name("Cauterizar")
                .school(SpellSchool.FIRE).rarity(Rarity.UNCOMMON)
                .mana(25).cooldown(160).damage(8F).range(0F)
                .lore("Auto-cura ardente. Cura 8HP + remove bleed/poison. Sofre 2dmg.")
                .cast(SpellLibrary::castCauterize).build());

        reg(SpellDef.builder("heat_wave")
                .name("Onda de Calor")
                .school(SpellSchool.FIRE).rarity(Rarity.RARE)
                .mana(40).cooldown(140).damage(6F).range(8F)
                .lore("Pulso radial. Empurra inimigos longe + queima 5s.")
                .cast(SpellLibrary::castHeatWave).build());
    }

    // ════════════════════════════════════════════════════════════════════════
    // ICE (8 spells)
    // ════════════════════════════════════════════════════════════════════════

    private static void registerIce() {
        reg(SpellDef.builder("frostbolt")
                .name("Projétil de Gelo")
                .school(SpellSchool.ICE).rarity(Rarity.COMMON)
                .mana(10).cooldown(30).damage(7F).range(24F)
                .lore("Projétil de gelo. Aplica Frozen (slow severo).")
                .cast(ctx -> projectile(ctx, SchoolDamageSource.ice(60), 0F, false)).build());

        reg(SpellDef.builder("ice_spike")
                .name("Estaca de Gelo")
                .school(SpellSchool.ICE).rarity(Rarity.UNCOMMON)
                .mana(18).cooldown(50).damage(12F).range(20F)
                .lore("Estaca afiada de gelo. Perfura armadura. +Slowness.")
                .cast(SpellLibrary::castIceSpike).build());

        reg(SpellDef.builder("frost_nova")
                .name("Nova Glacial")
                .school(SpellSchool.ICE).rarity(Rarity.RARE)
                .mana(35).cooldown(140).damage(8F).range(6F)
                .lore("Explosão de gelo radial. Congela todos em volta por 6s.")
                .cast(SpellLibrary::castFrostNova).build());

        reg(SpellDef.builder("glacial_storm")
                .name("Tempestade Glacial")
                .school(SpellSchool.ICE).rarity(Rarity.EPIC)
                .mana(55).cooldown(200).damage(3F).range(12F)
                .lore("Invoca tempestade local. Dano contínuo + slowness em área.")
                .cast(SpellLibrary::castGlacialStorm).build());

        reg(SpellDef.builder("frost_step")
                .name("Passo Glacial")
                .school(SpellSchool.ICE).rarity(Rarity.UNCOMMON)
                .mana(20).cooldown(80).damage(0F).range(16F)
                .lore("Teleport curto. Deixa trilha de gelo que congela inimigos.")
                .cast(SpellLibrary::castFrostStep).build());

        reg(SpellDef.builder("frozen_ground")
                .name("Solo Congelado")
                .school(SpellSchool.ICE).rarity(Rarity.RARE)
                .mana(30).cooldown(140).damage(0F).range(8F)
                .lore("Converte solo em gelo escorregadio num raio 8b.")
                .cast(SpellLibrary::castFrozenGround).build());

        reg(SpellDef.builder("ray_of_frost")
                .name("Raio de Geada")
                .school(SpellSchool.ICE).rarity(Rarity.UNCOMMON)
                .mana(22).cooldown(70).damage(10F).range(18F)
                .lore("Feixe contínuo de gelo. Aplica slow progressivo.")
                .cast(SpellLibrary::castRayOfFrost).build());

        reg(SpellDef.builder("ice_lance")
                .name("Lança de Gelo")
                .school(SpellSchool.ICE).rarity(Rarity.RARE)
                .mana(28).cooldown(70).damage(16F).range(28F)
                .lore("Lança gelada veloz. Dano dobrado contra alvos congelados.")
                .cast(SpellLibrary::castIceLance).build());
    }

    // ════════════════════════════════════════════════════════════════════════
    // LIGHTNING (8 spells)
    // ════════════════════════════════════════════════════════════════════════

    private static void registerLightning() {
        reg(SpellDef.builder("lightning_bolt")
                .name("Raio Mágico")
                .school(SpellSchool.LIGHTNING).rarity(Rarity.UNCOMMON)
                .mana(20).cooldown(60).damage(15F).range(32F)
                .lore("Invoca raio celestial. Atordoamento + 50% dmg vs water mobs.")
                .cast(SpellLibrary::castLightningBolt).build());

        reg(SpellDef.builder("spark_burst")
                .name("Explosão de Faíscas")
                .school(SpellSchool.LIGHTNING).rarity(Rarity.COMMON)
                .mana(12).cooldown(30).damage(5F).range(6F)
                .lore("Pequenas faíscas radial. Atordoamento curto.")
                .cast(SpellLibrary::castSparkBurst).build());

        reg(SpellDef.builder("chain_lightning")
                .name("Raio em Cadeia")
                .school(SpellSchool.LIGHTNING).rarity(Rarity.RARE)
                .mana(40).cooldown(120).damage(10F).range(20F)
                .lore("Pula entre até 5 inimigos. Cada salto -20% dano.")
                .cast(SpellLibrary::castChainLightning).build());

        reg(SpellDef.builder("shock")
                .name("Choque")
                .school(SpellSchool.LIGHTNING).rarity(Rarity.COMMON)
                .mana(8).cooldown(20).damage(4F).range(8F)
                .lore("Toque elétrico. Knockback + slowness curto.")
                .cast(SpellLibrary::castShock).build());

        reg(SpellDef.builder("thunder_step")
                .name("Passo do Trovão")
                .school(SpellSchool.LIGHTNING).rarity(Rarity.RARE)
                .mana(25).cooldown(90).damage(8F).range(20F)
                .lore("Teleport rápido. Cria raio no destino, atordoa próximos.")
                .cast(SpellLibrary::castThunderStep).build());

        reg(SpellDef.builder("storm_cloud")
                .name("Nuvem de Tempestade")
                .school(SpellSchool.LIGHTNING).rarity(Rarity.EPIC)
                .mana(60).cooldown(240).damage(8F).range(12F)
                .lore("Cria nuvem persistente. Raios aleatórios atingem inimigos.")
                .cast(SpellLibrary::castStormCloud).build());

        reg(SpellDef.builder("static_field")
                .name("Campo Estático")
                .school(SpellSchool.LIGHTNING).rarity(Rarity.UNCOMMON)
                .mana(30).cooldown(120).damage(2F).range(10F)
                .lore("Aura elétrica passiva 10s. Quem encosta toma dano + slow.")
                .cast(SpellLibrary::castStaticField).build());

        reg(SpellDef.builder("lightning_lance_spell")
                .name("Lança do Trovão")
                .school(SpellSchool.LIGHTNING).rarity(Rarity.RARE)
                .mana(35).cooldown(100).damage(18F).range(30F)
                .lore("Projétil elétrico veloz que penetra alvos múltiplos.")
                .cast(SpellLibrary::castLightningLanceSpell).build());
    }

    // ════════════════════════════════════════════════════════════════════════
    // BLOOD (8 spells)
    // ════════════════════════════════════════════════════════════════════════

    private static void registerBlood() {
        reg(SpellDef.builder("blood_step")
                .name("Passo Sanguíneo")
                .school(SpellSchool.BLOOD).rarity(Rarity.UNCOMMON)
                .mana(20).cooldown(60).damage(2F).range(12F)
                .lore("Teleport rápido. Custa HP em vez de mana extra. Deixa rastro de sangue.")
                .cast(SpellLibrary::castBloodStep).build());

        reg(SpellDef.builder("lifedrain")
                .name("Drenagem Vital")
                .school(SpellSchool.BLOOD).rarity(Rarity.RARE)
                .mana(25).cooldown(80).damage(8F).range(12F)
                .lore("Drena HP de alvo. 75% do dano vira cura pro caster.")
                .cast(SpellLibrary::castLifedrain).build());

        reg(SpellDef.builder("heartstop")
                .name("Parar Coração")
                .school(SpellSchool.BLOOD).rarity(Rarity.EPIC)
                .mana(55).cooldown(240).damage(0F).range(12F)
                .lore("Para o coração do alvo. Stun 4s + 50% HP atual dele em dano.")
                .cast(SpellLibrary::castHeartstop).build());

        reg(SpellDef.builder("blood_spear")
                .name("Lança de Sangue")
                .school(SpellSchool.BLOOD).rarity(Rarity.RARE)
                .mana(28).cooldown(80).damage(14F).range(24F)
                .lore("Lança feita do próprio sangue. Cura caster ao perfurar.")
                .cast(SpellLibrary::castBloodSpear).build());

        reg(SpellDef.builder("sanguine_bind")
                .name("Laço Sanguíneo")
                .school(SpellSchool.BLOOD).rarity(Rarity.RARE)
                .mana(30).cooldown(120).damage(4F).range(14F)
                .lore("Liga caster e alvo. Dano no caster também atinge alvo.")
                .cast(SpellLibrary::castSanguineBind).build());

        reg(SpellDef.builder("crimson_mist")
                .name("Névoa Carmesim")
                .school(SpellSchool.BLOOD).rarity(Rarity.RARE)
                .mana(35).cooldown(140).damage(3F).range(8F)
                .lore("Cria névoa em volta. Cura caster + dano em quem está dentro.")
                .cast(SpellLibrary::castCrimsonMist).build());

        reg(SpellDef.builder("vampiric_touch")
                .name("Toque Vampírico")
                .school(SpellSchool.BLOOD).rarity(Rarity.UNCOMMON)
                .mana(18).cooldown(40).damage(6F).range(4F)
                .lore("Toque sangrento. Drena HP no contato. Aplica Bleed 5s.")
                .cast(SpellLibrary::castVampiricTouch).build());

        reg(SpellDef.builder("blood_pact")
                .name("Pacto de Sangue")
                .school(SpellSchool.BLOOD).rarity(Rarity.EPIC)
                .mana(40).cooldown(600).damage(0F).range(0F)
                .lore("Sacrifica 8HP. Ganha Strength III + Speed II por 60s.")
                .cast(SpellLibrary::castBloodPact).build());
    }

    // ════════════════════════════════════════════════════════════════════════
    // ELDRITCH (6 spells)
    // ════════════════════════════════════════════════════════════════════════

    private static void registerEldritch() {
        reg(SpellDef.builder("void_tentacle")
                .name("Tentáculo do Vazio")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.RARE)
                .mana(30).cooldown(100).damage(10F).range(14F)
                .lore("Tentáculo eldritch puxa alvo até você. Aplica Confusion.")
                .cast(SpellLibrary::castVoidTentacle).build());

        reg(SpellDef.builder("mind_spike")
                .name("Estaca Mental")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.UNCOMMON)
                .mana(22).cooldown(60).damage(8F).range(20F)
                .lore("Atinge a mente do alvo. Ignora armadura. Confusion + Blindness.")
                .cast(SpellLibrary::castMindSpike).build());

        reg(SpellDef.builder("eldritch_blast")
                .name("Explosão Eldritch")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.RARE)
                .mana(35).cooldown(80).damage(14F).range(22F)
                .lore("Feixe de energia sobrenatural. Ignora armadura. Nausea.")
                .cast(SpellLibrary::castEldritchBlast).build());

        reg(SpellDef.builder("soul_tear")
                .name("Rasgar Alma")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.EPIC)
                .mana(50).cooldown(200).damage(20F).range(10F)
                .lore("Rasga a alma do alvo. Wither V + Weakness IV. Ignora armor.")
                .cast(SpellLibrary::castSoulTear).build());

        reg(SpellDef.builder("madness_wave")
                .name("Onda da Loucura")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.RARE)
                .mana(40).cooldown(160).damage(6F).range(10F)
                .lore("Pulso de loucura radial. Confusion + Blindness em todos.")
                .cast(SpellLibrary::castMadnessWave).build());

        reg(SpellDef.builder("cosmic_void_spell")
                .name("Vazio Cósmico")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.EPIC)
                .mana(70).cooldown(300).damage(25F).range(18F)
                .lore("Abre vortex cósmico. Suga inimigos + dano extremo no centro.")
                .cast(SpellLibrary::castCosmicVoid).build());
    }

    // ════════════════════════════════════════════════════════════════════════
    // HOLY (8 spells)
    // ════════════════════════════════════════════════════════════════════════

    private static void registerHoly() {
        reg(SpellDef.builder("greater_heal")
                .name("Cura Maior")
                .school(SpellSchool.HOLY).rarity(Rarity.RARE)
                .mana(35).cooldown(160).damage(16F).range(0F)
                .lore("Cura forte. Remove todos debuffs. Aplica Regeneration II 10s.")
                .cast(SpellLibrary::castGreaterHeal).build());

        reg(SpellDef.builder("smite")
                .name("Castigo Divino")
                .school(SpellSchool.HOLY).rarity(Rarity.UNCOMMON)
                .mana(20).cooldown(60).damage(12F).range(16F)
                .lore("Luz divina dos céus. +100% dmg vs undead/eldritch. Glow.")
                .cast(SpellLibrary::castSmite).build());

        reg(SpellDef.builder("divine_light")
                .name("Luz Divina")
                .school(SpellSchool.HOLY).rarity(Rarity.UNCOMMON)
                .mana(15).cooldown(80).damage(0F).range(0F)
                .lore("Buff: Resistance III + Glowing + Speed por 30s.")
                .cast(SpellLibrary::castDivineLight).build());

        reg(SpellDef.builder("sun_strike")
                .name("Golpe Solar")
                .school(SpellSchool.HOLY).rarity(Rarity.RARE)
                .mana(40).cooldown(120).damage(18F).range(20F)
                .lore("Raio solar concentrado em ponto único. Queima undead extra.")
                .cast(SpellLibrary::castSunStrike).build());

        reg(SpellDef.builder("holy_lance_spell")
                .name("Lança Sagrada")
                .school(SpellSchool.HOLY).rarity(Rarity.RARE)
                .mana(28).cooldown(70).damage(15F).range(26F)
                .lore("Lança dourada. Ignora armadura. Penetra alvos.")
                .cast(SpellLibrary::castHolyLance).build());

        reg(SpellDef.builder("healing_aura")
                .name("Aura Curativa")
                .school(SpellSchool.HOLY).rarity(Rarity.RARE)
                .mana(35).cooldown(200).damage(2F).range(8F)
                .lore("Cura passiva radial 15s. Aliados próximos curam 2HP/2s.")
                .cast(SpellLibrary::castHealingAura).build());

        reg(SpellDef.builder("sacred_ground")
                .name("Solo Sagrado")
                .school(SpellSchool.HOLY).rarity(Rarity.RARE)
                .mana(45).cooldown(240).damage(4F).range(6F)
                .lore("Marca chão sagrado. Undead em volta tomam dano contínuo.")
                .cast(SpellLibrary::castSacredGround).build());

        reg(SpellDef.builder("judgment")
                .name("Julgamento")
                .school(SpellSchool.HOLY).rarity(Rarity.EPIC)
                .mana(60).cooldown(300).damage(22F).range(20F)
                .lore("Convoca pilar de luz. Dano massivo + Strength pra aliados.")
                .cast(SpellLibrary::castJudgment).build());
    }

    // ════════════════════════════════════════════════════════════════════════
    // NATURE (6 spells)
    // ════════════════════════════════════════════════════════════════════════

    private static void registerNature() {
        reg(SpellDef.builder("vine_tangle")
                .name("Emaranhado de Cipós")
                .school(SpellSchool.NATURE).rarity(Rarity.UNCOMMON)
                .mana(18).cooldown(80).damage(2F).range(14F)
                .lore("Cipós prendem alvo. Slowness IV + Poison + dano leve.")
                .cast(SpellLibrary::castVineTangle).build());

        reg(SpellDef.builder("earth_wall")
                .name("Muro de Pedra")
                .school(SpellSchool.NATURE).rarity(Rarity.UNCOMMON)
                .mana(25).cooldown(100).damage(0F).range(6F)
                .lore("Ergue muro de cobblestone 3x3 à frente.")
                .cast(SpellLibrary::castEarthWall).build());

        reg(SpellDef.builder("stone_shard")
                .name("Lasca de Pedra")
                .school(SpellSchool.NATURE).rarity(Rarity.COMMON)
                .mana(10).cooldown(25).damage(7F).range(18F)
                .lore("Projétil pétreo. Knockback maior contra blocos.")
                .cast(SpellLibrary::castStoneShard).build());

        reg(SpellDef.builder("wisps_heal")
                .name("Cura dos Wisps")
                .school(SpellSchool.NATURE).rarity(Rarity.RARE)
                .mana(30).cooldown(150).damage(10F).range(12F)
                .lore("Wisps curativos giram em volta. Aliados próximos recebem +HP.")
                .cast(SpellLibrary::castWispsHeal).build());

        reg(SpellDef.builder("roots")
                .name("Raízes")
                .school(SpellSchool.NATURE).rarity(Rarity.COMMON)
                .mana(15).cooldown(60).damage(0F).range(10F)
                .lore("Raízes prendem alvo no chão. Imobiliza 4s.")
                .cast(SpellLibrary::castRoots).build());

        reg(SpellDef.builder("bramble_storm")
                .name("Tempestade de Espinhos")
                .school(SpellSchool.NATURE).rarity(Rarity.EPIC)
                .mana(50).cooldown(200).damage(8F).range(10F)
                .lore("Vortex de espinhos radial. Hits múltiplos + Poison.")
                .cast(SpellLibrary::castBrambleStorm).build());
    }

    // ════════════════════════════════════════════════════════════════════════
    // EVOCATION (4 spells)
    // ════════════════════════════════════════════════════════════════════════

    private static void registerEvocation() {
        reg(SpellDef.builder("magic_missile")
                .name("Míssil Mágico")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.COMMON)
                .mana(8).cooldown(20).damage(5F).range(20F)
                .lore("Projétil arcano básico. Auto-tracking parcial.")
                .cast(SpellLibrary::castMagicMissile).build());

        reg(SpellDef.builder("bone_spear")
                .name("Lança Óssea")
                .school(SpellSchool.BLOOD).rarity(Rarity.UNCOMMON)
                .mana(15).cooldown(50).damage(10F).range(22F)
                .lore("Lança feita de ossos. +100% dano contra vivos.")
                .cast(SpellLibrary::castBoneSpear).build());

        reg(SpellDef.builder("magic_shield")
                .name("Escudo Arcano")
                .school(SpellSchool.HOLY).rarity(Rarity.RARE)
                .mana(35).cooldown(180).damage(0F).range(0F)
                .lore("Escudo de absorção 15HP. Resistance III por 30s.")
                .cast(SpellLibrary::castMagicShield).build());

        reg(SpellDef.builder("summon_vex")
                .name("Invocar Vex")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.RARE)
                .mana(40).cooldown(400).damage(0F).range(0F)
                .lore("Invoca 2 vexes leais por 60s.")
                .cast(SpellLibrary::castSummonVex).build());

        // r138 — NECROMANCY (3 summon spells)
        reg(SpellDef.builder("raise_skeleton")
                .name("Erguer Esqueleto")
                .school(SpellSchool.BLOOD).rarity(Rarity.UNCOMMON)
                .mana(30).cooldown(300).damage(0F).range(0F)
                .lore("Invoca 1 esqueleto leal por 60s. Equipado com arco.")
                .cast(SpellLibrary::castRaiseSkeleton).build());

        reg(SpellDef.builder("summon_zombie_minion")
                .name("Servo Zumbi")
                .school(SpellSchool.BLOOD).rarity(Rarity.UNCOMMON)
                .mana(35).cooldown(300).damage(0F).range(0F)
                .lore("Invoca 2 zumbis leais por 60s. Tankam dano por você.")
                .cast(SpellLibrary::castSummonZombieMinion).build());

        reg(SpellDef.builder("spectral_wolf")
                .name("Lobo Espectral")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.RARE)
                .mana(45).cooldown(500).damage(0F).range(0F)
                .lore("Invoca 1 lobo espectral por 90s. Caça automaticamente o último alvo do caster.")
                .cast(SpellLibrary::castSpectralWolf).build());
    }

    // r138 — NECROMANCY IMPLEMENTATIONS

    private static boolean castRaiseSkeleton(CastContext ctx) {
        var skel = net.minecraft.world.entity.EntityType.SKELETON.create(ctx.level);
        if (skel == null) return false;
        Vec3 spawnPos = ctx.caster.position().add(ctx.lookVec().scale(2.0));
        skel.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, ctx.caster.getYRot(), 0);
        // Equipa arco + algumas flechas implicit
        skel.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
        // Marca como minion via NBT (target finder vai usar isso)
        skel.getPersistentData().putUUID("liberthia.summon_owner", ctx.caster.getUUID());
        skel.getPersistentData().putLong("liberthia.summon_expires",
                ctx.level.getGameTime() + 1200);
        skel.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0));
        skel.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1));
        ctx.level.addFreshEntity(skel);
        // VFX necromantic
        for (int i = 0; i < 30; i++) {
            ctx.level.sendParticles(ParticleTypes.SOUL,
                    spawnPos.x, spawnPos.y + 0.5, spawnPos.z,
                    1, 0.4, 0.6, 0.4, 0.08);
        }
        playCastSound(ctx, SoundEvents.SOUL_ESCAPE, 1.2F);
        return true;
    }

    private static boolean castSummonZombieMinion(CastContext ctx) {
        for (int i = 0; i < 2; i++) {
            var zomb = net.minecraft.world.entity.EntityType.ZOMBIE.create(ctx.level);
            if (zomb == null) continue;
            Vec3 spawnPos = ctx.caster.position()
                    .add(ctx.lookVec().scale(2.0))
                    .add((i - 0.5), 0, 0);
            zomb.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, ctx.caster.getYRot(), 0);
            zomb.getPersistentData().putUUID("liberthia.summon_owner", ctx.caster.getUUID());
            zomb.getPersistentData().putLong("liberthia.summon_expires",
                    ctx.level.getGameTime() + 1200);
            zomb.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1));
            zomb.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 2));
            zomb.setHealth(zomb.getMaxHealth() * 1.5F);
            ctx.level.addFreshEntity(zomb);
        }
        for (int i = 0; i < 40; i++) {
            ctx.level.sendParticles(ParticleTypes.SCULK_SOUL,
                    ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                    1, 0.8, 0.5, 0.8, 0.1);
        }
        playCastSound(ctx, SoundEvents.ZOMBIE_AMBIENT, 0.8F);
        return true;
    }

    private static boolean castSpectralWolf(CastContext ctx) {
        var wolf = net.minecraft.world.entity.EntityType.WOLF.create(ctx.level);
        if (wolf == null) return false;
        Vec3 spawnPos = ctx.caster.position().add(ctx.lookVec().scale(2.0));
        wolf.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, ctx.caster.getYRot(), 0);
        // Tame + spectral marker — ctx.caster sempre é ServerPlayer
        wolf.setTame(true);
        wolf.setOwnerUUID(ctx.caster.getUUID());
        wolf.getPersistentData().putUUID("liberthia.summon_owner", ctx.caster.getUUID());
        wolf.getPersistentData().putLong("liberthia.summon_expires",
                ctx.level.getGameTime() + 1800);
        wolf.getPersistentData().putBoolean("liberthia.spectral_wolf", true);
        wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1800, 2));
        wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 2));
        wolf.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1800, 0));
        wolf.setHealth(wolf.getMaxHealth());
        ctx.level.addFreshEntity(wolf);
        for (int i = 0; i < 50; i++) {
            ctx.level.sendParticles(ParticleTypes.END_ROD,
                    spawnPos.x, spawnPos.y + 0.3, spawnPos.z,
                    1, 0.4, 0.4, 0.4, 0.12);
        }
        playCastSound(ctx, SoundEvents.WOLF_HOWL, 1.5F);
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    // SHARED CAST IMPLEMENTATIONS
    // ════════════════════════════════════════════════════════════════════════

    /** r112: Spawna SpellProjectileEntity real (trail customizado emissivo + impacto VFX rico). */
    private static boolean projectile(CastContext ctx, SchoolDamageSource sds,
                                       float explosionRadius, boolean igniteBlocks) {
        Vec3 look = ctx.lookVec();
        // Velocidade do projétil — feitiços rápidos mas não instant
        Vec3 motion = look.scale(1.8);

        SpellProjectileEntity proj = new SpellProjectileEntity(
                ctx.level, ctx.caster, motion,
                ctx.def.school, ctx.def.damage);
        // Lifetime baseado em range: range / velocidade (1.8 blocos/tick)
        int life = Math.max(40, (int)(ctx.def.range / 1.8) + 10);
        proj.withConfig(life, false, explosionRadius, igniteBlocks);

        ctx.level.addFreshEntity(proj);

        // r165: VFX agora aparece À FRENTE do caster (não bloqueando a câmera).
        // castOrigin() = 2.0 blocos à frente (~não atrapalha visão).
        Vec3 vfxPos = ctx.castOrigin();
        int hex = ctx.def.school.colorHex();
        float r = ((hex >> 16) & 0xFF) / 255F;
        float g = ((hex >> 8) & 0xFF) / 255F;
        float b = (hex & 0xFF) / 255F;
        // Pulse menor (size reduzida 0.4 → 0.25, scale 0.85 → 0.6) — antes era ENORME
        ConfigurableParticleOptions castPulse = new ConfigurableParticleOptions(
                ModParticles.ENGINE_PARTICLE.get(),
                r, g, b, 0.85F,
                0.25F, 0.05F,
                10,
                0.0F, 0.6F,
                0.4F,
                false, true, true);
        ctx.level.sendParticles(castPulse, vfxPos.x, vfxPos.y, vfxPos.z,
                8, 0.2, 0.2, 0.2, 0.06);

        // r165: helix ring agora também spawnar à frente (não na mão), e menor (0.3 raio)
        long t = ctx.level.getGameTime();
        br.com.murilo.liberthia.magic.spell.vfx.HelixSpawner.spawnRing(
                ctx.level, vfxPos, ctx.def.school, 0.3, 10, (t * 0.15) % (Math.PI * 2));

        // r166: spawn sprite-based VFX burst at cast origin (per-school)
        br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type castVfx = pickSpellVfx(ctx.def.school);
        br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.spawn(
                ctx.level, vfxPos, ctx.caster, castVfx);

        // Som de cast (2 camadas: low whoosh + high zing)
        playCastSound(ctx, SoundEvents.ENDER_DRAGON_FLAP, 1.4F);
        ctx.level.playSound(null, ctx.caster.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.6F, 1.8F);
        return true;
    }

    /**
     * r166: Maps a SpellSchool to its preferred sprite VFX type for cast burst.
     * Used by the {@link #projectile} helper and any other caster that wants a
     * school-themed visual flourish.
     */
    private static br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type
            pickSpellVfx(br.com.murilo.liberthia.magic.school.SpellSchool school) {
        return switch (school) {
            case FIRE      -> br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.FIRE;
            case ICE       -> br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.FREEZING;
            case LIGHTNING -> br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.MAGICKA_HIT;
            case BLOOD     -> br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.MIDNIGHT;
            case ELDRITCH  -> br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.VORTEX;
            case HOLY      -> br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.SUNBURN;
            case NATURE    -> br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.MAGIC_BUBBLES;
        };
    }

    // ─── FIRE ───
    private static boolean castBurningDash(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        ctx.caster.setDeltaMovement(look.scale(2.0));
        ctx.caster.setSecondsOnFire(0); // immunity
        DamageSource ds = SchoolDamageSource.fire(60).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(3))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
            le.setSecondsOnFire(5);
        }
        for (int i = 0; i < 30; i++) {
            ctx.level.sendParticles(ParticleTypes.FLAME,
                    ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                    1, 0.5, 0.5, 0.5, 0.1);
        }
        playCastSound(ctx, SoundEvents.BLAZE_SHOOT, 1.0F);
        return true;
    }

    private static boolean castInferno(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        DamageSource ds = SchoolDamageSource.fire(160).toVanilla(ctx.level, ctx.caster);
        int hits = 0;
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            Vec3 toE = le.position().subtract(ctx.caster.position()).normalize();
            if (toE.dot(look) > 0.6) {
                le.hurt(ds, ctx.def.damage);
                le.setSecondsOnFire(8);
                hits++;
            }
        }
        for (double d = 0.5; d < ctx.def.range; d += 0.5) {
            Vec3 p = ctx.handOrigin().add(look.scale(d));
            ctx.level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z,
                    4, 0.5 * (d / ctx.def.range), 0.5 * (d / ctx.def.range), 0.5 * (d / ctx.def.range), 0.05);
        }
        playCastSound(ctx, SoundEvents.BLAZE_SHOOT, 0.8F);
        return hits > 0 || true;
    }

    private static boolean castMagmaBomb(CastContext ctx) {
        Vec3 target = ctx.pickHit(ctx.def.range);
        ctx.level.explode(ctx.caster, target.x, target.y, target.z, 2.5F, false,
                net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        DamageSource ds = SchoolDamageSource.fire(120).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(target, target).inflate(4))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
            le.setSecondsOnFire(6);
        }
        // Lava pool
        BlockPos bp = new BlockPos((int) target.x, (int) target.y, (int) target.z);
        for (BlockPos around : BlockPos.betweenClosed(bp.offset(-1, 0, -1), bp.offset(1, 0, 1))) {
            if (ctx.level.getBlockState(around.below()).isSolid()
                && ctx.level.getBlockState(around).isAir()) {
                ctx.level.setBlock(around, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
        return true;
    }

    private static boolean castSunBeam(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        Vec3 origin = ctx.handOrigin();
        DamageSource ds = SchoolDamageSource.fire(100).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            Vec3 toE = le.position().subtract(ctx.caster.position()).normalize();
            if (toE.dot(look) > 0.92) {
                float dmg = ctx.def.damage;
                if (le.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) dmg *= 1.5F;
                le.hurt(ds, dmg);
                le.setSecondsOnFire(10);
            }
        }
        for (double d = 0; d < ctx.def.range; d += 0.4) {
            Vec3 p = origin.add(look.scale(d));
            ctx.level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.1, 0.1, 0.1, 0);
            ctx.level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
        }
        playCastSound(ctx, SoundEvents.BEACON_ACTIVATE, 1.2F);
        return true;
    }

    private static boolean castPhoenixReborn(CastContext ctx) {
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400, 0, false, false));
        ctx.caster.getPersistentData().putBoolean("liberthia.phoenix_reborn", true);
        ctx.caster.getPersistentData().putLong("liberthia.phoenix_reborn_expires",
                ctx.level.getGameTime() + 2400);
        for (int i = 0; i < 40; i++) {
            ctx.level.sendParticles(ParticleTypes.FLAME,
                    ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                    1, 0.5, 0.8, 0.5, 0.15);
        }
        playCastSound(ctx, SoundEvents.BLAZE_AMBIENT, 0.6F);
        return true;
    }

    private static boolean castCauterize(CastContext ctx) {
        ctx.caster.heal(ctx.def.damage);
        ctx.caster.hurt(ctx.caster.damageSources().onFire(), 2F);
        ctx.caster.removeEffect(MobEffects.POISON);
        ctx.caster.removeEffect(MobEffects.WITHER);
        ctx.caster.removeEffect(MobEffects.HUNGER);
        ctx.level.sendParticles(ParticleTypes.LAVA,
                ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                20, 0.3, 0.5, 0.3, 0.05);
        return true;
    }

    private static boolean castHeatWave(CastContext ctx) {
        DamageSource ds = SchoolDamageSource.fire(100).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
            le.setSecondsOnFire(5);
            Vec3 away = le.position().subtract(ctx.caster.position()).normalize().scale(1.5);
            le.setDeltaMovement(away.x, 0.6, away.z);
            le.hurtMarked = true;
        }
        // r115: 3 anéis expansivos no chão (decals) sustentados 30 ticks cada,
        // disparados em sequência. Visual = explosão expansiva no chão.
        Vec3 center = ctx.caster.position();
        br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.spawnGroundDecal(
                ctx.level, center, ctx.def.school, ctx.def.range, 25);
        br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.spawnAreaPulse(
                ctx.level, center.add(0, 0.5, 0), ctx.def.school, ctx.def.range * 0.6, 30);
        playCastSound(ctx, SoundEvents.GENERIC_EXPLODE, 1.0F);
        return true;
    }

    // ─── ICE ───
    private static boolean castIceSpike(CastContext ctx) {
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        if (target == null) {
            ctx.caster.displayClientMessage(net.minecraft.network.chat.Component.literal("§7Aponte pra um alvo"), true);
            return false;
        }
        DamageSource ds = new SchoolDamageSource(SpellSchool.ICE, 0, 0, 60, true)
                .toVanilla(ctx.level, ctx.caster);
        target.hurt(ds, ctx.def.damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
        target.setTicksFrozen(target.getTicksFrozen() + 60);
        ctx.level.sendParticles(ParticleTypes.SNOWFLAKE,
                target.getX(), target.getY() + 1, target.getZ(),
                25, 0.3, 0.5, 0.3, 0.1);
        playCastSound(ctx, SoundEvents.GLASS_BREAK, 0.8F);
        return true;
    }

    private static boolean castFrostNova(CastContext ctx) {
        DamageSource ds = SchoolDamageSource.ice(120).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 4));
            le.setTicksFrozen(le.getTicksFrozen() + 100);
        }
        // r115: Ground decal expansivo + esfera de pulsos de gelo no centro
        Vec3 center = ctx.caster.position();
        br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.spawnGroundDecal(
                ctx.level, center, ctx.def.school, ctx.def.range, 40);
        br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.spawnAreaPulse(
                ctx.level, center.add(0, 1, 0), ctx.def.school, ctx.def.range * 0.7, 50);
        playCastSound(ctx, SoundEvents.GLASS_BREAK, 0.6F);
        return true;
    }

    private static boolean castGlacialStorm(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        DamageSource ds = SchoolDamageSource.ice(100).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage * 3);
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 3));
        }
        // r115: Tempestade SUSTAINED — esfera de partículas pulsando por 100t (5s)
        // + decal de chão expandindo pelos primeiros 30t
        br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.spawnGroundDecal(
                ctx.level, center, ctx.def.school, ctx.def.range, 30);
        br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.spawnAreaPulse(
                ctx.level, center.add(0, 2, 0), ctx.def.school, ctx.def.range, 100);
        playCastSound(ctx, SoundEvents.WEATHER_RAIN, 1.2F);
        return true;
    }

    private static boolean castFrostStep(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        Vec3 dest = ctx.caster.position().add(look.scale(ctx.def.range));
        // Trail of ice
        for (double d = 0; d <= ctx.def.range; d += 1) {
            Vec3 p = ctx.caster.position().add(look.scale(d));
            BlockPos bp = new BlockPos((int) p.x, (int) p.y - 1, (int) p.z);
            if (ctx.level.getBlockState(bp).isSolid()
                && ctx.level.getBlockState(bp.above()).isAir()) {
                ctx.level.setBlock(bp.above(), Blocks.SNOW.defaultBlockState(), 3);
            }
            ctx.level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 5, 0.2, 0.2, 0.2, 0);
        }
        ctx.caster.teleportTo(dest.x, dest.y, dest.z);
        // Freeze enemies in trail
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(ctx.caster.position(), dest).inflate(2))) {
            if (le == ctx.caster) continue;
            le.setTicksFrozen(le.getTicksFrozen() + 60);
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
        }
        playCastSound(ctx, SoundEvents.ENDERMAN_TELEPORT, 1.5F);
        return true;
    }

    private static boolean castFrozenGround(CastContext ctx) {
        BlockPos center = ctx.caster.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-(int) ctx.def.range, -1, -(int) ctx.def.range),
                center.offset((int) ctx.def.range, 0, (int) ctx.def.range))) {
            double d = Math.sqrt(pos.distSqr(center));
            if (d > ctx.def.range) continue;
            BlockState st = ctx.level.getBlockState(pos);
            if (st.is(Blocks.WATER)) ctx.level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
            else if (st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.DIRT))
                ctx.level.setBlock(pos, Blocks.PACKED_ICE.defaultBlockState(), 3);
        }
        for (int i = 0; i < 30; i++) {
            ctx.level.sendParticles(ParticleTypes.SNOWFLAKE,
                    ctx.caster.getX(), ctx.caster.getY(), ctx.caster.getZ(),
                    2, ctx.def.range * 0.5, 0.3, ctx.def.range * 0.5, 0.05);
        }
        return true;
    }

    private static boolean castRayOfFrost(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        Vec3 origin = ctx.handOrigin();
        DamageSource ds = SchoolDamageSource.ice(80).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            Vec3 toE = le.position().subtract(ctx.caster.position()).normalize();
            if (toE.dot(look) > 0.95) {
                le.hurt(ds, ctx.def.damage);
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                le.setTicksFrozen(le.getTicksFrozen() + 40);
            }
        }
        for (double d = 0; d < ctx.def.range; d += 0.3) {
            Vec3 p = origin.add(look.scale(d));
            ctx.level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
        }
        playCastSound(ctx, SoundEvents.GLASS_HIT, 0.8F);
        return true;
    }

    private static boolean castIceLance(CastContext ctx) {
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        if (target == null) return projectile(ctx, SchoolDamageSource.ice(80), 0F, false);
        float dmg = ctx.def.damage;
        if (target.getTicksFrozen() > 0) dmg *= 2.0F;
        target.hurt(SchoolDamageSource.ice(80).toVanilla(ctx.level, ctx.caster), dmg);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
        ctx.level.sendParticles(ParticleTypes.SNOWFLAKE,
                target.getX(), target.getY() + 1, target.getZ(),
                20, 0.3, 0.5, 0.3, 0.1);
        return true;
    }

    // ─── LIGHTNING ───
    private static boolean castLightningBolt(CastContext ctx) {
        Vec3 target = ctx.pickHit(ctx.def.range);
        LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(ctx.level);
        if (bolt != null) {
            bolt.moveTo(target.x, target.y, target.z);
            bolt.setVisualOnly(false);
            ctx.level.addFreshEntity(bolt);
        }
        return true;
    }

    private static boolean castSparkBurst(CastContext ctx) {
        DamageSource ds = SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
        for (int i = 0; i < 30; i++) {
            ctx.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                    1, 0.5, 0.5, 0.5, 0.3);
        }
        playCastSound(ctx, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F);
        return true;
    }

    private static boolean castChainLightning(CastContext ctx) {
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        if (target == null) {
            ctx.caster.displayClientMessage(net.minecraft.network.chat.Component.literal("§7Aponte pra um alvo"), true);
            return false;
        }
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        hit.add(target);
        target.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster),
                ctx.def.damage);
        LivingEntity current = target;
        float dmg = ctx.def.damage;
        for (int i = 0; i < 4; i++) {
            dmg *= 0.8F;
            LivingEntity next = null;
            double bestDist = 10;
            for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                    current.getBoundingBox().inflate(8))) {
                if (le == ctx.caster || hit.contains(le)) continue;
                double d = le.distanceTo(current);
                if (d < bestDist) { bestDist = d; next = le; }
            }
            if (next == null) break;
            hit.add(next);
            next.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster), dmg);
            spawnChainParticles(ctx, current.position().add(0, 1, 0),
                    next.position().add(0, 1, 0));
            current = next;
        }
        playCastSound(ctx, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8F);
        return true;
    }

    private static boolean castShock(CastContext ctx) {
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        if (target == null) return false;
        target.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster),
                ctx.def.damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        Vec3 away = target.position().subtract(ctx.caster.position()).normalize().scale(0.8);
        target.setDeltaMovement(away.x, 0.3, away.z);
        target.hurtMarked = true;
        ctx.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), target.getY() + 1, target.getZ(), 15, 0.3, 0.3, 0.3, 0.3);
        return true;
    }

    private static boolean castThunderStep(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        Vec3 dest = ctx.caster.position().add(look.scale(ctx.def.range));
        ctx.caster.teleportTo(dest.x, dest.y, dest.z);
        // Lightning at destination
        LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(ctx.level);
        if (bolt != null) {
            bolt.moveTo(dest);
            bolt.setVisualOnly(false);
            ctx.level.addFreshEntity(bolt);
        }
        DamageSource ds = SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(4))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
        }
        return true;
    }

    private static boolean castStormCloud(CastContext ctx) {
        Vec3 target = ctx.pickHit(ctx.def.range);
        // Strike 3 immediate bolts
        for (int i = 0; i < 3; i++) {
            double dx = (Math.random() - 0.5) * ctx.def.range;
            double dz = (Math.random() - 0.5) * ctx.def.range;
            LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(ctx.level);
            if (bolt != null) {
                bolt.moveTo(target.x + dx, target.y, target.z + dz);
                bolt.setVisualOnly(false);
                ctx.level.addFreshEntity(bolt);
            }
        }
        return true;
    }

    private static boolean castStaticField(CastContext ctx) {
        ctx.caster.getPersistentData().putLong("liberthia.static_field_expires",
                ctx.level.getGameTime() + 200);
        for (int i = 0; i < 30; i++) {
            ctx.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                    1, 1.5, 0.5, 1.5, 0.05);
        }
        return true;
    }

    private static boolean castLightningLanceSpell(CastContext ctx) {
        return projectile(ctx, SchoolDamageSource.of(SpellSchool.LIGHTNING), 0F, false);
    }

    // ─── BLOOD ───
    private static boolean castBloodStep(CastContext ctx) {
        ctx.caster.hurt(ctx.caster.damageSources().magic(), 2F);
        Vec3 look = ctx.lookVec();
        Vec3 dest = ctx.caster.position().add(look.scale(ctx.def.range));
        ctx.caster.teleportTo(dest.x, dest.y, dest.z);
        for (int i = 0; i < 20; i++) {
            ctx.level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    dest.x, dest.y + 1, dest.z, 1, 0.3, 0.5, 0.3, 0.05);
        }
        return true;
    }

    private static boolean castLifedrain(CastContext ctx) {
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        if (target == null) return false;
        target.hurt(SchoolDamageSource.blood(0.75F).toVanilla(ctx.level, ctx.caster), ctx.def.damage);
        ctx.caster.heal(ctx.def.damage * 0.75F);
        spawnChainParticles(ctx, target.position().add(0, 1, 0),
                ctx.caster.position().add(0, 1, 0));
        return true;
    }

    private static boolean castHeartstop(CastContext ctx) {
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        if (target == null) return false;
        float dmg = target.getHealth() * 0.5F;
        target.hurt(SchoolDamageSource.blood(0).toVanilla(ctx.level, ctx.caster), dmg);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 10));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 4));
        ctx.level.sendParticles(ParticleTypes.HEART,
                target.getX(), target.getY() + 1, target.getZ(),
                10, 0.3, 0.5, 0.3, 0.05);
        return true;
    }

    private static boolean castBloodSpear(CastContext ctx) {
        boolean ok = projectile(ctx, SchoolDamageSource.blood(0.4F), 0F, false);
        if (ok) ctx.caster.heal(ctx.def.damage * 0.4F);
        return ok;
    }

    private static boolean castSanguineBind(CastContext ctx) {
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        if (target == null) return false;
        target.getPersistentData().putUUID("liberthia.sanguine_bind_caster", ctx.caster.getUUID());
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
        ctx.level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                target.getX(), target.getY() + 1, target.getZ(),
                15, 0.3, 0.5, 0.3, 0.05);
        return true;
    }

    private static boolean castCrimsonMist(CastContext ctx) {
        DamageSource ds = SchoolDamageSource.blood(0.5F).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
            ctx.caster.heal(ctx.def.damage * 0.5F);
        }
        // r115: névoa carmesim sustained 60t — sphere de partículas + ground decal sangrento
        Vec3 mistCenter = ctx.caster.position();
        br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.spawnAreaPulse(
                ctx.level, mistCenter.add(0, 1, 0), ctx.def.school, ctx.def.range * 0.7, 60);
        br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.spawnGroundDecal(
                ctx.level, mistCenter, ctx.def.school, ctx.def.range * 0.6, 35);
        return true;
    }

    private static boolean castVampiricTouch(CastContext ctx) {
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        if (target == null) return false;
        target.hurt(SchoolDamageSource.blood(1.0F).toVanilla(ctx.level, ctx.caster), ctx.def.damage);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        ctx.caster.heal(ctx.def.damage);
        ctx.level.sendParticles(ParticleTypes.HEART,
                ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                5, 0.2, 0.3, 0.2, 0.05);
        return true;
    }

    private static boolean castBloodPact(CastContext ctx) {
        ctx.caster.hurt(ctx.caster.damageSources().magic(), 8F);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 2));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 1));
        for (int i = 0; i < 30; i++) {
            ctx.level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                    1, 0.4, 0.6, 0.4, 0.05);
        }
        return true;
    }

    // ─── ELDRITCH ───
    /**
     * r165: <b>Tentáculo do Vazio</b> — agora spawna 5 tentáculos eldritch reais
     * em arco frontal (-60°, -30°, 0°, +30°, +60°), 3-5 blocos do caster.
     * Cada tentáculo é uma entidade animada (8 frames) que vive 80 ticks e
     * causa dano + pull em qualquer entidade dentro de 3 blocos.
     */
    private static boolean castVoidTentacle(CastContext ctx) {
        Vec3 look = ctx.lookVec().normalize();
        Vec3 baseFront = ctx.caster.position().add(look.scale(4.0));
        // r165: ground-snap — encontra o chão abaixo de baseFront pra spawnar
        // o tentáculo no chão (3.2 alto, então y=floor)
        double groundY = findGround(ctx.level, baseFront);
        Vec3 ground = new Vec3(baseFront.x, groundY, baseFront.z);

        float dmg = ctx.def.damage * 1.4f;  // r165: BUFF de 10 → 14
        float[] angles = { -60, -30, 0, 30, 60 };
        for (float angleDeg : angles) {
            double rad = Math.toRadians(angleDeg);
            // Rotate look vector around Y-axis
            double cos = Math.cos(rad), sin = Math.sin(rad);
            double rx = look.x * cos - look.z * sin;
            double rz = look.x * sin + look.z * cos;
            Vec3 dir = new Vec3(rx, 0, rz).normalize();
            // Distance varies slightly per tentacle for organic look
            double dist = 3.5 + Math.abs(angleDeg) * 0.02;
            Vec3 spawnPos = ctx.caster.position().add(dir.scale(dist));
            double sy = findGround(ctx.level, spawnPos);
            var ent = new br.com.murilo.liberthia.magic.spell.voidspell.VoidTentacleEntity(
                    ctx.level, ctx.caster,
                    new Vec3(spawnPos.x, sy, spawnPos.z), dmg);
            ctx.level.addFreshEntity(ent);
        }
        // r165: custom cosmic sounds — tendril movement + reality distortion
        try {
            ctx.level.playSound(null, ctx.caster.blockPosition(),
                    br.com.murilo.liberthia.registry.ModSounds.COSMIC_TENDRIL_MOVEMENT.get(),
                    SoundSource.PLAYERS, 1.0F, 0.9F);
            ctx.level.playSound(null, ctx.caster.blockPosition(),
                    br.com.murilo.liberthia.registry.ModSounds.COSMIC_REALITY_DISTORTION.get(),
                    SoundSource.PLAYERS, 0.7F, 1.1F);
        } catch (Throwable ignored) {
            playCastSound(ctx, SoundEvents.WARDEN_EMERGE, 0.8F);
        }
        return true;
    }

    /** r165: helper — find first solid block below pos (max 6 blocks search). */
    private static double findGround(net.minecraft.server.level.ServerLevel lvl, Vec3 from) {
        net.minecraft.core.BlockPos.MutableBlockPos cursor =
                new net.minecraft.core.BlockPos.MutableBlockPos(
                        (int)from.x, (int)from.y, (int)from.z);
        for (int dy = 0; dy < 6; dy++) {
            cursor.setY((int)from.y - dy);
            if (!lvl.getBlockState(cursor).isAir()) {
                return cursor.getY() + 1.01;
            }
        }
        return from.y;
    }

    private static boolean castMindSpike(CastContext ctx) {
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        if (target == null) return false;
        // r165: dano BUFF +50% (8 → 12) + ignora armadura via direct hurt
        float dmg = ctx.def.damage * 1.5f;
        target.hurt(SchoolDamageSource.eldritch().toVanilla(ctx.level, ctx.caster), dmg);
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 240, 1));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
        // r165: VoidEffectEntity sprite-based MIND_SPIKE em cima do target
        Vec3 tp = target.position().add(0, 1.6, 0);
        var spike = new br.com.murilo.liberthia.magic.spell.voidspell.VoidEffectEntity(
                ctx.level, ctx.caster, tp,
                br.com.murilo.liberthia.magic.spell.voidspell.VoidEffectEntity.TYPE_MIND_SPIKE,
                1.4f);
        ctx.level.addFreshEntity(spike);
        // r165: custom particles VOID_LEAK (substitui SCULK_SOUL vanilla)
        for (int i = 0; i < 18; i++) {
            double a = (i / 18.0) * Math.PI * 2;
            double r = 0.7;
            ctx.level.sendParticles(br.com.murilo.liberthia.registry.ModParticles.VOID_LEAK.get(),
                    tp.x + Math.cos(a) * r,
                    tp.y + Math.sin(a * 2) * 0.2,
                    tp.z + Math.sin(a) * r,
                    1, 0, 0, 0, 0.02);
        }
        // r165: custom cosmic sound (no lugar de ENDERMAN_TELEPORT vanilla)
        try {
            ctx.level.playSound(null, target.blockPosition(),
                    br.com.murilo.liberthia.registry.ModSounds.COSMIC_REALITY_DISTORTION.get(),
                    SoundSource.PLAYERS, 1.0F, 1.3F);
        } catch (Throwable ignored) { playCastSound(ctx, SoundEvents.ENDERMAN_TELEPORT, 1.3F); }
        return true;
    }

    private static boolean castEldritchBlast(CastContext ctx) {
        return projectile(ctx, SchoolDamageSource.eldritch(), 1.5F, false);
    }

    private static boolean castSoulTear(CastContext ctx) {
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        if (target == null) return false;
        // r165: BUFF 20 → 28 dmg, plus debuff stack
        float dmg = ctx.def.damage * 1.4f;
        target.hurt(SchoolDamageSource.eldritch().toVanilla(ctx.level, ctx.caster), dmg);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 240, 4));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 240, 3));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1));
        // r165: VoidEffectEntity SOUL_TEAR aura anchored to target (lives 80t)
        Vec3 tp = target.position().add(0, 1.0, 0);
        var aura = new br.com.murilo.liberthia.magic.spell.voidspell.VoidEffectEntity(
                ctx.level, ctx.caster, tp,
                br.com.murilo.liberthia.magic.spell.voidspell.VoidEffectEntity.TYPE_SOUL_TEAR,
                1.0f);
        // r165: anchor aura to target — sustain VFX following target
        aura.startRiding(target, true);
        ctx.level.addFreshEntity(aura);
        // r165: custom cosmic sound + distant scream
        try {
            ctx.level.playSound(null, target.blockPosition(),
                    br.com.murilo.liberthia.registry.ModSounds.COSMIC_DISTANT_SCREAM.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        } catch (Throwable ignored) { playCastSound(ctx, SoundEvents.SOUL_ESCAPE, 1.0F); }
        try {
            ctx.level.playSound(null, ctx.caster.blockPosition(),
                    br.com.murilo.liberthia.registry.ModSounds.COSMIC_VOID_BREATHING.get(),
                    SoundSource.PLAYERS, 0.7F, 0.8F);
        } catch (Throwable ignored) {}
        return true;
    }

    private static boolean castMadnessWave(CastContext ctx) {
        DamageSource ds = SchoolDamageSource.eldritch().toVanilla(ctx.level, ctx.caster);
        // r165: BUFF 6 → 12 damage
        float dmg = ctx.def.damage * 2.0f;
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, dmg);
            le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 240, 1));
            le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            // Knock back outward
            Vec3 kb = le.position().subtract(ctx.caster.position()).normalize().scale(0.8);
            le.setDeltaMovement(le.getDeltaMovement().add(kb.x, 0.3, kb.z));
            le.hurtMarked = true;
        }
        Vec3 madCenter = ctx.caster.position();
        // r165: VoidEffectEntity MADNESS_WAVE no chão, expandindo horizontalmente
        Vec3 ground = new Vec3(madCenter.x, findGround(ctx.level, madCenter) + 0.05, madCenter.z);
        // Spawn 4 wave entities radiating in cardinal directions for full coverage
        for (int dir = 0; dir < 4; dir++) {
            double ang = dir * Math.PI / 2;
            Vec3 spawn = ground.add(Math.cos(ang) * 0.1, 0, Math.sin(ang) * 0.1);
            var wave = new br.com.murilo.liberthia.magic.spell.voidspell.VoidEffectEntity(
                    ctx.level, ctx.caster, spawn,
                    br.com.murilo.liberthia.magic.spell.voidspell.VoidEffectEntity.TYPE_MADNESS_WAVE,
                    (float)(ctx.def.range / 10.0));
            wave.setYRot((float) Math.toDegrees(ang));
            ctx.level.addFreshEntity(wave);
        }
        // Custom DIMENSIONAL_CRACK particles (substituiu SCULK_SOUL vanilla)
        for (int i = 0; i < 40; i++) {
            double a = (i / 40.0) * Math.PI * 2;
            double r = ctx.def.range * 0.5;
            ctx.level.sendParticles(br.com.murilo.liberthia.registry.ModParticles.DIMENSIONAL_CRACK.get(),
                    madCenter.x + Math.cos(a) * r,
                    madCenter.y + 0.5,
                    madCenter.z + Math.sin(a) * r,
                    1, 0.05, 0.1, 0.05, 0.02);
        }
        // r165: custom cosmic sound — distorção da realidade
        try {
            ctx.level.playSound(null, ctx.caster.blockPosition(),
                    br.com.murilo.liberthia.registry.ModSounds.COSMIC_REALITY_DISTORTION.get(),
                    SoundSource.PLAYERS, 1.2F, 0.8F);
            ctx.level.playSound(null, ctx.caster.blockPosition(),
                    br.com.murilo.liberthia.registry.ModSounds.COSMIC_DISTANT_WHISPERS.get(),
                    SoundSource.PLAYERS, 0.7F, 1.0F);
        } catch (Throwable ignored) {
            playCastSound(ctx, SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.8F);
        }
        return true;
    }

    private static boolean castCosmicVoid(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        DamageSource ds = SchoolDamageSource.eldritch().toVanilla(ctx.level, ctx.caster);
        // r165: BUFF 25 → 40 damage
        float dmg = ctx.def.damage * 1.6f;
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, dmg);
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 240, 4));
            le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
            // Pull to center stronger
            Vec3 pull = center.subtract(le.position()).normalize().scale(2.0);
            le.setDeltaMovement(pull.x, pull.y + 0.5, pull.z);
            le.hurtMarked = true;
        }
        // r165: VoidEffectEntity COSMIC_VOID vortex no centro (100t lifespan)
        Vec3 vortexPos = new Vec3(center.x, center.y + 1.5, center.z);
        var vortex = new br.com.murilo.liberthia.magic.spell.voidspell.VoidEffectEntity(
                ctx.level, ctx.caster, vortexPos,
                br.com.murilo.liberthia.magic.spell.voidspell.VoidEffectEntity.TYPE_COSMIC_VOID,
                (float)(ctx.def.range / 8.0));
        ctx.level.addFreshEntity(vortex);
        // r165: sustained pull during the vortex's life — re-pull tick
        for (int t = 0; t < 100; t += 10) {
            final int tk = t;
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(
                    ctx.level, tk, () -> {
                        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                                new AABB(center, center).inflate(ctx.def.range))) {
                            if (le == ctx.caster) continue;
                            Vec3 pull = center.subtract(le.position()).normalize().scale(0.5);
                            le.setDeltaMovement(le.getDeltaMovement().add(pull.x, 0.1, pull.z));
                            le.hurtMarked = true;
                        }
                    });
        }
        // r165: custom cosmic sounds — sky hum + audience presence
        try {
            ctx.level.playSound(null,
                    new net.minecraft.core.BlockPos((int)center.x, (int)center.y, (int)center.z),
                    br.com.murilo.liberthia.registry.ModSounds.COSMIC_SKY_HUM.get(),
                    SoundSource.PLAYERS, 1.5F, 0.5F);
            ctx.level.playSound(null,
                    new net.minecraft.core.BlockPos((int)center.x, (int)center.y, (int)center.z),
                    br.com.murilo.liberthia.registry.ModSounds.COSMIC_AUDIENCE_PRESENCE.get(),
                    SoundSource.PLAYERS, 1.0F, 0.7F);
        } catch (Throwable ignored) {
            playCastSound(ctx, SoundEvents.WITHER_DEATH, 0.5F);
        }
        return true;
    }

    // ─── HOLY ───
    private static boolean castGreaterHeal(CastContext ctx) {
        ctx.caster.heal(ctx.def.damage);
        ctx.caster.removeAllEffects();
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        // r166: sustained MAGIC_BUBBLES VFX on caster (50t lifetime)
        var bubbles = new br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxEntity(
                ctx.level, ctx.caster, ctx.caster.position().add(0, 1, 0),
                br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.MAGIC_BUBBLES, 1.5F);
        bubbles.startRiding(ctx.caster, true);
        ctx.level.addFreshEntity(bubbles);
        for (int i = 0; i < 20; i++) {
            ctx.level.sendParticles(ParticleTypes.HEART,
                    ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                    1, 0.3, 0.5, 0.3, 0);
        }
        playCastSound(ctx, SoundEvents.PLAYER_LEVELUP, 1.5F);
        return true;
    }

    private static boolean castSmite(CastContext ctx) {
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        if (target == null) return false;
        float dmg = ctx.def.damage;
        if (target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) dmg *= 2.0F;
        target.hurt(SchoolDamageSource.of(SpellSchool.HOLY).toVanilla(ctx.level, ctx.caster), dmg);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        // r166: SUNBURN sprite VFX on target
        br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.spawn(
                ctx.level, target.position().add(0, 1, 0), ctx.caster,
                br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.SUNBURN, 1.4F);
        // Lightning visual
        LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(ctx.level);
        if (bolt != null) {
            bolt.moveTo(target.position());
            bolt.setVisualOnly(true);
            ctx.level.addFreshEntity(bolt);
        }
        return true;
    }

    private static boolean castDivineLight(CastContext ctx) {
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 2));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
        // r166: PROTECTION circle on ground, follows caster (riding)
        var prot = new br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxEntity(
                ctx.level, ctx.caster, ctx.caster.position(),
                br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.PROTECTION, 2.0F);
        prot.startRiding(ctx.caster, true);
        ctx.level.addFreshEntity(prot);
        for (int i = 0; i < 30; i++) {
            ctx.level.sendParticles(ParticleTypes.END_ROD,
                    ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                    1, 0.3, 0.5, 0.3, 0.05);
        }
        return true;
    }

    private static boolean castSunStrike(CastContext ctx) {
        Vec3 target = ctx.pickHit(ctx.def.range);
        DamageSource ds = SchoolDamageSource.of(SpellSchool.HOLY).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(target, target).inflate(4))) {
            if (le == ctx.caster) continue;
            float dmg = ctx.def.damage;
            if (le.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) dmg *= 1.5F;
            le.hurt(ds, dmg);
        }
        // Pillar of light
        for (int y = 0; y < 30; y++) {
            ctx.level.sendParticles(ParticleTypes.END_ROD,
                    target.x, target.y + y, target.z, 3, 0.3, 0, 0.3, 0);
        }
        return true;
    }

    private static boolean castHolyLance(CastContext ctx) {
        return projectile(ctx,
                new SchoolDamageSource(SpellSchool.HOLY, 0, 0, 0, true),
                0F, false);
    }

    private static boolean castHealingAura(CastContext ctx) {
        ctx.caster.getPersistentData().putLong("liberthia.healing_aura_expires",
                ctx.level.getGameTime() + 300);
        for (Player p : ctx.level.getEntitiesOfClass(Player.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0));
        }
        for (int i = 0; i < 40; i++) {
            ctx.level.sendParticles(ParticleTypes.END_ROD,
                    ctx.caster.getX(), ctx.caster.getY() + 0.3, ctx.caster.getZ(),
                    1, ctx.def.range * 0.7, 0.3, ctx.def.range * 0.7, 0.02);
        }
        return true;
    }

    private static boolean castSacredGround(CastContext ctx) {
        BlockPos center = ctx.caster.blockPosition();
        ctx.caster.getPersistentData().putLong("liberthia.sacred_ground_expires",
                ctx.level.getGameTime() + 400);
        ctx.caster.getPersistentData().putLong("liberthia.sacred_ground_pos",
                center.asLong());
        return true;
    }

    private static boolean castJudgment(CastContext ctx) {
        Vec3 target = ctx.pickHit(ctx.def.range);
        DamageSource ds = SchoolDamageSource.of(SpellSchool.HOLY).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(target, target).inflate(8))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
        }
        for (Player p : ctx.level.getEntitiesOfClass(Player.class,
                ctx.caster.getBoundingBox().inflate(12))) {
            if (p == ctx.caster) continue;
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
        }
        for (int y = 0; y < 40; y++) {
            ctx.level.sendParticles(ParticleTypes.END_ROD,
                    target.x, target.y + y, target.z, 5, 0.5, 0, 0.5, 0);
        }
        playCastSound(ctx, SoundEvents.BEACON_ACTIVATE, 1.0F);
        return true;
    }

    // ─── NATURE ───
    private static boolean castVineTangle(CastContext ctx) {
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        if (target == null) return false;
        target.hurt(SchoolDamageSource.of(SpellSchool.NATURE).toVanilla(ctx.level, ctx.caster),
                ctx.def.damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        // Visually wrap in vines
        BlockPos around = target.blockPosition();
        if (ctx.level.getBlockState(around).isAir()) {
            ctx.level.setBlock(around, Blocks.VINE.defaultBlockState(), 3);
        }
        ctx.level.sendParticles(ParticleTypes.COMPOSTER,
                target.getX(), target.getY() + 1, target.getZ(),
                15, 0.3, 0.5, 0.3, 0.05);
        return true;
    }

    private static boolean castEarthWall(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        Vec3 front = ctx.caster.position().add(look.scale(3));
        BlockPos center = new BlockPos((int) front.x, (int) front.y, (int) front.z);
        // 3x3 wall
        int placed = 0;
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        for (int dy = 0; dy < 3; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                BlockPos p = center.offset((int)(right.x * dx), dy, (int)(right.z * dx));
                if (ctx.level.getBlockState(p).isAir()) {
                    ctx.level.setBlock(p, Blocks.COBBLESTONE.defaultBlockState(), 3);
                    placed++;
                }
            }
        }
        return placed > 0;
    }

    private static boolean castStoneShard(CastContext ctx) {
        return projectile(ctx, SchoolDamageSource.of(SpellSchool.NATURE), 0F, false);
    }

    private static boolean castWispsHeal(CastContext ctx) {
        for (Player p : ctx.level.getEntitiesOfClass(Player.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            p.heal(ctx.def.damage * 0.5F);
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        }
        ctx.caster.heal(ctx.def.damage);
        for (int i = 0; i < 20; i++) {
            double a = (i / 20.0) * Math.PI * 2;
            ctx.level.sendParticles(ParticleTypes.GLOW,
                    ctx.caster.getX() + Math.cos(a) * 3,
                    ctx.caster.getY() + 1,
                    ctx.caster.getZ() + Math.sin(a) * 3,
                    2, 0.1, 0.3, 0.1, 0.02);
        }
        return true;
    }

    private static boolean castRoots(CastContext ctx) {
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        if (target == null) return false;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 250, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, 80, 250, true, true));
        // Place roots block under
        BlockPos under = target.blockPosition().below();
        if (ctx.level.getBlockState(under.above()).isAir()) {
            ctx.level.setBlock(under.above(), Blocks.JUNGLE_LEAVES.defaultBlockState(), 3);
        }
        ctx.level.sendParticles(ParticleTypes.COMPOSTER,
                target.getX(), target.getY(), target.getZ(),
                15, 0.3, 0.1, 0.3, 0.05);
        return true;
    }

    private static boolean castBrambleStorm(CastContext ctx) {
        DamageSource ds = SchoolDamageSource.of(SpellSchool.NATURE).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
            le.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
        }
        // r115: tempestade de espinhos sustained 60t — espinhos brotando do chão
        Vec3 brambleCenter = ctx.caster.position();
        br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.spawnGroundDecal(
                ctx.level, brambleCenter, ctx.def.school, ctx.def.range * 0.5, 60);
        br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.spawnAreaPulse(
                ctx.level, brambleCenter.add(0, 1, 0), ctx.def.school, ctx.def.range * 0.5, 60);
        return true;
    }

    // ─── EVOCATION ───
    private static boolean castMagicMissile(CastContext ctx) {
        // Auto-track to nearest target in cone
        Vec3 look = ctx.lookVec();
        LivingEntity bestTarget = null;
        double bestScore = -1;
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            Vec3 toE = le.position().subtract(ctx.caster.position()).normalize();
            double dot = toE.dot(look);
            if (dot > 0.7 && dot > bestScore) {
                bestScore = dot;
                bestTarget = le;
            }
        }
        if (bestTarget != null) {
            bestTarget.hurt(SchoolDamageSource.eldritch().toVanilla(ctx.level, ctx.caster),
                    ctx.def.damage);
            spawnChainParticles(ctx, ctx.handOrigin(),
                    bestTarget.position().add(0, 1, 0));
            return true;
        }
        return projectile(ctx, SchoolDamageSource.eldritch(), 0F, false);
    }

    private static boolean castBoneSpear(CastContext ctx) {
        return projectile(ctx, new SchoolDamageSource(SpellSchool.BLOOD, 0, 0, 0, true), 0F, false);
    }

    private static boolean castMagicShield(CastContext ctx) {
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 7));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 2));
        for (int i = 0; i < 30; i++) {
            ctx.level.sendParticles(ParticleTypes.ENCHANT,
                    ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                    1, 0.5, 0.5, 0.5, 0.1);
        }
        playCastSound(ctx, SoundEvents.BEACON_POWER_SELECT, 1.2F);
        return true;
    }

    private static boolean castSummonVex(CastContext ctx) {
        // r150 FIX: vex era hostil ao caster. Soluções:
        //   1. Marca caster UUID em NBT pro tick listener filtrar
        //   2. Limpa targetSelector goals (vex passivo se mexer com mobs)
        //   3. Persistent flag pra sobreviver
        for (int i = 0; i < 2; i++) {
            net.minecraft.world.entity.monster.Vex vex =
                    net.minecraft.world.entity.EntityType.VEX.create(ctx.level);
            if (vex != null) {
                vex.moveTo(ctx.caster.getX() + (i - 0.5), ctx.caster.getY() + 1, ctx.caster.getZ(),
                        ctx.caster.getYRot(), 0);
                vex.setLimitedLife(1200);
                vex.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0));
                // Marca UUID do caster no persistent data — VexTargetGuard usa isso pra impedir targeting
                vex.getPersistentData().putUUID("liberthia.summoner", ctx.caster.getUUID());
                vex.setTarget(null);
                // Limpa todas as target goals — vex não vai atacar ninguém,
                // só fica em volta do caster com o DAMAGE_BOOST (decorativo + intimidação)
                vex.targetSelector.removeAllGoals(g -> true);
                ctx.level.addFreshEntity(vex);
            }
        }
        for (int i = 0; i < 20; i++) {
            ctx.level.sendParticles(ParticleTypes.SMOKE,
                    ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                    1, 0.5, 0.3, 0.5, 0.05);
        }
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    // r119 MOBILITY — flight + dashes
    // ════════════════════════════════════════════════════════════════════════

    private static void registerMobility() {
        reg(SpellDef.builder("wings_of_source")
                .name("Asas da Fonte")
                .school(SpellSchool.HOLY).rarity(Rarity.EPIC)
                .mana(60).cooldown(400).damage(0F).range(0F)
                .lore("Voo livre por 30 segundos. Mana drena passivamente enquanto ativo.")
                .cast(SpellLibrary::castWingsOfSource).build());

        reg(SpellDef.builder("phase_dash")
                .name("Salto de Fase")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.RARE)
                .mana(25).cooldown(60).damage(0F).range(12F)
                .lore("Teleporta-se 12 blocos à frente atravessando paredes finas.")
                .cast(SpellLibrary::castPhaseDash).build());

        reg(SpellDef.builder("wind_step")
                .name("Passo do Vento")
                .school(SpellSchool.NATURE).rarity(Rarity.UNCOMMON)
                .mana(15).cooldown(40).damage(0F).range(8F)
                .lore("Avanço de 8 blocos com queda suave por 6 segundos.")
                .cast(SpellLibrary::castWindStep).build());

        reg(SpellDef.builder("levitate_self")
                .name("Levitar")
                .school(SpellSchool.HOLY).rarity(Rarity.UNCOMMON)
                .mana(20).cooldown(80).damage(0F).range(0F)
                .lore("Sobe verticalmente por 8 segundos. Útil pra escalar montanhas.")
                .cast(SpellLibrary::castLevitateSelf).build());

        reg(SpellDef.builder("blink")
                .name("Piscar")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.UNCOMMON)
                .mana(18).cooldown(30).damage(0F).range(8F)
                .lore("Teleporte curto na direção da mira.")
                .cast(SpellLibrary::castBlink).build());
    }

    private static boolean castWingsOfSource(CastContext ctx) {
        // Custom flight buff via NBT — drained per tick by FlightTicker (registered separately)
        ctx.caster.getPersistentData().putLong("liberthia.flight_until",
                ctx.level.getGameTime() + 600);
        ctx.caster.getAbilities().mayfly = true;
        ctx.caster.getAbilities().flying = true;
        ctx.caster.onUpdateAbilities();
        // VFX
        for (int i = 0; i < 60; i++) {
            double ang = i * (Math.PI * 2.0 / 60.0);
            double rad = 1.2;
            ctx.level.sendParticles(ParticleTypes.END_ROD,
                    ctx.caster.getX() + Math.cos(ang) * rad,
                    ctx.caster.getY() + 1,
                    ctx.caster.getZ() + Math.sin(ang) * rad,
                    1, 0, 0.2, 0, 0.05);
        }
        playCastSound(ctx, SoundEvents.ENDER_DRAGON_FLAP, 1.2F);
        return true;
    }

    private static boolean castPhaseDash(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        Vec3 dest = ctx.caster.position().add(look.scale(12.0));
        // VFX trail before teleport
        for (int i = 0; i <= 12; i++) {
            Vec3 step = ctx.caster.position().add(look.scale(i));
            ctx.level.sendParticles(ParticleTypes.PORTAL,
                    step.x, step.y + 1, step.z, 8, 0.3, 0.3, 0.3, 0.05);
        }
        ctx.caster.teleportTo(dest.x, dest.y, dest.z);
        playCastSound(ctx, SoundEvents.ENDERMAN_TELEPORT, 1.5F);
        ctx.caster.fallDistance = 0F;
        return true;
    }

    private static boolean castWindStep(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        ctx.caster.setDeltaMovement(look.scale(2.5).add(0, 0.3, 0));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
        ctx.caster.fallDistance = 0F;
        // VFX trail
        for (int i = 0; i < 30; i++) {
            ctx.level.sendParticles(ParticleTypes.CLOUD,
                    ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                    1, 0.3, 0.3, 0.3, 0.2);
        }
        playCastSound(ctx, SoundEvents.PHANTOM_FLAP, 1.5F);
        return true;
    }

    private static boolean castLevitateSelf(CastContext ctx) {
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 160, 2));
        for (int i = 0; i < 40; i++) {
            ctx.level.sendParticles(ParticleTypes.END_ROD,
                    ctx.caster.getX(), ctx.caster.getY() + 0.5, ctx.caster.getZ(),
                    1, 0.4, 0.5, 0.4, 0.05);
        }
        playCastSound(ctx, SoundEvents.SHULKER_TELEPORT, 1.2F);
        return true;
    }

    private static boolean castBlink(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        Vec3 dest = ctx.caster.position().add(look.scale(8.0));
        // Don't teleport into solid blocks
        BlockHitResult hit = ctx.level.clip(new ClipContext(
                ctx.caster.position().add(0, 1, 0),
                dest.add(0, 1, 0),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                ctx.caster));
        if (hit.getType() != HitResult.Type.MISS) {
            dest = hit.getLocation().subtract(look.scale(1.0));
        }
        ctx.caster.teleportTo(dest.x, dest.y, dest.z);
        ctx.caster.fallDistance = 0F;
        for (int i = 0; i < 20; i++) {
            ctx.level.sendParticles(ParticleTypes.PORTAL,
                    ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                    1, 0.3, 0.5, 0.3, 0.1);
        }
        playCastSound(ctx, SoundEvents.ENDERMAN_TELEPORT, 1.8F);
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    // r119 APOLÃO TIER — Supreme spells with 1000+ damage
    // ════════════════════════════════════════════════════════════════════════

    private static void registerApolao() {
        reg(SpellDef.builder("solar_apocalypse")
                .name("Apocalipse Solar")
                .school(SpellSchool.FIRE).rarity(Rarity.EPIC)
                .mana(300).cooldown(1200).damage(1500F).range(48F)
                .lore("APOLÃO. Invoca o sol em forma de meteoro. Devastação total.")
                .cast(SpellLibrary::castSolarApocalypse).build());

        reg(SpellDef.builder("eldritch_meteor")
                .name("Meteoro Eldritch")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.EPIC)
                .mana(280).cooldown(1000).damage(1200F).range(50F)
                .lore("APOLÃO. Convoca uma estrela negra do vazio que aniquila tudo.")
                .cast(SpellLibrary::castEldritchMeteor).build());

        reg(SpellDef.builder("divine_judgment_apolao")
                .name("Juízo Divino Final")
                .school(SpellSchool.HOLY).rarity(Rarity.EPIC)
                .mana(260).cooldown(1000).damage(1000F).range(40F)
                .lore("APOLÃO. Pilar de luz divina destrói alvos hereges.")
                .cast(SpellLibrary::castDivineJudgmentApolao).build());

        reg(SpellDef.builder("apocalypse")
                .name("APOCALIPSE")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.EPIC)
                .mana(400).cooldown(2400).damage(1800F).range(64F)
                .lore("APOLÃO SUPREMO. O fim de tudo. Tela treme. Mundo grita.")
                .cast(SpellLibrary::castApocalypse).build());
    }

    private static boolean castSolarApocalypse(CastContext ctx) {
        Vec3 origin = ctx.pickHit(48);
        DamageSource ds = SchoolDamageSource.fire(100).toVanilla(ctx.level, ctx.caster);
        // Massive AABB damage 12 block radius
        AABB box = new AABB(origin.x - 12, origin.y - 12, origin.z - 12,
                            origin.x + 12, origin.y + 12, origin.z + 12);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
            le.setSecondsOnFire(40);
        }
        // Explosion VFX + crater fire blocks
        for (int i = 0; i < 360; i += 5) {
            double ang = Math.toRadians(i);
            for (double r = 0; r < 12; r += 1) {
                ctx.level.sendParticles(ParticleTypes.FLAME,
                        origin.x + Math.cos(ang) * r,
                        origin.y + 0.5,
                        origin.z + Math.sin(ang) * r,
                        2, 0.2, 0.4, 0.2, 0.2);
            }
        }
        // Screen shake — 30 ticks, intensity 1.0
        broadcastScreenShake(ctx, 30, 1.0F);
        ctx.level.explode(ctx.caster, origin.x, origin.y, origin.z, 6F, false,
                net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        playCastSound(ctx, SoundEvents.GENERIC_EXPLODE, 0.5F);
        playCastSound(ctx, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5F);
        return true;
    }

    private static boolean castEldritchMeteor(CastContext ctx) {
        Vec3 origin = ctx.pickHit(50);
        DamageSource ds = SchoolDamageSource.eldritch().toVanilla(ctx.level, ctx.caster);
        AABB box = new AABB(origin.x - 10, origin.y - 10, origin.z - 10,
                            origin.x + 10, origin.y + 10, origin.z + 10);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
            le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 2));
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 3));
        }
        // Purple void VFX
        for (int i = 0; i < 200; i++) {
            ctx.level.sendParticles(ParticleTypes.PORTAL,
                    origin.x + (ctx.level.random.nextDouble() - 0.5) * 20,
                    origin.y + (ctx.level.random.nextDouble() - 0.5) * 10,
                    origin.z + (ctx.level.random.nextDouble() - 0.5) * 20,
                    1, 0.5, 0.5, 0.5, 0.3);
            ctx.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    origin.x + (ctx.level.random.nextDouble() - 0.5) * 10,
                    origin.y,
                    origin.z + (ctx.level.random.nextDouble() - 0.5) * 10,
                    1, 0.5, 0.2, 0.5, 0.1);
        }
        broadcastScreenShake(ctx, 35, 1.2F);
        playCastSound(ctx, SoundEvents.WITHER_SPAWN, 0.8F);
        return true;
    }

    private static boolean castDivineJudgmentApolao(CastContext ctx) {
        Vec3 origin = ctx.pickHit(40);
        DamageSource ds = SchoolDamageSource.of(SpellSchool.HOLY).toVanilla(ctx.level, ctx.caster);
        // Pillar of light — 32 blocks tall, 5 wide
        AABB box = new AABB(origin.x - 5, origin.y, origin.z - 5,
                            origin.x + 5, origin.y + 32, origin.z + 5);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
            le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        }
        // White pillar VFX
        for (int y = 0; y < 32; y++) {
            for (int i = 0; i < 6; i++) {
                double ang = i * Math.PI / 3 + (y * 0.2);
                ctx.level.sendParticles(ParticleTypes.END_ROD,
                        origin.x + Math.cos(ang) * 4,
                        origin.y + y,
                        origin.z + Math.sin(ang) * 4,
                        1, 0.1, 0.1, 0.1, 0.02);
            }
        }
        for (int i = 0; i < 50; i++) {
            ctx.level.sendParticles(ParticleTypes.GLOW,
                    origin.x + (ctx.level.random.nextDouble() - 0.5) * 8,
                    origin.y + ctx.level.random.nextDouble() * 30,
                    origin.z + (ctx.level.random.nextDouble() - 0.5) * 8,
                    1, 0, 0, 0, 0);
        }
        broadcastScreenShake(ctx, 25, 0.8F);
        playCastSound(ctx, SoundEvents.BEACON_ACTIVATE, 0.7F);
        ctx.caster.heal(20F); // self-heal as reward
        return true;
    }

    private static boolean castApocalypse(CastContext ctx) {
        Vec3 origin = ctx.pickHit(64);
        DamageSource ds = SchoolDamageSource.eldritch().toVanilla(ctx.level, ctx.caster);
        AABB box = new AABB(origin.x - 16, origin.y - 16, origin.z - 16,
                            origin.x + 16, origin.y + 16, origin.z + 16);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 5));
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 4));
            le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400, 0));
        }
        // Multi-stage VFX: rings of fire + soul flames + portal swirls + lightning
        for (int ring = 0; ring < 8; ring++) {
            double radius = ring * 2;
            for (int i = 0; i < 60; i++) {
                double ang = i * Math.PI / 30;
                ctx.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        origin.x + Math.cos(ang) * radius,
                        origin.y + 1,
                        origin.z + Math.sin(ang) * radius,
                        1, 0.1, 0.2, 0.1, 0.05);
                ctx.level.sendParticles(ParticleTypes.FLAME,
                        origin.x + Math.cos(ang) * radius,
                        origin.y + 2,
                        origin.z + Math.sin(ang) * radius,
                        1, 0.1, 0.3, 0.1, 0.1);
            }
        }
        for (int i = 0; i < 300; i++) {
            ctx.level.sendParticles(ParticleTypes.PORTAL,
                    origin.x + (ctx.level.random.nextDouble() - 0.5) * 30,
                    origin.y + (ctx.level.random.nextDouble() - 0.5) * 15,
                    origin.z + (ctx.level.random.nextDouble() - 0.5) * 30,
                    1, 0.5, 0.5, 0.5, 0.3);
        }
        // Multiple lightning strikes around the area
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4;
            double r = 8;
            LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(ctx.level);
            if (bolt != null) {
                bolt.moveTo(origin.x + Math.cos(ang) * r, origin.y, origin.z + Math.sin(ang) * r);
                bolt.setVisualOnly(true);
                ctx.level.addFreshEntity(bolt);
            }
        }
        broadcastScreenShake(ctx, 60, 1.8F);
        playCastSound(ctx, SoundEvents.WITHER_SPAWN, 1.0F);
        playCastSound(ctx, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0F);
        ctx.level.explode(ctx.caster, origin.x, origin.y, origin.z, 8F, false,
                net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        return true;
    }

    /** r119: broadcast screen shake to nearby players. */
    private static void broadcastScreenShake(CastContext ctx, int ticks, float intensity) {
        try {
            var pkt = new br.com.murilo.liberthia.magic.spell.ScreenShakeS2CPacket(intensity, ticks);
            var origin = ctx.caster.blockPosition();
            for (var sp : ctx.level.players()) {
                if (sp.distanceToSqr(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5) <= 64.0 * 64.0) {
                    br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp, pkt);
                }
            }
        } catch (Throwable t) {
            // soft fail — screen shake not critical
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // r120 VAZIO — feitiço supremo das ametistas, com infecção e mini buracos
    // ════════════════════════════════════════════════════════════════════════

    private static void registerVoid() {
        reg(SpellDef.builder("void")
                .name("VAZIO")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.EPIC)
                .mana(150).cooldown(200).damage(200F).range(40F)
                .lore("VAZIO. Convoca 3 mini buracos negros + aplica Infecção do Vazio.")
                .cast(SpellLibrary::castVoid).build());

        reg(SpellDef.builder("void_laser")
                .name("Vazio: Laser Canalizado")
                .school(SpellSchool.ELDRITCH).rarity(Rarity.EPIC)
                .mana(40).cooldown(2).damage(0F).range(28F)  // damage is dynamic via tracker
                .lore("Laser stackável. Cada click +250 dmg, cap 5000. Infecta no impacto.")
                .cast(SpellLibrary::castVoidLaser).build());
    }

    private static boolean castVoid(CastContext ctx) {
        Vec3 origin = ctx.pickHit(40);
        // Spawn 3 mini black holes em triângulo
        for (int i = 0; i < 3; i++) {
            double ang = i * Math.PI * 2 / 3.0;
            double offX = Math.cos(ang) * 2.5;
            double offZ = Math.sin(ang) * 2.5;
            try {
                var mbh = br.com.murilo.liberthia.registry.ModEntities.MINI_BLACK_HOLE.get()
                        .create(ctx.level);
                if (mbh != null) {
                    mbh.moveTo(origin.x + offX, origin.y + 1, origin.z + offZ, 0F, 0F);
                    mbh.setDamage(ctx.def.damage);
                    mbh.setCaster(ctx.caster);
                    ctx.level.addFreshEntity(mbh);
                }
            } catch (Throwable ignored) {}
        }

        // Aplica VoidInfection em alvos próximos
        AABB infectBox = new AABB(origin.x - 6, origin.y - 4, origin.z - 6,
                                  origin.x + 6, origin.y + 4, origin.z + 6);
        var voidEffect = br.com.murilo.liberthia.registry.ModMobEffects.VOID_INFECTION.get();
        if (voidEffect != null) {
            for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class, infectBox)) {
                if (le == ctx.caster) continue;
                le.addEffect(new MobEffectInstance(voidEffect, 200, 0));
            }
        }

        // VFX cast — onda roxa
        for (int i = 0; i < 80; i++) {
            double ang = ctx.level.random.nextDouble() * Math.PI * 2;
            double r = ctx.level.random.nextDouble() * 6;
            ctx.level.sendParticles(ParticleTypes.PORTAL,
                    origin.x + Math.cos(ang) * r,
                    origin.y + ctx.level.random.nextDouble() * 3,
                    origin.z + Math.sin(ang) * r,
                    1, 0.1, 0.2, 0.1, 0.2);
        }

        playCastSound(ctx, SoundEvents.WITHER_SPAWN, 0.6F);
        playCastSound(ctx, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F);
        return true;
    }

    private static boolean castVoidLaser(CastContext ctx) {
        // Stack damage
        int currentDmg = br.com.murilo.liberthia.magic.spell.voidspell.VoidLaserTracker
                .addStack(ctx.caster);

        // Picka alvo
        LivingEntity target = ctx.pickTarget(28);
        Vec3 from = ctx.handOrigin();
        Vec3 to;
        if (target != null) {
            to = target.position().add(0, target.getBbHeight() / 2, 0);
            // Damage
            DamageSource ds = SchoolDamageSource.eldritch().toVanilla(ctx.level, ctx.caster);
            target.hurt(ds, currentDmg);
            // Infection apply
            var voidEffect = br.com.murilo.liberthia.registry.ModMobEffects.VOID_INFECTION.get();
            if (voidEffect != null) {
                target.addEffect(new MobEffectInstance(voidEffect, 200, 0));
            }
            // Hit particles
            for (int i = 0; i < 12; i++) {
                ctx.level.sendParticles(ParticleTypes.PORTAL,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        1, 0.3, 0.3, 0.3, 0.15);
            }
        } else {
            to = from.add(ctx.lookVec().scale(28));
        }

        // VFX laser beam contínuo (denso pra parecer um raio)
        Vec3 dir = to.subtract(from);
        double dist = dir.length();
        int steps = (int) Math.min(60, dist * 2);
        if (steps > 0) {
            Vec3 stepV = dir.scale(1.0 / steps);
            for (int i = 0; i < steps; i++) {
                Vec3 p = from.add(stepV.scale(i));
                ctx.level.sendParticles(ParticleTypes.PORTAL,
                        p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
                // duplo: round particle para "core" do laser
                if (i % 3 == 0) {
                    ctx.level.sendParticles(
                            br.com.murilo.liberthia.registry.ModParticles.VOID_INFECTION.get(),
                            p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }

        // Feedback ao caster
        ctx.caster.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        String.format("§5VAZIO §7[§d%d§7/§85000§7]", currentDmg)), true);

        playCastSound(ctx, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F);
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private static void spawnChainParticles(CastContext ctx, Vec3 from, Vec3 to) {
        // r114: upgrade — usa SpellBeam (LineData) ao invés de dust dots.
        // LineData particles renderizam como linhas esticadas (lerp start→dest)
        // ao longo de seu lifetime, dando aparência de raio sustentado.
        br.com.murilo.liberthia.magic.spell.vfx.SpellBeam.drawSegment(
                ctx.level, from, to, ctx.def.school, 0.12F, 6);
    }

    private static Vector3f colorVec(SpellSchool school) {
        int hex = school.colorHex();
        return new Vector3f(
                ((hex >> 16) & 0xFF) / 255F,
                ((hex >> 8) & 0xFF) / 255F,
                (hex & 0xFF) / 255F);
    }

    private static void playCastSound(CastContext ctx,
                                       net.minecraft.sounds.SoundEvent event, float pitch) {
        ctx.level.playSound(null, ctx.caster.blockPosition(), event,
                SoundSource.PLAYERS, 1.0F, pitch);
    }
}
