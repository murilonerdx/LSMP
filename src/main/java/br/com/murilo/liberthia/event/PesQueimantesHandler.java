package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.compat.CuriosCompat;
import br.com.murilo.liberthia.item.FlameKeyItem;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks os Pés Queimantes equipados — aplica Speed III, deixa rastro
 * de fogo no chão e aplica debuffs em mobs nesse fogo.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class PesQueimantesHandler {

    private static final Map<UUID, BlockPos> LAST_POS = new HashMap<>();

    private PesQueimantesHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        ItemStack pes = CuriosCompat.findEquippedPesQueimantes(sp);
        if (pes.isEmpty()) {
            LAST_POS.remove(sp.getUUID());
            // v0.1.22 r13: desequipou — reset flag pra próxima vez ganhar key
            sp.getPersistentData().remove(NBT_KEY_GIVEN);
            return;
        }
        // v0.1.35: Speed IV (era III) — user pediu mais velocidade.
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 3, true, false, false));
        // Fire-immune (cobre dano de fogo, mas player ainda pega visual de fire).
        sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false, false));
        // v0.1.35: limpa animação de fogo no player (clearFire) — user pediu pra
        // não aparecer chamas no corpo dele com a bota ativa.
        if (sp.isOnFire()) sp.clearFire();

        // v0.1.35: spawna FlameKey periodicamente se botas equipadas e key não
        // está no inv. Garante que a key reapareça quando re-equipa as botas.
        if (sp.tickCount % 20 == 0) {
            giveFlameKeyOnce(sp);
        }

        // v0.1.35: durabilidade decai LENTAMENTE — 1 ponto a cada 10s.
        // Antes não decaia, agora "vai acabando aos poucos" conforme user pediu.
        if (sp.tickCount % 200 == 0) {
            CuriosCompat.damagePesQueimantes(sp, 1);
        }

        // Check toggle via FlameKey if present
        boolean fireEnabled = isFireEnabled(sp);
        if (!fireEnabled) {
            LAST_POS.put(sp.getUUID(), sp.blockPosition());
            return;
        }

        BlockPos cur = sp.blockPosition();
        BlockPos last = LAST_POS.get(sp.getUUID());
        if (last == null || !cur.equals(last)) {
            placeFireUnder(sp.serverLevel(), cur);
            LAST_POS.put(sp.getUUID(), cur);
        }

        // Affect mobs near fire — search within 4 block radius for fire trails.
        // Otimização: scan só a cada 10 ticks (efeitos duram 60t, então cobre).
        if (sp.tickCount % 10 == 0) {
            AABB area = new AABB(sp.getX() - 4, sp.getY() - 2, sp.getZ() - 4,
                    sp.getX() + 4, sp.getY() + 2, sp.getZ() + 4);
            List<LivingEntity> targets = sp.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != sp && e.isAlive());
            for (LivingEntity t : targets) {
                BlockPos tpos = t.blockPosition();
                if (sp.serverLevel().getBlockState(tpos).is(Blocks.FIRE)) {
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, true));
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
                    t.setSecondsOnFire(2);
                }
            }
        }
    }

    private static boolean isFireEnabled(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.FLAME_KEY.get())) {
                return FlameKeyItem.isFireEnabled(s);
            }
        }
        // No key — default on
        return true;
    }

    /**
     * v0.1.35: garante que a FlameKey esteja no inv enquanto botas equipadas.
     *
     * <p>v0.1.22 r13: FIX DUPLICATION. Antes spawnava UMA key quando o inv
     * não tinha — mas se o player DROPASSE a key, respawnava → INFINITAS.
     * Usa flag persistente NBT: spawna UMA VEZ ao equipar e NÃO respawna mais.
     * Desequipar reseta a flag.
     *
     * <p>report #81 (chaves infinitas): havia DUAS fontes de key — este handler
     * (com flag) E o {@code onEquip} do curio em {@code CuriosBridge}, que dava
     * key SEM dedup. Agora AMBOS chamam este helper idempotente. Dedup duplo:
     * (1) flag {@code NBT_KEY_GIVEN}; (2) {@code hasFlameKey} — nunca cria uma 2ª
     * se o player já tem qualquer FlameKey (mesmo que a flag tenha resetado ou a
     * key venha da outra fonte).
     */
    private static final String NBT_KEY_GIVEN = "liberthia.flame_key_given";

    public static void giveFlameKeyOnce(Player player) {
        if (player.level().isClientSide() || player.getAbilities().instabuild) return;
        var data = player.getPersistentData();
        if (data.getBoolean(NBT_KEY_GIVEN)) return; // já entregue nesta sessão de equip
        if (hasFlameKey(player)) { data.putBoolean(NBT_KEY_GIVEN, true); return; }
        ItemStack key = new ItemStack(ModItems.FLAME_KEY.get());
        FlameKeyItem.setOwner(key, player.getUUID());
        FlameKeyItem.setFireEnabled(key, true);
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, key);
        } else if (!player.getInventory().add(key)) {
            player.drop(key, false);
        }
        data.putBoolean(NBT_KEY_GIVEN, true);
    }

    private static boolean hasFlameKey(Player p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(ModItems.FLAME_KEY.get())) return true;
        }
        return false;
    }

    /**
     * v0.1.35: cancela TODO dano de fogo no player com pés queimantes
     * equipados. Defesa em profundidade — FIRE_RESISTANCE effect já cobre,
     * mas algumas sources (lava, magma block) podem ser exceções vanilla.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (CuriosCompat.findEquippedPesQueimantes(sp).isEmpty()) return;
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
        }
    }

    private static void placeFireUnder(ServerLevel level, BlockPos playerPos) {
        BlockPos at = playerPos;
        BlockPos below = at.below();
        // Place fire AT player feet position if floor below is solid
        if (level.getBlockState(at).isAir() && level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
            level.setBlockAndUpdate(at, Blocks.FIRE.defaultBlockState());
        }
    }
}
