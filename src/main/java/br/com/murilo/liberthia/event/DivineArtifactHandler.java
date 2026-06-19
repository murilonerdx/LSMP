package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r180: aplica os efeitos dos artefatos divinos. Funciona com o item na mão, no
 * inventário OU equipado num slot Curios.
 *
 * <ul>
 *   <li><b>Selo da Ascensão</b>: queda lenta sempre + regeneração sob o sol; ativa a
 *       flag {@code liberthia.ascended} (gating de áreas divinas).</li>
 *   <li><b>Coroa da Eternidade</b>: regeneração divina SE puro (sanidade ≥ 60); senão rejeita.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class DivineArtifactHandler {

    public static final String NBT_ASCENDED = "liberthia.ascended";
    private static final int PURITY_THRESHOLD = 60;
    /** Raio da ÁREA DIVINA projetada por um Santuário da Ordem. */
    private static final int DIVINE_RADIUS = 8;

    private DivineArtifactHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return; // 1×/s

        boolean hasSeal = has(sp, ModItems.ASCENSION_SEAL.get());
        boolean hasCrown = has(sp, ModItems.ETERNITY_CROWN.get());

        // Selo da Ascensão — favor divino + flag de permissão
        sp.getPersistentData().putBoolean(NBT_ASCENDED, hasSeal);
        if (hasSeal) {
            sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false));
            boolean sunlit = sp.level().isDay() && !sp.level().isRaining()
                    && sp.level().canSeeSky(sp.blockPosition().above());
            if (sunlit) {
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, false));
            }
        }

        // Coroa da Eternidade — regeneração divina SE puro
        if (hasCrown) {
            if (SpiritDimension.getSanity(sp) >= PURITY_THRESHOLD) {
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, true, false));
                sp.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, true, false));
            } else if (sp.tickCount % 100 == 0) {
                sp.displayClientMessage(Component.literal("§cA Coroa da Eternidade rejeita os impuros..."), true);
            }
        }

        // ── GATE de ÁREA DIVINA: o Santuário da Ordem projeta uma zona sagrada. ──
        // "Só os marcados podem subir": com o Selo → bem-vindo; sem ele → REJEITADO.
        if (!sp.isCreative() && !sp.isSpectator()) {
            BlockPos shrine = findNearbyShrine(sp);
            if (shrine != null) {
                if (hasSeal) {
                    sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
                } else {
                    Vec3 away = new Vec3(sp.getX() - (shrine.getX() + 0.5), 0, sp.getZ() - (shrine.getZ() + 0.5));
                    if (away.lengthSqr() < 1.0e-3) {
                        away = new Vec3(sp.getRandom().nextDouble() - 0.5, 0, sp.getRandom().nextDouble() - 0.5);
                    }
                    away = away.normalize();
                    sp.push(away.x * 0.7, 0.28, away.z * 0.7); // empurra pra FORA da área
                    sp.hurtMarked = true;
                    sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, true, false));
                    sp.hurt(sp.damageSources().magic(), 2.0F); // ira divina (ignora armadura)
                    if (sp.tickCount % 60 == 0) {
                        sp.displayClientMessage(Component.literal(
                                "§e✦ §rA presença divina te rejeita — você não é §emarcado§r."), true);
                    }
                }
            }
        }
    }

    /** Santuário da Ordem mais próximo dentro do raio divino (ou null). */
    private static BlockPos findNearbyShrine(ServerPlayer sp) {
        BlockPos c = sp.blockPosition();
        Block shrine = ModBlocks.ORDER_SHRINE.get();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -DIVINE_RADIUS; dx <= DIVINE_RADIUS; dx++) {
            for (int dz = -DIVINE_RADIUS; dz <= DIVINE_RADIUS; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (sp.level().getBlockState(m).is(shrine)) {
                        double d = c.distSqr(m);
                        if (d < bestD) { bestD = d; best = m.immutable(); }
                    }
                }
            }
        }
        return best;
    }

    /** True se o player tem o item na mão/inventário OU equipado num slot Curios. */
    private static boolean has(Player p, Item item) {
        if (p.getInventory().contains(new net.minecraft.world.item.ItemStack(item))) return true;
        try {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                    .findFirstCurio(p, s -> s.is(item)).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
