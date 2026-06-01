package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.capability.IInfectionData;
import br.com.murilo.liberthia.logic.BloodKin;
import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * <b>Seringa de Sangue</b> — uma por pessoa. Inspirada no {@code ItemBloodExtractor}
 * do EvilCraft, mas com regras próprias do Liberthia:
 *
 * <ul>
 *   <li><b>Vazia</b>: clique-direito num ser vivo extrai 2 HP de sangue. A
 *       seringa fica <b>presa àquela pessoa</b> (guarda nome, UUID, infecção e
 *       perfil de matéria DM/WM/YM).</li>
 *   <li><b>Cheia (trancada)</b>: NÃO dá pra esvaziar nem reusar — é uma amostra
 *       permanente daquela pessoa. Pra outra amostra, faça outra seringa.</li>
 *   <li><b>Rastrear</b>: com a seringa cheia, clique-direito no ar mostra a
 *       <b>posição exata</b> da pessoa naquele momento (coordenadas + dimensão),
 *       desde que ela esteja online/carregada.</li>
 * </ul>
 *
 * <p>Sangue de blood-kin (cultistas, padres, vermes, mãe) não pode ser extraído.
 */
public class BloodSyringeItem extends Item {

    private static final String TAG_FILLED = "liberthia_syringe_filled";
    private static final String TAG_SOURCE_TAINTED = "liberthia_syringe_tainted";
    /** Infecção (0-100) do alvo de quem o sangue foi tirado. -1 = desconhecida. */
    private static final String TAG_INFECTION = "liberthia_blood_infection";
    /** Perfil de matéria do alvo (DM/WM/YM) + nome — pra análise no Matter Analyzer. */
    private static final String TAG_DM = "liberthia_blood_dm";
    private static final String TAG_WM = "liberthia_blood_wm";
    private static final String TAG_YM = "liberthia_blood_ym";
    private static final String TAG_SRC = "liberthia_blood_src";
    /** UUID do alvo — usado pra rastrear a posição em tempo real. */
    private static final String TAG_SRC_UUID = "liberthia_blood_uuid";

    /** Nível de infecção armazenado na amostra (-1 se vazia/sem leitura). */
    public static int getStoredInfection(ItemStack stack) {
        return (stack.hasTag() && stack.getTag().contains(TAG_INFECTION))
                ? stack.getTag().getInt(TAG_INFECTION) : -1;
    }

