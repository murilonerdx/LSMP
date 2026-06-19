package br.com.murilo.liberthia.cosmic.curse;

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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r45: <b>Horror Items</b> — 8 itens novos pra Cosmic Horror Expansion.
 *
 * <ol>
 *   <li><b>CursedEffigy</b>: amaldiçoado, não solta, +sanity drain, +paranoia</li>
 *   <li><b>WatcherMark</b>: rclick num player → marca ele como VICTIM (vê hallucinations)</li>
 *   <li><b>PhantomCaller</b>: rclick → spawn 3 phantoms estáticos atrás de players próximos</li>
 *   <li><b>VultoLens</b>: HOLD use → renderiza "olhos" em todos mobs vivos num raio (glow)</li>
 *   <li><b>InsanityCrown</b>: admin-only, ativa "modo entidade cósmica" — invul + flight + AoE</li>
 *   <li><b>TendrilSigil</b>: lança tentáculo agarrador (slow IV + dmg)</li>
 *   <li><b>VoiceCurseAmulet</b>: amaldiçoado — mensagens de chat do victim são distorcidas</li>
 *   <li><b>SilentWitnessCloak</b>: amaldiçoado — invisibilidade ETERNA, mas tudo te ignora (incluindo curas)</li>
 * </ol>
 */
public final class HorrorItems {

    private HorrorItems() {}

