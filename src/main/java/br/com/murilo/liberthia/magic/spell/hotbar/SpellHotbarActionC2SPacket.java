package br.com.murilo.liberthia.magic.spell.hotbar;

import br.com.murilo.liberthia.magic.spell.CastContext;
import br.com.murilo.liberthia.magic.spell.SpellDef;
import br.com.murilo.liberthia.magic.spell.SpellLevels;
import br.com.murilo.liberthia.magic.spell.SpellLibrary;
import br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem;
import br.com.murilo.liberthia.magic.spell.composition.SpellComposition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.162 r138: <b>SpellHotbarActionC2SPacket</b> — client manda 2 ações:
 * <ul>
 *   <li>{@code ACTION_CAST}: castar spell do slot N (segurar tecla Z/X/C)</li>
 *   <li>{@code ACTION_BIND}: bindar scroll da main hand no slot N (Shift+Z/X/C)</li>
 * </ul>
 */
public class SpellHotbarActionC2SPacket {

    public static final byte ACTION_CAST = 0;
    public static final byte ACTION_BIND = 1;
    public static final byte ACTION_CLEAR = 2;

    public final byte action;
    public final byte slot;

    public SpellHotbarActionC2SPacket(byte action, byte slot) {
        this.action = action;
        this.slot = slot;
    }

