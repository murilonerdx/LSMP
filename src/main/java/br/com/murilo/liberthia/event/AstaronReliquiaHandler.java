package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.compat.CuriosCompat;
import br.com.murilo.liberthia.item.AstaronAccessKeyItem;
import br.com.murilo.liberthia.item.ReliquiaProtecaoAstaronItem;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hook que dispara a Relíquia de Proteção de Astaron quando o player vai
 * tomar dano fatal/maior que 50% do max HP.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AstaronReliquiaHandler {

    private AstaronReliquiaHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        float amount = event.getAmount();
        float max = sp.getMaxHealth();

        // v0.1.22 r14: gatilho aceita MUITO mais tipos de dano:
        //   - Dano "grande" (≥ 50% do max HP) — hits pesados de mob/PvP/queda
        //   - Dano FATAL (≥ HP atual) — qualquer source que mataria (mesmo 1 ❤
        //     de drown/fire que finaliza). User pediu: "deve salvar de
        //     afogamento, fogo, todo tipo de dano".
        boolean isFatal = amount >= sp.getHealth();
        boolean isBig = amount >= max * 0.5f;
        if (!isFatal && !isBig) return;

        ItemStack relic = CuriosCompat.findEquippedReliquiaAstaron(sp);
        if (relic.isEmpty()) return;
        int charges = ReliquiaProtecaoAstaronItem.getCharges(relic);
        if (charges <= 0) return;

        long now = sp.level().getGameTime();
        long lastTrigger = ReliquiaProtecaoAstaronItem.getLastTrigger(relic);
        if (now - lastTrigger < ReliquiaProtecaoAstaronItem.TRIGGER_COOLDOWN_TICKS) return;

        // Trigger — consume charge, save position, teleport to safe spot
        ReliquiaProtecaoAstaronItem.setCharges(relic, charges - 1);
        ReliquiaProtecaoAstaronItem.setLastTrigger(relic, now);

        // Cancel damage
        event.setCanceled(true);
        sp.setHealth(Math.max(sp.getHealth(), max * 0.3f));
        // Limpa estado de "morrendo" pra não re-aplicar no próximo tick
        sp.clearFire();           // se tava queimando, apaga
        sp.setAirSupply(sp.getMaxAirSupply()); // restaura ar pra afogamento
        sp.fallDistance = 0;

        BlockPos saved = sp.blockPosition();
        ResourceLocation dim = sp.level().dimension().location();

        // v0.1.22 r14: TELEPORT INTELIGENTE baseado no tipo de dano.
        // - Afogamento: procura terra SECA próxima (raio 32) — não joga
        //   no spawn, fica perto do incidente.
        // - Fogo/lava: procura local sem fogo num raio, preferindo
        //   proximidade a água (apaga + impede re-ignição).
        // - Outros (hit/queda/etc): respawn ou world spawn (comportamento
        //   original).
        ServerLevel safeLevel = sp.serverLevel();
        BlockPos finalPos;

        var src = event.getSource();
        boolean isDrowning = src.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING);
        boolean isFire = src.is(net.minecraft.tags.DamageTypeTags.IS_FIRE);

        if (isDrowning) {
            finalPos = findDryGroundNear(safeLevel, saved);
            sp.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§b≋ Astaron te tira da água").withStyle(
                            net.minecraft.ChatFormatting.AQUA), true);
        } else if (isFire) {
            finalPos = findFireSafeSpot(safeLevel, saved);
            sp.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§b🔥 Astaron te resgata das chamas").withStyle(
                            net.minecraft.ChatFormatting.GOLD), true);
        } else {
            // Default: respawn position ou world spawn
            BlockPos safePos = null;
            BlockPos respawn = sp.getRespawnPosition();
            if (respawn != null && sp.getRespawnDimension() != null) {
                ServerLevel rs = sp.server.getLevel(sp.getRespawnDimension());
                if (rs != null) {
                    safeLevel = rs;
                    safePos = respawn;
                }
            }
            if (safePos == null) {
                safePos = safeLevel.getSharedSpawnPos();
            }
            finalPos = findSafePos(safeLevel, safePos);
            sp.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Astaron te protege.")
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
                    true);
        }

        sp.teleportTo(safeLevel, finalPos.getX() + 0.5, finalPos.getY(), finalPos.getZ() + 0.5,
                sp.getYRot(), sp.getXRot());
        sp.fallDistance = 0;

        safeLevel.playSound(null, finalPos, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        safeLevel.sendParticles(ParticleTypes.PORTAL,
                finalPos.getX() + 0.5, finalPos.getY() + 1.0, finalPos.getZ() + 0.5,
                40, 0.6, 1.0, 0.6, 0.5);

        // Regen + resistance + absorption (cura pós-resgate)
        sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, true));
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4, false, true));
        sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2, false, true));
        if (isFire) {
            sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, true));
        }
        if (isDrowning) {
            sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0, false, true));
        }

        // Atualiza key com return pos (sem dropar nova — anti-dup)
        ItemStack existingKey = findAccessKey(sp);
        if (existingKey.isEmpty()) {
            ItemStack key = new ItemStack(ModItems.ASTARON_ACCESS_KEY.get());
            AstaronAccessKeyItem.setOwner(key, sp.getUUID());
            AstaronAccessKeyItem.setReturnPos(key, dim, saved);
            if (!sp.getInventory().add(key)) sp.drop(key, false);
        } else {
            AstaronAccessKeyItem.setReturnPos(existingKey, dim, saved);
        }
    }

    /**
     * v0.1.22 r14: defesa em profundidade — se outro handler converteu o
     * dano pra LivingDamageEvent (ou se o ataque era unblockable como
     * /kill, void, magic kill) e nosso onHurt não pegou, tentamos
     * recuperar aqui antes da morte. Mesmo trigger que onHurt mas no
     * estágio DAMAGE.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        // Só atua se for fatal e o sp ainda tá vivo (LivingHurt não pegou).
        if (event.getAmount() < sp.getHealth()) return;

        ItemStack relic = CuriosCompat.findEquippedReliquiaAstaron(sp);
        if (relic.isEmpty()) return;
        int charges = ReliquiaProtecaoAstaronItem.getCharges(relic);
        if (charges <= 0) return;

        long now = sp.level().getGameTime();
        long lastTrigger = ReliquiaProtecaoAstaronItem.getLastTrigger(relic);
        if (now - lastTrigger < ReliquiaProtecaoAstaronItem.TRIGGER_COOLDOWN_TICKS) return;

        // Reduz dano a 0 e cura — força não morrer
        event.setAmount(0f);
        sp.setHealth(Math.max(sp.getHealth(), sp.getMaxHealth() * 0.3f));
        // Trigger via simulated LivingHurt — reaproveita a lógica completa
        net.minecraftforge.event.entity.living.LivingHurtEvent fake =
                new net.minecraftforge.event.entity.living.LivingHurtEvent(
                        sp, event.getSource(), sp.getMaxHealth());
        onHurt(fake);
    }

    /**
     * v0.1.35: garante AccessKey no inv enquanto player tem Relíquia equipada.
     * Sem return pos no início (aguardando trigger).
     *
     * <p>v0.1.22 r13: FIX DUPLICATION. Antes spawnava UMA key por player
     * quando o inv não tinha — mas se o player DROPASSE a key, o inv ficava
     * sem ela e respawnava → INFINITAS keys. Agora usa flag persistente
     * NBT no player: spawna uma única vez ao equipar a relíquia, e NÃO
     * respawna mais. Se desequipar e re-equipar, flag reseta → nova key.
     */
    private static final String NBT_KEY_GIVEN = "liberthia.astaron_key_given";

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return; // a cada 1s
        ItemStack relic = CuriosCompat.findEquippedReliquiaAstaron(sp);
        var data = sp.getPersistentData();
        if (relic.isEmpty()) {
            // Desequipou — reset flag pra próxima vez ganhar nova key
            data.remove(NBT_KEY_GIVEN);
            return;
        }
        // Já entregou key nesta sessão de equipamento? Não respawna.
        if (data.getBoolean(NBT_KEY_GIVEN)) return;
        // Spawna key sem return pos + marca flag
        ItemStack key = new ItemStack(ModItems.ASTARON_ACCESS_KEY.get());
        AstaronAccessKeyItem.setOwner(key, sp.getUUID());
        if (!sp.getInventory().add(key)) sp.drop(key, false);
        data.putBoolean(NBT_KEY_GIVEN, true);
    }

    private static ItemStack findAccessKey(Player p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(ModItems.ASTARON_ACCESS_KEY.get())) return s;
        }
        return ItemStack.EMPTY;
    }

    private static BlockPos findSafePos(ServerLevel level, BlockPos start) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(start.getX(), start.getY(), start.getZ());
        for (int dy = 0; dy < 30; dy++) {
            if (level.getBlockState(m).isAir() && level.getBlockState(m.above()).isAir()) {
                return m.immutable();
            }
            m.move(0, 1, 0);
        }
        return start;
    }

    /**
     * v0.1.22 r14: procura terra seca (sem água) próxima ao ponto de
     * afogamento. Spiral search no plano XZ, raio até 32 blocos. Pra cada
     * candidato, sobe verticalmente até achar bloco com:
     *   - bloco em pé NÃO líquido
     *   - 2 blocos de ar acima
     *   - bloco abaixo sólido (chão)
     * Se não achar, fallback pro spawn do level.
     */
    private static BlockPos findDryGroundNear(ServerLevel level, BlockPos drowningAt) {
        for (int r = 1; r <= 32; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // só borda do quadrado
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int x = drowningAt.getX() + dx;
                    int z = drowningAt.getZ() + dz;
                    int topY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos ground = new BlockPos(x, topY, z);
                    var below = level.getBlockState(ground.below());
                    if (below.getFluidState().isSource() || below.getFluidState().getAmount() > 0) continue;
                    if (!below.isFaceSturdy(level, ground.below(), net.minecraft.core.Direction.UP)) continue;
                    if (!level.getBlockState(ground).isAir()) continue;
                    if (!level.getBlockState(ground.above()).isAir()) continue;
                    if (level.getBlockState(ground).getFluidState().getAmount() > 0) continue;
                    return ground;
                }
            }
        }
        return level.getSharedSpawnPos();
    }

    /**
     * v0.1.22 r14: procura local sem fogo/lava próxima. Critério:
     *   - chão sólido, 2 blocos de ar acima
     *   - sem bloco de FIRE/SOUL_FIRE/MAGMA_BLOCK/LAVA num raio 5
     *   - bônus: prefere lugar com água a até 8 blocos (apaga player +
     *     impede re-ignição imediata).
     */
    private static BlockPos findFireSafeSpot(ServerLevel level, BlockPos burningAt) {
        BlockPos best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int r = 2; r <= 32; r += 2) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int x = burningAt.getX() + dx;
                    int z = burningAt.getZ() + dz;
                    int topY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos ground = new BlockPos(x, topY, z);
                    var below = level.getBlockState(ground.below());
                    if (!below.isFaceSturdy(level, ground.below(), net.minecraft.core.Direction.UP)) continue;
                    if (!level.getBlockState(ground).isAir()) continue;
                    if (!level.getBlockState(ground.above()).isAir()) continue;
                    if (hasFireNearby(level, ground, 5)) continue;

                    // Score: longe do fogo original (+), perto de água (+)
                    int score = (int) ground.distSqr(burningAt) / 10;
                    if (hasWaterNearby(level, ground, 8)) score += 50;
                    if (score > bestScore) {
                        bestScore = score;
                        best = ground;
                    }
                }
            }
            if (best != null) return best;
        }
        return level.getSharedSpawnPos();
    }

    private static boolean hasFireNearby(ServerLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    var s = level.getBlockState(m);
                    if (s.is(net.minecraft.world.level.block.Blocks.FIRE)
                            || s.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)
                            || s.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                            || s.is(net.minecraft.world.level.block.Blocks.LAVA)
                            || s.getFluidState().getType() == net.minecraft.world.level.material.Fluids.LAVA
                            || s.getFluidState().getType() == net.minecraft.world.level.material.Fluids.FLOWING_LAVA) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasWaterNearby(ServerLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.getBlockState(m).getFluidState().getType() == net.minecraft.world.level.material.Fluids.WATER
                            || level.getBlockState(m).getFluidState().getType() == net.minecraft.world.level.material.Fluids.FLOWING_WATER) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
