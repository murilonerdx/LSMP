package br.com.murilo.liberthia.magic.custom;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r166 FIX: os feitiços customizados ficam em {@code player.getPersistentData()},
 * que sobrevive a logout/restart do servidor — mas o Forge <b>NÃO copia</b> esse
 * tag ao respawnar (morte) nem ao trocar de dimensão. Por isso os feitiços
 * "sumiam". Aqui copiamos os feitiços (e o selecionado) do player antigo pro novo
 * em {@link PlayerEvent.Clone}, garantindo que persistam por morte/dimensão.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CustomSpellPersistence {

    private CustomSpellPersistence() {}

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        try {
            CompoundTag oldData = event.getOriginal().getPersistentData();
            CompoundTag newData = event.getEntity().getPersistentData();

            if (oldData.contains(CustomSpellStorage.NBT_KEY)) {
                newData.put(CustomSpellStorage.NBT_KEY, oldData.get(CustomSpellStorage.NBT_KEY).copy());
            }
            if (oldData.hasUUID(CustomSpellStorage.NBT_SELECTED)) {
                newData.putUUID(CustomSpellStorage.NBT_SELECTED, oldData.getUUID(CustomSpellStorage.NBT_SELECTED));
            }
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.debug("[CustomSpell] clone-copy falhou: {}", t.toString());
        }
    }
}
