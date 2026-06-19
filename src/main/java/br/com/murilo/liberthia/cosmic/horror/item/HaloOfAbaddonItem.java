package br.com.murilo.liberthia.cosmic.horror.item;

import br.com.murilo.liberthia.cosmic.framework.HorrorFramework;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * r81 — <b>Halo of Abaddon</b> (Religious Horror artifact).
 *
 * <p>Mantém o player com um anel de END_ROD particles girando acima da cabeça.
 * Boost passivo de exposure RELIGIOUS pra player e qualquer player próximo.
 * Mobs próximos olham pro player sempre que ele está perto.
 *
 * <p>Mantido em inventário (não precisa equipar).
 */
public class HaloOfAbaddonItem extends Item {

    public HaloOfAbaddonItem(Properties props) {
        super(props.stacksTo(1).durability(0));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer sp)) return;
        if (level.getGameTime() % 5 != 0) return;

        // Particles em anel acima da cabeça
        double angle = (level.getGameTime() / 4.0) % (Math.PI * 2);
        if (level instanceof ServerLevel sl) {
            for (int i = 0; i < 4; i++) {
                double a = angle + (i / 4.0) * Math.PI * 2;
                double rx = sp.getX() + Math.cos(a) * 0.8;
                double ry = sp.getY() + 2.3;
                double rz = sp.getZ() + Math.sin(a) * 0.8;
                sl.sendParticles(ParticleTypes.END_ROD, rx, ry, rz, 1, 0, 0, 0, 0);
            }
            // Eye particle on top
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    sp.getX(), sp.getY() + 2.7, sp.getZ(),
                    1, 0.1, 0, 0.1, 0);
        }

        // Mobs próximos ENCARAM o player fixamente (intimidação angélica).
        // Roda a cada 5t (não 60t) e re-aplica head+body rot + LookControl pra
        // "segurar" o olhar contra a IA do mob, que tenta virar a cabeça todo tick.
        // (Antes só rodava 1x a cada 3s e só setava head rot — a IA desfazia no
        // tick seguinte, então parecia que não funcionava.)
        var mobs = sp.serverLevel().getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                sp.getBoundingBox().inflate(16),
                e -> e != sp && e.isAlive()
                        && !(e instanceof net.minecraft.world.entity.player.Player));
        for (var e : mobs) {
            double dx = sp.getX() - e.getX();
            double dz = sp.getZ() - e.getZ();
            double dy = sp.getEyeY() - e.getEyeY();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));
            e.setYHeadRot(yaw);
            e.yHeadRotO = yaw;
            e.setYBodyRot(yaw);
            e.setYRot(yaw);
            e.setXRot(pitch);
            if (e instanceof net.minecraft.world.entity.Mob mob) {
                mob.getLookControl().setLookAt(sp.getX(), sp.getEyeY(), sp.getZ(), 180F, 180F);
            }
        }

        // A cada 60t, ganha exposure RELIGIOUS lentamente.
        if (level.getGameTime() % 60 == 0) {
            HorrorFramework.getState(sp).addExposure(HorrorType.RELIGIOUS, 1.0F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Halo angélico flutuante."));
        tooltip.add(Component.literal("§eMobs próximos te observam fixamente."));
        tooltip.add(Component.literal("§5§oAcumula horror RELIGIOSO em quem carrega."));
    }
}
