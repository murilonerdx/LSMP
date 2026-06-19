package br.com.murilo.liberthia.telemetry.events;

import br.com.murilo.liberthia.telemetry.TelemetryManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Coletor de eventos Forge → PlayerEvent → TelemetryManager.record().
 *
 * Princípio:
 *  - Apenas extrai dados — não processa, não classifica.
 *  - LOWEST priority pra não atrapalhar outros mods.
 *  - Throttle de MOVE feito no TelemetryManager (não aqui).
 *
 * Eventos cobertos no MVP:
 *  ✓ Login / Logout
 *  ✓ Movement (via PlayerTickEvent + comparação de posição)
 *  ✓ Damage dealt / taken / death / kill
 *  ✓ Block break / place
 *  ✓ Container open
 *  ✓ Craft
 *  ✓ Dimension change
 *
 * TODO (próxima iteração):
 *  - Chat: AsyncPlayerChatEvent (já tem em CommunityHooks — duplicar com cuidado)
 *  - Inventory slot change: SlotClickEvent
 *  - Jump / Fall: custom (PlayerTickEvent + yMotion check)
 *  - Player nearby: scan a cada 5s
 */
public class EventCollector {

    // ===== Lifecycle do server (start/stop do telemetry manager) =====

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent ev) {
        TelemetryManager.start(ev.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent ev) {
        TelemetryManager.stop();
    }

    // ===== Session =====

    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent ev) {
        if (!TelemetryManager.isActive()) return;
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        TelemetryManager.get().onPlayerLogin(p.getUUID());
        PlayerEvent e = PlayerEvent.builder(EventType.LOGIN, p.getUUID())
                .at(p.getX(), p.getY(), p.getZ())
                .looking(p.getYRot(), p.getXRot())
                .inDim(p.level().dimension().location().toString())
                .data("name", p.getName().getString())
                .build();
        TelemetryManager.get().record(e);
    }

    @SubscribeEvent
    public void onLogout(PlayerLoggedOutEvent ev) {
        if (!TelemetryManager.isActive()) return;
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        PlayerEvent e = PlayerEvent.builder(EventType.LOGOUT, p.getUUID())
                .at(p.getX(), p.getY(), p.getZ())
                .inDim(p.level().dimension().location().toString())
                .build();
        TelemetryManager.get().record(e);
        TelemetryManager.get().onPlayerLogout(p.getUUID());
    }

    @SubscribeEvent
    public void onDimChange(PlayerChangedDimensionEvent ev) {
        if (!TelemetryManager.isActive()) return;
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        PlayerEvent e = PlayerEvent.builder(EventType.DIMENSION_CHANGE, p.getUUID())
                .at(p.getX(), p.getY(), p.getZ())
                .inDim(ev.getTo().location().toString())
                .data("from", ev.getFrom().location().toString())
                .build();
        TelemetryManager.get().record(e);
    }

    // ===== Movement via tick (pra capturar X/Y/Z + rotação) =====

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerTick(TickEvent.PlayerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        if (!TelemetryManager.isActive()) return;
        if (!(ev.player instanceof ServerPlayer p)) return;
        // Throttle aplicado no TelemetryManager — aqui só envia.
        PlayerEvent e = PlayerEvent.builder(EventType.MOVE, p.getUUID())
                .at(p.getX(), p.getY(), p.getZ())
                .looking(p.getYRot(), p.getXRot())
                .inDim(p.level().dimension().location().toString())
                .build();
        TelemetryManager.get().record(e);
    }

    // ===== Combat =====

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent ev) {
        if (!TelemetryManager.isActive()) return;
        LivingEntity target = ev.getEntity();
        var source = ev.getSource().getEntity();

        // Dano DADO por player
        if (source instanceof ServerPlayer attacker) {
            PlayerEvent e = PlayerEvent.builder(EventType.DAMAGE_DEALT, attacker.getUUID())
                    .at(attacker.getX(), attacker.getY(), attacker.getZ())
                    .inDim(attacker.level().dimension().location().toString())
                    .data("damage", ev.getAmount())
                    .data("targetType", target.getType().builtInRegistryHolder().key().location().toString())
                    .data("weapon", attacker.getMainHandItem().getItem().builtInRegistryHolder().key().location().toString())
                    .build();
            TelemetryManager.get().record(e);
        }

        // Dano RECEBIDO por player
        if (target instanceof ServerPlayer victim) {
            PlayerEvent e = PlayerEvent.builder(EventType.DAMAGE_TAKEN, victim.getUUID())
                    .at(victim.getX(), victim.getY(), victim.getZ())
                    .inDim(victim.level().dimension().location().toString())
                    .data("damage", ev.getAmount())
                    .data("damageType", ev.getSource().getMsgId())
                    .data("sourceType", source != null ? source.getType().builtInRegistryHolder().key().location().toString() : "unknown")
                    .build();
            TelemetryManager.get().record(e);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent ev) {
        if (!TelemetryManager.isActive()) return;
        LivingEntity target = ev.getEntity();
        var killer = ev.getSource().getEntity();

        if (target instanceof ServerPlayer victim) {
            PlayerEvent e = PlayerEvent.builder(EventType.DEATH, victim.getUUID())
                    .at(victim.getX(), victim.getY(), victim.getZ())
                    .inDim(victim.level().dimension().location().toString())
                    .data("cause", ev.getSource().getMsgId())
                    .data("killer", killer != null ? killer.getType().builtInRegistryHolder().key().location().toString() : "unknown")
                    .build();
            TelemetryManager.get().record(e);
        }
        if (killer instanceof ServerPlayer slayer) {
            PlayerEvent e = PlayerEvent.builder(EventType.KILL, slayer.getUUID())
                    .at(slayer.getX(), slayer.getY(), slayer.getZ())
                    .inDim(slayer.level().dimension().location().toString())
                    .data("targetType", target.getType().builtInRegistryHolder().key().location().toString())
                    .build();
            TelemetryManager.get().record(e);
        }
    }

    // ===== World =====

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent ev) {
        if (!TelemetryManager.isActive()) return;
        Player p = ev.getPlayer();
        if (!(p instanceof ServerPlayer sp)) return;
        PlayerEvent e = PlayerEvent.builder(EventType.BLOCK_BREAK, sp.getUUID())
                .at(ev.getPos().getX(), ev.getPos().getY(), ev.getPos().getZ())
                .inDim(sp.level().dimension().location().toString())
                .data("block", ev.getState().getBlock().builtInRegistryHolder().key().location().toString())
                .build();
        TelemetryManager.get().record(e);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent ev) {
        if (!TelemetryManager.isActive()) return;
        if (!(ev.getEntity() instanceof ServerPlayer sp)) return;
        PlayerEvent e = PlayerEvent.builder(EventType.BLOCK_PLACE, sp.getUUID())
                .at(ev.getPos().getX(), ev.getPos().getY(), ev.getPos().getZ())
                .inDim(sp.level().dimension().location().toString())
                .data("block", ev.getPlacedBlock().getBlock().builtInRegistryHolder().key().location().toString())
                .build();
        TelemetryManager.get().record(e);
    }

    // ===== Inventory =====

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open ev) {
        if (!TelemetryManager.isActive()) return;
        if (!(ev.getEntity() instanceof ServerPlayer sp)) return;
        // Tudo que não seja o inv vanilla do player (player inv = sempre aberta)
        // a maioria dos containers cai aqui. Diferencia inv vs container pelo
        // tipo do menu.
        EventType type = EventType.CONTAINER_OPEN;
        try {
            String menuType = ev.getContainer().getType().toString();
            if (menuType.contains("inventory")) type = EventType.INVENTORY_OPEN;
        } catch (Exception ignored) {}
        PlayerEvent e = PlayerEvent.builder(type, sp.getUUID())
                .at(sp.getX(), sp.getY(), sp.getZ())
                .inDim(sp.level().dimension().location().toString())
                .build();
        TelemetryManager.get().record(e);
    }

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close ev) {
        if (!TelemetryManager.isActive()) return;
        if (!(ev.getEntity() instanceof ServerPlayer sp)) return;
        PlayerEvent e = PlayerEvent.builder(EventType.INVENTORY_CLOSE, sp.getUUID())
                .at(sp.getX(), sp.getY(), sp.getZ())
                .inDim(sp.level().dimension().location().toString())
                .build();
        TelemetryManager.get().record(e);
    }

    @SubscribeEvent
    public void onCraft(ItemCraftedEvent ev) {
        if (!TelemetryManager.isActive()) return;
        if (!(ev.getEntity() instanceof ServerPlayer sp)) return;
        PlayerEvent e = PlayerEvent.builder(EventType.CRAFT, sp.getUUID())
                .at(sp.getX(), sp.getY(), sp.getZ())
                .inDim(sp.level().dimension().location().toString())
                .data("item", ev.getCrafting().getItem().builtInRegistryHolder().key().location().toString())
                .data("count", ev.getCrafting().getCount())
                .build();
        TelemetryManager.get().record(e);
    }
}
