package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.antimagic.AntiMagic;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r183 — corrige o bug de "efeitos anti-magia continuam após morrer/desequipar":
 * <ul>
 *   <li><b>Morte</b> ({@link PlayerEvent.Clone} isWasDeath): remove o efeito Antimagia +
 *       zera os markers de supressão/conjuração no player que respawna.</li>
 *   <li><b>Trocar equipamento</b> ({@link LivingEquipmentChangeEvent}): se o player não está
 *       mais usando NENHUMA peça anti-magia/Bastião, o efeito Antimagia residual é removido —
 *       ele só permanece enquanto há item equipado/fonte ativa.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AntiMagicDeathHandler {
    private AntiMagicDeathHandler() {}

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (!e.isWasDeath()) return;
        Player np = e.getEntity();
        Player orig = e.getOriginal();
        if (ModEffects.ANTIMAGIA.get() != null) {
            np.removeEffect(ModEffects.ANTIMAGIA.get());
            if (orig != null) orig.removeEffect(ModEffects.ANTIMAGIA.get());
        }
        var d = np.getPersistentData();
        d.putInt(AntiMagic.NBT_SUPPRESS, 0);
        d.putInt(AntiMagic.NBT_CASTFLASH, 0);
        // preserva o contador de mineração entre mortes (dreno de sanidade)
        if (orig != null) d.putInt("liberthia.sanityMineCount", orig.getPersistentData().getInt("liberthia.sanityMineCount"));
    }

    @SubscribeEvent
    public static void onEquipChange(LivingEquipmentChangeEvent e) {
        if (!(e.getEntity() instanceof Player p) || p.level().isClientSide) return;
        // Antimagia só faz sentido com gear anti-magia equipado OU fonte ativa (campo).
        // Ao desequipar, se não há mais nenhuma peça anti-magia/Bastião nem supressão ativa, remove o resíduo.
        if (AntiMagic.isSuppressed(p)) return; // campo ativo mantém
        if (hasAntiMagicGear(p)) return;       // ainda equipado mantém
        if (p.hasEffect(ModEffects.ANTIMAGIA.get())) {
            var inst = p.getEffect(ModEffects.ANTIMAGIA.get());
            if (inst != null && inst.getDuration() > 4) p.removeEffect(ModEffects.ANTIMAGIA.get());
        }
    }

    private static boolean hasAntiMagicGear(Player p) {
        for (var st : p.getArmorSlots()) {
            var it = st.getItem();
            if (it instanceof br.com.murilo.liberthia.item.tech.WardedArmorItem
                    || it instanceof br.com.murilo.liberthia.item.AntiMagicArmorItem) return true;
        }
        return false;
    }
}
