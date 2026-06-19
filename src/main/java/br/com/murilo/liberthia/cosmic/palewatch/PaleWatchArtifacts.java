package br.com.murilo.liberthia.cosmic.palewatch;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * v0.1.22 r55: <b>Pale Watch Artifacts</b> — items espirituais avançados
 * pra dimensão Spirit / Loom. Itens raros que precisam de novos minérios.
 *
 * <h2>Items</h2>
 * <ul>
 *   <li>{@link CloneArmyItem}: spawna 5 cópias suas que atacam quem te ferir</li>
 *   <li>{@link StaredownPendantItem}: se alguém te encara muito, mouse dele
 *       gira sozinho por 8s (curios)</li>
 *   <li>{@link PaleBlinkPendantItem}: shift+rclick teleporta atrás do alvo</li>
 *   <li>{@link ParalyzePendantItem}: quando você toma dano, atacante fica
 *       imóvel por 3s</li>
 *   <li>{@link SpiritGuideItem}: aponta pra portal de saída do spirit world</li>
 * </ul>
 *
 * <h2>Lore</h2>
 * Itens cunhados com Pale Riftite / Umbral / Voidite (minérios do spirit world).
 * Cada um carrega uma fração da capacidade da dimensão de re-escrever as regras
 * da realidade. Use com cuidado — sanidade desce ao usar.
 */
public final class PaleWatchArtifacts {

    private PaleWatchArtifacts() {}

    // ──────────── Clone Army ────────────

    /**
     * r58 FIX: Spawna até 5 cópias REAIS do player (ReflectionEntity com skin,
     * armor, hand copiados). Elas atacam quem ferir o invocador.
     */
    public static class CloneArmyItem extends Item {
        public CloneArmyItem(Properties p) { super(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)); }

        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

            // Cooldown 5 min
            if (sp.getCooldowns().isOnCooldown(this)) {
                sp.displayClientMessage(Component.literal("§5✦ Em recarga..."), true);
                return InteractionResultHolder.fail(stack);
            }