    public static void encode(SpellHotbarActionC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.action);
        buf.writeByte(pkt.slot);
    }

    public static SpellHotbarActionC2SPacket decode(FriendlyByteBuf buf) {
        return new SpellHotbarActionC2SPacket(buf.readByte(), buf.readByte());
    }

    public static void handle(SpellHotbarActionC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            if (pkt.slot < 0 || pkt.slot >= SpellHotbarData.SLOTS) return;
            switch (pkt.action) {
                case ACTION_CAST -> handleCast(sp, pkt.slot);
                case ACTION_BIND -> handleBind(sp, pkt.slot);
                case ACTION_CLEAR -> handleClear(sp, pkt.slot);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleBind(ServerPlayer sp, byte slot) {
        ItemStack main = sp.getMainHandItem();

        // r164 fix: aceita os DOIS tipos — Universal (prebuilt) E DynamicSpellItem (factory)
        String spellId = null;
        if (main.getItem() instanceof UniversalSpellScrollItem scroll) {
            spellId = scroll.spellId(main);
        } else if (main.getItem() instanceof br.com.murilo.liberthia.magic.factory.DynamicSpellItem) {
            spellId = br.com.murilo.liberthia.magic.factory.DynamicSpellItem.spellId(main);
        }

        if (spellId == null) {
            sp.displayClientMessage(Component.literal(
                "§e⚠ Segure um Spell Scroll (prebuilt ou factory) na mão pra bindar"), true);
            return;
        }

        // Copia composition tag se existir (só faz sentido pra UniversalSpellScrollItem)
        CompoundTag compTag = null;
        if (main.hasTag() && main.getTag().contains(SpellComposition.NBT_COMPOSITION)) {
            compTag = main.getTag().getCompound(SpellComposition.NBT_COMPOSITION).copy();
        }
        SpellHotbarData.bind(sp, slot, spellId, compTag);
        SpellDef d = SpellLibrary.get(spellId);
        String name = d == null ? spellId : d.displayName().getString();
        sp.displayClientMessage(Component.literal(
            "§a✓ Bindou §e" + name + " §a→ slot " + (slot + 1)), true);
        // Sync state pro client
        br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                SpellHotbarSyncS2CPacket.snapshot(sp));
    }

    private static void handleClear(ServerPlayer sp, byte slot) {
        SpellHotbarData.clear(sp, slot);
        sp.displayClientMessage(Component.literal(
            "§7Slot " + (slot + 1) + " limpo"), true);
        br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                SpellHotbarSyncS2CPacket.snapshot(sp));
    }

    private static void handleCast(ServerPlayer sp, byte slot) {
        String spellId = SpellHotbarData.getSpellId(sp, slot);
        if (spellId == null) {
            sp.displayClientMessage(Component.literal(
                "§7Slot " + (slot + 1) + " vazio — shift+tecla com scroll na mão pra bindar"), true);
            return;
        }
        SpellDef d = SpellLibrary.get(spellId);
        if (d == null) {
            sp.displayClientMessage(Component.literal(
                "§c⚠ Spell desconhecido: " + spellId), true);
            return;
        }

        // Construct ItemStack "virtual" pra reusar resolução de cooldown/composition.
        // Usa SPELL_FIREBALL como base mas overrida spell_id via NBT (UniversalSpellScrollItem
        // checa NBT antes do defaultSpellId)
        ItemStack virtualScroll = new ItemStack(
                br.com.murilo.liberthia.registry.ModItems.SPELL_FIREBALL.get());
        virtualScroll.getOrCreateTag().putString("liberthia.spell_id", spellId);
        CompoundTag compTag = SpellHotbarData.getCompositionTag(sp, slot);
        if (compTag != null) {
            virtualScroll.getTag().put(SpellComposition.NBT_COMPOSITION, compTag);
        }

        // Mana cost (sem armor mult discount pra simplificar — usa global only)
        int spellLevel = SpellLevels.getLevel(virtualScroll);
        float globalMult = 0.70F;
        float armorMult = br.com.murilo.liberthia.magic.armor.ManaArmorEffects.costMultiplierFor(sp);
        int finalMana = (int)(d.manaCost * globalMult * armorMult
                * SpellLevels.manaMultiplier(spellLevel));
        int finalCd = (int)(d.cooldownTicks * SpellLevels.cooldownMultiplier(spellLevel));

        // Use SpellHotbarItem cooldown shared key
        if (sp.getCooldowns().isOnCooldown(virtualScroll.getItem())) return;

        int sourceCur = br.com.murilo.liberthia.observation.source.SourceData.get(sp);
        if (sourceCur < finalMana) {
            sp.displayClientMessage(Component.literal(
                "§c⚠ Source insuficiente §7(" + sourceCur + "/" + finalMana + ")"), true);
            return;
        }

        ServerLevel sl = sp.serverLevel();
        SpellDef levDef = SpellDef.builder(d.id)
                .name(d.name).school(d.school).rarity(d.rarity)
                .mana(finalMana).cooldown(finalCd)
                .damage(d.damage * SpellLevels.damageMultiplier(spellLevel))
                .range(d.range * SpellLevels.rangeMultiplier(spellLevel))
                .lore(d.lore).cast(d.cast).build();
        CastContext ctx2 = new CastContext(sp, sl, InteractionHand.MAIN_HAND, virtualScroll, levDef);
        boolean ok;
        try {
            ok = levDef.cast.execute(ctx2);
            // Composition post-cast
            var comp = SpellComposition.fromStack(virtualScroll);
            if (ok && comp != null && comp.isComposed()) {
                br.com.murilo.liberthia.magic.spell.composition.CompositionEffectApplier
                        .applyPostCast(ctx2, comp);
                LivingEntity tgt = ctx2.pickTarget(Math.min(levDef.range, 24));
                if (tgt != null) {
                    br.com.murilo.liberthia.magic.spell.composition.CompositionEffectApplier
                            .applyOnHit(ctx2, tgt, comp);
                }
            }
        } catch (Throwable t) {
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.warn(
                "[SpellHotbar] cast error for {}: {}", d.id, t.toString());
            ok = false;
        }
        if (!ok) return;

        sp.getCooldowns().addCooldown(virtualScroll.getItem(), finalCd);
        br.com.murilo.liberthia.observation.source.SourceData.consume(sp, finalMana);
        sl.playSound(null, sp.blockPosition(),
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.6F);
    }
}
