package br.com.murilo.liberthia.cosmic.artifacts;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * v0.1.22 r56: 15 novos <b>Cosmic Artifacts</b> — artefatos cósmicos com efeitos
 * misteriosos, tooltips crípticos e mecânicas que o player deve descobrir.
 *
 * <h2>Design rule</h2>
 * <ul>
 *   <li>Tooltips são §opoéticas§r, nunca explicam mecânica diretamente</li>
 *   <li>Cooldowns longos — uso estratégico</li>
 *   <li>Cada um tem custo (sanidade, dano, etc)</li>
 *   <li>Visual SFX sempre cósmico — soul, portal, smoke, ink</li>
 * </ul>
 *
 * <h2>Artefatos</h2>
 * <ol>
 *   <li>Pulled String — knockback reverso (atacante volta pra mais perto)</li>
 *   <li>Quiet Mark — marca inimigo, dano +50% por 10s</li>
 *   <li>Lonely Echo — som de player fake atrás de quem te olha</li>
 *   <li>Folded Distance — troca posição com a entidade mirada</li>
 *   <li>Throat Salt — silencia ataques mágicos no raio de 8b por 6s</li>
 *   <li>Soft Wound — próximo ataque seu é delayed 3s mas dá 3x dano</li>
 *   <li>Looking Glass — projétil que se aproxima volta na direção oposta</li>
 *   <li>Half Step — esquiva automática do primeiro ataque (10min cd)</li>
 *   <li>Bent Iron — armas inimigas têm 50% chance de perder durabilidade</li>
 *   <li>Pale Coin — quando você morre, drop +XP em vez de items</li>
 *   <li>Wet Bell — sino que põe slowness 4 em todos próximos por 5s</li>
 *   <li>Marrow Whistle — invoca esqueleto aliado que persiste 60s</li>
 *   <li>Listening Glass — vê HP de qualquer entidade mirada</li>
 *   <li>Sunken Ring — water breathing + slow fall infinitos</li>
 *   <li>Hand on Glass — segura o último item dropado quando morrer</li>
 * </ol>
 */
public final class CosmicArtifactsR56 {

    private CosmicArtifactsR56() {}

    // ─────────────────────────────────────────────────────
    // Helper base
    // ─────────────────────────────────────────────────────
    static abstract class CrypticItem extends Item {
        private final String[] lore;
        CrypticItem(Properties p, String... lore) {
            super(p.stacksTo(1).rarity(Rarity.RARE));
            this.lore = lore;
        }
        @Override public boolean isFoil(ItemStack s) { return true; }
        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            for (String line : lore) {
                t.add(Component.literal(line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
    }

    // 1. Pulled String — knockback reverso
    public static class PulledStringItem extends CrypticItem {
        public PulledStringItem(Properties p) {
            super(p,
                "§5§oUma linha sem fim.",
                "§8§oQuem te empurra, se aproxima.",
                "§8§oEquipe pra ativar.");
        }
    }

    // 2. Quiet Mark — marca alvo
    public static class QuietMarkItem extends CrypticItem {
        public QuietMarkItem(Properties p) {
            super(p,
                "§5§oUma marca silenciosa.",
                "§8§oVocê o vê. Ele não sabe.");
        }
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            LivingEntity target = br.com.murilo.liberthia.util.EntityRaycast.pickLiving(sp, 40.0);
            if (target == null) {
                return InteractionResultHolder.fail(stack);
            }
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            target.getPersistentData().putLong("liberthia.quiet_marked_until",
                    target.tickCount + 200L);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SQUID_INK,
                        target.getX(), target.getY() + 1, target.getZ(),
                        15, 0.3, 0.3, 0.3, 0.02);
            }
            sp.getCooldowns().addCooldown(this, 400);
            return InteractionResultHolder.consume(stack);
        }
    }

    // 3. Lonely Echo — som fake (passive via events)
    public static class LonelyEchoItem extends CrypticItem {
        public LonelyEchoItem(Properties p) {
            super(p,
                "§5§oUm passo que não é seu.",
                "§8§oO eco mente.");
        }
    }

