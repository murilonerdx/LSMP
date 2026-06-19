package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.tech.JetpackItem;
import br.com.murilo.liberthia.item.tech.TechEnergy;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.JetpackActiveC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r180c — lado CLIENTE do jetpack (só carrega no client: {@code value = Dist.CLIENT}).
 * Lê a tecla PULAR e aplica o impulso vertical ao jogador local (movimento de player é
 * autoritativo no cliente); avisa o servidor p/ drenar FE. Nunca é carregado no servidor
 * dedicado → sem risco de crash de dist.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JetpackClientHandler {
    private JetpackClientHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer pl = mc.player;
        if (pl == null || e.player != pl) return;
        if (pl.getAbilities().flying) return; // não interfere no voo criativo

        ItemStack chest = pl.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof JetpackItem) || !TechEnergy.has(chest)) return;
        if (!mc.options.keyJump.isDown()) return;

        Vec3 m = pl.getDeltaMovement();
        double up = Math.min(0.62, m.y + 0.18);
        pl.setDeltaMovement(m.x * 1.03, up, m.z * 1.03);
        pl.fallDistance = 0;
        pl.hasImpulse = true;

        // partículas de propulsão (cliente)
        Vec3 feet = pl.position();
        for (int i = 0; i < 2; i++)
            pl.level().addParticle(ParticleTypes.FLAME,
                    feet.x + (pl.getRandom().nextDouble() - 0.5) * 0.4,
                    feet.y + 0.1,
                    feet.z + (pl.getRandom().nextDouble() - 0.5) * 0.4,
                    0, -0.1, 0);

        ModNetwork.CHANNEL.sendToServer(new JetpackActiveC2SPacket());
    }
}