            ServerLevel sl = (ServerLevel) level;
            // r58: Spawna 5 ReflectionEntities (cópias REAIS do player) em volta
            int spawned = 0;
            for (int i = 0; i < 5; i++) {
                double a = (i / 5.0) * Math.PI * 2;
                double r = 2.5;
                double sx = sp.getX() + Math.cos(a) * r;
                double sz = sp.getZ() + Math.sin(a) * r;
                var reflection = br.com.murilo.liberthia.registry.ModEntities
                        .REFLECTION_ENTITY.get().create(sl);
                if (reflection == null) continue;
                reflection.moveTo(sx, sp.getY(), sz, sp.getYRot(), 0);
                // Copia skin via OWNER_UUID
                try {
                    var ownerField = reflection.getClass().getMethod("setOwnerUuid", java.util.UUID.class);
                    ownerField.invoke(reflection, sp.getUUID());
                } catch (Throwable ignored) {
                    // Fallback: NBT
                    reflection.getPersistentData().putUUID("liberthia.owner_uuid", sp.getUUID());
                }
                // Copia armadura, mainhand, offhand
                reflection.copyEquipmentFrom(sp);
                // Custom name "Cópia Pálida"
                reflection.setCustomName(Component.literal("§5§lCópia Pálida"));
                reflection.setCustomNameVisible(true);
                // Lifetime longo + AI offensiva
                reflection.spawnedTick = sl.getGameTime();
                reflection.getPersistentData().putUUID("liberthia.clone_master", sp.getUUID());
                reflection.getPersistentData().putBoolean("liberthia.attack_aggressors", true);
                reflection.getPersistentData().putBoolean("liberthia.explodes_on_death", true);
                reflection.getPersistentData().putLong("liberthia.clone_despawn_at",
                        sl.getGameTime() + 6000); // 5 min lifetime
                // Boost stats
                reflection.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6000, 2));
                reflection.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 2));
                reflection.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 1));
                reflection.setHealth(20);
                // Não-invulnerável (pra explodir ao morrer)
                reflection.setInvulnerable(false);
                sl.addFreshEntity(reflection);

                // Particles spawn
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        sx, sp.getY() + 1, sz, 20, 0.3, 0.5, 0.3, 0.05);
                spawned++;
            }

            sp.displayClientMessage(Component.literal(
                    "§5§l✦ Cinco Cópias Pálidas surgiram para te proteger."), false);
            sp.getCooldowns().addCooldown(this, 6000); // 5 min
            // Custo de sanidade
            try {
                br.com.murilo.liberthia.dimension.SpiritDimension.addSanity(sp, -15);
            } catch (Throwable ignored) {}
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oExército Pálido").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Cinco eles. Você é apenas um."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"A multidão é a forma mais antiga de oração.\""));
        }
    }

    // ──────────── Staredown Pendant ────────────

    /**
     * Curio: se um player olha você por mais de 3s, o mouse dele vira
     * sozinho por 8s. Damage também.
     *
     * <p>Quando equipado, registra o player como "watched" no
     * {@link br.com.murilo.liberthia.cosmic.staredown.StaredownPendantTracker}.
     */
    public static class StaredownPendantItem extends Item {
        public StaredownPendantItem(Properties p) { super(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)); }

        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oOlhar Reverso").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Quem te observa, observa demais."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"O olhar não é gratuito.\""));
        }
    }

    // ──────────── Pale Blink Pendant ────────────

    /**
     * Curios: shift+rclick teleporta o player pra atrás do mob/player que ele
     * está mirando, dentro de 30 blocos. Cooldown 15s.
     */
    public static class PaleBlinkPendantItem extends Item {
        public PaleBlinkPendantItem(Properties p) { super(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)); }

        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (!sp.isShiftKeyDown()) {
                sp.displayClientMessage(Component.literal(
                        "§7Use shift+right-click pra teleportar atrás do alvo."), true);
                return InteractionResultHolder.fail(stack);
            }
            if (sp.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.fail(stack);
            }

            // r179 FIX: raycast de entidade real (Entity.pick() só pega blocos)
            LivingEntity target = br.com.murilo.liberthia.util.EntityRaycast.pickLiving(sp, 30.0);
            if (target == null) {
                sp.displayClientMessage(Component.literal("§7Mire em uma entidade primeiro."), true);
                return InteractionResultHolder.fail(stack);
            }

            // Calcula posição atrás do target
            Vec3 look = target.getLookAngle();
            Vec3 behind = target.position().subtract(look.scale(1.5));
            sp.teleportTo(behind.x, behind.y, behind.z);
            // Yaw aponta pro target
            sp.setYRot(target.getYRot());

            // VFX
            ServerLevel sl = (ServerLevel) level;
            sl.sendParticles(ParticleTypes.PORTAL,
                    behind.x, behind.y + 1, behind.z, 30, 0.3, 0.5, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.SOUL,
                    behind.x, behind.y + 1, behind.z, 15, 0.2, 0.4, 0.2, 0.02);

            sp.getCooldowns().addCooldown(this, 300); // 15s
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oRespiração Pálida").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7A distância não te pertence."));
            t.add(Component.literal("§7Shift+Right-click: §dteleporta§r§7 atrás do alvo"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Você sempre esteve atrás.\""));
        }
    }

    // ──────────── Paralyze Pendant ────────────

    /**
     * Curios passivo: quando você toma dano de uma entidade, ela fica
     * imóvel por 3s (slowness 6 + jump prevent).
     * Cooldown 10s pra não ser broken.
     */
    public static class ParalyzePendantItem extends Item {
        public ParalyzePendantItem(Properties p) { super(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)); }

        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oParalisia Branca").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Quem te toca, congela."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"O contato é um contrato.\""));
        }
    }

    // ──────────── Spirit Guide ────────────

    /**
     * Item que dá indicação visual de onde fica um portal de saída pro
     * overworld quando o player está no spirit world. Particles trail.
     */
    public static class SpiritGuideItem extends Item {
        public SpiritGuideItem(Properties p) { super(p.stacksTo(1)); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

            if (!br.com.murilo.liberthia.dimension.SpiritDimension.isInSpiritWorld(sp)) {
                sp.displayClientMessage(Component.literal(
                        "§7§oA bússola só funciona no Outro Lado."), true);
                return InteractionResultHolder.fail(stack);
            }

            // Aponta pra return pos
            Vec3 returnPos = br.com.murilo.liberthia.dimension.SpiritDimension.getSavedReturnPos(sp);
            if (returnPos == null) {
                sp.displayClientMessage(Component.literal(
                        "§4§oVocê foi esquecido — não há retorno."), true);
                return InteractionResultHolder.fail(stack);
            }
            // Particles trail apontando
            Vec3 dir = returnPos.subtract(sp.position()).normalize();
            ServerLevel sl = (ServerLevel) level;
            for (int i = 0; i < 30; i++) {
                Vec3 p = sp.position().add(dir.scale(i * 0.5));
                sl.sendParticles(ParticleTypes.END_ROD,
                        p.x, p.y + 1, p.z, 1, 0.05, 0.05, 0.05, 0);
            }

            double dist = sp.position().distanceTo(returnPos);
            sp.displayClientMessage(Component.literal(
                    "§e§l✦ Caminho do retorno: §r§e" + (int)dist + " blocos"), true);

            sp.getCooldowns().addCooldown(this, 100); // 5s
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§e§oGuia Pálida").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Aponta pra costura de retorno."));
            t.add(Component.literal("§7Só funciona no Outro Lado."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Todo abismo tem um lado.\""));
        }
    }
}
