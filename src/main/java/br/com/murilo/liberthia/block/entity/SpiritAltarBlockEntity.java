package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.entity.SoulBodyEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * v0.1.22 r24: BE do Spirit Altar — gerencia o ritual de 100 ticks (5s) que
 * envia players num raio pro Spirit World.
 *
 * <h2>Estado</h2>
 * <ul>
 *   <li>{@code initiatorUuid}: UUID do player que começou o ritual (nullable)</li>
 *   <li>{@code progressTicks}: 0-100. Avança por tick enquanto initiator
 *       estiver no raio. Reset a 0 se sair.</li>
 * </ul>
 *
 * <h2>Animação</h2>
 * <ul>
 *   <li>0-30t: partículas SOUL leves + som AMBIENT_CAVE</li>
 *   <li>30-70t: PORTAL particles + SOUL_FIRE_FLAME + som ENCHANTMENT_TABLE</li>
 *   <li>70-99t: erupção SOUL massiva + drum beat</li>
 *   <li>100t: TELEPORT MASSIVO — todos players no raio vão pro spirit</li>
 * </ul>
 */
public class SpiritAltarBlockEntity extends BlockEntity {

    private static final int TOTAL_TICKS = 100; // 5s
    private static final double RADIUS = 3.0;

    @org.jetbrains.annotations.Nullable
    private UUID initiatorUuid = null;
    private int progressTicks = 0;

