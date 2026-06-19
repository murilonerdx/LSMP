package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * v0.1.22 r23: Client-side dos efeitos cosméticos cósmicos:
 * <ul>
 *   <li>Madness Aura → flash de chars random / sons / partículas</li>
 *   <li>Maddening Gaze → overlay "FUJA" vermelho piscando</li>
 *   <li>Mirror of Insanity → overlay "QUEM SOU EU?" + (futuro) skin swap</li>
 * </ul>
 *
 * <p>Render via {@code RenderGuiOverlayEvent.Post} no HOTBAR overlay (mais
 * compatível). Estado: 3 timers em ticks que decrescem cada client tick.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class MadnessClient {

    private static final Random RNG = new Random();

    /** Timer (ticks) do flash "QUEM SOU EU?" do Mirror. */
    private static int mirrorTicks = 0;
    /** Timer do overlay "FUJA" do Gaze. */
    private static int runOverlayTicks = 0;
    /** Timer do flash do Madness Aura (tipo 0 = chars random). */
    private static int charFlashTicks = 0;
    /** Seed do flash atual (define texto). */
    private static int charFlashSeed = 0;
    /** Timer do void flash (tipo 3). */
    private static int voidFlashTicks = 0;

    /** Caracteres pra alucinação tipo SCREEN_FLASH. */
    private static final String GLYPHS = "Ω∮⊕⊗◊█▓▒░!?#§¶";

    private MadnessClient() {}

    // ─────────────────────────────────────────────────────── Server triggers

    public static void onHallucination(int type, int seed) {
        switch (type) {
            case 0 -> { // SCREEN_FLASH
                charFlashSeed = seed;
                charFlashTicks = 20; // 1s
            }
            case 1 -> playRandomMobSound();
            case 2 -> playWhisper();
            case 3 -> voidFlashTicks = 6; // ~300ms blackout
        }
    }

    public static void onMaddenedTarget() {
        runOverlayTicks = 40; // 2s
    }

    public static void onMirrorActive(int ticks) {
        mirrorTicks = ticks;
    }

    // ───────────────────────────────────────────────────────── Tick decay

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mirrorTicks > 0) mirrorTicks--;
        if (runOverlayTicks > 0) runOverlayTicks--;
        if (charFlashTicks > 0) charFlashTicks--;
        if (voidFlashTicks > 0) voidFlashTicks--;
    }

    // ─────────────────────────────────────────── Overlay render (in-world HUD)

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();

        // (1) VOID FLASH — tela escurece briefly
        if (voidFlashTicks > 0) {
            int alpha = Math.min(220, voidFlashTicks * 40);
            g.fill(0, 0, w, h, (alpha << 24) | 0x000000);
        }

        // (2) RUN overlay (Maddening Gaze target)
        if (runOverlayTicks > 0) {
            // Piscar — só mostra se tick par
            if ((runOverlayTicks / 4) % 2 == 0) {
                Component msg1 = Component.literal("VOCÊ ESTÁ ENLOUQUECENDO")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
                Component msg2 = Component.literal("F U J A").withStyle(
                        ChatFormatting.RED, ChatFormatting.BOLD);
                int tw1 = mc.font.width(msg1);
                int tw2 = mc.font.width(msg2);
                g.drawString(mc.font, msg1, w / 2 - tw1 / 2, h / 3, 0xFFFF3030);
                g.pose().pushPose();
                g.pose().scale(2.5f, 2.5f, 1f);
                g.drawString(mc.font, msg2,
                        (int) ((w / 2 - tw2 * 2.5f / 2) / 2.5f),
                        (int) ((h / 3 + 20) / 2.5f), 0xFFFF0000);
                g.pose().popPose();
            }
        }

        // (3) MIRROR overlay (QUEM SOU EU?)
        if (mirrorTicks > 0 && (mirrorTicks / 8) % 2 == 0) {
            Component msg = Component.literal("QUEM SOU EU?").withStyle(
                    ChatFormatting.AQUA, ChatFormatting.ITALIC);
            int tw = mc.font.width(msg);
            g.pose().pushPose();
            g.pose().scale(2.0f, 2.0f, 1f);
            g.drawString(mc.font, msg,
                    (int) ((w / 2 - tw * 2.0f / 2) / 2.0f),
                    (int) ((h / 2 - 30) / 2.0f), 0xAA66FFFF);
            g.pose().popPose();
        }

        // (4) CHAR FLASH (Madness Aura type 0) — chars random em posições random
        if (charFlashTicks > 0) {
            Random r = new Random(charFlashSeed);
            int count = 5 + r.nextInt(8);
            for (int i = 0; i < count; i++) {
                char glyph = GLYPHS.charAt(r.nextInt(GLYPHS.length()));
                String s = String.valueOf(glyph);
                int x = r.nextInt(w);
                int y = r.nextInt(h);
                int color = 0xFFE03030 + r.nextInt(0x202020);
                g.drawString(mc.font, s, x, y, color);
            }
        }
    }

    // ────────────────────────────────────── Audio triggers

    private static void playRandomMobSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        SoundEvent[] pool = {
                SoundEvents.ZOMBIE_AMBIENT, SoundEvents.SKELETON_AMBIENT,
                SoundEvents.ENDERMAN_AMBIENT, SoundEvents.GHAST_AMBIENT,
                SoundEvents.WITHER_AMBIENT, SoundEvents.WARDEN_AMBIENT,
                SoundEvents.HUSK_AMBIENT, SoundEvents.CREEPER_PRIMED,
                SoundEvents.SOUL_ESCAPE
        };
        SoundEvent picked = pool[RNG.nextInt(pool.length)];
        // Tocar com offset random pra parecer vir de outra direção
        double dx = (RNG.nextDouble() - 0.5) * 15.0;
        double dz = (RNG.nextDouble() - 0.5) * 15.0;
        Vec3 pos = mc.player.position().add(dx, 0, dz);
        mc.level.playLocalSound(pos.x, pos.y, pos.z, picked,
                net.minecraft.sounds.SoundSource.AMBIENT, 0.4F, 0.7F + RNG.nextFloat() * 0.6F, false);
    }

    private static void playWhisper() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // Som de "respiração" + soul escape combinados
        mc.player.playSound(SoundEvents.SOUL_ESCAPE, 0.3F, 0.4F + RNG.nextFloat() * 0.4F);
        mc.player.playSound(SoundEvents.AMBIENT_CAVE.value(), 0.2F, 1.5F + RNG.nextFloat() * 0.3F);
    }
}
