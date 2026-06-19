package br.com.murilo.liberthia.cosmic.eldritch;

import br.com.murilo.liberthia.cosmic.CosmicHorrorManager;
import br.com.murilo.liberthia.cosmic.CosmicHorrorPhase;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r46: <b>Eldritch Artifacts</b> — 10 itens cosmic horror com
 * mecânicas reais de horror psicológico, hallucinations, corrupção,
 * manipulação de realidade.
 *
 * <p>Cada item:
 * <ul>
 *   <li>NÃO apenas dá dano</li>
 *   <li>Manipula percepção do player ou de vítimas</li>
 *   <li>Sincroniza com o {@link HallucinationManager}</li>
 *   <li>Consome/dá sanity via {@link InsanityData}</li>
 * </ul>
 */
public final class EldritchArtifacts {

    private EldritchArtifacts() {}

    // ════════════════════════════════════════════════════════════
    // 1. WATCHING EYE — olho que rastreia e sussurra
    // ════════════════════════════════════════════════════════════
    public static class WatchingEyeItem extends Item {
        public WatchingEyeItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean sel) {
            if (level.isClientSide) return;
            if (!(entity instanceof ServerPlayer sp)) return;
            ServerLevel sl = (ServerLevel) level;
            long now = level.getGameTime();
            // A cada 60 ticks (3s) — passive horror
            if (now % 60 != 0) return;

            // Spawn eye particle orbitando o player
            double angle = (now * 0.05) % (Math.PI * 2);
            sl.sendParticles(ModParticles.GLARING_EYE_PULSE.get(),
                    sp.getX() + Math.cos(angle) * 1.2,
                    sp.getY() + 1.6,
                    sp.getZ() + Math.sin(angle) * 1.2,
                    1, 0, 0, 0, 0);

            // A cada 200t (10s) — whispers + footstep
            if (now % 200 == 0) {
                HallucinationManager.force(sp, HallucinationType.FAKE_WHISPER, 0.6F, 30, "");
                if (Math.random() < 0.4) {
                    HallucinationManager.force(sp, HallucinationType.FAKE_FOOTSTEP, 0.7F, 20, "");
                }
                // Reveal invisible entities nearby (4b)
                for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class,
                        sp.getBoundingBox().inflate(4))) {
                    if (le.isInvisible()) {
                        le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, true));
                    }
                }
            }

            // A cada 600t (30s) — chat sussurro
            if (now % 600 == 0 && Math.random() < 0.4) {
                String[] msgs = {
                        "§7§o*o olho pisca enquanto você não olha*",
                        "§7§o*algo te observa de volta*",
                        "§7§o*você ouve um sussurro: \"" + sp.getName().getString() + "\"*",
                        "§4§o*pupila se contrai*"
                };
                sp.displayClientMessage(Component.literal(msgs[(int)(Math.random() * msgs.length)]), true);
                InsanityData.addParanoia(sp, 1);
            }
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§4§o§lO Olho que Observa").withStyle(ChatFormatting.DARK_RED));
            t.add(Component.literal("§7§oUm olho vivo aprisionado em metal preto."));
            t.add(Component.empty());
            t.add(Component.literal("§7Passivo: sussurros, fake footsteps, hallucinations"));
            t.add(Component.literal("§7Revela entidades invisíveis num raio §e4b"));
            t.add(Component.literal("§7+paranoia ao longo do tempo"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Você não o segura. Ele segura você.\""));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 2. BLACK SIGNAL RADIO — fake admin broadcasts
    // ════════════════════════════════════════════════════════════
    public static class BlackSignalRadioItem extends Item {
        public BlackSignalRadioItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            // Toggle broadcast mode
            boolean active = !stack.getOrCreateTag().getBoolean("Broadcasting");
            stack.getOrCreateTag().putBoolean("Broadcasting", active);
            stack.getOrCreateTag().putLong("BroadcastStart", level.getGameTime());

            sp.displayClientMessage(Component.literal(active
                    ? "§4§l✦ §r§4O rádio sintoniza outra dimensão..."
                    : "§7O rádio cai em silêncio."), true);

            level.playSound(null, sp.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.get(),
                    SoundSource.PLAYERS, 1.5F, 0.5F);
            sp.getCooldowns().addCooldown(this, 200);
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean sel) {
            if (level.isClientSide) return;
            if (!(entity instanceof ServerPlayer sp)) return;
            if (!stack.getOrCreateTag().getBoolean("Broadcasting")) return;
            long now = level.getGameTime();
            long start = stack.getOrCreateTag().getLong("BroadcastStart");
            // Stop after 30s
            if (now - start > 600) {
                stack.getOrCreateTag().putBoolean("Broadcasting", false);
                return;
            }
            // Every 60t (3s) send fake broadcast to nearby players
            if (now % 60 != 0) return;
            String[] fakeBroadcasts = {
                    "§7[§cAdmin§7] " + sp.getName().getString() + " banned for cheating",
                    "§7[§cSistema§7] §cConnection unstable... reconnecting",
                    "§7[§cServer§7] §7Saving world...",
                    "§7[§4??§7] §4we hear you breathing",
                    "§7[§dRádio§7] §o...stay where you are...",
                    "§7[§4Sistema§7] §cChunk corruption detected at " + sp.getX() + ", " + sp.getZ(),
                    "§7[§dRádio§7] §o*static* ...come back...",
                    "§7[§4??§7] §4they are inside",
                    "§7[§7Sistema§7] §7" + sp.getName().getString() + " has joined the game"
            };
            String msg = fakeBroadcasts[(int)(Math.random() * fakeBroadcasts.length)];
            // Send to nearby players (16b)
            for (ServerPlayer near : ((ServerLevel) level).getPlayers(
                    p -> p.distanceToSqr(sp) < 16 * 16)) {
                HallucinationManager.force(near, HallucinationType.FAKE_CHAT_MESSAGE,
                        1.0F, 1, msg);
            }
            // Sometimes play distorted audio
            if (Math.random() < 0.4) {
                for (ServerPlayer near : ((ServerLevel) level).getPlayers(
                        p -> p.distanceToSqr(sp) < 16 * 16)) {
                    HallucinationManager.force(near, HallucinationType.REVERSE_AUDIO_PULSE,
                            0.7F, 30, "");
                }
            }
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            boolean active = s.getOrCreateTag().getBoolean("Broadcasting");
            t.add(Component.literal("§4§o§lRádio do Sinal Negro").withStyle(ChatFormatting.DARK_RED));
            t.add(Component.literal("§7§oRecebe transmissões de outra dimensão."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click: §ltoggle broadcast§r§7 30s"));
            t.add(Component.literal("§7Status: " + (active ? "§a§lON-AIR" : "§7off")));
            t.add(Component.literal("§7Players num raio §e16b§r§7 recebem mensagens fake de"));
            t.add(Component.literal("§7admin/sistema + reverse audio pulses"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"O sinal nunca para. Só você ouve.\""));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 3. HOLLOW MASK — paranoia + FOV warp + sky corruption
    // ════════════════════════════════════════════════════════════
    public static class HollowMaskItem extends Item {
        public HollowMaskItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean sel) {
            if (level.isClientSide) return;
            if (!(entity instanceof ServerPlayer sp)) return;
            if (level.getGameTime() % 40 != 0) return;

            ServerLevel sl = (ServerLevel) level;

            // Black liquid drops from face
            sl.sendParticles(ParticleTypes.SQUID_INK,
                    sp.getX(), sp.getY() + 1.7, sp.getZ(),
                    2, 0.1, 0, 0.1, 0.02);

            // +paranoia constante
            if (level.getGameTime() % 200 == 0) {
                InsanityData.addParanoia(sp, 2);
                InsanityData.addCorruption(sp, 1);
            }

            // A cada 100t (5s) — hallucination
            if (level.getGameTime() % 100 == 0 && Math.random() < 0.5) {
                HallucinationType[] options = {
                        HallucinationType.FAKE_ENTITY_PERIPHERAL,
                        HallucinationType.SHADOW_MOVEMENT,
                        HallucinationType.NAME_WHISPER,
                        HallucinationType.HEARTBEAT_PULSE
                };
                HallucinationManager.force(sp,
                        options[(int)(Math.random() * options.length)], 0.7F, 40, "");
            }

            // Cosmic horror SUBTLE phase é forçada
            if (CosmicHorrorManager.getPhase(sp) == CosmicHorrorPhase.DORMANT) {
                CosmicHorrorManager.setPhase(sp, CosmicHorrorPhase.SUBTLE_PRESENCE);
            }
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§f§o§lMáscara Oca").withStyle(ChatFormatting.WHITE));
            t.add(Component.literal("§7§oPorcelana rachada. Líquido negro escorre dos olhos."));
            t.add(Component.empty());
            t.add(Component.literal("§7Passivo: +paranoia/10s + corrupção/10s"));
            t.add(Component.literal("§7Hallucinations a cada 5s (50% chance)"));
            t.add(Component.literal("§7Ativa §dCosmic Horror SUBTLE§r§7 enquanto carrega"));
            t.add(Component.literal("§7Pinga §0ink particles§r§7 do rosto"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"O rosto por trás dela é o seu mesmo.\""));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 4. FLESH LANTERN — reveal entities, organic fog, attract cosmic
    // ════════════════════════════════════════════════════════════
    public static class FleshLanternItem extends Item {
        public FleshLanternItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean sel) {
            if (level.isClientSide) return;
            if (!(entity instanceof ServerPlayer sp)) return;
            if (!sel && sp.getOffhandItem() != stack) return;
            long now = level.getGameTime();
            ServerLevel sl = (ServerLevel) level;

            // Heartbeat-synced glow (a cada 30 ticks = 1.5s ~ heartbeat)
            if (now % 30 == 0) {
                // Reveal all living entities in radius
                for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class,
                        sp.getBoundingBox().inflate(16))) {
                    if (le == sp) continue;
                    le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 50, 0, true, false));
                }
                // Organic fog particles around lantern (player level)
                for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 1.5 + Math.random();
                    sl.sendParticles(ParticleTypes.SOUL,
                            sp.getX() + Math.cos(a) * r,
                            sp.getY() + 0.3,
                            sp.getZ() + Math.sin(a) * r,
                            1, 0, 0.05, 0, 0.01);
                }
                // Cursed pulse on player (lantern's heartbeat)
                sl.sendParticles(ModParticles.CURSED_PULSE.get(),
                        sp.getX(), sp.getY() + 1.2, sp.getZ(),
                        1, 0, 0, 0, 0);
            }

            // Attract cosmic creatures — hostis ficam mais agressivos perto
            if (now % 60 == 0) {
                for (Monster m : sl.getEntitiesOfClass(Monster.class,
                        sp.getBoundingBox().inflate(32))) {
                    if (m.getTarget() == null) {
                        m.setTarget(sp);
                    }
                    // Speed boost
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, true, false));
                }
            }

            // +corruption nearby — area de horror
            if (now % 100 == 0) {
                for (ServerPlayer near : sl.getPlayers(p -> p.distanceToSqr(sp) < 8 * 8)) {
                    if (near != sp) InsanityData.addCorruption(near, 1);
                }
            }
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§c§o§lLanterna de Carne").withStyle(ChatFormatting.RED));
            t.add(Component.literal("§7§oCarne viva pulsa em ritmo de coração."));
            t.add(Component.empty());
            t.add(Component.literal("§7Segure pra ativar:"));
            t.add(Component.literal("§7• §aGlowing§r§7 em TODA entidade num raio §e16b§r §7(1.5s sync)"));
            t.add(Component.literal("§7• §cAtrai hostis§r§7 num raio §e32b§r§7 + Speed boost"));
            t.add(Component.literal("§7• §dOrganic fog§r§7 (soul particles)"));
            t.add(Component.literal("§7• +corruption em players próximos (não você)"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Ela respira quando você não olha.\""));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 5. FALSE TOTEM — shared hallucinations + weather + mob behavior
    // ════════════════════════════════════════════════════════════
    public static class FalseTotemItem extends Item {
        public FalseTotemItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            ServerLevel sl = sp.serverLevel();
            // Weather + sky corruption
            sl.setWeatherParameters(0, 6000, true, true); // 5 min de chuva+thunder
            // Shared hallucination to ALL nearby players
            for (ServerPlayer near : sl.getPlayers(p -> p.distanceToSqr(sp) < 32 * 32)) {
                HallucinationManager.force(near, HallucinationType.REALITY_SHAKE, 0.8F, 60, "");
                HallucinationManager.force(near, HallucinationType.SCREEN_GLITCH_BURST, 0.6F, 40, "");
                InsanityData.addInsanity(near, 5);
                InsanityData.addCosmicInfluence(near, 3);
            }
            // Mob behavior: TODOS os mobs num raio 24 vão pra modo "afraid" - dispersam
            for (Mob m : sl.getEntitiesOfClass(Mob.class, sp.getBoundingBox().inflate(24))) {
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2, true, false));
                m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2, true, false));
            }
            // Floating runes around totem
            for (int i = 0; i < 20; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 1 + Math.random() * 3;
                sl.sendParticles(ModParticles.DEMON_GLYPH.get(),
                        sp.getX() + Math.cos(a) * r,
                        sp.getY() + 1 + Math.random() * 2,
                        sp.getZ() + Math.sin(a) * r,
                        1, 0, 0, 0, 0);
            }
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5O ídolo se manifesta. A realidade local se distorce."), false);
            sl.playSound(null, sp.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                    SoundSource.PLAYERS, 2.0F, 0.4F);
            sp.getCooldowns().addCooldown(this, 6000); // 5 min cd
            return InteractionResultHolder.consume(stack);
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§o§lÍdolo Falso").withStyle(ChatFormatting.DARK_PURPLE));
            t.add(Component.literal("§7§oPedra preta que muda quando ninguém vê."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click pra ativar:"));
            t.add(Component.literal("§7• §lWeather change§r§7: thunder por 5 min"));
            t.add(Component.literal("§7• §cShared hallucinations§r§7 — TODOS num raio §e32b"));
            t.add(Component.literal("§7  (reality shake + screen glitch + insanity)"));
            t.add(Component.literal("§7• §dMobs dispersam§r§7 (Speed III + Weakness III)"));
            t.add(Component.literal("§7• §6Floating runes§r§7 em volta"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Adoradores esqueceram o nome. O ídolo, não.\""));
            t.add(Component.literal("§c§oCD 5min"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 6. INFECTION NEEDLE — infect target, spreads corruption
    // ════════════════════════════════════════════════════════════
    public static class InfectionNeedleItem extends Item {
        public InfectionNeedleItem(Properties p) { super(p.rarity(Rarity.RARE).stacksTo(1).durability(8)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player attacker,
                                                      LivingEntity target, InteractionHand hand) {
            if (attacker.level().isClientSide) return InteractionResult.SUCCESS;
            if (!(attacker instanceof ServerPlayer sp)) return InteractionResult.PASS;

            // Infliges target
            target.hurt(sp.damageSources().magic(), 4);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 2, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 0, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1, true, true));

            // If target is player — corrupção crescente + stalker hallucinations
            if (target instanceof ServerPlayer victim) {
                InsanityData.addCorruption(victim, 25);
                InsanityData.addInsanity(victim, 15);
                // Stalker hallucinations pelos próximos 5 min
                victim.getPersistentData().putLong("liberthia.infection_until",
                        victim.level().getGameTime() + 6000);
                HallucinationManager.force(victim, HallucinationType.FAKE_DAMAGE_INDICATOR, 1.0F, 40, "");
                HallucinationManager.force(victim, HallucinationType.SCREEN_GLITCH_BURST, 0.8F, 30, "");
                victim.displayClientMessage(Component.literal(
                        "§4§l⚠ §r§4§oVocê foi infectado. Algo dentro... se move."), false);
            }

            // Visual: fleshy needle injection
            ServerLevel sl = (ServerLevel) attacker.level();
            sl.sendParticles(ParticleTypes.SCULK_SOUL,
                    target.getX(), target.getY() + 1, target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.05);
            sl.sendParticles(ModParticles.CURSED_PULSE.get(),
                    target.getX(), target.getY() + 1, target.getZ(),
                    5, 0.3, 0.5, 0.3, 0);

            // Damage durabilidade
            stack.hurtAndBreak(1, sp,
                    p -> p.broadcastBreakEvent(hand));

            sl.playSound(null, target.blockPosition(), SoundEvents.ARROW_HIT,
                    SoundSource.PLAYERS, 1.5F, 0.5F);
            return InteractionResult.CONSUME;
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§o§lAgulha da Infecção").withStyle(ChatFormatting.LIGHT_PURPLE));
            t.add(Component.literal("§7§oSeringa biomecânica com fluido vivo brilhando."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click numa criatura:"));
            t.add(Component.literal("§7• §c4 dmg§r§7 + §cWither III§r§7 (20s) + §dConfusion§r§7 + §2Poison II"));
            t.add(Component.literal("§7• Se for player: §c+25 corruption§r§7 + §c+15 insanity"));
            t.add(Component.literal("§7• Stalker hallucinations §e5 min§r"));
            t.add(Component.empty());
            t.add(Component.literal("§7Durability: §e8 usos"));
            t.add(Component.literal("§8§o\"A primeira injeção é sempre a mais doce.\""));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 7. BOOK OF IMPOSSIBLE GEOMETRY — sanity drain + ritual unlock
    // ════════════════════════════════════════════════════════════
    public static class BookOfImpossibleGeometryItem extends Item {
        public BookOfImpossibleGeometryItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            // "Read" the book
            int readings = stack.getOrCreateTag().getInt("Readings");
            readings++;
            stack.getOrCreateTag().putInt("Readings", readings);

            // Sanity cost escalating
            int sanityCost = 5 + readings;
            InsanityData.addInsanity(sp, sanityCost);
            InsanityData.addForbiddenKnowledge(sp, 8);
            InsanityData.addObsession(sp, 3);

            // Reveal hidden structures (Glowing on nearby mobs as "hidden enemies")
            ServerLevel sl = sp.serverLevel();
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class,
                    sp.getBoundingBox().inflate(48))) {
                if (le == sp) continue;
                le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0, true, false));
            }

            // Visual: pages flying around player
            for (int i = 0; i < 30; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 3;
                sl.sendParticles(ModParticles.DEMON_GLYPH.get(),
                        sp.getX() + Math.cos(a) * r,
                        sp.getY() + Math.random() * 2,
                        sp.getZ() + Math.sin(a) * r,
                        1, 0, 0, 0, 0);
            }

            // Trigger cosmic horror if readings ≥ 5
            if (readings >= 5) {
                CosmicHorrorManager.trigger(sp, "book_impossible");
                stack.getOrCreateTag().putInt("Readings", 0);
                sp.displayClientMessage(Component.literal(
                        "§4§l✦ §r§4§oA quinta leitura é a que ressoa."), false);
            } else {
                sp.displayClientMessage(Component.literal(
                        "§5§o*as páginas mudam* §7(leitura " + readings + "/5)"), true);
            }

            sl.playSound(null, sp.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.5F, 0.6F);
            sp.getCooldowns().addCooldown(this, 200); // 10s
            return InteractionResultHolder.consume(stack);
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            int readings = s.getOrCreateTag().getInt("Readings");
            t.add(Component.literal("§5§o§lLivro da Geometria Impossível").withStyle(ChatFormatting.DARK_PURPLE));
            t.add(Component.literal("§7§oAs páginas mudam quando você não olha."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click pra ler. Cada leitura:"));
            t.add(Component.literal("§7• §c+5-15 insanity"));
            t.add(Component.literal("§7• §d+8 forbidden knowledge"));
            t.add(Component.literal("§7• §6+3 obsession"));
            t.add(Component.literal("§7• §aGlowing§r§7 em entidades raio §e48b"));
            t.add(Component.literal("§7• 5ª leitura = §4§lCOSMIC HORROR RITUAL§r"));
            t.add(Component.empty());
            t.add(Component.literal("§7Leituras: §e" + readings + "§7/5"));
            t.add(Component.literal("§8§o\"Cada página foi escrita por quem não voltou.\""));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 8. MIMIC HEART — spawn fake player clones
    // ════════════════════════════════════════════════════════════
    public static class MimicHeartItem extends Item {
        public MimicHeartItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            // r49: REWRITE — spawn CÓPIAS EXATAS (ReflectionEntity) com skin/armor/hand do player
            ServerLevel sl = sp.serverLevel();
            for (int i = 0; i < 3; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 4 + Math.random() * 4;
                double x = sp.getX() + Math.cos(angle) * dist;
                double z = sp.getZ() + Math.sin(angle) * dist;
                int spawnY = sl.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        (int) x, (int) z);

                br.com.murilo.liberthia.cosmic.observatory.ReflectionEntity clone =
                        br.com.murilo.liberthia.registry.ModEntities.REFLECTION_ENTITY.get().create(sl);
                if (clone != null) {
                    clone.setOwnerUuid(sp.getUUID());
                    clone.setOwnerName(sp.getName().getString());
                    clone.copyEquipmentFrom(sp);              // skin + armor + hands
                    clone.spawnedTick = sl.getGameTime();
                    // Override lifetime curto pra mimic heart (10s)
                    clone.getPersistentData().putLong("liberthia.mimic_expire",
                            sl.getGameTime() + 200);
                    clone.moveTo(x + 0.5, spawnY, z + 0.5,
                            (float)(Math.random() * 360), 0);
                    sl.addFreshEntity(clone);
                }
            }

            // All nearby players get FAKE_ENTITY hallucination AND temporal ghost
            for (ServerPlayer near : sl.getPlayers(p -> p.distanceToSqr(sp) < 24 * 24)) {
                if (near == sp) continue;
                HallucinationManager.force(near, HallucinationType.TEMPORAL_GHOST, 1.0F, 80, "");
                HallucinationManager.force(near, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                        "§7<" + sp.getName().getString() + "§7> *§o" +
                                pickMimicLine() + "§r§7*");
            }

            sp.displayClientMessage(Component.literal(
                    "§c§l✦ §r§c§oO coração imita você. 3 cópias andam por aí."), true);
            sl.playSound(null, sp.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.PLAYERS, 2.0F, 0.7F);
            // Self-damage (coração é caro)
            sp.hurt(sp.damageSources().magic(), 6);
            sp.getCooldowns().addCooldown(this, 1200); // 1 min
            return InteractionResultHolder.consume(stack);
        }

        private static String pickMimicLine() {
            String[] lines = {
                    "espera, vem aqui",
                    "to com problema",
                    "olha o que achei",
                    "voltei",
                    "te vejo depois",
                    "to atrás de você"
            };
            return lines[(int)(Math.random() * lines.length)];
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§c§o§lCoração Mimético").withStyle(ChatFormatting.RED));
            t.add(Component.literal("§7§oUm coração vivo capaz de imitar você."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click pra ativar:"));
            t.add(Component.literal("§7• Spawn §c3 clones§r§7 com seu nome num raio §e8b§r §7(10s)"));
            t.add(Component.literal("§7• Players próximos veem §dghost hallucinations§r§7 + fake chat seu"));
            t.add(Component.literal("§7• §c-6 HP§r§7 self damage (custo da imitação)"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Quando ele bate, alguém finge ser você em outro lugar.\""));
            t.add(Component.literal("§c§oCD 1min"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 9. RED TAPE — global sky corruption, screen static
    // ════════════════════════════════════════════════════════════
    public static class RedTapeItem extends Item {
        public RedTapeItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            // Server-wide visual corruption for 60 seconds
            ServerLevel sl = sp.serverLevel();
            for (ServerPlayer all : sl.getServer().getPlayerList().getPlayers()) {
                HallucinationManager.force(all, HallucinationType.SCREEN_GLITCH_BURST, 1.0F, 200, "");
                HallucinationManager.force(all, HallucinationType.DISTORTED_AUDIO, 0.8F, 100, "");
                HallucinationManager.force(all, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                        "§7[§4SIGNAL§7] §4§o*-- WE INTERRUPT --*");
                InsanityData.addCosmicInfluence(all, 5);
            }

            // Set thunder/dark sky
            sl.setWeatherParameters(0, 2400, true, true);

            sp.displayClientMessage(Component.literal(
                    "§4§l✦ §r§4§o*play*"), false);
            sl.playSound(null, sp.blockPosition(), SoundEvents.UI_TOAST_IN,
                    SoundSource.PLAYERS, 3.0F, 0.2F);
            sp.getCooldowns().addCooldown(this, 12000); // 10 min
            return InteractionResultHolder.consume(stack);
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§4§o§lFita Vermelha").withStyle(ChatFormatting.DARK_RED));
            t.add(Component.literal("§7§oUma VHS com etiqueta escrita em sangue."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click pra dar §lPLAY§r§7:"));
            t.add(Component.literal("§7• §lTODOS§r§7 os players do server recebem:"));
            t.add(Component.literal("§7  - §dscreen static§r§7 10s"));
            t.add(Component.literal("§7  - §6distorted audio§r§7 5s"));
            t.add(Component.literal("§7  - fake transmission no chat"));
            t.add(Component.literal("§7  - §c+5 cosmic influence"));
            t.add(Component.literal("§7• Thunder global por 2 min"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Eles gravaram. Você assistiu. Não pode reverter.\""));
            t.add(Component.literal("§c§oCD 10min"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 10. NULL BELL — silence field, freeze entities, dimensional echo
    // ════════════════════════════════════════════════════════════
    public static class NullBellItem extends Item {
        public NullBellItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            ServerLevel sl = sp.serverLevel();
            // Freeze TODAS entidades num raio 16 por 6s
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class,
                    sp.getBoundingBox().inflate(16))) {
                if (le == sp) continue;
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 5, true, false));
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 3, true, false));
                if (le instanceof Mob m) {
                    m.getNavigation().stop();
                    m.setDeltaMovement(0, m.getDeltaMovement().y, 0);
                }
            }

            // Silence field — players próximos ouvem silêncio + heartbeat só
            for (ServerPlayer near : sl.getPlayers(p -> p.distanceToSqr(sp) < 16 * 16)) {
                HallucinationManager.force(near, HallucinationType.HEARTBEAT_PULSE, 1.0F, 120, "");
                near.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, true, false));
            }

            // Reality cracks (dimensional echo)
            for (int i = 0; i < 30; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 1 + Math.random() * 6;
                sl.sendParticles(ModParticles.DIMENSIONAL_CRACK.get(),
                        sp.getX() + Math.cos(a) * r,
                        sp.getY() + 1 + Math.random() * 2,
                        sp.getZ() + Math.sin(a) * r,
                        1, 0, 1.5 + Math.random(), 0, 0);
            }

            sp.displayClientMessage(Component.literal(
                    "§7§l✦ §r§7§oO sino toca o silêncio. 6 segundos de quietude."), false);
            sl.playSound(null, sp.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                    SoundSource.PLAYERS, 0.3F, 0.1F); // very quiet
            sp.getCooldowns().addCooldown(this, 600); // 30s
            return InteractionResultHolder.consume(stack);
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§7§o§lSino do Vazio").withStyle(ChatFormatting.GRAY));
            t.add(Component.literal("§7§oUm sino que toca a ausência."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click — campo de silêncio §e6s§r§7:"));
            t.add(Component.literal("§7• §lFreezes§r§7 entidades num raio §e16b§r §7(Slow VI + Weak IV)"));
            t.add(Component.literal("§7• Players próximos: §0Darkness§r§7 + heartbeat só"));
            t.add(Component.literal("§7• Dimensional cracks em volta"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Toca o sino. O som que falta é o que conta.\""));
            t.add(Component.literal("§c§oCD 30s"));
        }
    }
}
