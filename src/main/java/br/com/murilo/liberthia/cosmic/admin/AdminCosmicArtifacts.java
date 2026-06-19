package br.com.murilo.liberthia.cosmic.admin;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r51: <b>Admin Cosmic Artifacts</b> — 10 itens admin-only com
 * mecânicas de horror cósmico avançadas. TODOS exigem op level ≥ 2.
 *
 * <p>Cada item:
 * <ul>
 *   <li>Verifica permissão antes de ativar</li>
 *   <li>Efeitos per-player via HallucinationManager (NÃO broadcasta)</li>
 *   <li>Cooldown longo pra evitar spam</li>
 *   <li>Tooltip detalhado descrevendo a mecânica</li>
 * </ul>
 */
public final class AdminCosmicArtifacts {

    private AdminCosmicArtifacts() {}

    /** Base class — verifica permissão admin no use. */
    public static abstract class AdminItem extends Item {
        protected final int cooldownTicks;

        public AdminItem(Properties p, int cd) {
            super(p.rarity(Rarity.EPIC).stacksTo(1));
            this.cooldownTicks = cd;
        }

        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (!sp.hasPermissions(2) && !sp.isCreative()) {
                sp.displayClientMessage(Component.literal(
                        "§cVocê não é um Caretaker."), true);
                return InteractionResultHolder.fail(stack);
            }
            if (sp.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.fail(stack);
            }
            boolean ok = activate(sp, (ServerLevel) level, stack);
            if (ok) sp.getCooldowns().addCooldown(this, cooldownTicks);
            return ok ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
        }

