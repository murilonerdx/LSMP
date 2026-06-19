package br.com.murilo.liberthia.occult;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r32 — Sistema OCULTO COMPLETO.
 *
 * <p>Containers de todas as classes de itens ocultos:
 * <ul>
 *   <li><b>5 Chalks</b> (White, Gold, Purple, Red, Black) — desenham {@link ChalkMarkBlock}</li>
 *   <li><b>Lighter</b> — acende candles e TNT</li>
 *   <li><b>11 Sigils</b> — nomes secretos de entidades (Bael, Metatron, Lúcifer etc.)</li>
 *   <li><b>Ritual Dagger</b> — usada em sacrifícios</li>
 *   <li><b>Ritual Chalice</b> — recipiente de oferendas líquidas</li>
 * </ul>
 *
 * <h2>Inspiração</h2>
 * Sistema baseado em Occultism mod + Goetia (Lemegeton) + Kabbalah +
 * Hermetism. Cada item tem função ESPECÍFICA em rituais.
 */
public final class OccultItems {

    private OccultItems() {}

    /** Os 5 tipos de chalk no sistema. Cada um abre rituais diferentes. */
    public enum ChalkColor {
        WHITE("white",   0xFFFFFFFF, "branca",   "Banimento, proteção, purificação"),
        GOLDEN("golden", 0xFFFFD700, "dourada",  "Anjos, evocações divinas"),
        PURPLE("purple", 0xFF8A2BE2, "roxa",     "Astral, espíritos, conexão"),
        RED("red",       0xFFCC0033, "vermelha", "Demônios goéticos, sangue"),
        BLACK("black",   0xFF1A1A1A, "preta",    "Necromancia, mortos");

        public final String id;
        public final int color;
        public final String displayName;
        public final String useCase;

        ChalkColor(String id, int color, String displayName, String useCase) {
            this.id = id;
            this.color = color;
            this.displayName = displayName;
            this.useCase = useCase;
        }
    }

    /**
     * Item de giz — coloca {@link ChalkMarkBlock} no bloco que clicar (face up).
     * Right-click sneaking cicla shape: dot → line → circle → sigil → dot.
     */
    public static class ChalkItem extends Item {
        public final ChalkColor color;
        public ChalkItem(Properties p, ChalkColor c) { super(p.durability(64)); this.color = c; }
        @Override public boolean isFoil(ItemStack s) { return color == ChalkColor.GOLDEN; }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Level level = ctx.getLevel();
            BlockPos pos = ctx.getClickedPos();
            BlockState target = level.getBlockState(pos);
            Direction face = ctx.getClickedFace();
            // Só desenha em top face de bloco sólido
            if (face != Direction.UP) return InteractionResult.PASS;
            BlockPos above = pos.above();
            if (!level.getBlockState(above).isAir()) return InteractionResult.PASS;
            if (!target.isFaceSturdy(level, pos, Direction.UP)) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;

