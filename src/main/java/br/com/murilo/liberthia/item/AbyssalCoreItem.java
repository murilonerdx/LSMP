package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>Núcleo do Abismo</b> / <b>Núcleo do Vazio Cristalizado</b> — item de lore do
 * Vazio/Terminus (Kali). Uma esfera de cristal pulsante.
 *
 * <p>Dois estados, controlados pela tag NBT {@code crystallized}:
 * <ul>
 *   <li><b>Núcleo do Abismo</b> (base, azul+amarelo): no inventário/mão concede
 *       <b>Proteção do Mar</b> (Conduit Power); partículas do conduíte.</li>
 *   <li><b>Núcleo do Vazio Cristalizado</b> (com ametista, roxo): concede
 *       <b>Absorção I + Regeneração I</b>; partículas do conduíte + portal.</li>
 * </ul>
 * A cristalização é feita por uma receita shapeless (núcleo + ametista) que devolve
 * o item já com {@code crystallized=true} (ver {@code abyssal_core_crystallize.json}).
 */
public class AbyssalCoreItem extends Item {

    public static final String TAG_CRYSTALLIZED = "crystallized";

    public AbyssalCoreItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    public static boolean isCrystallized(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_CRYSTALLIZED);
    }

    /** Cria a versão cristalizada (usada pela receita / criativo). */
    public static ItemStack crystallized(Item item) {
        ItemStack s = new ItemStack(item);
        s.getOrCreateTag().putBoolean(TAG_CRYSTALLIZED, true);
        return s;
    }

    /**
     * Cristalização: segure o Núcleo do Abismo e clique com o botão direito tendo
     * uma <b>Ametista</b> no inventário. Consome 1 ametista e transforma o núcleo no
     * <b>Núcleo do Vazio Cristalizado</b> (efeito visual + som ressonante).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isCrystallized(stack)) return InteractionResultHolder.pass(stack);

        // procura 1 ametista no inventário
        int found = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.AMETHYST_SHARD)) { found = i; break; }
        }
        if (found < 0) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.literal("§5Precisa de uma Ametista para cristalizar o núcleo.")
                        , true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            player.getInventory().getItem(found).shrink(1);
            stack.getOrCreateTag().putBoolean(TAG_CRYSTALLIZED, true);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 1.0F, 0.6F);
            level.playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE,
                    SoundSource.PLAYERS, 0.7F, 1.4F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                        40, 0.5, 0.8, 0.5, 0.3);
            }
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;          // tudo server-side (efeitos + sendParticles)
        if (!(entity instanceof Player player)) return;
        boolean crystal = isCrystallized(stack);

        // ── efeitos: reaplica a cada 1s com 3s de duração → SEMPRE ativo, nunca pisca ──
        if (player.tickCount % 20 == 0) {
            if (crystal) {
                applyEffect(player, MobEffects.ABSORPTION);    // Absorção I
                applyEffect(player, MobEffects.REGENERATION);  // Regeneração I
            } else {
                applyEffect(player, MobEffects.CONDUIT_POWER); // Proteção do Mar (Conduit)
            }
        }

        // ── partículas do conduíte (NAUTILUS) seguindo o portador, server-side ──
        if (level instanceof ServerLevel sl && player.tickCount % 6 == 0) {
            double ang = sl.random.nextDouble() * Math.PI * 2;
            double r = 0.6 + sl.random.nextDouble() * 0.5;
            double x = player.getX() + Math.cos(ang) * r;
            double z = player.getZ() + Math.sin(ang) * r;
            double y = player.getY() + 0.5 + sl.random.nextDouble() * 1.1;
            // partícula do conduíte (NAUTILUS) — o "azul" pedido
            sl.sendParticles(ParticleTypes.NAUTILUS, x, y, z, 2, 0.1, 0.15, 0.1, 0.0);
            if (crystal) {
                // cristalizado: portal roxo extra
                sl.sendParticles(ParticleTypes.PORTAL, x, y, z, 3, 0.2, 0.3, 0.2, 0.02);
            } else {
                // base: reforço azul (caso NAUTILUS não apareça, este sempre aparece)
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                        x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }

    /** Aplica o efeito com ÍCONE visível no HUD, 3s, nível 1 (amp 0), sem partículas vanilla. */
    private static void applyEffect(Player player, MobEffect effect) {
        // (effect, duration=60t, amp=0, ambient=false, showParticles=false, showIcon=true)
        player.addEffect(new MobEffectInstance(effect, 60, 0, false, false, true));
    }

    @Override
    public Component getName(ItemStack stack) {
        return isCrystallized(stack)
                ? Component.translatable("item.liberthia.abyssal_core.crystallized")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                : Component.translatable("item.liberthia.abyssal_core")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        if (isCrystallized(stack)) {
            // roxo apenas
            tip.add(Component.literal("A fusão do Abismo com o Vazio.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            tip.add(Component.literal("Cristalizado por ametistas carregadas de vozes")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            tip.add(Component.literal("ecoantes de uma entidade antiga.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            tip.add(Component.empty());
            tip.add(Component.literal("✦ Absorção & Regeneração")
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        } else {
            // azul do núcleo + amarelo
            tip.add(Component.literal("Um núcleo abissal surgido de um corpo ancestral.")
                    .withStyle(ChatFormatting.AQUA));
            tip.add(Component.literal("Pulsa com energia selada.")
                    .withStyle(ChatFormatting.YELLOW));
            tip.add(Component.empty());
            tip.add(Component.literal("✦ Proteção do Mar")
                    .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
            tip.add(Component.literal("Na bancada: Núcleo + Ametista → Vazio Cristalizado")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // brilho encantado nos dois modos
    }
}