    public static float getDark(ItemStack stack)   { return stack.hasTag() ? stack.getTag().getFloat(TAG_DM) : 0f; }
    public static float getWhite(ItemStack stack)  { return stack.hasTag() ? stack.getTag().getFloat(TAG_WM) : 0f; }
    public static float getYellow(ItemStack stack) { return stack.hasTag() ? stack.getTag().getFloat(TAG_YM) : 0f; }
    public static String getSource(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getString(TAG_SRC) : "";
    }
    public static UUID getSourceUuid(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().hasUUID(TAG_SRC_UUID)) return stack.getTag().getUUID(TAG_SRC_UUID);
        return null;
    }

    /** Rótulo + cor do sangue conforme a infecção — "a cor do sangue é diferente". */
    private static ChatFormatting bloodStyle(int inf) {
        if (inf <= 0) return ChatFormatting.GREEN;
        if (inf < 25) return ChatFormatting.YELLOW;
        if (inf < 50) return ChatFormatting.GOLD;
        if (inf < 75) return ChatFormatting.RED;
        return ChatFormatting.DARK_RED;
    }

    private static String bloodLabel(int inf) {
        if (inf <= 0) return "Limpo";
        if (inf < 25) return "Levemente infectado";
        if (inf < 50) return "Infectado";
        if (inf < 75) return "Muito infectado";
        return "Sangue podre";
    }

    public BloodSyringeItem(Properties properties) {
        super(properties);
    }

    /** Convenience: is this stack currently holding a sample? */
    public static boolean isFilled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_FILLED);
    }

    private static boolean isTainted(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_SOURCE_TAINTED);
    }

    private static void setFilled(ItemStack stack, boolean tainted) {
        stack.getOrCreateTag().putBoolean(TAG_FILLED, true);
        stack.getOrCreateTag().putBoolean(TAG_SOURCE_TAINTED, tainted);
    }

    // --- Extrai sangue de outro ser vivo (clique-direito) — trava na pessoa ---
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;

        // IMPORTANTE: em creative o vanilla passa uma CÓPIA do item segurado pra
        // este método (pra proteger o item do criativo). Mexer na cópia não
        // persiste → a seringa nunca "trancava" e dava pra extrair infinitas
        // vezes. Por isso operamos no item REAL da mão.
        ItemStack real = player.getItemInHand(hand);
        if (!(real.getItem() instanceof BloodSyringeItem)) real = stack;

        if (isFilled(real)) {
            player.displayClientMessage(
                    Component.literal("Essa seringa já está presa a " + getSource(real) + " — faça outra.")
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.FAIL;
        }
        if (BloodKin.is(target)) {
            player.displayClientMessage(
                    Component.literal("Esse sangue já está amaldiçoado — inútil.").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResult.FAIL;
        }
        // Extrai: 2 HP do alvo, marca a seringa. Contaminada se o alvo tem Blood Infection.
        target.hurt(target.damageSources().magic(), 2.0F);
        boolean tainted = target.hasEffect(ModEffects.BLOOD_INFECTION.get());
        setFilled(real, tainted);

        int infection = target.getCapability(ModCapabilities.INFECTION)
                .map(IInfectionData::getInfection).orElse(0);
        real.getOrCreateTag().putInt(TAG_INFECTION, infection);

        final float[] prof = {0f, 0f, 0f};
        target.getCapability(br.com.murilo.liberthia.matter.MatterProfileProvider.CAP).ifPresent(p -> {
            prof[0] = p.getDark();
            prof[1] = p.getWhite();
            prof[2] = p.getYellow();
        });
        var bt = real.getOrCreateTag();
        bt.putFloat(TAG_DM, prof[0]);
        bt.putFloat(TAG_WM, prof[1]);
        bt.putFloat(TAG_YM, prof[2]);
        bt.putString(TAG_SRC, target.getName().getString());
        bt.putUUID(TAG_SRC_UUID, target.getUUID());

        // Garante que a marcação "cole" no item real (inclusive em creative).
        player.setItemInHand(hand, real);

        player.displayClientMessage(Component.literal("Amostra de ")
                        .append(target.getDisplayName())
                        .append(": " + bloodLabel(infection) + " (" + infection + "%)")
                        .withStyle(bloodStyle(infection)), true);

        if (target.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    10, 0.2, 0.2, 0.2, 0.05);
            sl.playSound(null, target.blockPosition(),
                    SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
        return InteractionResult.SUCCESS;
    }

    // --- Cheia: clique-direito no ar RASTREIA a pessoa (não esvazia) ---
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);

        if (!isFilled(stack)) {
            player.displayClientMessage(
                    Component.literal("Seringa vazia — clique-direito num ser vivo para extrair sangue (1 por amostra).")
                            .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.pass(stack);
        }

        String src = getSource(stack);
        if (src.isEmpty()) src = "alvo";
        UUID id = getSourceUuid(stack);
        MinecraftServer server = level.getServer();
        LivingEntity found = (id != null && server != null) ? findLiving(server, id) : null;

        if (found == null) {
            player.displayClientMessage(
                    Component.literal("§4❤ §f" + src + "§c fora de alcance (offline / não carregado).")
                            .withStyle(ChatFormatting.RED), true);
            if (level instanceof ServerLevel sl)
                sl.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(),
                        SoundSource.PLAYERS, 0.7F, 0.6F);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        BlockPos p = found.blockPosition();
        String dim = found.level().dimension().location().getPath();
        player.displayClientMessage(
                Component.literal("§4❤ §f" + src + "§7: §a" + p.getX() + ", " + p.getY() + ", " + p.getZ()
                        + " §7[" + dim + "]"), false);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.6, player.getZ(),
                    3, 0.2, 0.2, 0.2, 0.01);
            sl.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(),
                    SoundSource.PLAYERS, 0.8F, 1.8F);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /** Acha o ser vivo (player online ou entidade carregada) pelo UUID. */
    private static LivingEntity findLiving(MinecraftServer server, UUID id) {
        net.minecraft.server.level.ServerPlayer sp = server.getPlayerList().getPlayer(id);
        if (sp != null) return sp;
        for (ServerLevel lvl : server.getAllLevels()) {
            Entity e = lvl.getEntity(id);
            if (e instanceof LivingEntity le && le.isAlive()) return le;
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isFilled(stack)) {
            String src = getSource(stack);
            tooltip.add(Component.literal("Amostra de: §f" + (src.isEmpty() ? "?" : src))
                    .withStyle(ChatFormatting.RED));
            int inf = getStoredInfection(stack);
            if (inf >= 0) {
                tooltip.add(Component.literal("Sangue: " + bloodLabel(inf) + " (" + inf + "%)")
                        .withStyle(bloodStyle(inf)));
            }
            tooltip.add(Component.literal("Clique direito: rastrear posição").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Amostra permanente (não reutilizável)").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Vazia").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Clique direito em ser vivo: extrair sangue").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
