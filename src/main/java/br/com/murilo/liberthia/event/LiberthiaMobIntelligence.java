package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * <b>Inteligência global de perseguição</b> pros mobs HOSTIS do Liberthia.
 *
 * <p>Se o alvo se esconde atrás de blocos (parede) ou sobe (torre), o mob
 * <b>quebra o caminho pra alcançá-lo</b> — escavação/subida estilo o Caçador,
 * mas aplicado a TODOS os mobs hostis do mod de uma vez, sem reescrever cada
 * entidade. Mobs com AI própria de SmartBrainLib (Caçador/Espreitador) são
 * pulados (já cavam sozinhos). Respeita o gamerule {@code mobGriefing}.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class LiberthiaMobIntelligence {

    private LiberthiaMobIntelligence() {}

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity le = event.getEntity();
        if (le.level().isClientSide) return;
        if (!(le instanceof Mob mob)) return;
        if (!(mob instanceof Enemy)) return;                                   // só hostis
        if (mob instanceof net.tslat.smartbrainlib.api.SmartBrainOwner) return; // já tem AI própria
        if ((mob.tickCount & 7) != 0) return;                                   // throttle ~1x/8 ticks

        // Só mobs registrados pelo Liberthia (não mexe em mobs vanilla/outros mods).
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (id == null || !id.getNamespace().equals(LiberthiaMod.MODID)) return;

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;
        if (mob.distanceToSqr(target) > 22.0 * 22.0) return;

        boolean blockedHoriz = mob.horizontalCollision;
        boolean targetAbove = target.getY() > mob.getY() + 1.2;
        if (!blockedHoriz && !targetAbove) return;

        if (!(mob.level() instanceof ServerLevel sl)) return;
        if (!sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;  // respeita mobGriefing

        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        Direction dir = Math.abs(dx) > Math.abs(dz)
                ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);

        BlockPos base = mob.blockPosition();
        boolean broke = tryBreak(sl, mob, base.relative(dir));
        broke = tryBreak(sl, mob, base.relative(dir).above()) || broke;
        if (targetAbove) {
            broke = tryBreak(sl, mob, base.above(2)) || broke;
            broke = tryBreak(sl, mob, base.relative(dir).above(2)) || broke;
        }
        if (broke) {
            mob.getJumpControl().jump();
        }
    }

    private static boolean tryBreak(ServerLevel sl, Mob mob, BlockPos pos) {
        BlockState state = sl.getBlockState(pos);
        if (state.isAir()) return false;
        if (state.is(BlockTags.WITHER_IMMUNE)) return false;   // bedrock/barrier/etc
        if (state.hasBlockEntity()) return false;              // não destrói baús/máquinas
        float hardness = state.getDestroySpeed(sl, pos);
        if (hardness < 0 || hardness > 5.0F) return false;     // inquebrável ou muito duro
        sl.destroyBlock(pos, true, mob);
        sl.playSound(null, pos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 0.5F, 0.7F);
        return true;
    }
}
