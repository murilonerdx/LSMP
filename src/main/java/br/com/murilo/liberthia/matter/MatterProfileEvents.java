package br.com.murilo.liberthia.matter;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.MatterProfileSyncS2CPacket;
import br.com.murilo.liberthia.registry.ModMobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hub central do sistema de matéria — anexa capability, ticka decay/sync,
 * aplica os 5 MobEffects conforme o perfil ativo.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MatterProfileEvents {

    public static final ResourceLocation CAP_KEY =
            new ResourceLocation(LiberthiaMod.MODID, "matter_profile");

    // DECAY DESATIVADO — matter deve ser PERMANENTE até alguém setar via API
    // ou item. Antes decaia 1 ponto/minuto e os players reclamavam que as
    // estatísticas sumiam sozinhas. Set DECAY_PERIOD pra 0 (ou simplesmente
    // não chamar decayAll). Pra reabilitar no futuro, descomentar o bloco.
    /** Período de sync mesmo sem mudança — protege contra perda de pacotes. */
    private static final int SYNC_PERIOD = 100; // 5s

    /**
     * Lembra qual era o último profile type por player — pra detectar TRANSIÇÃO
     * (quando o type muda, toca som privado pro player; sem isso, som tocaria
     * a cada tick que checa).
     */
    private static final java.util.Map<java.util.UUID, MatterProfileType> LAST_TYPE_PER_PLAYER =
            new java.util.concurrent.ConcurrentHashMap<>();

    private MatterProfileEvents() {}

    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(CAP_KEY, new MatterProfileProvider());
        }
    }

    /** Quando jogador respawna, copia valores antigos. */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(MatterProfileProvider.CAP).ifPresent(oldProfile -> {
            event.getEntity().getCapability(MatterProfileProvider.CAP).ifPresent(newProfile -> {
                newProfile.copyFrom(oldProfile);
            });
        });
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncTo(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        sp.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            // Decay passivo REMOVIDO — matter agora é permanente. Era 1pt/min.
            // Sync periódico (mantém o cliente atualizado mesmo sem mudança)
            if (sp.tickCount % SYNC_PERIOD == 0) {
                syncTo(sp);
            }
            // Aplica efeitos do perfil ativo
            applyProfileEffects(sp, profile);
            // v0.1.13: efeitos por PROXIMIDADE de blocos de matéria (independente do perfil ativo)
            applyProximityMatterEffects(sp, profile);
        });
    }

    /**
     * Ganho de matter por tick de proximity check (a cada 2s) por bloco
     * detectado dentro do raio. Valores são MULTIPLICADOS pelo número de
     * blocos daquele tipo presentes — então 10 ores DM próximos = 10 × 0.3 = 3
     * pontos de DM a cada 2s. Profile fica saturado (100) em ~70s nesse
     * cenário extremo, ou em ~5min com 1 ore só.
     *
     * <p><b>Filosofia dos valores:</b> blocos "ativos / esporulantes" (spore
     * bloom, crescimento, dust) infectam mais rápido. Blocos "passivos /
     * sólidos" (ore, soil) infectam devagar. Pure matter block é o pior.
     *
     * <p>Não é definido em campos estáticos {@code Map<Block, Float>} porque
     * os blocos do mod são {@link net.minecraftforge.registries.RegistryObject},
     * e referenciá-los em inicializador estático ANTES do mod registrar
     * tudo dispara NPE. Usa branches if-instanceof in-place no scanner.
     */
    private static final float GAIN_DARK_MATTER_BLOCK    = 0.50f; // bloco puro DM
    private static final float GAIN_CORRUPTED_SOIL        = 0.25f; // soil contaminada
    private static final float GAIN_DARK_MATTER_ORE       = 0.30f; // minério DM
    private static final float GAIN_DEEPSLATE_DM_ORE      = 0.35f; // deepslate = mais denso
    private static final float GAIN_INFECTION_GROWTH      = 0.40f; // crescimento ativo
    private static final float GAIN_SPORE_BLOOM           = 0.60f; // espalha esporos = pior
    private static final float GAIN_CRYSTALLIZER          = 0.45f; // máquina processando DM
    private static final float GAIN_DM_INFECTED_VARIANT   = 0.20f; // variante (dirt/sand/stone/grass infected)

    private static final float GAIN_CLEAR_MATTER_BLOCK    = 0.50f;
    private static final float GAIN_WHITE_MATTER_ORE      = 0.30f;
    private static final float GAIN_WM_INFECTED_VARIANT   = 0.20f;

    private static final float GAIN_YELLOW_MATTER_BLOCK   = 0.50f;
    private static final float GAIN_YM_INFECTED_VARIANT   = 0.20f;

    /** Cap total de matter ganho por proximity check (2s) — protege contra
     * cenários patológicos onde player tá cercado de 50 ores e ganharia 15
     * pontos por tick. Limitado a 5pts/check = ~40s pra encher de 0 a 100. */
    private static final float MAX_GAIN_PER_CHECK = 5.0f;

    /**
     * Ganho por fluid block contado no scanner — fluidos no chão (poça de DM
     * deixada por um bucket vazado) infectam mais que blocos sólidos porque
     * são "ativos" (escorrem, esporulam).
     */
    private static final float GAIN_DM_FLUID_NEARBY     = 0.45f;
    private static final float GAIN_WM_FLUID_NEARBY     = 0.45f;
    private static final float GAIN_YM_FLUID_NEARBY     = 0.45f;

    /**
     * Ganho quando o player está SUBMERSO no fluido (em pé dentro da poça
     * com a feet/eye dentro do fluid type). Contagiar INSTANTANEAMENTE é o
     * realismo esperado: você não fica nadando em matter sem absorver. Não
     * tem cap interno aqui — toma diretamente 2.0 pontos/check de cada tipo
     * imerso, somando aos ganhos de proximity dos blocos sólidos em volta.
     */
    private static final float GAIN_DM_SUBMERGED        = 2.0f;
    private static final float GAIN_WM_SUBMERGED        = 2.0f;
    private static final float GAIN_YM_SUBMERGED        = 2.0f;

    /**
     * v0.1.13: Player perto de blocos de matéria recebe efeitos que ESCALAM
     * com o número de blocos no raio de 5. Quanto mais blocos, mais forte.
     *
     * <p>v0.1.14: ADICIONALMENTE acumula matter NO PROFILE — antes só dava
     * MobEffects temporários (Wither, Slowness, etc) mas o profile.dark do
     * player não subia. Agora cada bloco contribui um valor (definido em
     * constantes GAIN_*) e o profile.dark/white/yellow vai subindo até saturar
     * em 100. Diferentes blocos contribuem em rates diferentes — ore < block,
     * passive < spore_bloom, etc.
     *
     * <p>Roda a cada 40 ticks (2s) — escaneia 11×11×11 = 1331 blocos em volta.
     * É barato porque BlockPos.betweenClosed é lazy + interrompemos cedo.
     *
     * <p>Escala dos MobEffects (por tipo):
     * <ul>
     *   <li>1-4 blocos próximos: tier I — efeito leve, 60 ticks</li>
     *   <li>5-9 blocos próximos: tier II — efeito médio, 80 ticks</li>
     *   <li>10+ blocos próximos: tier III — efeito forte, 120 ticks + amp+1</li>
     * </ul>
     */
    private static void applyProximityMatterEffects(ServerPlayer sp, MatterProfile profile) {
        if (sp.tickCount % 40 != 0) return;

        // v0.1.13: White Matter Pendant suprime TODOS efeitos de proximidade.
        // v0.1.14 nota: o pendant ainda suprime os MobEffects, mas matter ainda
        // ACUMULA no profile — o pendant protege contra dano agudo, não contra
        // a infecção em si. Isso mantém a tensão: pendant te salva no curto
        // prazo mas seu perfil ainda evolui se você fica horas perto dos blocos.
        boolean pendantActive = br.com.murilo.liberthia.compat.CuriosCompat.isPendantActive(sp);
        if (pendantActive) {
            // Drena 1 ponto de durabilidade a cada tick de proximity check (2s).
            // Pendant de 7200 durab dura 7200 * 2s = 4h reais de exposição.
            br.com.murilo.liberthia.compat.CuriosCompat.damagePendant(sp, 1);
        }

        var level = sp.serverLevel();
        var center = sp.blockPosition();
        int radius = 5;

        int darkCount = 0, whiteCount = 0, yellowCount = 0;
        float darkGain = 0, whiteGain = 0, yellowGain = 0;
        // Itera AABB de blocos em volta
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(
                center.offset(-radius, -2, -radius),
                center.offset(radius, 2, radius))) {
            var state = level.getBlockState(pos);
            if (state.isAir()) continue;
            var b = state.getBlock();
            // Variantes infectadas (já têm type embutido)
            if (b instanceof br.com.murilo.liberthia.block.InfectedVariantBlock infected) {
                switch (infected.getType()) {
                    case DARK   -> { darkCount++;   darkGain   += GAIN_DM_INFECTED_VARIANT; }
                    case WHITE  -> { whiteCount++;  whiteGain  += GAIN_WM_INFECTED_VARIANT; }
                    case YELLOW -> { yellowCount++; yellowGain += GAIN_YM_INFECTED_VARIANT; }
                }
                continue;
            }
            // Blocos DM (ordem do mais comum/leve pro mais pesado pra hit rate)
            if (state.is(br.com.murilo.liberthia.registry.ModBlocks.CORRUPTED_SOIL.get())) {
                darkCount++; darkGain += GAIN_CORRUPTED_SOIL;
            } else if (state.is(br.com.murilo.liberthia.registry.ModBlocks.DARK_MATTER_ORE.get())) {
                darkCount++; darkGain += GAIN_DARK_MATTER_ORE;
            } else if (state.is(br.com.murilo.liberthia.registry.ModBlocks.DEEPSLATE_DARK_MATTER_ORE.get())) {
                darkCount++; darkGain += GAIN_DEEPSLATE_DM_ORE;
            } else if (state.is(br.com.murilo.liberthia.registry.ModBlocks.INFECTION_GROWTH.get())) {
                darkCount++; darkGain += GAIN_INFECTION_GROWTH;
            } else if (state.is(br.com.murilo.liberthia.registry.ModBlocks.SPORE_BLOOM.get())) {
                darkCount++; darkGain += GAIN_SPORE_BLOOM;
            } else if (state.is(br.com.murilo.liberthia.registry.ModBlocks.CRYSTALLIZER.get())) {
                darkCount++; darkGain += GAIN_CRYSTALLIZER;
            } else if (state.is(br.com.murilo.liberthia.registry.ModBlocks.DARK_MATTER_BLOCK.get())) {
                darkCount++; darkGain += GAIN_DARK_MATTER_BLOCK;
            }
            // Fluid blocks DM/WM/YM (poças no chão) — contam como proximity
            // porque a poça é uma fonte ativa irradiando matter.
            else if (state.is(br.com.murilo.liberthia.registry.ModBlocks.DARK_MATTER_FLUID_BLOCK.get())) {
                darkCount++; darkGain += GAIN_DM_FLUID_NEARBY;
            } else if (state.is(br.com.murilo.liberthia.registry.ModBlocks.CLEAR_MATTER_FLUID_BLOCK.get())) {
                whiteCount++; whiteGain += GAIN_WM_FLUID_NEARBY;
            } else if (state.is(br.com.murilo.liberthia.registry.ModBlocks.YELLOW_MATTER_FLUID_BLOCK.get())) {
                yellowCount++; yellowGain += GAIN_YM_FLUID_NEARBY;
            }
            // Blocos WM
            else if (state.is(br.com.murilo.liberthia.registry.ModBlocks.WHITE_MATTER_ORE.get())) {
                whiteCount++; whiteGain += GAIN_WHITE_MATTER_ORE;
            } else if (state.is(br.com.murilo.liberthia.registry.ModBlocks.CLEAR_MATTER_BLOCK.get())) {
                whiteCount++; whiteGain += GAIN_CLEAR_MATTER_BLOCK;
            }
            // Blocos YM
            else if (state.is(br.com.murilo.liberthia.registry.ModBlocks.YELLOW_MATTER_BLOCK.get())) {
                yellowCount++; yellowGain += GAIN_YELLOW_MATTER_BLOCK;
            }
        }

        // ─── Player SUBMERSO em fluido de matter ───
        // Boost forte de matter — você não "passa por" um fluido sem absorver.
        // isInFluidType chega a true se qualquer parte da hitbox tá no fluido,
        // então pisar de leve numa poça já conta. Isso resolve o report de bug
        // "entrei no balde de dark matter e nada aconteceu".
        if (sp.isInFluidType(br.com.murilo.liberthia.registry.ModFluids.DARK_MATTER_TYPE.get())) {
            darkGain += GAIN_DM_SUBMERGED;
        }
        if (sp.isInFluidType(br.com.murilo.liberthia.registry.ModFluids.CLEAR_MATTER_TYPE.get())) {
            whiteGain += GAIN_WM_SUBMERGED;
        }
        if (sp.isInFluidType(br.com.murilo.liberthia.registry.ModFluids.YELLOW_MATTER_TYPE.get())) {
            yellowGain += GAIN_YM_SUBMERGED;
        }

        // v0.1.16 bug fix: CLEAR_SHIELD bloqueia o ganho por proximity também.
        // Aplica nas 3 pílulas por 60s. Player toma pill → -25 daquela matter +
        // 60s de imunidade pra não re-ganhar no próximo tick de proximity.
        boolean shielded = sp.hasEffect(br.com.murilo.liberthia.registry.ModEffects.CLEAR_SHIELD.get());

        // v0.1.30: Refined Containment Pendant também BLOQUEIA totalmente o
        // ganho ambient (proximidade + fluidos + radiação). Trata-se como
        // shielded — pendant ativo => imune ao ganho passivo. Drena durab
        // proporcional à exposição (1 ponto por check com gain > 0).
        boolean refinedActive =
                br.com.murilo.liberthia.compat.CuriosCompat.isRefinedPendantActive(sp);
        if (refinedActive) {
            float totalGain = darkGain + whiteGain + yellowGain;
            if (totalGain > 0) {
                br.com.murilo.liberthia.compat.CuriosCompat.damageRefinedPendant(sp, 1);
            }
            // Trata como shielded pra pular o ganho abaixo + pular o dano por saturação
            shielded = true;
        }

        // v0.1.16 bug fix: armadura Clear Matter agora REDUZ ganho de matter.
        // Bug reportado: "Armadura de Clear Matter n proteje contra as materias".
        // 4 peças vestidas = 100% redução (imune). 1 peça = 25%. Calcula uma
        // vez por tick pra aplicar em DM/WM/YM uniformemente. Não interage com
        // shield — ambos podem cumular (shield bloqueia totalmente).
        int clearArmorPieces = countClearMatterArmor(sp);
        float armorMultiplier = 1.0f - (clearArmorPieces * 0.25f); // 0/4=1.0, 1/4=0.75, ..., 4/4=0.0

        // v1 — Specific matter pendants ZERO the gain per type and damage
        // the pendant proportional to the GAIN (ambient matter density). Quanto
        // mais bloco/fluido daquele tipo no raio, mais rápido o pendant quebra.
        // Antes a fórmula usava profile.getDark()/10 (matter ATUAL do player)
        // mas isso era 0 se o pendant tava prevenindo o ganho desde o início —
        // o pendant nunca quebrava. Agora baseia em `gain * 2`: 1 ponto de gain
        // = 2 durab consumida, ceil pra garantir wear mínimo.
        boolean darkPendant = br.com.murilo.liberthia.compat.CuriosCompat.isDarkPendantActive(sp);
        boolean clearPendant = br.com.murilo.liberthia.compat.CuriosCompat.isClearPendantActive(sp);
        boolean yellowPendantSpecific = br.com.murilo.liberthia.compat.CuriosCompat.isYellowPendantActive(sp);

        if (darkPendant) {
            if (darkGain > 0) {
                int wear = Math.max(1, (int) Math.ceil(darkGain * 2f));
                br.com.murilo.liberthia.compat.CuriosCompat.damageDarkMatterPendant(sp, wear);
            }
            darkGain = 0;
        }
        if (clearPendant) {
            if (whiteGain > 0) {
                int wear = Math.max(1, (int) Math.ceil(whiteGain * 2f));
                br.com.murilo.liberthia.compat.CuriosCompat.damageClearMatterPendant(sp, wear);
            }
            whiteGain = 0;
        }
        if (yellowPendantSpecific) {
            if (yellowGain > 0) {
                int wear = Math.max(1, (int) Math.ceil(yellowGain * 2f));
                br.com.murilo.liberthia.compat.CuriosCompat.damageYellowMatterPendant(sp, wear);
            }
            yellowGain = 0;
        }

        // Aplica matter gain no profile. Cap por tipo protege contra
        // saturação instantânea em cenários patológicos (player cercado).
        // Profile faz clamp(0..100) internamente, então passar do max é no-op.
        //
        // v0.1.51: cada tipo é zerado se o player tem a resistance correspondente
        // (tomou a pílula). User pediu: "pílula impede que [a infecção] aumente".
        boolean changed = false;
        boolean dmResist = MatterResistance.blocked(sp, MatterResistance.Type.DARK);
        boolean cmResist = MatterResistance.blocked(sp, MatterResistance.Type.CLEAR);
        boolean ymResist = MatterResistance.blocked(sp, MatterResistance.Type.YELLOW);
        if (!shielded && !dmResist && darkGain > 0) {
            float effective = Math.min(darkGain, MAX_GAIN_PER_CHECK) * armorMultiplier;
            if (effective > 0) { profile.addDark(effective); changed = true; }
        }
        if (!shielded && !cmResist && whiteGain > 0) {
            float effective = Math.min(whiteGain, MAX_GAIN_PER_CHECK) * armorMultiplier;
            if (effective > 0) { profile.addWhite(effective); changed = true; }
        }
        if (!shielded && !ymResist && yellowGain > 0) {
            float effective = Math.min(yellowGain, MAX_GAIN_PER_CHECK) * armorMultiplier;
            if (effective > 0) { profile.addYellow(effective); changed = true; }
        }
        // Re-sync pro cliente se algo mudou — sem isso o HUD do player não
        // reflete o novo valor até o próximo sync periódico (5s).
        if (changed) syncTo(sp);

        // v0.1.16 bug fix: dano por saturação. Quando qualquer matter ≥ 95,
        // aplica 1 ponto de dano (meio coração) por proximity check (2s).
        // Bug reportado: "devia levar dano ou até morrer quando chegar ao
        // maximo da [matter] e n aconteceu nada". DamageSource genérico GENERIC
        // pra não dar drop loot, só death message neutro.
        if (!pendantActive && !shielded) {
            float maxMatter = Math.max(profile.getDark(),
                    Math.max(profile.getWhite(), profile.getYellow()));
            if (maxMatter >= 95f) {
                sp.hurt(sp.damageSources().magic(), 1.0F);
            }
        }

        // Aplica efeitos escalados — só se pendant NÃO tá ativo (pendant suprime
        // os MobEffects mas não o ganho de matter, como documentado acima).
        if (!pendantActive) {
            applyTierEffect(sp, darkCount, net.minecraft.world.effect.MobEffects.WITHER, net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            applyTierEffect(sp, whiteCount, net.minecraft.world.effect.MobEffects.WEAKNESS, net.minecraft.world.effect.MobEffects.GLOWING);
            applyTierEffect(sp, yellowCount, net.minecraft.world.effect.MobEffects.CONFUSION, net.minecraft.world.effect.MobEffects.HUNGER);
        }
    }

    private static void applyTierEffect(ServerPlayer sp, int blockCount,
                                         net.minecraft.world.effect.MobEffect mild,
                                         net.minecraft.world.effect.MobEffect severe) {
        if (blockCount <= 0) return;
        int amplifier;
        int duration;
        if (blockCount >= 10) {
            amplifier = 1; duration = 120;
        } else if (blockCount >= 5) {
            amplifier = 0; duration = 80;
        } else {
            amplifier = 0; duration = 60;
        }
        // Tier alto também adiciona o efeito severo
        sp.addEffect(new MobEffectInstance(mild, duration, amplifier, true, false, false));
        if (blockCount >= 10) {
            sp.addEffect(new MobEffectInstance(severe, duration / 2, 0, true, false, false));
        }
    }

    /** Aplica/refresca os MobEffects do perfil ativo a cada tick. */
    private static void applyProfileEffects(ServerPlayer sp, MatterProfile profile) {
        // v0.1.16 bug fix: WHITE Esquecido — perda aleatória de XP. Bug
        // reportado: "passei 10min infectado e n perdi XP como diz no livro".
        // Triggers a cada ~10s (200 ticks) com chance de 50% — efeito
        // probabilístico evita drain constante, mantém a sensação de
        // "esquecimento aleatório". Tira 1 ponto de XP por trigger (não nível
        // inteiro pra não punir demais; jogador perde ~3 XP/min em média).
        // Só roda quando perfil ativo é WHITE (Esquecido), sem pendant.
        var activeType = profile.getActiveType();
        if (activeType == MatterProfileType.WHITE && sp.tickCount % 200 == 0
                && !br.com.murilo.liberthia.compat.CuriosCompat.isPendantActive(sp)) {
            if (sp.getRandom().nextFloat() < 0.5f && sp.totalExperience > 0) {
                // giveExperiencePoints com valor negativo subtrai
                sp.giveExperiencePoints(-1);
            }
        }

        // Remove todos os efeitos de perfil antes de aplicar o atual.
        // Só re-aplicamos a cada 40 ticks pra não floodar pacotes de status.
        if (sp.tickCount % 40 != 0) return;

        var type = profile.getActiveType();
        // Limpa os outros (não interfere em efeitos vanilla/poções)
        for (var t : MatterProfileType.values()) {
            if (t == type) continue;
            var eff = effectFor(t);
            if (eff != null && sp.hasEffect(eff)) sp.removeEffect(eff);
        }
        var current = effectFor(type);
        if (current != null) {
            int amplifier = computeAmplifier(profile, type);
            // 100 ticks (5s) — refresca a cada 40t pra ficar permanente enquanto perfil está ativo
            sp.addEffect(new MobEffectInstance(current, 100, amplifier, true, false, true));
        }

        // Som de transição PRIVADO — só o player afetado escuta. Toca apenas
        // quando o type MUDA (não a cada tick). Usa playNotifySound que envia
        // ClientboundSoundPacket SÓ pro player; outros players próximos não escutam.
        MatterProfileType previousType = LAST_TYPE_PER_PLAYER.put(sp.getUUID(), type);
        if (previousType != type) {
            var sound = soundFor(type);
            if (sound != null) {
                sp.playNotifySound(sound, net.minecraft.sounds.SoundSource.AMBIENT, 0.8f, pitchFor(type));
            }
        }
    }

    /** Sons distintos por tipo de matter — atmosfera única pra cada perfil. */
    private static @org.jetbrains.annotations.Nullable net.minecraft.sounds.SoundEvent soundFor(MatterProfileType t) {
        return switch (t) {
            // DARK — agressivo, sombrio. Som de wither aproximando.
            case DARK         -> net.minecraft.sounds.SoundEvents.WITHER_AMBIENT;
            // DARK_WHITE — contido, mas sinistro. Portal end ambient.
            case DARK_WHITE   -> net.minecraft.sounds.SoundEvents.PORTAL_AMBIENT;
            // WHITE — esquecimento, calmaria. Beacon ativando.
            case WHITE        -> net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE;
            // YELLOW — caos emocional. Guardian attack — agitado.
            case YELLOW       -> net.minecraft.sounds.SoundEvents.GUARDIAN_AMBIENT;
            // YELLOW_WHITE — foco frio. Sino — claridade súbita.
            case YELLOW_WHITE -> net.minecraft.sounds.SoundEvents.BELL_RESONATE;
            default           -> null;
        };
    }
    /** Pitch por tipo — leve variação pra cada matter ter "personalidade". */
    private static float pitchFor(MatterProfileType t) {
        return switch (t) {
            case DARK         -> 0.7f;  // grave
            case DARK_WHITE   -> 0.9f;
            case WHITE        -> 1.0f;
            case YELLOW       -> 1.4f;  // agudo
            case YELLOW_WHITE -> 1.2f;
            default           -> 1.0f;
        };
    }

    /** Limpa cache quando player desloga (evita memory leak + permite re-tocar som ao reconectar). */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_TYPE_PER_PLAYER.remove(event.getEntity().getUUID());
    }

    private static @org.jetbrains.annotations.Nullable net.minecraft.world.effect.MobEffect effectFor(MatterProfileType t) {
        return switch (t) {
            case DARK         -> ModMobEffects.AGGRESSION.get();
            case DARK_WHITE   -> ModMobEffects.CONTAINED.get();
            case WHITE        -> ModMobEffects.FORGETFULNESS.get();
            case YELLOW       -> ModMobEffects.EMOTIONAL_CHAOS.get();
            case YELLOW_WHITE -> ModMobEffects.COLD_FOCUS.get();
            default           -> null;
        };
    }

    /**
     * Amplifier (potência) do efeito, baseado em quão alto está o valor relevante.
     * 0 → I, 1 → II, 2 → III.
     */
    private static int computeAmplifier(MatterProfile p, MatterProfileType t) {
        float v = switch (t) {
            case DARK         -> p.getDark();
            case WHITE        -> p.getWhite();
            case YELLOW       -> p.getYellow();
            case DARK_WHITE   -> Math.min(p.getDark(), p.getWhite());
            case YELLOW_WHITE -> Math.min(p.getYellow(), p.getWhite());
            default           -> 0;
        };
        if (v >= 75) return 2;
        if (v >= 50) return 1;
        return 0;
    }

    /** Empacota e envia pra o cliente. */
    public static void syncTo(ServerPlayer sp) {
        sp.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            ModNetwork.sendToPlayer(sp,
                    new MatterProfileSyncS2CPacket(profile.getDark(), profile.getWhite(), profile.getYellow()));
        });
    }

    /**
     * Conta quantas peças de Clear Matter armor (helmet/chestplate/leggings/boots)
     * o player tá vestindo nos slots de armor.
     *
     * <p>Usado pra reduzir ganho de matter por proximity em 25% por peça —
     * 4 peças = imunidade total. Bug fix v0.1.16 (Armadura Clear Matter não
     * protegia contra matter por proximity).
     */
    private static int countClearMatterArmor(ServerPlayer sp) {
        int count = 0;
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET}) {
            var stack = sp.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            var item = stack.getItem();
            if (item == br.com.murilo.liberthia.registry.ModItems.CLEAR_MATTER_HELMET.get()
                    || item == br.com.murilo.liberthia.registry.ModItems.CLEAR_MATTER_CHESTPLATE.get()
                    || item == br.com.murilo.liberthia.registry.ModItems.CLEAR_MATTER_LEGGINGS.get()
                    || item == br.com.murilo.liberthia.registry.ModItems.CLEAR_MATTER_BOOTS.get()) {
                count++;
            }
        }
        return count;
    }
}
