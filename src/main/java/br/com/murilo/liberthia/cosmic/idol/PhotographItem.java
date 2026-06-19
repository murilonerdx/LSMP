package br.com.murilo.liberthia.cosmic.idol;

import br.com.murilo.liberthia.cosmic.scare.ScareS2CPacket;
import br.com.murilo.liberthia.cosmic.scare.ScareType;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>Fotografia</b> — o resultado de usar a {@link CameraItem}. Guarda na NBT um
 * {@code PhotoId} (string única). No CLIENTE, esse id resolve para a imagem PNG real
 * que a câmera capturou (ver {@code PhotoStore}); um {@code IItemDecorator} desenha
 * a miniatura da foto por cima do slot no inventário. Se a foto "saiu estranha", a
 * flag {@code Cursed} muda a moldura/descrição.
 */
public class PhotographItem extends Item {

    public static final String TAG_PHOTO_ID = "PhotoId";
    public static final String TAG_CURSED = "Cursed";
    public static final String TAG_CAPTION = "Caption";

    public PhotographItem(Properties props) {
        super(props.stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    public static String getPhotoId(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getString(TAG_PHOTO_ID) : "";
    }

    public static boolean isCursed(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_CURSED);
    }

    public static ItemStack create(Item item, String photoId, boolean cursed, String caption) {
        ItemStack s = new ItemStack(item);
        CompoundTag tag = s.getOrCreateTag();
        tag.putString(TAG_PHOTO_ID, photoId);
        tag.putBoolean(TAG_CURSED, cursed);
        if (caption != null && !caption.isEmpty()) tag.putString(TAG_CAPTION, caption);
        return s;
    }

    @Override
    public Component getName(ItemStack stack) {
        return isCursed(stack)
                ? Component.translatable("item.liberthia.photograph.cursed").withStyle(ChatFormatting.DARK_RED)
                : Component.translatable("item.liberthia.photograph");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains(TAG_CAPTION)) {
            tip.add(Component.literal("§7\"" + stack.getTag().getString(TAG_CAPTION) + "\"")
                    .withStyle(ChatFormatting.ITALIC));
        }
        if (isCursed(stack)) {
            tip.add(Component.literal("Há algo no fundo que você não viu.")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        } else {
            tip.add(Component.literal("Um instante preservado.")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        tip.add(Component.literal("§8Botão direito: §7ver a foto"));
        if (isCursed(stack)) {
            tip.add(Component.literal("§8Shift + direito: §crasgar (descartar)"));
        }
    }

    /**
     * r179: clicar com o botão direito numa foto AMALDIÇOADA a <b>rasga/joga fora</b>
     * (destrói 1) — com um susto, partículas e dreno de sanidade ("algo se solta").
     * Fotos normais não fazem nada ao usar.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // r180: SHIFT + amaldiçoada → rasga (descarte) com susto + dreno de sanidade
        if (player.isShiftKeyDown() && isCursed(stack)) {
            if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
                ModNetwork.sendToPlayer(sp, new ScareS2CPacket(ScareType.FLASH, 10, sp.getRandom().nextInt(11), ""));
                SpiritDimension.addSanity(sp, -5);
                ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
                sl.sendParticles(ParticleTypes.SMOKE, sp.getX(), sp.getY() + 1.2, sp.getZ(), 22, 0.3, 0.4, 0.3, 0.02);
                sl.sendParticles(ParticleTypes.SOUL, sp.getX(), sp.getY() + 1.2, sp.getZ(), 8, 0.2, 0.3, 0.2, 0.01);
                sl.playSound(null, sp.blockPosition(), SoundEvents.ITEM_FRAME_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
                sp.displayClientMessage(Component.literal("§4Você rasga a fotografia amaldiçoada... algo se solta.")
                        .withStyle(ChatFormatting.ITALIC), true);
            }
            stack.shrink(1);
            player.getCooldowns().addCooldown(this, 20);
            return InteractionResultHolder.success(stack);
        }

        // r180: right-click normal → ABRE o visualizador da foto (tela grande, client-side)
        if (level.isClientSide && net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            ViewOpener.open(getPhotoId(stack), isCursed(stack), getCaption(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static String getCaption(ItemStack stack) {
        return (stack.hasTag() && stack.getTag().contains(TAG_CAPTION)) ? stack.getTag().getString(TAG_CAPTION) : "";
    }

    /** Wrapper isolado — JVM só carrega a Screen quando chamado (client). */
    private static final class ViewOpener {
        static void open(String id, boolean cursed, String caption) {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new br.com.murilo.liberthia.client.screen.PhotographViewScreen(id, cursed, caption));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isCursed(stack);
    }
}
