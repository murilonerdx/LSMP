package br.com.murilo.liberthia.packet;

import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CreateImageBookPacket {

    private static final int MAX_IMAGES = 50;
    private static final int MAX_URL_LENGTH = 2048;
    private static final int MAX_TITLE_LENGTH = 64;

    private final String bookTitle;
    private final List<String> imageUrls;

    public CreateImageBookPacket(String bookTitle, List<String> imageUrls) {
        this.bookTitle = bookTitle;
        this.imageUrls = imageUrls;
    }

    public static void encode(CreateImageBookPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.bookTitle == null ? "" : packet.bookTitle, MAX_TITLE_LENGTH);

        int size = Math.min(packet.imageUrls.size(), MAX_IMAGES);
        buffer.writeInt(size);

        for (int i = 0; i < size; i++) {
            String url = packet.imageUrls.get(i);
            buffer.writeUtf(url, MAX_URL_LENGTH);
        }
    }

    public static CreateImageBookPacket decode(FriendlyByteBuf buffer) {
        String title = buffer.readUtf(MAX_TITLE_LENGTH);

        int size = buffer.readInt();
        int safeSize = Math.max(0, Math.min(size, MAX_IMAGES));

        List<String> urls = new ArrayList<>();

        for (int i = 0; i < safeSize; i++) {
            urls.add(buffer.readUtf(MAX_URL_LENGTH));
        }

        return new CreateImageBookPacket(title, urls);
    }

    public static void handle(CreateImageBookPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null) {
                return;
            }

            List<String> validUrls = packet.imageUrls.stream()
                    .map(String::trim)
                    .filter(url -> !url.isBlank())
                    .filter(CreateImageBookPacket::isValidImageUrl)
                    .limit(MAX_IMAGES)
                    .toList();

            if (validUrls.isEmpty()) {
                player.displayClientMessage(
                        Component.literal("Nenhum link de imagem válido foi informado.")
                                .withStyle(ChatFormatting.RED),
                        false
                );
                return;
            }

            String safeTitle = sanitizeTitle(packet.bookTitle);

            ItemStack book = new ItemStack(ModItems.IMAGE_FRAME_BOOK.get());
            book.setHoverName(Component.literal(safeTitle).withStyle(ChatFormatting.LIGHT_PURPLE));

            CompoundTag tag = book.getOrCreateTag();
            ListTag listTag = new ListTag();

            for (String url : validUrls) {
                listTag.add(StringTag.valueOf(url));
            }

            tag.putString("ImageBookTitle", safeTitle);
            tag.put("ImageUrls", listTag);
            tag.putInt("ImageCount", validUrls.size());

            boolean added = player.getInventory().add(book);

            if (!added) {
                player.drop(book, false);
            }

            player.displayClientMessage(
                    Component.literal("Livro criado: ")
                            .append(Component.literal(safeTitle).withStyle(ChatFormatting.LIGHT_PURPLE))
                            .append(Component.literal(" com " + validUrls.size() + " página(s).")),
                    false
            );
        });

        context.setPacketHandled(true);
    }

    private static String sanitizeTitle(String title) {
        if (title == null || title.trim().isBlank()) {
            return "Livro de Imagens";
        }

        String clean = title.trim();

        if (clean.length() > MAX_TITLE_LENGTH) {
            clean = clean.substring(0, MAX_TITLE_LENGTH);
        }

        return clean;
    }

    private static boolean isValidImageUrl(String url) {
        try {
            URI uri = URI.create(url);

            String scheme = uri.getScheme();

            if (scheme == null) {
                return false;
            }

            boolean validScheme = scheme.equalsIgnoreCase("http")
                    || scheme.equalsIgnoreCase("https");

            if (!validScheme) {
                return false;
            }

            String lower = url.toLowerCase();

            return lower.endsWith(".png")
                    || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg")
                    || lower.endsWith(".webp")
                    || lower.contains(".png?")
                    || lower.contains(".jpg?")
                    || lower.contains(".jpeg?")
                    || lower.contains(".webp?");
        } catch (Exception ignored) {
            return false;
        }
    }
}