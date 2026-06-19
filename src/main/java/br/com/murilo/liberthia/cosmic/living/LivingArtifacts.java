package br.com.murilo.liberthia.cosmic.living;

import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r47: <b>Living Server Artifacts</b> — 4 itens ORIGINAIS que
 * interagem com os sistemas living (chunk memory, observation pressure,
 * second sky).
 */
public final class LivingArtifacts {

    private LivingArtifacts() {}

    // ════════════════════════════════════════════════════════════
    // 1. GEOMETRY KEY — chave com ângulos impossíveis
    // ════════════════════════════════════════════════════════════
    public static class GeometryKeyItem extends Item {
        public GeometryKeyItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            ServerLevel sl = sp.serverLevel();
            // "Revela" — busca chunks com emotional index alto num raio 5 chunks
            ChunkPos myChunk = new ChunkPos(sp.blockPosition());
            ChunkMemoryStorage storage = ChunkMemoryStorage.get(sl);
            int hidden = 0;
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    ChunkPos cp = new ChunkPos(myChunk.x + dx, myChunk.z + dz);
                    ChunkMemory cm = storage.peek(cp);
                    if (cm != null && cm.emotionalIndex() >= 40) {
                        hidden++;
                        // Pulse particle no centro do chunk
                        sl.sendParticles(sp, ModParticles.DEMON_GLYPH.get(), true,
                                cp.getMiddleBlockX(), sp.getY() + 1, cp.getMiddleBlockZ(),
                                10, 1, 1, 1, 0);
                        // Aumenta pressure (chave fortalece)
                        ObservationPressure.add(cp, 5);
                    }
                }
            }

            // Inventory "rotation" hallucination — fake inventory item
            HallucinationManager.force(sp, HallucinationType.FAKE_INVENTORY_ITEM, 1.0F, 30, "");

            sp.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5A chave revela §e" + hidden + "§5 chunks contaminados §o(idx ≥ 40)"), true);
            sl.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.5F, 1.8F);
            // Cost: insanity + cosmic
            InsanityData.addInsanity(sp, 3);
            InsanityData.addForbiddenKnowledge(sp, 2);
            sp.getCooldowns().addCooldown(this, 300); // 15s
            return InteractionResultHolder.consume(stack);
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§o§lChave da Geometria").withStyle(ChatFormatting.DARK_PURPLE));
            t.add(Component.literal("§7§oFeita de ângulos que não existem."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click: §lREVELA§r§7 chunks com memória ≥40"));
            t.add(Component.literal("§7num raio §e5 chunks§r§7 — pulse particle no centro"));
            t.add(Component.literal("§7Fortalece os chunks (§e+5 observation pressure§r§7)"));
            t.add(Component.literal("§7Inventory glitch hallucination"));
            t.add(Component.empty());
            t.add(Component.literal("§c-3 insanity por uso"));
            t.add(Component.literal("§8§o\"Onde a chave entra, a porta nunca esteve.\""));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 2. LOW SIGNAL — rádio que toca sons de longe
    // ════════════════════════════════════════════════════════════
    public static class LowSignalItem extends Item {
        public LowSignalItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            ServerLevel sl = sp.serverLevel();
            // Acha o chunk com MAIOR emotional index num raio amplo (até 50 chunks)
            // e replica echoes desse chunk pro player atual
            ChunkPos myChunk = new ChunkPos(sp.blockPosition());
            ChunkMemoryStorage storage = ChunkMemoryStorage.get(sl);
            ChunkMemory best = null;
            int bestIdx = 0;
            for (int dx = -50; dx <= 50; dx += 5) {
                for (int dz = -50; dz <= 50; dz += 5) {
                    ChunkPos cp = new ChunkPos(myChunk.x + dx, myChunk.z + dz);
                    ChunkMemory cm = storage.peek(cp);
                    if (cm != null && cm.emotionalIndex() > bestIdx) {
                        bestIdx = cm.emotionalIndex();
                        best = cm;
                    }
                }
            }

            if (best == null) {
                sp.displayClientMessage(Component.literal(
                        "§7§oO rádio responde com... silêncio."), true);
                return InteractionResultHolder.fail(stack);
            }

            // Toca 3 echoes consecutivas com 1s entre cada
            sp.displayClientMessage(Component.literal(
                    "§4§l✦ §r§4Sinal capturado §7(distância: chunk " + best.pos.x + ", " + best.pos.z + ")"), false);
            for (int i = 0; i < 3; i++) {
                ChunkMemory.Echo echo = best.pickEcho();
                if (echo == null) break;
                String txt = "§7§o*" + echo.type.name().toLowerCase() + "*";
                if (echo.type == ChunkMemory.Echo.Type.CHAT && !echo.data.isEmpty()) {
                    txt = "§7§o<???> §o" + echo.data;
                }
                HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1, txt);
            }
            // Distorted audio
            HallucinationManager.force(sp, HallucinationType.REVERSE_AUDIO_PULSE, 0.8F, 60, "");
            sl.playSound(null, sp.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.get(),
                    SoundSource.PLAYERS, 1.0F, 0.3F);

            InsanityData.addInsanity(sp, 2);
            sp.getCooldowns().addCooldown(this, 600); // 30s
            return InteractionResultHolder.consume(stack);
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§4§o§lSinal Baixo").withStyle(ChatFormatting.DARK_RED));
            t.add(Component.literal("§7§oRecebe ecos de chunks distantes."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click: encontra o chunk mais §lcontaminado§r§7 num raio"));
            t.add(Component.literal("§7§e50 chunks§r§7 e replica 3 echoes dele aqui."));
            t.add(Component.literal("§7Distorted audio pulse"));
            t.add(Component.empty());
            t.add(Component.literal("§c-2 insanity por uso"));
            t.add(Component.literal("§8§o\"O sinal não vem de fora. Vem de antes.\""));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 3. PALE THREAD — fio que revela caminhos invisíveis
    // ════════════════════════════════════════════════════════════
    public static class PaleThreadItem extends Item {
        public PaleThreadItem(Properties p) { super(p.rarity(Rarity.RARE).stacksTo(8)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

            ServerLevel sl = sp.serverLevel();
            // "Walks" — buffs de phasing por 8s
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 3, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.JUMP, 160, 2, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 160, 0, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0, true, false));
            // Set Walking through mobs flag (Curios/feature would handle — here just resistance)
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 4, true, false));

            // Visual: trail of pale particles ao redor
            for (int i = 0; i < 30; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 1.5;
                sl.sendParticles(ParticleTypes.END_ROD,
                        sp.getX() + Math.cos(a) * r,
                        sp.getY() + Math.random() * 2,
                        sp.getZ() + Math.sin(a) * r,
                        1, 0, 0.1, 0, 0.02);
            }

            sp.displayClientMessage(Component.literal(
                    "§f§l✦ §r§fO fio guia por entre os chunks. §7(8s)"), true);
            sl.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.0F, 2.0F);

            stack.shrink(1);
            sp.getCooldowns().addCooldown(this, 800); // 40s
            return InteractionResultHolder.consume(stack);
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§f§o§lFio Pálido").withStyle(ChatFormatting.WHITE));
            t.add(Component.literal("§7§oFio fino de cor branca-cinza, frio ao toque."));
            t.add(Component.empty());
            t.add(Component.literal("§7Right-click — §lprojeção etérea§r§7 8s:"));
            t.add(Component.literal("§7• §aSpeed IV§r§7 + §aJump III§r§7 + §aSlow Falling§r§7"));
            t.add(Component.literal("§7• §aInvisibility§r§7 + §aResistance V§r§7 + §aNight Vision§r§7"));
            t.add(Component.literal("§7• Caminhe pelos §dvãos invisíveis§r§7 entre chunks"));
            t.add(Component.empty());
            t.add(Component.literal("§7Consome 1 por uso"));
            t.add(Component.literal("§8§o\"O fio passa onde o corpo não pode.\""));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 4. MIRROR FRUIT — comida com efeitos de mundo espelhado
    // ════════════════════════════════════════════════════════════
    public static class MirrorFruitItem extends Item {
        public MirrorFruitItem(Properties p) { super(p.rarity(Rarity.RARE).stacksTo(4)
                .food(new FoodProperties.Builder()
                        .nutrition(2)
                        .saturationMod(0.3F)
                        .alwaysEat()
                        .build())); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            ItemStack result = super.finishUsingItem(stack, level, entity);
            if (level.isClientSide || !(entity instanceof ServerPlayer sp)) return result;

            ServerLevel sl = sp.serverLevel();

            // 1. Levitation breve (gravidade reversa)
            sp.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0, true, true));
            sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, true, true));

            // 2. Spawn "mirrored mob" hallucinations — vê mobs invertidos
            for (int i = 0; i < 4; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 3 + Math.random() * 3;
                sl.sendParticles(sp, ModParticles.VULTO_SHADOW.get(), true,
                        sp.getX() + Math.cos(a) * r,
                        sp.getY() + 0.5,
                        sp.getZ() + Math.sin(a) * r,
                        3, 0, 0, 0, 0);
            }

            // 3. Alternate weather — toggle weather state localmente (visual)
            HallucinationManager.force(sp, HallucinationType.SCREEN_GLITCH_BURST, 0.6F, 40, "");
            HallucinationManager.force(sp, HallucinationType.IMPOSSIBLE_MOON, 0.8F, 100, "");

            // 4. Reveal duplicate world — fake_block_flash em volta
            for (int i = 0; i < 8; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 2 + Math.random() * 5;
                HallucinationManager.forceAt(sp, HallucinationType.FAKE_BLOCK_FLASH, 0.7F, 40,
                        (float)(Math.cos(a) * r), 0,
                        (float)(Math.sin(a) * r), "");
            }

            // Cost
            InsanityData.addInsanity(sp, 4);
            InsanityData.addCosmicInfluence(sp, 2);

            sp.displayClientMessage(Component.literal(
                    "§b§l✦ §r§bO mundo reflete o que não deveria existir."), false);
            return result;
        }

        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§b§o§lFruto Espelho").withStyle(ChatFormatting.AQUA));
            t.add(Component.literal("§7§oUma fruta viva que reflete o mundo errado."));
            t.add(Component.empty());
            t.add(Component.literal("§7§lComer:"));
            t.add(Component.literal("§7• §aLevitation 2s§r§7 + §aSlow Falling 10s§r §7(gravidade reversa)"));
            t.add(Component.literal("§7• §dMobs espelhados§r§7 em volta (vulto particles)"));
            t.add(Component.literal("§7• §6Impossible moon§r§7 no céu"));
            t.add(Component.literal("§7• §0Fake blocks§r§7 piscando ao redor"));
            t.add(Component.empty());
            t.add(Component.literal("§c-4 insanity §7(consumo único)"));
            t.add(Component.literal("§8§o\"A boca sente o gosto de outro mundo.\""));
        }
    }
}
