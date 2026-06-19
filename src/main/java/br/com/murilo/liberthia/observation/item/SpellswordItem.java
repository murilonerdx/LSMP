package br.com.murilo.liberthia.observation.item;

import br.com.murilo.liberthia.observation.ObservationParts;
import br.com.murilo.liberthia.observation.api.ObservationResolver;
import br.com.murilo.liberthia.observation.api.ObservationSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r61: <b>Spellsword</b> — espada que carrega uma {@link ObservationSpell}.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>Hit normal: dano de espada (Tier diamond)</li>
 *   <li>Hit + low cd: casta o spell embutido no alvo</li>
 *   <li>Right-click no ar: casta o spell mirado</li>
 *   <li>Shift+Right-click: cycle spell (4 presets internos)</li>
 * </ul>
 *
 * <h2>4 Spellsword presets</h2>
 * <ol>
 *   <li>Tendril Sword — DirectGaze + Tendril</li>
 *   <li>Decay Sword — DirectGaze + Decay + Linger</li>
 *   <li>Silence Sword — DirectGaze + Silence + Amplify</li>
 *   <li>Whisper Sword — DirectGaze + Whisper + Echo</li>
 * </ol>
 *
 * <p>NBT key {@code spellsword.preset} (int 0-3) determina qual.
 */
public class SpellswordItem extends SwordItem {

    public static final String NBT_PRESET = "liberthia.spellsword.preset";

    public SpellswordItem(Properties properties) {
        super(Tiers.DIAMOND, 4, -2.4F,
            properties.stacksTo(1).rarity(Rarity.EPIC).durability(1500));
    }

    @Override public boolean isFoil(ItemStack s) { return true; }

    /** Right-click no ar — casta. Shift+rclick = cycle. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        if (sp.isShiftKeyDown()) {
            // Cycle preset
            int current = stack.getOrCreateTag().getInt(NBT_PRESET);
            int next = (current + 1) % 4;
            stack.getOrCreateTag().putInt(NBT_PRESET, next);
            sp.displayClientMessage(Component.literal(
                "§5§l✦ §rEspada: §e" + presetName(next)), true);
            return InteractionResultHolder.success(stack);
        }

        // Cast spell
        ObservationSpell spell = buildPreset(stack.getOrCreateTag().getInt(NBT_PRESET));
        boolean ok = ObservationResolver.cast(sp, spell);
        if (ok) {
            sp.getCooldowns().addCooldown(this, 40); // 2s cooldown
            stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    /** Hit entidade — chance de proc do spell. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean hit = super.hurtEnemy(stack, target, attacker);
        if (hit && attacker instanceof ServerPlayer sp && !sp.getCooldowns().isOnCooldown(this)) {
            // 30% chance proc
            if (Math.random() < 0.30) {
                ObservationSpell spell = buildPreset(stack.getOrCreateTag().getInt(NBT_PRESET));
                ObservationResolver.cast(sp, spell);
                sp.getCooldowns().addCooldown(this, 60); // 3s after proc
            }
        }
        return hit;
    }

    private ObservationSpell buildPreset(int preset) {
        return switch (preset) {
            case 1 -> ObservationSpell.builder("Decay", 0x3D6010)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.DECAY)
                .add(ObservationParts.LINGER)
                .build();
            case 2 -> ObservationSpell.builder("Silence", 0xCCCCDD)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.SILENCE_MANIFEST)
                .add(ObservationParts.AMPLIFY)
                .build();
            case 3 -> ObservationSpell.builder("Whisper", 0x5544AA)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.WHISPER)
                .add(ObservationParts.ECHO)
                .build();
            default -> ObservationSpell.builder("Tendril", 0x9d4dd6)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.TENDRIL)
                .build();
        };
    }

    private String presetName(int preset) {
        return switch (preset) {
            case 1 -> "Decay";
            case 2 -> "Silence";
            case 3 -> "Whisper";
            default -> "Tendril";
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§5§oLâmina de Observação").withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.literal("§7Right-click: casta feitiço"));
        tooltip.add(Component.literal("§7Shift+Right-click: muda feitiço"));
        tooltip.add(Component.literal("§7Hit: 30% chance proc"));
        tooltip.add(Component.empty());
        int preset = stack.getOrCreateTag().getInt(NBT_PRESET);
        tooltip.add(Component.literal("§5§lAtivo: §r§e" + presetName(preset)));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§8§o\"A lâmina olha junto.\""));
    }
}