    public SpiritAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPIRIT_ALTAR.get(), pos, state);
    }

    public boolean isActive() {
        return initiatorUuid != null;
    }

    public int getProgressTicks() {
        return progressTicks;
    }

    /** Inicia o ritual. Chamado pelo {@link br.com.murilo.liberthia.block.SpiritAltarBlock#use}. */
    public void startRitual(UUID who) {
        this.initiatorUuid = who;
        this.progressTicks = 0;
        setChanged();
    }

    public void cancelRitual(String reason) {
        if (level instanceof ServerLevel sl && initiatorUuid != null) {
            ServerPlayer init = sl.getServer().getPlayerList().getPlayer(initiatorUuid);
            if (init != null) {
                init.displayClientMessage(Component.literal(
                        "§c✦ Ritual cancelado: §7" + reason
                                + " §7(§a+10 XP refund§7)").withStyle(ChatFormatting.RED), true);
                // r26 BUGFIX: refund dos 10 níveis XP descontados no use().
                // Player não deve perder XP por cancel involuntário (saiu do
                // raio sem querer, logoff, etc).
                init.giveExperienceLevels(10);
            }
            sl.playSound(null, worldPosition, SoundEvents.GLASS_BREAK,
                    SoundSource.BLOCKS, 0.5F, 0.6F);
        }
        this.initiatorUuid = null;
        this.progressTicks = 0;
        setChanged();
    }

    /** Tick server-side. Chamado pelo Block.getTicker. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, SpiritAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel sl)) return;
        if (!altar.isActive()) return;
        ServerPlayer init = sl.getServer().getPlayerList().getPlayer(altar.initiatorUuid);
        if (init == null) {
            altar.cancelRitual("iniciador desconectou");
            return;
        }
        // Verifica que iniciador está no raio
        if (init.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > RADIUS * RADIUS) {
            altar.cancelRitual("você saiu do raio");
            return;
        }
        altar.progressTicks++;
        altar.animationTick(sl);
        if (altar.progressTicks >= TOTAL_TICKS) {
            altar.executeRitual(sl);
        }
    }

    private void animationTick(ServerLevel sl) {
        // Particles concentricos no centro do altar
        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.6;
        double cz = worldPosition.getZ() + 0.5;
        // Fases:
        if (progressTicks < 30) {
            // Build-up: SOUL leve
            if (progressTicks % 5 == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy + 0.5, cz,
                        2, 0.3, 0.3, 0.3, 0.02);
            }
        } else if (progressTicks < 70) {
            // Mid: PORTAL spiral + SOUL_FIRE
            double t = (progressTicks - 30) / 40.0;
            double angle = t * Math.PI * 6;
            double r = 1.5 + Math.sin(progressTicks * 0.1) * 0.3;
            for (int i = 0; i < 4; i++) {
                double a = angle + i * Math.PI / 2;
                double px = cx + Math.cos(a) * r;
                double pz = cz + Math.sin(a) * r;
                sl.sendParticles(ParticleTypes.PORTAL, px, cy + t, pz,
                        1, 0, 0, 0, 0);
            }
            if (progressTicks % 10 == 0) {
                sl.playSound(null, worldPosition,
                        SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS, 0.4F, 0.5F + (float) t);
            }
        } else if (progressTicks < TOTAL_TICKS) {
            // Final: erupção SOUL massiva
            sl.sendParticles(ParticleTypes.SOUL, cx, cy + 1.0, cz,
                    8, 1.0, 1.0, 1.0, 0.1);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy + 2.0, cz,
                    4, 0.8, 0.5, 0.8, 0.15);
            if (progressTicks % 5 == 0) {
                sl.playSound(null, worldPosition,
                        SoundEvents.BEACON_AMBIENT,
                        SoundSource.BLOCKS, 0.6F, 0.4F);
            }
        }
    }

    private void executeRitual(ServerLevel sl) {
        // Coleta todos players no raio
        AABB box = new AABB(worldPosition).inflate(RADIUS);
        var players = sl.getEntitiesOfClass(ServerPlayer.class, box);

        for (ServerPlayer sp : players) {
            // r25 BUGFIX: skip players que já estão em spirit (orphan body bug)
            if (SpiritDimension.isInSpiritWorld(sp)) {
                sp.displayClientMessage(Component.literal(
                        "§7Você já está no mundo espiritual."), true);
                continue;
            }
            // r25 BUGFIX: limpa body órfão antes de criar novo
            var oldData = sp.getPersistentData();
            if (oldData.hasUUID(SpiritDimension.NBT_BODY_UUID)) {
                java.util.UUID oldUuid = oldData.getUUID(SpiritDimension.NBT_BODY_UUID);
                for (var lvl : sp.server.getAllLevels()) {
                    var e = lvl.getEntity(oldUuid);
                    if (e != null) {
                        e.discard();
                        break;
                    }
                }
                oldData.remove(SpiritDimension.NBT_BODY_UUID);
            }

            // Cria SoulBody + manda pra spirit
            SoulBodyEntity body = ModEntities.SOUL_BODY.get().create(sl);
            if (body == null) continue;
            body.moveTo(sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), sp.getXRot());
            body.setOwnerUuid(sp.getUUID());
            body.setOwnerName(sp.getName().getString());
            body.setCustomName(Component.literal(sp.getName().getString()));
            body.setCustomNameVisible(true);
            body.setHealth(sp.getHealth());
            sl.addFreshEntity(body);

            sp.getPersistentData().putUUID(SpiritDimension.NBT_BODY_UUID, body.getUUID());
            // Custo de sanidade — ritual mais forte = mais sanity (perdem 30)
            SpiritDimension.addSanity(sp, -30);
            // r25 BUGFIX: sync inicial pro HUD aparecer imediato
            br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                    new br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket(
                            SpiritDimension.getSanity(sp)));

            SpiritDimension.enterSpiritWorld(sp);
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ O ritual te separou do mundo físico.").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }

        // Explosão final de partículas + som
        sl.sendParticles(ParticleTypes.SOUL,
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                80, RADIUS, 1.0, RADIUS, 0.5);
        sl.playSound(null, worldPosition,
                SoundEvents.WITHER_DEATH, SoundSource.BLOCKS, 0.5F, 0.7F);
        sl.playSound(null, worldPosition,
                SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.5F, 0.4F);

        // Reset
        this.initiatorUuid = null;
        this.progressTicks = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (initiatorUuid != null) {
            tag.putUUID("InitiatorUuid", initiatorUuid);
        }
        tag.putInt("Progress", progressTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("InitiatorUuid")) {
            this.initiatorUuid = tag.getUUID("InitiatorUuid");
        }
        this.progressTicks = tag.getInt("Progress");
    }
}
