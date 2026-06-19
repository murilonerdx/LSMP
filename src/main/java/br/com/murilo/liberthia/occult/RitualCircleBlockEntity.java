package br.com.murilo.liberthia.occult;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * v0.1.22 r32: BE do ritual circle — toda lógica de detecção, validação e
 * execução de rituais.
 */
public class RitualCircleBlockEntity extends BlockEntity {

    /** Raio em blocos pra detectar chalks, candles, items. */
    public static final int SCAN_RADIUS = 4;

    private String activeRitualName = "";
    private long ritualStartTick = -1;
    private int ritualDurationTicks = 0;
    private java.util.UUID ritualCasterUuid;

    public RitualCircleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RITUAL_CIRCLE.get(), pos, state);
    }

    public boolean isRitualActive() {
        return ritualStartTick >= 0 && !activeRitualName.isEmpty();
    }

    /** Server-side tick — gerencia ritual ativo. */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   RitualCircleBlockEntity be) {
        if (!be.isRitualActive()) return;
        long elapsed = level.getGameTime() - be.ritualStartTick;

        // Particles + som
        ServerLevel sl = (ServerLevel) level;
        if (elapsed % 5 == 0) {
            for (int i = 0; i < 8; i++) {
                double a = (level.getGameTime() / 5.0 + i * 45) * Math.PI / 180;
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        pos.getX() + 0.5 + Math.cos(a) * 1.5,
                        pos.getY() + 0.5 + (Math.sin(elapsed * 0.1) * 0.3),
                        pos.getZ() + 0.5 + Math.sin(a) * 1.5,
                        1, 0, 0.02, 0, 0);
            }
        }
        if (elapsed % 20 == 0) {
            sl.playSound(null, pos, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS,
                    0.5F, 0.3F);
        }

        // Completed?
        if (elapsed >= be.ritualDurationTicks) {
            be.completeRitual(sl);
        }
    }

    /** Tenta iniciar um ritual com o sigilo fornecido. */
    public InteractionResult attemptStartRitual(Player player, ItemStack sigilStack) {
        if (isRitualActive()) {
            player.displayClientMessage(Component.literal(
                    "§cRitual em andamento. Aguarde."), true);
            return InteractionResult.FAIL;
        }
        RitualRegistry.Ritual ritual = RitualRegistry.findBySigil(sigilStack.getItem());
        if (ritual == null) {
            player.displayClientMessage(Component.literal(
                    "§cEste sigilo não está catalogado."), true);
            return InteractionResult.FAIL;
        }
        // Valida recursos
        ValidationResult val = validate(ritual);
        if (!val.success) {
            player.displayClientMessage(Component.literal(
                    "§c✗ " + val.error), true);
            // Mostra requisitos
            showRitualRequirements(player, ritual);
            return InteractionResult.FAIL;
        }
        // Consome itens
        for (ItemEntity ie : val.itemsToConsume) ie.discard();

        // Inicia ritual
        activeRitualName = ritual.name;
        ritualStartTick = level.getGameTime();
        ritualDurationTicks = ritual.durationTicks;
        ritualCasterUuid = player.getUUID();
        setChanged();

        ((ServerLevel) level).sendParticles(ParticleTypes.LARGE_SMOKE,
                worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5,
                30, 1.0, 0.5, 1.0, 0.05);
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.5F, 0.5F);
        player.displayClientMessage(Component.literal(
                "§5§l✦ §r§5RITUAL INICIADO: §d" + ritual.name + "§r§5 §7(" +
                        (ritual.durationTicks / 20) + "s)").withStyle(ChatFormatting.LIGHT_PURPLE), false);

        LiberthiaMod.LOGGER.info("[Ritual] {} iniciou '{}' em {}",
                player.getName().getString(), ritual.name, worldPosition);
        return InteractionResult.CONSUME;
    }

    /** Mostra status atual + chalks/candles detectados. */
    public void showStatus(Player player) {
        if (isRitualActive()) {
            long left = ritualDurationTicks - (level.getGameTime() - ritualStartTick);
            player.displayClientMessage(Component.literal(
                    "§5✦ Ritual ativo: §d" + activeRitualName + "§7 — §e" +
                            (left / 20) + "s restantes"), true);
            return;
        }
        AABB box = new AABB(worldPosition).inflate(SCAN_RADIUS);
        Map<OccultItems.ChalkColor, Integer> chalks = countChalks(box);
        Map<OccultItems.ChalkColor, Integer> candles = countCandles(box);
        int items = level.getEntitiesOfClass(ItemEntity.class, box).size();

        player.displayClientMessage(Component.literal(
                "§5✦ §dRitual Circle §7§oinativo"), false);
        StringBuilder sb = new StringBuilder("§7Chalks: ");
        for (var e : chalks.entrySet()) sb.append("§f").append(e.getValue()).append(" ")
                .append(e.getKey().displayName).append("§7, ");
        player.displayClientMessage(Component.literal(sb.toString()), false);
        sb = new StringBuilder("§7Velas ACESAS: ");
        for (var e : candles.entrySet()) sb.append("§f").append(e.getValue()).append(" ")
                .append(e.getKey().displayName).append("§7, ");
        player.displayClientMessage(Component.literal(sb.toString()), false);
        player.displayClientMessage(Component.literal(
                "§7Items soltos: §f" + items), false);
        player.displayClientMessage(Component.literal(
                "§8Segure um §dSigilo§r§8 e click-direito pra começar."), false);
    }

    private void showRitualRequirements(Player player, RitualRegistry.Ritual r) {
        player.displayClientMessage(Component.literal(
                "§5✦ §dRequisitos: §o" + r.description), false);
        StringBuilder sb = new StringBuilder("§7Chalks: ");
        Map<OccultItems.ChalkColor, Long> need = r.chalkColors.stream()
                .collect(java.util.stream.Collectors.groupingBy(c -> c,
                        java.util.stream.Collectors.counting()));
        for (var e : need.entrySet()) sb.append("§f").append(e.getValue()).append("× ")
                .append(e.getKey().displayName).append("§7, ");
        player.displayClientMessage(Component.literal(sb.toString()), false);
        sb = new StringBuilder("§7Velas ACESAS: ");
        Map<OccultItems.ChalkColor, Long> needC = r.candleColors.stream()
                .collect(java.util.stream.Collectors.groupingBy(c -> c,
                        java.util.stream.Collectors.counting()));
        for (var e : needC.entrySet()) sb.append("§f").append(e.getValue()).append("× ")
                .append(e.getKey().displayName).append("§7, ");
        player.displayClientMessage(Component.literal(sb.toString()), false);
        sb = new StringBuilder("§7Items: ");
        for (var item : r.requiredItems) sb.append("§f")
                .append(item.get().getDescription().getString()).append("§7, ");
        player.displayClientMessage(Component.literal(sb.toString()), false);
        if (r.sacrificeRequired) {
            player.displayClientMessage(Component.literal(
                    "§c✗ Sacrifício recente (Adaga Ritualística)"), false);
        }
    }

    private static class ValidationResult {
        boolean success;
        String error;
        List<ItemEntity> itemsToConsume = new ArrayList<>();
    }

    private ValidationResult validate(RitualRegistry.Ritual r) {
        ValidationResult res = new ValidationResult();
        AABB box = new AABB(worldPosition).inflate(SCAN_RADIUS);

        // Chalk check
        Map<OccultItems.ChalkColor, Integer> chalksHave = countChalks(box);
        Map<OccultItems.ChalkColor, Long> chalksNeed = r.chalkColors.stream()
                .collect(java.util.stream.Collectors.groupingBy(c -> c,
                        java.util.stream.Collectors.counting()));
        for (var e : chalksNeed.entrySet()) {
            int have = chalksHave.getOrDefault(e.getKey(), 0);
            if (have < e.getValue()) {
                res.error = "Faltam " + (e.getValue() - have) + "× chalk "
                        + e.getKey().displayName;
                return res;
            }
        }
        // Candle check
        Map<OccultItems.ChalkColor, Integer> candlesHave = countCandles(box);
        Map<OccultItems.ChalkColor, Long> candlesNeed = r.candleColors.stream()
                .collect(java.util.stream.Collectors.groupingBy(c -> c,
                        java.util.stream.Collectors.counting()));
        for (var e : candlesNeed.entrySet()) {
            int have = candlesHave.getOrDefault(e.getKey(), 0);
            if (have < e.getValue()) {
                res.error = "Faltam " + (e.getValue() - have) + "× vela acesa "
                        + e.getKey().displayName;
                return res;
            }
        }
        // Items check
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box);
        Map<Item, Long> need = r.requiredItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(s -> s.get(),
                        java.util.stream.Collectors.counting()));
        for (var e : need.entrySet()) {
            int needCount = e.getValue().intValue();
            int found = 0;
            List<ItemEntity> toConsume = new ArrayList<>();
            for (ItemEntity ie : items) {
                if (toConsume.contains(ie)) continue;
                if (ie.getItem().is(e.getKey())) {
                    int take = Math.min(ie.getItem().getCount(), needCount - found);
                    found += take;
                    toConsume.add(ie);
                    if (found >= needCount) break;
                }
            }
            if (found < needCount) {
                res.error = "Falta item: " + e.getKey().getDescription().getString()
                        + " (" + (needCount - found) + ")";
                return res;
            }
            res.itemsToConsume.addAll(toConsume);
        }
        // Sacrifice check (procura mob morto recentemente com tag de sacrifício)
        if (r.sacrificeRequired) {
            boolean foundSacrifice = false;
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (le.getPersistentData().contains("liberthia.ritual_sacrifice_time")
                        && level.getGameTime() - le.getPersistentData()
                                .getLong("liberthia.ritual_sacrifice_time") < 100) {
                    foundSacrifice = true;
                    break;
                }
            }
            if (!foundSacrifice) {
                res.error = "Sacrifício recente necessário (Adaga Ritualística)";
                return res;
            }
        }
        res.success = true;
        return res;
    }

    private Map<OccultItems.ChalkColor, Integer> countChalks(AABB box) {
        Map<OccultItems.ChalkColor, Integer> m = new HashMap<>();
        BlockPos.betweenClosedStream(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ)).forEach(p -> {
            var s = level.getBlockState(p);
            if (s.getBlock() instanceof ChalkMarkBlock cmb) {
                m.merge(cmb.color, 1, Integer::sum);
            }
        });
        return m;
    }

    private Map<OccultItems.ChalkColor, Integer> countCandles(AABB box) {
        Map<OccultItems.ChalkColor, Integer> m = new HashMap<>();
        BlockPos.betweenClosedStream(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ)).forEach(p -> {
            var s = level.getBlockState(p);
            // r180: VELAS VANILLA acesas valem pro ritual (cor mapeada). Preferido.
            if (s.getBlock() instanceof net.minecraft.world.level.block.CandleBlock
                    && s.getValue(net.minecraft.world.level.block.CandleBlock.LIT)) {
                OccultItems.ChalkColor c = vanillaCandleColor(s);
                if (c != null) m.merge(c, 1, Integer::sum);
            }
            // velas ocultas customizadas continuam valendo (não quebra o que já existe)
            else if (s.getBlock() instanceof CandleBlock cb && s.getValue(CandleBlock.LIT)) {
                m.merge(cb.color, 1, Integer::sum);
            }
        });
        return m;
    }

    /** Mapeia a cor de uma vela VANILLA pra cor de ritual (null = cor sem ritual). */
    private static OccultItems.ChalkColor vanillaCandleColor(BlockState s) {
        if (s.is(net.minecraft.world.level.block.Blocks.BLACK_CANDLE)) return OccultItems.ChalkColor.BLACK;
        if (s.is(net.minecraft.world.level.block.Blocks.RED_CANDLE)) return OccultItems.ChalkColor.RED;
        if (s.is(net.minecraft.world.level.block.Blocks.PURPLE_CANDLE)
                || s.is(net.minecraft.world.level.block.Blocks.MAGENTA_CANDLE)) return OccultItems.ChalkColor.PURPLE;
        if (s.is(net.minecraft.world.level.block.Blocks.YELLOW_CANDLE)
                || s.is(net.minecraft.world.level.block.Blocks.ORANGE_CANDLE)) return OccultItems.ChalkColor.GOLDEN;
        if (s.is(net.minecraft.world.level.block.Blocks.WHITE_CANDLE)
                || s.is(net.minecraft.world.level.block.Blocks.CANDLE)) return OccultItems.ChalkColor.WHITE;
        return null;
    }

    private void completeRitual(ServerLevel sl) {
        RitualRegistry.Ritual r = null;
        for (RitualRegistry.Ritual rr : RitualRegistry.ALL) {
            if (rr.name.equals(activeRitualName)) { r = rr; break; }
        }
        if (r == null) {
            cleanup();
            return;
        }
        ServerPlayer caster = sl.getServer().getPlayerList().getPlayer(ritualCasterUuid);

        // FX final
        sl.sendParticles(ParticleTypes.END_ROD,
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                80, 1.5, 1.5, 1.5, 0.3);
        sl.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT,
                SoundSource.BLOCKS, 2.0F, 1.5F);

        // Execute result
        switch (r.resultType) {
            case SUMMON_ENTITY -> {
                var rl = net.minecraft.resources.ResourceLocation.tryParse(r.resultData);
                if (rl != null) {
                    var type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(rl);
                    if (type != null) {
                        Entity e = type.create(sl);
                        if (e != null) {
                            e.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1,
                                    worldPosition.getZ() + 0.5,
                                    sl.random.nextFloat() * 360, 0);
                            // Loyal: marca dono no NBT
                            if (caster != null) {
                                e.getPersistentData().putString("liberthia.summoned_by",
                                        caster.getUUID().toString());
                            }
                            sl.addFreshEntity(e);
                        }
                    }
                }
            }
            case GIVE_ITEMS -> {
                // formato: "minecraft:diamond x3; liberthia:bound_foliot_crystal x1"
                for (String spec : r.resultData.split(";")) {
                    spec = spec.trim();
                    if (spec.isEmpty()) continue;
                    String[] parts = spec.split("\\s*x\\s*");
                    var rl = net.minecraft.resources.ResourceLocation.tryParse(parts[0].trim());
                    int count = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 1;
                    if (rl == null) continue;
                    var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
                    if (item == null) continue;
                    ItemStack stack = new ItemStack(item, count);
                    var ie = new ItemEntity(sl,
                            worldPosition.getX() + 0.5, worldPosition.getY() + 1.5,
                            worldPosition.getZ() + 0.5, stack);
                    ie.setDeltaMovement(0, 0.3, 0);
                    sl.addFreshEntity(ie);
                }
            }
            case APPLY_EFFECT -> {
                if (caster != null) {
                    if ("BANISH_AREA".equals(r.resultData)) {
                        // Banishing especial: teleporta hostis 32b
                        for (var m : sl.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                                new AABB(worldPosition).inflate(32))) {
                            Vec3 away = m.position().subtract(Vec3.atCenterOf(worldPosition)).normalize();
                            m.teleportTo(m.getX() + away.x * 40, m.getY(), m.getZ() + away.z * 40);
                        }
                        caster.displayClientMessage(Component.literal(
                                "§e§l✦ §r§eBANIMENTO COMPLETO."), false);
                    } else {
                        // formato: "minecraft:strength 6000 4; minecraft:fire_resistance 6000 0"
                        for (String spec : r.resultData.split(";")) {
                            String[] p = spec.trim().split("\\s+");
                            if (p.length < 1) continue;
                            var rl = net.minecraft.resources.ResourceLocation.tryParse(p[0]);
                            var effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(rl);
                            if (effect == null) continue;
                            int dur = p.length > 1 ? Integer.parseInt(p[1]) : 600;
                            int amp = p.length > 2 ? Integer.parseInt(p[2]) : 0;
                            caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                    effect, dur, amp));
                        }
                    }
                }
            }
            case TELEPORT -> {
                if (caster != null && "liberthia:spirit_world".equals(r.resultData)) {
                    br.com.murilo.liberthia.dimension.SpiritDimension.enterSpiritWorld(caster);
                }
            }
            case TEACH_SPELL -> {
                // r36: resultData = spell id (ex: "void_bolt")
                if (caster != null) {
                    boolean learned = br.com.murilo.liberthia.magic.PlayerSpellKnowledge
                            .learn(caster, r.resultData);
                    if (!learned) {
                        caster.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                "§7Você já conhecia esse feitiço — recebe 1 cristal-bonus."), false);
                    }
                    // Sempre dá um Crystal Spirit Ore como bonus
                    var bonus = new net.minecraft.world.item.ItemStack(
                            br.com.murilo.liberthia.registry.ModItems.CRYSTAL_SPIRIT_ORE_ITEM.get());
                    var ie = new ItemEntity(sl,
                            worldPosition.getX() + 0.5, worldPosition.getY() + 1.5,
                            worldPosition.getZ() + 0.5, bonus);
                    sl.addFreshEntity(ie);
                }
            }
        }

        if (caster != null) {
            caster.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5Ritual §d" + r.name + "§5 COMPLETO."), false);
        }
        LiberthiaMod.LOGGER.info("[Ritual] Completed: {} → {} ({})",
                r.name, r.resultType, r.resultData);
        cleanup();
    }

    private void cleanup() {
        activeRitualName = "";
        ritualStartTick = -1;
        ritualCasterUuid = null;
        setChanged();
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Ritual", activeRitualName);
        tag.putLong("StartTick", ritualStartTick);
        tag.putInt("Duration", ritualDurationTicks);
        if (ritualCasterUuid != null) tag.putUUID("Caster", ritualCasterUuid);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        activeRitualName = tag.getString("Ritual");
        ritualStartTick = tag.getLong("StartTick");
        ritualDurationTicks = tag.getInt("Duration");
        if (tag.hasUUID("Caster")) ritualCasterUuid = tag.getUUID("Caster");
    }
}