        protected abstract boolean activate(ServerPlayer sp, ServerLevel sl, ItemStack stack);
    }

    // ════════════════════════════════════════════════════════════
    // 1. BLACK VEIL — progressive vision loss + shadow whispers
    // ════════════════════════════════════════════════════════════
    public static class BlackVeilItem extends AdminItem {
        public BlackVeilItem(Properties p) { super(p, 600); }
        @Override
        protected boolean activate(ServerPlayer sp, ServerLevel sl, ItemStack s) {
            // Pra cada player num raio 24b, aplica progressive darkness
            int affected = 0;
            for (ServerPlayer near : sl.getPlayers(p -> p.distanceToSqr(sp) < 24 * 24)) {
                if (near == sp) continue;
                near.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, false, false));
                near.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false));
                HallucinationManager.force(near, HallucinationType.FAKE_WHISPER, 0.8F, 60, "");
                HallucinationManager.force(near, HallucinationType.SHADOW_MOVEMENT, 0.7F, 80, "");
                InsanityData.addParanoia(near, 5);
                affected++;
            }
            // Particles em volta do veil (caster)
            for (int i = 0; i < 20; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 1 + Math.random() * 2;
                sl.sendParticles(ParticleTypes.SQUID_INK,
                        sp.getX() + Math.cos(a) * r,
                        sp.getY() + 1 + Math.random(),
                        sp.getZ() + Math.sin(a) * r,
                        1, 0.05, 0.1, 0.05, 0.02);
            }
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5O Véu se desdobra. " + affected + " olhos se fecham."), true);
            sl.playSound(null, sp.blockPosition(), SoundEvents.SOUL_ESCAPE,
                    SoundSource.PLAYERS, 1.5F, 0.4F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§l§oO Véu Negro").withStyle(ChatFormatting.DARK_PURPLE));
            t.add(Component.literal("§7§oTecido de noite movendo sem vento."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click: players num raio §e24b§r§7"));
            t.add(Component.literal("§7• §0Darkness 10s§r§7 + §dBlindness 5s§r§7"));
            t.add(Component.literal("§7• Whispers + shadow movement"));
            t.add(Component.literal("§7• §c+5 paranoia"));
            t.add(Component.literal("§c§oADMIN | CD 30s"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 2. TENDRIL CROWN — invisible tendrils slow + force rotation
    // ════════════════════════════════════════════════════════════
    public static class TendrilCrownItem extends AdminItem {
        public TendrilCrownItem(Properties p) { super(p, 800); }
        @Override
        protected boolean activate(ServerPlayer sp, ServerLevel sl, ItemStack s) {
            int affected = 0;
            for (ServerPlayer near : sl.getPlayers(p -> p.distanceToSqr(sp) < 20 * 20)) {
                if (near == sp) continue;
                // Slow + weak
                near.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1, false, false));
                near.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, false));
                // Force rotation (vision pulling sideways)
                br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(near,
                        new br.com.murilo.liberthia.cosmic.CosmicForceRotationS2CPacket(
                                (float)(Math.random() * 40 - 20), 0));
                // Fake touch sound + breathing
                HallucinationManager.force(near, HallucinationType.FAKE_FOOTSTEP, 1.0F, 40, "");
                HallucinationManager.force(near, HallucinationType.HEARTBEAT_PULSE, 0.8F, 60, "");
                // Tendril particles em volta do near
                for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 1 + Math.random() * 1.5;
                    sl.sendParticles(ModParticles.TENTACLE_WRITHE.get(),
                            near.getX() + Math.cos(a) * r,
                            near.getY() + Math.random() * 1.5,
                            near.getZ() + Math.sin(a) * r,
                            1, 0, 0, 0, 0);
                }
                affected++;
            }
            sp.displayClientMessage(Component.literal(
                    "§4§l✦ §r§4Os tentáculos despertam. " + affected + " presos."), true);
            sl.playSound(null, sp.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK,
                    SoundSource.PLAYERS, 1.5F, 0.5F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§4§l§oCoroa de Tentáculos").withStyle(ChatFormatting.DARK_RED));
            t.add(Component.literal("§7§oCoroa viva de cosmico orgânico."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click: players num raio §e20b§r§7"));
            t.add(Component.literal("§7• §lSlow II + Weakness§r§7 10s"));
            t.add(Component.literal("§7• Câmera vira sozinha ±20°"));
            t.add(Component.literal("§7• Tendril particles em volta"));
            t.add(Component.literal("§7• Fake touch + heartbeat"));
            t.add(Component.literal("§c§oADMIN | CD 40s"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 3. FALSE SUN — sky corruption, fake eclipse
    // ════════════════════════════════════════════════════════════
    public static class FalseSunItem extends AdminItem {
        public FalseSunItem(Properties p) { super(p, 2400); }
        @Override
        protected boolean activate(ServerPlayer sp, ServerLevel sl, ItemStack s) {
            int affected = 0;
            for (ServerPlayer near : sl.getPlayers(p -> p.distanceToSqr(sp) < 64 * 64)) {
                if (near == sp) continue;
                // Sky darken + impossible moon
                HallucinationManager.force(near, HallucinationType.IMPOSSIBLE_MOON, 1.0F, 200, "");
                HallucinationManager.force(near, HallucinationType.DISTORTED_AUDIO, 0.6F, 100, "");
                near.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 300, 0, false, false));
                InsanityData.addCosmicInfluence(near, 3);
                affected++;
            }
            // Spawn "second sun" particles no céu acima do caster
            double skyY = sp.getY() + 50;
            for (int i = 0; i < 60; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 4;
                sl.sendParticles(ParticleTypes.FLAME,
                        sp.getX() + Math.cos(a) * r,
                        skyY + Math.random() * 6,
                        sp.getZ() + Math.sin(a) * r,
                        1, 0, 0, 0, 0);
            }
            // Set thunder
            sl.setWeatherParameters(0, 1200, true, false);
            sp.displayClientMessage(Component.literal(
                    "§6§l✦ §r§6O segundo sol arde. " + affected + " olham pra cima."), false);
            sl.playSound(null, sp.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.AMBIENT, 2.0F, 0.5F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§6§l§oFalso Sol").withStyle(ChatFormatting.GOLD));
            t.add(Component.literal("§7§oUma estrela que arde com luz invertida."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click: players num raio §e64b§r§7"));
            t.add(Component.literal("§7• §lImpossible moon§r§7 hallucination"));
            t.add(Component.literal("§7• §0Darkness 15s§r§7 + Distorted audio"));
            t.add(Component.literal("§7• Particles no céu (segundo sol)"));
            t.add(Component.literal("§7• Thunder 1min"));
            t.add(Component.literal("§7• §c+3 cosmic influence"));
            t.add(Component.literal("§c§oADMIN | CD 2min"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 4. MIRROR PULSE — spawn reflection clones of caster at distance
    // ════════════════════════════════════════════════════════════
    public static class MirrorPulseItem extends AdminItem {
        public MirrorPulseItem(Properties p) { super(p, 3600); }
        @Override
        protected boolean activate(ServerPlayer sp, ServerLevel sl, ItemStack s) {
            // Spawn 4 ReflectionEntity em 4 direções cardinais 25b de distância
            int spawned = 0;
            for (int i = 0; i < 4; i++) {
                double angle = i * (Math.PI / 2) + Math.random() * 0.3;
                double dist = 25 + Math.random() * 10;
                double x = sp.getX() + Math.cos(angle) * dist;
                double z = sp.getZ() + Math.sin(angle) * dist;
                int y = sl.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        (int) x, (int) z);
                var clone = br.com.murilo.liberthia.registry.ModEntities.REFLECTION_ENTITY.get().create(sl);
                if (clone != null) {
                    clone.setOwnerUuid(sp.getUUID());
                    clone.setOwnerName(sp.getName().getString());
                    clone.copyEquipmentFrom(sp);
                    clone.spawnedTick = sl.getGameTime();
                    clone.setHostile(true); // r179: TE CAÇAM e atacam, não somem ao olhar
                    clone.moveTo(x + 0.5, y, z + 0.5, (float)(Math.random() * 360), 0);
                    sl.addFreshEntity(clone);
                    spawned++;
                }
            }
            // Particles pulse no caster
            for (int i = 0; i < 40; i++) {
                double a = i * (Math.PI * 2 / 40);
                sl.sendParticles(ParticleTypes.END_ROD,
                        sp.getX() + Math.cos(a) * 2,
                        sp.getY() + 1,
                        sp.getZ() + Math.sin(a) * 2,
                        1, 0, 0, 0, 0);
            }
            sp.displayClientMessage(Component.literal(
                    "§b§l✦ §r§bO pulso espelha. " + spawned + " versões §oerradas§r§b."), false);
            sl.playSound(null, sp.blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, 2.0F, 1.5F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§b§l§oPulso do Espelho").withStyle(ChatFormatting.AQUA));
            t.add(Component.literal("§7§oMetal líquido com geometria impossível."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click:"));
            t.add(Component.literal("§7• Spawn §e4 ReflectionEntity§r§7 cardinais a 25-35b"));
            t.add(Component.literal("§7• Cópias EXATAS de você (skin+armor+hands)"));
            t.add(Component.literal("§c• TE CAÇAM e atacam"));
            t.add(Component.literal("§7• NÃO somem ao olhar — precisa matá-las"));
            t.add(Component.literal("§c§oADMIN | CD 3min"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 5. SILENT BELL — 8s silence + delayed audio + post-whispers
    // ════════════════════════════════════════════════════════════
    public static class SilentBellItem extends AdminItem {
        public SilentBellItem(Properties p) { super(p, 1200); }
        @Override
        protected boolean activate(ServerPlayer sp, ServerLevel sl, ItemStack s) {
            int affected = 0;
            for (ServerPlayer near : sl.getPlayers(p -> p.distanceToSqr(sp) < 32 * 32)) {
                if (near == sp) continue;
                near.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0, false, false));
                HallucinationManager.force(near, HallucinationType.HEARTBEAT_PULSE, 1.0F, 160, "");
                HallucinationManager.force(near, HallucinationType.REVERSE_AUDIO_PULSE, 0.7F, 80, "");
                // Pre-schedule fake footsteps + whispers que vão aparecer depois (8s)
                // Apenas trigger imediato — sistema natural fará o resto
                affected++;
            }
            // Dimensional cracks em volta
            for (int i = 0; i < 24; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 1 + Math.random() * 5;
                sl.sendParticles(ModParticles.DIMENSIONAL_CRACK.get(),
                        sp.getX() + Math.cos(a) * r,
                        sp.getY() + 1 + Math.random() * 2,
                        sp.getZ() + Math.sin(a) * r,
                        1, 0, 1.5, 0, 0);
            }
            sp.displayClientMessage(Component.literal(
                    "§7§l✦ §r§7§oO sino que não toca silencia o mundo. " + affected + " ouvem nada."), false);
            // Som muito baixo (silencio dramático)
            sl.playSound(null, sp.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                    SoundSource.PLAYERS, 0.3F, 0.1F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§7§l§oSino Silencioso").withStyle(ChatFormatting.GRAY));
            t.add(Component.literal("§7§oObsidiana com runas cravadas. Sem badalo."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click: players num raio §e32b§r§7"));
            t.add(Component.literal("§7• §0Darkness 8s§r§7"));
            t.add(Component.literal("§7• Heartbeat constante (só ele)"));
            t.add(Component.literal("§7• Reverse audio pulse"));
            t.add(Component.literal("§7• Dimensional cracks em volta"));
            t.add(Component.literal("§c§oADMIN | CD 1min"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 6. OPEN EYE — progressive blindness when held
    // ════════════════════════════════════════════════════════════
    public static class OpenEyeItem extends AdminItem {
        public OpenEyeItem(Properties p) { super(p, 1200); }
        @Override
        protected boolean activate(ServerPlayer sp, ServerLevel sl, ItemStack s) {
            int affected = 0;
            for (ServerPlayer near : sl.getPlayers(p -> p.distanceToSqr(sp) < 32 * 32)) {
                if (near == sp) continue;
                // Progressive blindness: Blur (effect via Mining Fatigue) → Blindness → Darkness
                near.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400, 0, false, false));
                near.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 400, 1, false, false));
                near.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false));
                HallucinationManager.force(near, HallucinationType.SCREEN_GLITCH_BURST, 0.7F, 60, "");
                HallucinationManager.force(near, HallucinationType.FAKE_ENTITY_PERIPHERAL, 1.0F, 120, "");
                // Spawn eye particle atrás do near
                Vec3 behind = near.position().subtract(near.getLookAngle().scale(3));
                sl.sendParticles(ModParticles.GLARING_EYE_PULSE.get(),
                        behind.x, behind.y + 1.5, behind.z, 5, 0.3, 0.3, 0.3, 0);
                affected++;
            }
            sp.displayClientMessage(Component.literal(
                    "§4§l✦ §r§4O Olho se abre. " + affected + " perdem a visão."), false);
            sl.playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_STARE,
                    SoundSource.PLAYERS, 2.5F, 0.5F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§4§l§oO Olho Aberto").withStyle(ChatFormatting.DARK_RED));
            t.add(Component.literal("§7§oRelíquia viva. A íris segue você."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click: players num raio §e32b§r§7"));
            t.add(Component.literal("§7• §lBlindness 20s + Darkness II 20s§r§7"));
            t.add(Component.literal("§7• Confusion 10s"));
            t.add(Component.literal("§7• Screen glitch + fake entity peripheral"));
            t.add(Component.literal("§7• §c'Olho' spawn atrás de cada vítima"));
            t.add(Component.literal("§c§oADMIN | CD 1min"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 7. THREAD OF DISTANCE — perception desync
    // ════════════════════════════════════════════════════════════
    public static class ThreadOfDistanceItem extends AdminItem {
        public ThreadOfDistanceItem(Properties p) { super(p, 1800); }
        @Override
        protected boolean activate(ServerPlayer sp, ServerLevel sl, ItemStack s) {
            int affected = 0;
            for (ServerPlayer near : sl.getPlayers(p -> p.distanceToSqr(sp) < 32 * 32)) {
                if (near == sp) continue;
                // Slow Falling + Slowness gives "delayed" feeling
                near.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false));
                near.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1, false, false));
                near.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0, false, false));
                HallucinationManager.force(near, HallucinationType.TEMPORAL_GHOST, 0.9F, 100, "");
                HallucinationManager.force(near, HallucinationType.FAKE_ENTITY_PERIPHERAL, 0.6F, 60, "");
                // Spawn ghost duplicates nas posições passadas
                Vec3 prev = near.position().subtract(near.getDeltaMovement().scale(20));
                sl.sendParticles(ModParticles.VULTO_SHADOW.get(),
                        prev.x, prev.y + 1, prev.z, 5, 0.3, 0.5, 0.3, 0);
                affected++;
            }
            // White thread particles
            for (int i = 0; i < 30; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 4;
                sl.sendParticles(ParticleTypes.END_ROD,
                        sp.getX() + Math.cos(a) * r,
                        sp.getY() + Math.random() * 2,
                        sp.getZ() + Math.sin(a) * r,
                        1, 0, 0.1, 0, 0.01);
            }
            sp.displayClientMessage(Component.literal(
                    "§f§l✦ §r§fO fio mede distâncias erradas. " + affected + " desync."), true);
            sl.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.5F, 2.0F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§f§l§oFio da Distância").withStyle(ChatFormatting.WHITE));
            t.add(Component.literal("§7§oFio branco que flutua sem suporte."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click: players num raio §e32b§r§7"));
            t.add(Component.literal("§7• §aSlow Falling 20s§r§7 + §cSlow II 10s"));
            t.add(Component.literal("§7• Levitation flash 1s (sensação de delay)"));
            t.add(Component.literal("§7• Temporal ghost + ghost trail nas posições anteriores"));
            t.add(Component.literal("§c§oADMIN | CD 1.5min"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 8. FLESH SIGNAL — impossible transmissions
    // ════════════════════════════════════════════════════════════
    public static class FleshSignalItem extends AdminItem {
        public FleshSignalItem(Properties p) { super(p, 2400); }
        @Override
        protected boolean activate(ServerPlayer sp, ServerLevel sl, ItemStack s) {
            int affected = 0;
            String[] transmissions = {
                    "§7[§4SIGNAL§7] §o" + sp.getName().getString() + " killed by §7?",
                    "§7[§4COORDS§7] §e" + (int)(sp.getX() + 1000) + ", " + (int)(sp.getY() + 50) + ", " + (int)(sp.getZ() - 800),
                    "§7[§4VOICE§7] §o*scream*",
                    "§7[§4SIGNAL§7] §o*reverse static*",
                    "§7[§4FUTURE§7] §oyou will hear this again",
                    "§7[§4COORDS§7] §cinvalid position detected",
                    "§7[§4BROADCAST§7] §oeles estão aqui"
            };
            for (ServerPlayer near : sl.getPlayers(p -> p.distanceToSqr(sp) < 48 * 48)) {
                if (near == sp) continue;
                // Manda 3 transmissions diferentes pra cada
                for (int i = 0; i < 3; i++) {
                    String msg = transmissions[(int)(Math.random() * transmissions.length)];
                    HallucinationManager.force(near, HallucinationType.FAKE_CHAT_MESSAGE,
                            1.0F, 1, msg);
                }
                HallucinationManager.force(near, HallucinationType.REVERSE_AUDIO_PULSE,
                        0.8F, 100, "");
                HallucinationManager.force(near, HallucinationType.DISTORTED_AUDIO,
                        0.7F, 80, "");
                InsanityData.addCosmicInfluence(near, 2);
                affected++;
            }
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5A carne transmite. " + affected + " escutam."), true);
            sl.playSound(null, sp.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.get(),
                    SoundSource.PLAYERS, 2.0F, 0.3F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§l§oSinal da Carne").withStyle(ChatFormatting.DARK_PURPLE));
            t.add(Component.literal("§7§oRádio de osso e carne respirando."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click: players num raio §e48b§r§7"));
            t.add(Component.literal("§7• §c3 transmissões fake§r§7 cada"));
            t.add(Component.literal("§7  - coords impossíveis"));
            t.add(Component.literal("§7  - fake deaths"));
            t.add(Component.literal("§7  - reverse + distorted audio"));
            t.add(Component.literal("§7• §c+2 cosmic influence cada"));
            t.add(Component.literal("§c§oADMIN | CD 2min"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 9. DEEP WATER — underwater ambience + flooded vision
    // ════════════════════════════════════════════════════════════
    public static class DeepWaterItem extends AdminItem {
        public DeepWaterItem(Properties p) { super(p, 1800); }
        @Override
        protected boolean activate(ServerPlayer sp, ServerLevel sl, ItemStack s) {
            int affected = 0;
            for (ServerPlayer near : sl.getPlayers(p -> p.distanceToSqr(sp) < 32 * 32)) {
                if (near == sp) continue;
                // Underwater feel: water breathing + slow + dolphin (positive bait)
                near.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0, false, false));
                near.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 2, false, false));
                near.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 300, 0, false, false));
                // Whale-like sounds + distorted
                HallucinationManager.force(near, HallucinationType.DISTORTED_AUDIO, 0.9F, 200, "");
                HallucinationManager.force(near, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                        "§3§o*você ouve canto de baleia distante*");
                HallucinationManager.force(near, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                        "§3§o*metal afundado range*");
                // Bubble particles em volta
                for (int i = 0; i < 12; i++) {
                    sl.sendParticles(ParticleTypes.BUBBLE,
                            near.getX() + (Math.random() - 0.5) * 2,
                            near.getY() + Math.random() * 2,
                            near.getZ() + (Math.random() - 0.5) * 2,
                            1, 0, 0.1, 0, 0.02);
                }
                affected++;
            }
            sp.displayClientMessage(Component.literal(
                    "§3§l✦ §r§3O abismo respira. " + affected + " submergem."), false);
            sl.playSound(null, sp.blockPosition(), SoundEvents.AMBIENT_UNDERWATER_ENTER,
                    SoundSource.AMBIENT, 3.0F, 0.4F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§3§l§oÁgua Profunda").withStyle(ChatFormatting.DARK_AQUA));
            t.add(Component.literal("§7§oOrbe contendo um oceano negro."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click: players num raio §e32b§r§7"));
            t.add(Component.literal("§7• §bWater Breathing 30s§r§7 (mas em terra)"));
            t.add(Component.literal("§7• §cSlow III + Darkness§r§7"));
            t.add(Component.literal("§7• Canto de baleia + metal afundado"));
            t.add(Component.literal("§7• Bubbles em volta"));
            t.add(Component.literal("§c§oADMIN | CD 1.5min"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 10. AUDIENCE MARK — marca target pra escalation crescente
    // ════════════════════════════════════════════════════════════
    public static class AudienceMarkItem extends AdminItem {
        public AudienceMarkItem(Properties p) { super(p, 200); }
        @Override
        protected boolean activate(ServerPlayer sp, ServerLevel sl, ItemStack s) {
            sp.displayClientMessage(Component.literal(
                    "§7Mire em um §dplayer§r§7 e right-click pra marcar."), true);
            return false;
        }
        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player carrier,
                                                      LivingEntity target, InteractionHand hand) {
            if (carrier.level().isClientSide) return InteractionResult.SUCCESS;
            if (!(carrier instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (!sp.hasPermissions(2) && !sp.isCreative()) {
                sp.displayClientMessage(Component.literal("§cVocê não é Caretaker."), true);
                return InteractionResult.FAIL;
            }
            if (!(target instanceof ServerPlayer victim)) {
                sp.displayClientMessage(Component.literal("§7Só funciona em players."), true);
                return InteractionResult.FAIL;
            }
            // Usa ObservationInjection (do r48) — mark + auto escalation
            br.com.murilo.liberthia.cosmic.observatory.ObservationInjection.mark(victim);
            // Particles dramáticos no target
            ServerLevel sl = victim.serverLevel();
            for (int i = 0; i < 30; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 2;
                sl.sendParticles(ModParticles.DEMON_GLYPH.get(),
                        victim.getX() + Math.cos(a) * r,
                        victim.getY() + Math.random() * 2,
                        victim.getZ() + Math.sin(a) * r,
                        1, 0, 0, 0, 0);
            }
            sp.displayClientMessage(Component.literal(
                    "§4§l✦ §r§4§o" + victim.getName().getString()
                            + " marcado. A Audiência observa agora."), false);
            sp.getCooldowns().addCooldown(this, 600);
            sl.playSound(null, victim.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                    SoundSource.AMBIENT, 0.5F, 0.4F);
            return InteractionResult.CONSUME;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§4§l§oMarca da Audiência").withStyle(ChatFormatting.DARK_RED));
            t.add(Component.literal("§7§oSigilo vivo. A geometria se move."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click num §dplayer§r§7: marca pra"));
            t.add(Component.literal("§lObservation Injection§r§7 — escalation"));
            t.add(Component.literal("§7automática em 5 tiers ao longo de 25 min:"));
            t.add(Component.empty());
            t.add(Component.literal("§7  Tier 1 (0-5min): whispers"));
            t.add(Component.literal("§7  Tier 2 (5-10min): footsteps, block flash"));
            t.add(Component.literal("§7  Tier 3 (10-15min): entidades fake, heartbeat"));
            t.add(Component.literal("§7  Tier 4 (15-20min): nome whisper, reverse audio"));
            t.add(Component.literal("§7  Tier 5 (20+min): glitch, reality shake, fake death"));
            t.add(Component.empty());
            t.add(Component.literal("§7Remove via §o/liberthia caretaker unmark§r§7."));
            t.add(Component.literal("§c§oADMIN | CD 30s"));
        }
    }
}