    // 4. Folded Distance — swap position
    public static class FoldedDistanceItem extends CrypticItem {
        public FoldedDistanceItem(Properties p) {
            super(p,
                "§5§oA distância é maleável.",
                "§8§oShift+Right-click.");
        }
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (!sp.isShiftKeyDown()) return InteractionResultHolder.fail(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            LivingEntity target = br.com.murilo.liberthia.util.EntityRaycast.pickLiving(sp, 30.0);
            if (target == null) {
                return InteractionResultHolder.fail(stack);
            }
            // Swap positions
            double tx = target.getX(), ty = target.getY(), tz = target.getZ();
            double px = sp.getX(), py = sp.getY(), pz = sp.getZ();
            target.teleportTo(px, py, pz);
            sp.teleportTo(tx, ty, tz);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.PORTAL, tx, ty + 1, tz, 30, 0.3, 0.5, 0.3, 0.1);
                sl.sendParticles(ParticleTypes.PORTAL, px, py + 1, pz, 30, 0.3, 0.5, 0.3, 0.1);
            }
            sp.getCooldowns().addCooldown(this, 800);
            return InteractionResultHolder.consume(stack);
        }
    }

    // 5. Throat Salt — silences area magic
    public static class ThroatSaltItem extends CrypticItem {
        public ThroatSaltItem(Properties p) {
            super(p,
                "§5§oNada deve ser dito.",
                "§8§oRight-click pra exalar.");
        }
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            ServerLevel sl = (ServerLevel) level;
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class,
                    sp.getBoundingBox().inflate(8))) {
                if (le == sp) continue;
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2));
                le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                le.getPersistentData().putLong("liberthia.silenced_until",
                        le.tickCount + 120L);
            }
            sl.sendParticles(ParticleTypes.WHITE_ASH,
                    sp.getX(), sp.getY() + 1, sp.getZ(), 80, 4, 1.5, 4, 0.05);
            sp.getCooldowns().addCooldown(this, 600);
            return InteractionResultHolder.consume(stack);
        }
    }

    // 6. Soft Wound — delayed 3x damage
    public static class SoftWoundItem extends CrypticItem {
        public SoftWoundItem(Properties p) {
            super(p,
                "§5§oA dor que espera.",
                "§8§oAs vezes, espera ajuda.");
        }
    }

    // 7. Looking Glass — projectile reflect
    public static class LookingGlassItem extends CrypticItem {
        public LookingGlassItem(Properties p) {
            super(p,
                "§5§oA imagem volta.",
                "§8§oTudo que vem de longe.");
        }
    }

    // 8. Half Step — auto dodge first hit
    public static class HalfStepItem extends CrypticItem {
        public HalfStepItem(Properties p) {
            super(p,
                "§5§oUm passo não dado.",
                "§8§oEle não te pega. Uma vez.");
        }
    }

    // 9. Bent Iron — degrades enemy weapons
    public static class BentIronItem extends CrypticItem {
        public BentIronItem(Properties p) {
            super(p,
                "§5§oFerro que esquece sua forma.",
                "§8§oArmas em volta tornam-se cansadas.");
        }
    }

    // 10. Pale Coin — XP boost on death
    public static class PaleCoinItem extends CrypticItem {
        public PaleCoinItem(Properties p) {
            super(p,
                "§5§oVocê paga em outra moeda.",
                "§8§oQuando cai. Sempre.");
        }
    }

    // 11. Wet Bell — AoE slow
    public static class WetBellItem extends CrypticItem {
        public WetBellItem(Properties p) {
            super(p,
                "§5§oUm sino encharcado.",
                "§8§oRight-click pra tocar.");
        }
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            ServerLevel sl = (ServerLevel) level;
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class,
                    sp.getBoundingBox().inflate(12))) {
                if (le == sp) continue;
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
            }
            sl.sendParticles(ParticleTypes.SPLASH,
                    sp.getX(), sp.getY() + 1, sp.getZ(), 40, 1, 0.5, 1, 0.2);
            sl.playSound(null, sp.blockPosition(),
                    SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.5F, 0.4F);
            sp.getCooldowns().addCooldown(this, 400);
            return InteractionResultHolder.consume(stack);
        }
    }

    // 12. Marrow Whistle — summon skeleton ally
    public static class MarrowWhistleItem extends CrypticItem {
        public MarrowWhistleItem(Properties p) {
            super(p,
                "§5§oUm assobio de ossos.",
                "§8§oQuem responde, persiste pouco.");
        }
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            ServerLevel sl = (ServerLevel) level;
            var skel = net.minecraft.world.entity.EntityType.SKELETON.create(sl);
            if (skel != null) {
                Vec3 pos = sp.position().add(sp.getLookAngle().scale(2));
                skel.moveTo(pos.x, pos.y, pos.z, sp.getYRot(), 0);
                skel.setCustomName(Component.literal("§5Companheiro de Marfim"));
                skel.setCustomNameVisible(true);
                skel.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0));
                skel.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1));
                skel.setPersistenceRequired();
                skel.getPersistentData().putUUID("liberthia.marrow_master", sp.getUUID());
                skel.getPersistentData().putLong("liberthia.marrow_despawn_at",
                        sl.getGameTime() + 1200);
                sl.addFreshEntity(skel);
                sl.sendParticles(ParticleTypes.SOUL,
                        pos.x, pos.y + 1, pos.z, 30, 0.3, 0.5, 0.3, 0.05);
            }
            sp.getCooldowns().addCooldown(this, 2400);
            return InteractionResultHolder.consume(stack);
        }
    }

    // 13. Listening Glass — see HP
    public static class ListeningGlassItem extends CrypticItem {
        public ListeningGlassItem(Properties p) {
            super(p,
                "§5§oVocê escuta o que pulsa.",
                "§8§oRight-click pra mirar.");
        }
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

            LivingEntity target = br.com.murilo.liberthia.util.EntityRaycast.pickLiving(sp, 50.0);
            if (target == null) {
                sp.displayClientMessage(Component.literal("§7Você não escuta nada."), true);
                return InteractionResultHolder.fail(stack);
            }
            sp.displayClientMessage(Component.literal(
                    "§5§o[" + target.getDisplayName().getString() + "] §r§ehp = §f"
                            + (int)target.getHealth() + "§7/§f" + (int)target.getMaxHealth()), true);
            return InteractionResultHolder.consume(stack);
        }
    }

    // 14. Sunken Ring — water breathing + slow fall passive
    public static class SunkenRingItem extends CrypticItem {
        public SunkenRingItem(Properties p) {
            super(p,
                "§5§oAfogado, mas vivo.",
                "§8§oVocê não afunda. Você se equilibra.");
        }
    }

    // 15. Hand on Glass — preserve last item on death
    public static class HandOnGlassItem extends CrypticItem {
        public HandOnGlassItem(Properties p) {
            super(p,
                "§5§oUma mão te segura quando cai.",
                "§8§oUm item permanece. Apenas um.");
        }
    }
}
