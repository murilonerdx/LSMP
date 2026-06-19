package br.com.murilo.liberthia.magic.scrying;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * v0.1.24 r85: <b>Scrying Lens</b> — item que dá ao player visão de blocos
 * específicos através de paredes num raio.
 *
 * <p>Right-click: alterna entre "alvos" rotativos:
 * <ul>
 *   <li>1. Diamond Ore + Diamond Block</li>
 *   <li>2. Ancient Debris + Netherite Block</li>
 *   <li>3. Gold Ore + Gold Block</li>
 *   <li>4. Emerald Ore</li>
 *   <li>5. Liberthia matter ores (dark/white/yellow)</li>
 *   <li>6. Source Jar + Sourcelink blocks (magic infrastructure)</li>
 * </ul>
 *
 * <p>Shift+Right-click: ativa SCRYING — por 30s, blocos do tipo selecionado
 * num raio de 32 blocos ganham GLOWING tag (visible through walls).
 *
 * <p>Cost: 200 Source (TODO: integrar com Source capability quando
 * conectar). Por enquanto cooldown de 60s.
 */
public class ScryingLensItem extends Item {

    /** NBT key — qual preset (0-5) está ativo. */
    public static final String NBT_PRESET = "liberthia.scry_preset";

    public static final String[] PRESET_NAMES = {
            "Diamond", "Ancient Debris", "Ouro", "Esmeralda",
            "Matter (DM/WM/YM)", "Magic Infrastructure"
    };

    public ScryingLensItem(Properties props) {
        super(props.stacksTo(1).durability(0));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }

        if (sp.isShiftKeyDown()) {
            // SCRYING ACTIVATE
            if (sp.getCooldowns().isOnCooldown(this)) {
                sp.displayClientMessage(Component.literal("§5✦ Lente repousa..."), true);
                return InteractionResultHolder.fail(stack);
            }
            int preset = stack.getOrCreateTag().getInt(NBT_PRESET);
            activateScrying(sp, level, preset);
            sp.getCooldowns().addCooldown(this, 1200);
            return InteractionResultHolder.success(stack);
        } else {
            // CYCLE PRESET
            CompoundTag tag = stack.getOrCreateTag();
            int current = tag.getInt(NBT_PRESET);
            int next = (current + 1) % PRESET_NAMES.length;
            tag.putInt(NBT_PRESET, next);
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5Lente: §b" + PRESET_NAMES[next]), true);
            return InteractionResultHolder.success(stack);
        }
    }

    private void activateScrying(ServerPlayer sp, Level level, int preset) {
        List<Block> targets = getTargetBlocks(preset);
        if (targets.isEmpty()) return;

        BlockPos origin = sp.blockPosition();
        int radius = 32;
        int highlighted = 0;
        var sl = sp.serverLevel();

        // Spawn glowing particles em blocos matchando
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dy = -16; dy <= 16; dy += 2) {
                for (int dz = -radius; dz <= radius; dz += 2) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    Block b = sl.getBlockState(p).getBlock();
                    if (targets.contains(b)) {
                        // Spawn glowing trail de particles na position
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                                p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                                12, 0.3, 0.3, 0.3, 0.02);
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                                p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                                3, 0.2, 0.2, 0.2, 0);
                        highlighted++;
                    }
                }
            }
        }

        sp.displayClientMessage(Component.literal(
                "§5§l✦ §r§5" + highlighted + " §5blocos de §b" + PRESET_NAMES[preset]
                + " §5revelados em §b32 §5blocos"), false);

        // Sound de "scrying activated"
        sl.playSound(null, origin,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.5F);
    }

    private List<Block> getTargetBlocks(int preset) {
        return switch (preset) {
            case 0 -> List.of(
                    net.minecraft.world.level.block.Blocks.DIAMOND_ORE,
                    net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE,
                    net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK);
            case 1 -> List.of(
                    net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS,
                    net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK);
            case 2 -> List.of(
                    net.minecraft.world.level.block.Blocks.GOLD_ORE,
                    net.minecraft.world.level.block.Blocks.DEEPSLATE_GOLD_ORE,
                    net.minecraft.world.level.block.Blocks.GOLD_BLOCK,
                    net.minecraft.world.level.block.Blocks.NETHER_GOLD_ORE);
            case 3 -> List.of(
                    net.minecraft.world.level.block.Blocks.EMERALD_ORE,
                    net.minecraft.world.level.block.Blocks.DEEPSLATE_EMERALD_ORE,
                    net.minecraft.world.level.block.Blocks.EMERALD_BLOCK);
            case 4 -> {
                // Liberthia matter ores
                java.util.List<Block> list = new java.util.ArrayList<>();
                try {
                    list.add(br.com.murilo.liberthia.registry.ModBlocks.DARK_MATTER_ORE.get());
                    list.add(br.com.murilo.liberthia.registry.ModBlocks.DEEPSLATE_DARK_MATTER_ORE.get());
                    list.add(br.com.murilo.liberthia.registry.ModBlocks.WHITE_MATTER_ORE.get());
                } catch (Throwable ignored) {}
                yield list;
            }
            case 5 -> {
                // r164: sourcelinks removidos, lente agora foca em source jar + relay
                java.util.List<Block> list = new java.util.ArrayList<>();
                try {
                    list.add(br.com.murilo.liberthia.registry.ModBlocks.SOURCE_JAR.get());
                    list.add(br.com.murilo.liberthia.registry.ModBlocks.SOURCE_RELAY.get());
                } catch (Throwable ignored) {}
                yield list;
            }
            default -> List.of();
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int preset = stack.getOrCreateTag().getInt(NBT_PRESET);
        tooltip.add(Component.literal("§7Preset: §b" + PRESET_NAMES[preset]));
        tooltip.add(Component.literal("§7Right-click: ciclar preset"));
        tooltip.add(Component.literal("§7Shift+Right: revelar blocos (32 raio)"));
        tooltip.add(Component.literal("§8§oCooldown: 60s"));
    }
}