    // ════════════════════════════════════════════════════════════
    // 1. CURSED EFFIGY — amaldiçoado, drain de sanity
    // ════════════════════════════════════════════════════════════
    public static class CursedEffigyItem extends Item {
        public CursedEffigyItem(Properties p) { super(p.rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            if (level.isClientSide) return;
            if (!(entity instanceof ServerPlayer sp)) return;
            if (level.getGameTime() % 60 != 0) return; // a cada 3s

            // Spawn tentáculos visuais ao redor (só quando segurando)
            if (selected || sp.getOffhandItem() == stack) {
                ServerLevel sl = (ServerLevel) level;
                for (int i = 0; i < 2; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = 0.6;
                    sl.sendParticles(ModParticles.TENTACLE_WRITHE.get(),
                            sp.getX() + Math.cos(angle) * r,
                            sp.getY() + 0.5 + Math.random() * 0.5,
                            sp.getZ() + Math.sin(angle) * r,
                            1, 0, 0, 0, 0);
                }
            }
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§4§o§lEFÍGIE AMALDIÇOADA").withStyle(ChatFormatting.DARK_RED));
            t.add(Component.literal("§7§oUm boneco costurado com cabelo humano."));
            t.add(Component.empty());
            t.add(Component.literal("§c§l⚠ NÃO PODE SER SOLTA"));
            t.add(Component.literal("§7+1 corruption / 3s no inventário"));
            t.add(Component.literal("§7+1 insanity / 10s"));
            t.add(Component.literal("§7Hallucinations aleatórias"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"O que costuram, costura de volta.\""));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 2. WATCHER MARK — marca um player como victim
    // ════════════════════════════════════════════════════════════
    public static class WatcherMarkItem extends Item {
        public WatcherMarkItem(Properties p) { super(p.rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player carrier,
                                                       LivingEntity target, InteractionHand hand) {
            if (carrier.level().isClientSide) return InteractionResult.SUCCESS;
            if (!(carrier instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (!(target instanceof ServerPlayer victim)) {
                sp.displayClientMessage(Component.literal(
                        "§7A marca só funciona em §dplayers§r§7."), true);
                return InteractionResult.FAIL;
            }
            if (victim == sp) {
                sp.displayClientMessage(Component.literal(
                        "§cVocê não pode se marcar."), true);
                return InteractionResult.FAIL;
            }
            // Set target
            TargetedCurseStorage.setTarget(sp, victim.getUUID());
            sp.displayClientMessage(Component.literal(
                    "§4§l✦ §r§4§o" + victim.getName().getString()
                            + " foi MARCADO. Ele verá coisas em breve..."), false);
            sp.getCooldowns().addCooldown(this, 600); // 30s
            // Visual: glyph demoníaco no target
            ServerLevel sl = (ServerLevel) carrier.level();
            sl.sendParticles(ModParticles.DEMON_GLYPH.get(),
                    victim.getX(), victim.getY() + 1.5, victim.getZ(),
                    10, 0.4, 0.5, 0.4, 0);
            sl.playSound(null, victim.blockPosition(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.5F, 0.4F);
            return InteractionResult.CONSUME;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            // Shift+rclick AR = limpa target
            if (sp.isShiftKeyDown()) {
                TargetedCurseStorage.setTarget(sp, null);
                sp.displayClientMessage(Component.literal(
                        "§aMarca dispersa. Vítima livre."), true);
                return InteractionResultHolder.success(stack);
            }
            sp.displayClientMessage(Component.literal(
                    "§7Mire em um §dplayer§r§7 e right-click pra marcar."), true);
            return InteractionResultHolder.pass(stack);
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§4§oMarca do Observador").withStyle(ChatFormatting.DARK_RED));
            t.add(Component.literal("§7Right-click num §dplayer§r§7: marca ele como vítima"));
            t.add(Component.literal("§7A vítima começa a ver §c§lvultos§r§7, §c§lwhispers§r§7, §c§lentidades§r"));
            t.add(Component.literal("§7Escalada em 3 tiers (0-2min, 2-5min, 5+min)"));
            t.add(Component.literal("§7Shift+Right-click §oAR§7: dispersa a marca"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Você não mata. Você abre os olhos dela.\""));
            t.add(Component.literal("§c§oCD 30s"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 3. PHANTOM CALLER — toca o sino, spawn de phantoms estáticos
    // ════════════════════════════════════════════════════════════
    public static class PhantomCallerItem extends Item {
        public PhantomCallerItem(Properties p) { super(p.rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            ServerLevel sl = sp.serverLevel();
            // Pra cada player num raio 32 (incluindo self), spawn 3 vultos atrás
            int count = 0;
            for (ServerPlayer near : sl.getPlayers(p -> p.distanceToSqr(sp) < 32 * 32)) {
                Vec3 look = near.getLookAngle();
                for (int i = 0; i < 3; i++) {
                    double offset = (Math.random() - 0.5) * 4;
                    double dist = 4 + Math.random() * 3;
                    double bx = near.getX() - look.x * dist + offset;
                    double bz = near.getZ() - look.z * dist + offset;
                    StaticStalkerManager.spawnAt(sl, bx, near.getY(), bz, near.getUUID());
                }
                // Trigger hallucination
                HallucinationManager.force(near, HallucinationType.FAKE_FOOTSTEP, 1.0F, 60, "");
                count++;
            }
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5O sino toca. " + count + " players ouviram."), true);
            sl.playSound(null, sp.blockPosition(),
                    SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.5F, 0.5F);
            sp.getCooldowns().addCooldown(this, 1200); // 1min
            return InteractionResultHolder.consume(stack);
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oChamador Fantasma").withStyle(ChatFormatting.LIGHT_PURPLE));
            t.add(Component.literal("§7Right-click: §ltoca o sino§r§7."));
            t.add(Component.literal("§7Pra cada player num raio §d32b§r§7, §c3 vultos§r§7 spawnam atrás."));
            t.add(Component.literal("§7Os vultos §ldesaparecem quando observados§r§7."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"O sino chama o que está sempre por perto.\""));
            t.add(Component.literal("§c§oCD 60s"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 4. VULTO LENS — vê ghosts/mobs através de paredes
    // ════════════════════════════════════════════════════════════
    public static class VultoLensItem extends Item {
        public VultoLensItem(Properties p) { super(p.rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            ServerLevel sl = sp.serverLevel();
            // Aplica Glowing em todos LivingEntity num raio 48
            int n = 0;
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class,
                    sp.getBoundingBox().inflate(48))) {
                if (le == sp) continue;
                le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0, true, false));
                // Render eye particle perto da cabeça
                sl.sendParticles(ModParticles.GLARING_EYE_PULSE.get(),
                        le.getX(), le.getY() + le.getBbHeight() + 0.3, le.getZ(),
                        2, 0.2, 0, 0.2, 0);
                n++;
            }
            sp.displayClientMessage(Component.literal(
                    "§b§l✦ §r§bA lente revela " + n + " presenças."), true);
            sl.playSound(null, sp.blockPosition(),
                    SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1.0F, 0.6F);
            // Side effect: sanity drain
            InsanityData.addInsanity(sp, 3);
            sp.getCooldowns().addCooldown(this, 600);
            return InteractionResultHolder.consume(stack);
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§b§oLente do Vulto").withStyle(ChatFormatting.AQUA));
            t.add(Component.literal("§7Right-click: §lGlowing 30s§r§7 em §dTUDO§r§7 num raio §e48b§r§7."));
            t.add(Component.literal("§7Vê através de paredes."));
            t.add(Component.empty());
            t.add(Component.literal("§c-3 sanity por uso"));
            t.add(Component.literal("§c§oCD 30s"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 5. INSANITY CROWN — admin-only, poder cósmico
    // ════════════════════════════════════════════════════════════
    public static class InsanityCrownItem extends Item {
        public InsanityCrownItem(Properties p) { super(p.rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(1).fireResistant()); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            // Permissions check
            if (!sp.hasPermissions(2) && !sp.isCreative()) {
                sp.displayClientMessage(Component.literal(
                        "§cVocê não é digno. (Requer OP / Creative)"), true);
                sp.hurt(sp.damageSources().magic(), 8);
                return InteractionResultHolder.fail(stack);
            }
            // Toggle "cosmic entity mode"
            boolean active = stack.getOrCreateTag().getBoolean("CosmicMode");
            if (!active) {
                stack.getOrCreateTag().putBoolean("CosmicMode", true);
                // Buffs cósmicos
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 4, false, false));
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 6000, 2, false, false));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6000, 2, false, false));
                sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, false, false));
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 4, false, false));
                sp.getAbilities().mayfly = true;
                sp.getAbilities().invulnerable = true;
                sp.onUpdateAbilities();
                // Aura permanente — set CosmicHorror SUBTLE no caster
                CosmicHorrorManager.setPhase(sp, CosmicHorrorPhase.DIMENSIONAL_CORRUPTION);
                sp.displayClientMessage(Component.literal(
                        "§4§l✦ MODO ENTIDADE CÓSMICA ATIVADO ✦").withStyle(ChatFormatting.DARK_RED), false);
            } else {
                stack.getOrCreateTag().putBoolean("CosmicMode", false);
                sp.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                sp.removeEffect(MobEffects.REGENERATION);
                sp.removeEffect(MobEffects.MOVEMENT_SPEED);
                sp.removeEffect(MobEffects.DAMAGE_BOOST);
                sp.removeEffect(MobEffects.NIGHT_VISION);
                sp.getAbilities().mayfly = sp.isCreative();
                sp.getAbilities().invulnerable = sp.isCreative();
                sp.onUpdateAbilities();
                CosmicHorrorManager.reset(sp);
                sp.displayClientMessage(Component.literal(
                        "§7Modo Entidade Cósmica desativado."), true);
            }
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            if (level.isClientSide) return;
            if (!(entity instanceof ServerPlayer sp)) return;
            boolean active = stack.getOrCreateTag().getBoolean("CosmicMode");
            if (!active) return;
            if (level.getGameTime() % 10 != 0) return;

            ServerLevel sl = (ServerLevel) level;
            // Aura de tentáculos orbitais
            for (int i = 0; i < 3; i++) {
                double a = (level.getGameTime() * 0.1 + i * (Math.PI * 2 / 3)) % (Math.PI * 2);
                double r = 1.5;
                sl.sendParticles(ModParticles.TENTACLE_WRITHE.get(),
                        sp.getX() + Math.cos(a) * r,
                        sp.getY() + 1.2 + Math.sin(level.getGameTime() * 0.05) * 0.3,
                        sp.getZ() + Math.sin(a) * r,
                        1, 0, 0, 0, 0);
            }
            // Damage radial 2/sec a hostis num raio 6
            if (level.getGameTime() % 40 == 0) {
                for (Monster m : sl.getEntitiesOfClass(Monster.class, sp.getBoundingBox().inflate(6))) {
                    m.hurt(sp.damageSources().magic(), 6);
                    m.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                }
            }
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            boolean active = s.getOrCreateTag().getBoolean("CosmicMode");
            t.add(Component.literal("§4§l§oCoroa da Insanidade").withStyle(ChatFormatting.DARK_RED));
            t.add(Component.literal("§c§oADMIN/CREATIVE ONLY"));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click: toggle §lModo Entidade Cósmica§r§7"));
            t.add(Component.literal("§7Status: " + (active ? "§a§lATIVO" : "§7§linativo")));
            t.add(Component.empty());
            t.add(Component.literal("§7Buffs no modo ativo:"));
            t.add(Component.literal("§7• §aResistance V§7 + §aRegen III§7 + §aSpeed III§7 + §aStrength V"));
            t.add(Component.literal("§7• §aMayFly + Invulnerable"));
            t.add(Component.literal("§7• Aura de §dtentáculos§r§7 orbital"));
            t.add(Component.literal("§7• §c6 dmg/2s§r§7 em hostis num raio §d6b"));
            t.add(Component.literal("§7• Wither II em hostis atingidos"));
            t.add(Component.literal("§7• Visão cósmica corrompida (shaders ativos)"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 6. TENDRIL SIGIL — projétil tentáculo agarrador
    // ════════════════════════════════════════════════════════════
    public static class TendrilSigilItem extends Item {
        public TendrilSigilItem(Properties p) { super(p.rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            ServerLevel sl = sp.serverLevel();
            // r164 FIX: sp.pick só retorna BlockHitResult — usa entity raycast real
            LivingEntity le = br.com.murilo.liberthia.util.EntityRaycast.pickLiving(sp, 20.0);
            if (le == null) {
                sp.displayClientMessage(Component.literal("§7Mire em uma criatura."), true);
                return InteractionResultHolder.fail(stack);
            }
            // Tentáculo visual do player até o alvo
            Vec3 from = sp.position().add(0, 1, 0);
            Vec3 to = le.position().add(0, 1, 0);
            double dist = from.distanceTo(to);
            for (double d = 0; d <= dist; d += 0.3) {
                double t = d / dist;
                Vec3 p = from.add(to.subtract(from).scale(t));
                sl.sendParticles(ModParticles.TENTACLE_WRITHE.get(),
                        p.x + (Math.random() - 0.5) * 0.3,
                        p.y + Math.sin(t * Math.PI * 2) * 0.3,
                        p.z + (Math.random() - 0.5) * 0.3,
                        1, 0, 0, 0, 0);
            }
            // Damage + grab effects
            le.hurt(sp.damageSources().magic(), 8);
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            // Knockback toward caster
            Vec3 toward = sp.position().subtract(le.position()).normalize().scale(0.8);
            le.setDeltaMovement(toward.x, 0.2, toward.z);
            le.hurtMarked = true;
            sl.playSound(null, sp.blockPosition(),
                    SoundEvents.SLIME_ATTACK, SoundSource.PLAYERS, 1.5F, 0.6F);
            sp.getCooldowns().addCooldown(this, 100);
            return InteractionResultHolder.consume(stack);
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oSigilo do Tentáculo").withStyle(ChatFormatting.LIGHT_PURPLE));
            t.add(Component.literal("§7Right-click numa criatura: tentáculo orgânico agarra."));
            t.add(Component.literal("§7Dano §c8§r§7 + §lSlow IV§r§7 + §lWeak II§r§7 + puxa pra você."));
            t.add(Component.literal("§c§oCD 5s"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 7. VOICE CURSE AMULET — amaldiçoado, distorce chat alheio
    // ════════════════════════════════════════════════════════════
    public static class VoiceCurseAmuletItem extends Item {
        public VoiceCurseAmuletItem(Properties p) { super(p.rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§4§oAmuleto da Voz Amaldiçoada").withStyle(ChatFormatting.DARK_RED));
            t.add(Component.literal("§c§l⚠ NÃO PODE SER SOLTO"));
            t.add(Component.literal("§7Seu chat aparece §odistorcido§r§7 pra outros players."));
            t.add(Component.literal("§7Players próximos ouvem §dwhispers§r§7 quando você fala."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"As palavras saem mas a língua não é mais sua.\""));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 8. SILENT WITNESS CLOAK — invisibilidade permanente amaldiçoada
    // ════════════════════════════════════════════════════════════
    public static class SilentWitnessCloakItem extends Item {
        public SilentWitnessCloakItem(Properties p) { super(p.rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            if (level.isClientSide) return;
            if (!(entity instanceof ServerPlayer sp)) return;
            if (level.getGameTime() % 40 != 0) return;

            // Aplica invisibilidade permanente
            sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
            // Mas com sanity drain crescente
            if (level.getGameTime() % 200 == 0) {
                InsanityData.addInsanity(sp, 1);
                InsanityData.addParanoia(sp, 1);
                // 10% chance whisper
                if (Math.random() < 0.10) {
                    HallucinationManager.force(sp, HallucinationType.FAKE_WHISPER, 0.6F, 30, "");
                }
            }
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oCapa da Testemunha Silenciosa").withStyle(ChatFormatting.LIGHT_PURPLE));
            t.add(Component.literal("§c§l⚠ NÃO PODE SER SOLTA"));
            t.add(Component.literal("§7Invisibilidade §lpermanente§r§7 enquanto carrega."));
            t.add(Component.literal("§c+1 insanity / 10s"));
            t.add(Component.literal("§c+1 paranoia / 10s"));
            t.add(Component.literal("§7Hallucinations aleatórias."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Você vê tudo. Nada mais vê você.\""));
        }
    }
}
