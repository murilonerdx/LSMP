package br.com.murilo.liberthia.client.texture;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class ImageUrlTextureCache {

    private static final ConcurrentHashMap<String, CompletableFuture<LoadedImage>> CACHE = new ConcurrentHashMap<>();

    private ImageUrlTextureCache() {
    }

    public static Optional<LoadedImage> get(String url) {
        CompletableFuture<LoadedImage> future = CACHE.computeIfAbsent(url, ImageUrlTextureCache::loadAsync);

        if (!future.isDone()) {
            return Optional.empty();
        }

        if (future.isCompletedExceptionally()) {
            LiberthiaMod.LOGGER.error("A imagem falhou ao carregar: {}", url);
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(future.getNow(null));
        } catch (Exception exception) {
            LiberthiaMod.LOGGER.error("Erro lendo imagem carregada: {}", url, exception);
            return Optional.empty();
        }
    }

    private static CompletableFuture<LoadedImage> loadAsync(String url) {
        CompletableFuture<LoadedImage> result = new CompletableFuture<>();

        CompletableFuture
                .supplyAsync(() -> downloadImage(url))
                .thenAccept(nativeImage -> {
                    Minecraft minecraft = Minecraft.getInstance();

                    minecraft.execute(() -> {
                        try {
                            int width = nativeImage.getWidth();
                            int height = nativeImage.getHeight();

                            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);

                            ResourceLocation location = new ResourceLocation(
                                    LiberthiaMod.MODID,
                                    "image_book/" + safeHash(url)
                            );

                            minecraft.getTextureManager().register(location, dynamicTexture);

                            result.complete(new LoadedImage(location, width, height));
                        } catch (Exception exception) {
                            result.completeExceptionally(exception);
                        }
                    });
                })
                .exceptionally(exception -> {
                    LiberthiaMod.LOGGER.error("Erro carregando imagem do livro: {}", url, exception);
                    result.completeExceptionally(exception);
                    return null;
                });

        return result;
    }

    private static NativeImage downloadImage(String urlText) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(urlText);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 Minecraft-Liberthia-ImageBook/1.0");
            connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/png,image/jpeg,image/*,*/*;q=0.8");

            int code = connection.getResponseCode();

            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP inválido: " + code + " para " + urlText);
            }

            String contentType = connection.getContentType();
            LiberthiaMod.LOGGER.info("Baixando imagem do livro: {} | Content-Type: {}", urlText, contentType);

            byte[] bytes;

            try (InputStream inputStream = connection.getInputStream()) {
                bytes = inputStream.readAllBytes();
            }

            try {
                return NativeImage.read(new ByteArrayInputStream(bytes));
            } catch (Exception nativeImageException) {
                LiberthiaMod.LOGGER.warn("NativeImage não conseguiu ler. Tentando ImageIO: {}", urlText);
                return readWithImageIo(bytes, urlText, nativeImageException);
            }
        } catch (Exception exception) {
            throw new RuntimeException("Falha ao carregar imagem: " + urlText, exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static NativeImage readWithImageIo(byte[] bytes, String urlText, Exception originalException) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));

            if (bufferedImage == null) {
                throw new IllegalStateException("ImageIO não encontrou reader para a imagem: " + urlText, originalException);
            }

            BufferedImage argbImage = new BufferedImage(
                    bufferedImage.getWidth(),
                    bufferedImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );

            argbImage.getGraphics().drawImage(bufferedImage, 0, 0, null);

            return bufferedImageToNativeImage(argbImage);
        } catch (Exception imageIoException) {
            throw new RuntimeException("ImageIO também falhou ao ler a imagem: " + urlText, imageIoException);
        }
    }

    private static NativeImage bufferedImageToNativeImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);

                int alpha = (argb >> 24) & 0xFF;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;

                int abgr = alpha << 24 | blue << 16 | green << 8 | red;

                nativeImage.setPixelRGBA(x, y, abgr);
            }
        }

        return nativeImage;
    }

    private static String safeHash(String value) {
        return Integer.toHexString(value.hashCode()).replace("-", "n");
    }

    public record LoadedImage(ResourceLocation location, int width, int height) {
    }
}