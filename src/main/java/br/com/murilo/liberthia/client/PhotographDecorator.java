package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.idol.PhotoStore;
import br.com.murilo.liberthia.cosmic.idol.PhotographItem;
import br.com.murilo.liberthia.registry.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Desenha a FOTO REAL capturada por cima do slot da {@link PhotographItem} no
 * inventário — a miniatura 128×128 salva pela câmera, encolhida pra ~14×14 dentro
 * do slot, com uma moldura branca (vermelha se "cursed"). Assim a foto que você
 * tirou aparece de verdade no item, não só um ícone genérico.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PhotographDecorator {

    private PhotographDecorator() {}

    @SubscribeEvent
    public static void onRegister(RegisterItemDecorationsEvent event) {
        event.register(ModItems.PHOTOGRAPH.get(), new PhotoDeco());
    }

    private static final class PhotoDeco implements IItemDecorator {
        @Override
        public boolean render(GuiGraphics g, Font font, ItemStack stack, int x, int y) {
            String id = PhotographItem.getPhotoId(stack);
            ResourceLocation tex = PhotoStore.texture(id);
            boolean cursed = PhotographItem.isCursed(stack);

            // moldura (1px) + foto 14×14 dentro do slot 16×16
            int frame = cursed ? 0xFF7A0000 : 0xFFEDEDED;
            g.fill(x + 0, y + 0, x + 16, y + 16, 0xFF101014);     // fundo escuro
            g.fill(x + 1, y + 1, x + 15, y + 15, frame);          // borda
            // a foto em si — escala a imagem inteira (qualquer tamanho) pra 12×12
            RenderSystem.enableBlend();
            // blit(tex, x,y, drawW,drawH, u,v, srcW,srcH, texW,texH)
            g.blit(tex, x + 2, y + 2, 12, 12, 0F, 0F, PhotoStore.texWidth(id), PhotoStore.texHeight(id),
                    PhotoStore.texWidth(id), PhotoStore.texHeight(id));
            RenderSystem.disableBlend();
            // r179: foto AMALDIÇOADA = corrompida — scanlines glitch animadas por cima
            if (cursed) {
                long t = System.currentTimeMillis() / 90L;
                for (int i = 0; i < 3; i++) {
                    int ly = y + 2 + (int) ((t + i * 4L) % 12L);
                    g.fill(x + 2, ly, x + 14, ly + 1, (i % 2 == 0) ? 0xAAFF0033 : 0xAA000000);
                }
                // bloco deslocado (artefato de corrupção) intermitente
                if ((t % 7L) < 2L) {
                    g.fill(x + 2, y + 6, x + 14, y + 9, 0x6620FF40);
                }
                // canto vermelho piscando (pista)
                if ((t / 4L) % 2L == 0L) {
                    g.fill(x + 11, y + 2, x + 14, y + 4, 0xFFFF3030);
                }
            }
            return true; // já desenhamos o item (substitui o ícone padrão)
        }
    }
}
