package br.com.murilo.liberthia.magic.drops;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.factory.DynamicSpellItem;
import br.com.murilo.liberthia.magic.spell.SpellDef;
import br.com.murilo.liberthia.magic.spell.SpellLibrary;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * r156: Sistema de drop de spell scrolls baseado em raridade do mob.
 *
 * <p>Probabilidades por tier:
 * <ul>
 *   <li>Mob comum (Monster): 3% — UNCOMMON pool</li>
 *   <li>Raider/HP>50: 10% — RARE pool</li>
 *   <li>Boss (Wither/EnderDragon): 100% — EPIC pool</li>
 * </ul>
 *
 * <p>Drop só ocorre se kill foi causada por Player.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SpellDropHandler {

    private SpellDropHandler() {}

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel sl)) return;
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof Player)) return;

        float chance;
        List<Rarity> pool = new ArrayList<>();

        if (dead instanceof WitherBoss || dead instanceof EnderDragon) {
            chance = 1.0F;
            pool.add(Rarity.EPIC);
            pool.add(Rarity.RARE);
        } else if (dead.getMaxHealth() > 50 || dead instanceof Raider) {
            chance = 0.10F;
            pool.add(Rarity.RARE);
            pool.add(Rarity.UNCOMMON);
        } else if (dead instanceof Monster) {
            chance = 0.03F;
            pool.add(Rarity.UNCOMMON);
            pool.add(Rarity.COMMON);
        } else {
            return;
        }

        if (sl.getRandom().nextFloat() > chance) return;

        Rarity pickRarity = pool.get(sl.getRandom().nextInt(pool.size()));
        List<String> candidates = new ArrayList<>();
        for (SpellDef def : SpellLibrary.all()) {
            if (def.rarity == pickRarity) candidates.add(def.id);
        }
        if (candidates.isEmpty()) {
            for (SpellDef def : SpellLibrary.all()) candidates.add(def.id);
            if (candidates.isEmpty()) return;
        }

        String chosen = candidates.get(sl.getRandom().nextInt(candidates.size()));
        ItemStack drop = DynamicSpellItem.stackFor(ModItems.FACTORY_SPELL_SCROLL.get(), chosen);

        BlockPos pos = dead.blockPosition();
        net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(
                sl, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
        ie.setDefaultPickUpDelay();
        sl.addFreshEntity(ie);
    }
}