            ChalkMarkBlock chalkBlock = ChalkMarkBlock.getBlockFor(color);
            if (chalkBlock == null) return InteractionResult.FAIL;
            // Cycle shape se sneaking
            ChalkMarkBlock.Shape shape = ChalkMarkBlock.Shape.DOT;
            if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) {
                CompoundTagHolder.cycleShape(ctx.getItemInHand());
                shape = CompoundTagHolder.getShape(ctx.getItemInHand());
            } else {
                shape = CompoundTagHolder.getShape(ctx.getItemInHand());
            }
            BlockState placed = chalkBlock.defaultBlockState()
                    .setValue(ChalkMarkBlock.SHAPE, shape);
            level.setBlock(above, placed, 3);
            level.playSound(null, above, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.3F, 1.5F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.WHITE_ASH,
                        above.getX() + 0.5, above.getY() + 0.1, above.getZ() + 0.5,
                        4, 0.2, 0.05, 0.2, 0);
            }
            // Consome durabilidade
            ItemStack stack = ctx.getItemInHand();
            stack.hurtAndBreak(1, ctx.getPlayer(), p -> p.broadcastBreakEvent(ctx.getHand()));
            return InteractionResult.CONSUME;
        }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§7Giz " + color.displayName).withStyle(ChatFormatting.GRAY));
            t.add(Component.literal("§8• " + color.useCase).withStyle(ChatFormatting.DARK_GRAY));
            t.add(Component.empty());
            t.add(Component.literal("§7Use no §echão§r§7 pra desenhar marca."));
            t.add(Component.literal("§7Shift+Use cicla: §8dot §7→ §8line §7→ §8circle §7→ §8sigil"));
            t.add(Component.literal("§7Forma atual: §f" + CompoundTagHolder.getShape(s).name()));
        }

        /** Helper pra ler/escrever Shape no NBT do chalk item. */
        public static final class CompoundTagHolder {
            public static ChalkMarkBlock.Shape getShape(ItemStack stack) {
                if (!stack.hasTag()) return ChalkMarkBlock.Shape.DOT;
                int idx = stack.getTag().getInt("Shape");
                ChalkMarkBlock.Shape[] all = ChalkMarkBlock.Shape.values();
                return all[idx % all.length];
            }
            public static void cycleShape(ItemStack stack) {
                int next = (getShape(stack).ordinal() + 1) % ChalkMarkBlock.Shape.values().length;
                stack.getOrCreateTag().putInt("Shape", next);
            }
        }
    }

    /**
     * <b>Isqueiro</b> — acende {@code CandleBlock} (estado lit=true) ou ignita
     * TNT/Campfire. Tem durabilidade.
     */
    public static class LighterItem extends Item {
        public LighterItem(Properties p) { super(p.durability(80)); }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Level level = ctx.getLevel();
            BlockPos pos = ctx.getClickedPos();
            BlockState state = level.getBlockState(pos);
            Player player = ctx.getPlayer();
            // Acende candle custom
            if (state.getBlock() instanceof CandleBlock) {
                if (!state.getValue(CandleBlock.LIT)) {
                    if (level.isClientSide) return InteractionResult.SUCCESS;
                    level.setBlock(pos, state.setValue(CandleBlock.LIT, true), 3);
                    level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE,
                            SoundSource.BLOCKS, 0.8F, 1.2F);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.FLAME,
                                pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5,
                                5, 0.1, 0.05, 0.1, 0.01);
                    }
                    ctx.getItemInHand().hurtAndBreak(1, player,
                            p -> p.broadcastBreakEvent(ctx.getHand()));
                    return InteractionResult.CONSUME;
                }
                return InteractionResult.PASS;
            }
            // Acende TNT
            if (state.is(Blocks.TNT)) {
                if (level.isClientSide) return InteractionResult.SUCCESS;
                level.removeBlock(pos, false);
                var tnt = new net.minecraft.world.entity.item.PrimedTnt(level,
                        pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player);
                level.addFreshEntity(tnt);
                level.playSound(null, pos, SoundEvents.TNT_PRIMED,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                ctx.getItemInHand().hurtAndBreak(1, player,
                        p -> p.broadcastBreakEvent(ctx.getHand()));
                return InteractionResult.CONSUME;
            }
            // Fogo simples em air adjacente (como flint&steel mas só 1 charge)
            if (level.getBlockState(pos.relative(ctx.getClickedFace())).isAir()) {
                if (level.isClientSide) return InteractionResult.SUCCESS;
                level.setBlock(pos.relative(ctx.getClickedFace()),
                        Blocks.FIRE.defaultBlockState(), 3);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                ctx.getItemInHand().hurtAndBreak(1, player,
                        p -> p.broadcastBreakEvent(ctx.getHand()));
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§7Isqueiro Ritualístico").withStyle(ChatFormatting.GRAY));
            t.add(Component.literal("§8Acende velas, TNT, fogo"));
        }
    }

    /**
     * <b>Sigilo (Nome Secreto)</b> — item que representa o NOME VERDADEIRO de
     * uma entidade da Goetia / hierarquia angélica. Necessário em ritual pra
     * invocar a entidade específica.
     */
    public static class SigilItem extends Item {
        public final String entityName;
        public final String tradition; // "goetia", "shemhamphorash", "kabbalah", "cosmic"
        public final String lore;

        public SigilItem(Properties p, String entityName, String tradition, String lore) {
            super(p.stacksTo(1));
            this.entityName = entityName;
            this.tradition = tradition;
            this.lore = lore;
        }

        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§d§oSigilo de " + entityName).withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Tradição: §5" + tradition).withStyle(ChatFormatting.DARK_PURPLE));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"" + lore + "\""));
            t.add(Component.empty());
            t.add(Component.literal("§7Use em §dRitual Circle§r§7 pra invocar."));
        }
    }

    /**
     * <b>Adaga Ritualística</b> — usada pra sacrifícios em rituais.
     * Right-click em mob inside ritual area → mata + marca ritual.
     */
    public static class RitualDaggerItem extends Item {
        public RitualDaggerItem(Properties p) { super(p.stacksTo(1).durability(128)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player user,
                                                       LivingEntity target, InteractionHand hand) {
            if (user.level().isClientSide) return InteractionResult.SUCCESS;
            if (!(user instanceof ServerPlayer sp)) return InteractionResult.PASS;
            // Marca o target como sacrifício ritualístico
            target.getPersistentData().putString("liberthia.ritual_sacrifice",
                    sp.getUUID().toString());
            target.getPersistentData().putLong("liberthia.ritual_sacrifice_time",
                    sp.level().getGameTime());
            // Dano grande + bleeding
            target.hurt(target.damageSources().playerAttack(sp), 12.0F);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WITHER, 100, 1));
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        target.getX(), target.getY() + 1, target.getZ(),
                        25, 0.3, 0.5, 0.3, 0.05);
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        8, 0.2, 0.2, 0.2, 0.05);
            }
            sp.level().playSound(null, target.blockPosition(),
                    SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5F, 0.5F);
            stack.hurtAndBreak(1, user, p -> p.broadcastBreakEvent(hand));
            return InteractionResult.CONSUME;
        }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§4§oAdaga Ritualística").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Use em mob/animal pra §csacrificar§r§7"));
            t.add(Component.literal("§7(necessário em alguns rituais)."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Athame — a lâmina cerimonial. Corta véus,"));
            t.add(Component.literal("§8§o nunca carne mundana.\""));
        }
    }

    /**
     * <b>Cálice Ritualístico</b> — recipiente pra oferendas líquidas em rituais.
     * Right-click em água/lava → enche. Use em ritual circle → oferece líquido.
     */
    public static class RitualChaliceItem extends Item {
        public RitualChaliceItem(Properties p) { super(p.stacksTo(1)); }
        @Override public boolean isFoil(ItemStack s) { return true; }

        /**
         * r164 FIX (bug #38): item agora ENCHE de verdade.
         * <ul>
         *   <li>Right-click em água → guarda "water"</li>
         *   <li>Right-click em lava → guarda "lava"</li>
         *   <li>Right-click numa entidade hostil viva → tira 1 dano + guarda "blood"</li>
         *   <li>Right-click com cálice cheio (não em fluido) → esvazia</li>
         * </ul>
         */
        @Override
        public net.minecraft.world.InteractionResultHolder<ItemStack> use(
                Level level, net.minecraft.world.entity.player.Player player,
                net.minecraft.world.InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);

            // Raycast: pega bloco mirado (com fluido) pra detectar água/lava
            net.minecraft.world.phys.HitResult hr = player.pick(5.0, 1.0F, true);
            if (hr instanceof net.minecraft.world.phys.BlockHitResult bhr) {
                net.minecraft.world.level.material.FluidState fs =
                        level.getFluidState(bhr.getBlockPos());
                if (!fs.isEmpty()) {
                    String fluidId = null;
                    if (fs.is(net.minecraft.tags.FluidTags.WATER)) fluidId = "water";
                    else if (fs.is(net.minecraft.tags.FluidTags.LAVA)) fluidId = "lava";
                    if (fluidId != null) {
                        net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
                        tag.putString("Fluid", fluidId);
                        if (!level.isClientSide) {
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                    "§6Cálice cheio com §b" + fluidId), true);
                            level.playSound(null, player.blockPosition(),
                                    net.minecraft.sounds.SoundEvents.BOTTLE_FILL,
                                    net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);
                        }
                        return net.minecraft.world.InteractionResultHolder.success(stack);
                    }
                }
            }

            // Cálice já cheio + mão vazia/RClick livre → esvazia
            if (stack.hasTag() && stack.getTag().contains("Fluid")) {
                if (!level.isClientSide) {
                    String prev = stack.getTag().getString("Fluid");
                    stack.getTag().remove("Fluid");
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "§7Cálice esvaziado (§o" + prev + "§7)"), true);
                    level.playSound(null, player.blockPosition(),
                            net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.7f);
                }
                return net.minecraft.world.InteractionResultHolder.success(stack);
            }

            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }

        /**
         * r164: RClick numa entidade viva hostil → drena 1 HP e guarda "blood".
         */
        @Override
        public net.minecraft.world.InteractionResult interactLivingEntity(
                ItemStack stack, net.minecraft.world.entity.player.Player player,
                net.minecraft.world.entity.LivingEntity target,
                net.minecraft.world.InteractionHand hand) {
            if (player.level().isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
            if (target.isDeadOrDying()) return net.minecraft.world.InteractionResult.PASS;
            // Drena 1 HP do alvo
            target.hurt(player.damageSources().playerAttack(player), 1.0F);
            net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
            tag.putString("Fluid", "blood");
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§4Cálice cheio com §csangue§4 de " + target.getName().getString()), true);
            player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.HONEY_BLOCK_FALL,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.4f);
            return net.minecraft.world.InteractionResult.CONSUME;
        }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§6§oCálice Ritualístico").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Recipiente pra oferendas líquidas."));
            t.add(Component.literal("§8• RClick em água/lava pra encher"));
            t.add(Component.literal("§8• RClick em criatura pra coletar §csangue"));
            t.add(Component.literal("§8• Cheio + RClick no ar = esvaziar"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"O cálice contém o vinho do espírito."));
            t.add(Component.literal("§8§o Bebido, transmuta.\""));
            // Mostra conteúdo se houver
            if (s.hasTag() && s.getTag().contains("Fluid")) {
                t.add(Component.empty());
                String fluid = s.getTag().getString("Fluid");
                String color = fluid.equals("blood") ? "§c"
                        : fluid.equals("lava") ? "§6" : "§9";
                t.add(Component.literal("§7Contém: " + color + fluid));
            }
        }
    }
}
