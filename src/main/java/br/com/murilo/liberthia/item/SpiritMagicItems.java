package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r31 — REWRITE: 20 itens com identidade própria de OCULTISMO
 * CERIMONIAL REAL.
 *
 * <h2>Inspiração / fontes</h2>
 * <ul>
 *   <li><b>Goetia / Lemegeton</b> — 72 demônios + sigilos próprios (Bael, Lilith)</li>
 *   <li><b>Magia Enoquiana</b> (Dr. John Dee) — Watchtowers, sigilos angélicos</li>
 *   <li><b>Cabala (Kabbalah)</b> — Sephirot, Metatron, Sandalphon, Ophanim</li>
 *   <li><b>Vodou Haitiano</b> — Veve, Papa Legba das encruzilhadas</li>
 *   <li><b>Chaos magick</b> — sigilos servidores, banishing rituals</li>
 *   <li><b>Hauntologia</b> (Mark Fisher) — presente assombrado pelo que não foi</li>
 *   <li><b>Lovecraft</b> — Azathoth, Nyarlathotep, geometria não-euclidiana</li>
 *   <li><b>Cthulhu Mythos</b> — sussurros, testemunha cósmica</li>
 * </ul>
 *
 * <h2>Princípio de design</h2>
 * Cada item tem mecânica que ressoa com sua tradição. Não é só "right-click
 * pra effect X" — tem flavor narrativo, lore tooltip, e referência clara.
 */
public final class SpiritMagicItems {

    private SpiritMagicItems() {}

    /** Base — active com cooldown. */
    public static abstract class EffectOnUseItem extends Item {
        protected final int cooldownTicks;
        public EffectOnUseItem(Properties p, int cd) { super(p); this.cooldownTicks = cd; }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
            boolean ok = doUse(sp, level, stack);
            if (ok) sp.getCooldowns().addCooldown(this, cooldownTicks);
            return ok ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
        }
        protected abstract boolean doUse(ServerPlayer sp, Level level, ItemStack stack);
    }

    // ═════════════════════════════════════════════════════════════
    //  COSMIC HORROR — Lovecraft + hauntologia + ocultismo escuro
    // ═════════════════════════════════════════════════════════════

    /**
     * <b>Véu da Testemunha Silenciosa</b> — você se torna testemunha de todo
     * sofrimento no servidor. Passive: quando qualquer player toma dano,
     * você recebe um sussurro com nome + dano em chat.
     * Referência: hauntologia — você é assombrado pelo que outros sofrem.
     */
    public static class WhisperingVeil extends Item {
        public WhisperingVeil(Properties p) { super(p); }
        @Override public boolean isFoil(ItemStack s) { return true; }
        /** Static checked by event handler. */
        public static boolean hasVeil(LivingEntity entity) {
            if (!(entity instanceof Player p)) return false;
            var v = br.com.murilo.liberthia.registry.ModItems.WHISPERING_VEIL.get();
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).is(v)) return true;
            }
            return false;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oVéu da Testemunha").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Você ouve cada §dgrito§r§7 do servidor."));
            t.add(Component.literal("§7Sussurros de dano alheio chegam ao seu chat."));
            t.add(Component.empty());
            t.add(Component.literal("§8§oA hauntologia te assombra. Você sente o que outros sentem,"));
            t.add(Component.literal("§8§odeitado no mesmo continuum espectral."));
        }
    }

    /**
     * <b>Olho de Azathoth</b> — vidência abissal. Reveala TODAS entidades
     * num raio 64b com Glowing por 30s + mostra HP delas no chat.
     * Custo: 3 dano + 100 ticks de Confusion (você vê o "Sultão Daemoníaco" e
     * sua mente fratura).
     */
    public static class EyesOfAbyss extends EffectOnUseItem {
        public EyesOfAbyss(Properties p) { super(p, 1200); }
        @Override
        protected boolean doUse(ServerPlayer sp, Level level, ItemStack s) {
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, true, false));
            ServerLevel sl = (ServerLevel) level;
            int counted = 0;
            StringBuilder sb = new StringBuilder("§5✦ §dRevelação Abissal: ");
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                    sp.getBoundingBox().inflate(64))) {
                if (e == sp) continue;
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0, true, false));
                if (counted < 5) {
                    sb.append(String.format("§f%s §c%.0f/%.0f§7 ",
                            e.getName().getString(), e.getHealth(), e.getMaxHealth()));
                }
                counted++;
            }
            if (counted == 0) sb.append("§8(vazio cósmico)");
            else if (counted > 5) sb.append("§8...+").append(counted - 5).append(" outros");
            sp.displayClientMessage(Component.literal(sb.toString()), false);
            // Custo: 3 dano + confusão (Azathoth quebra a mente)
            sp.hurt(sp.damageSources().magic(), 3.0F);
            sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false));
            sl.playSound(null, sp.blockPosition(), SoundEvents.PORTAL_AMBIENT,
                    SoundSource.PLAYERS, 1.2F, 0.4F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oOlho do Sultão Daemoníaco").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Revela §dTODAS§r§7 entidades num raio §e64 blocos§r§7"));
            t.add(Component.literal("§7+ §emostra HP§r§7 das 5 mais próximas no chat."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Os olhos d'Aquele-Que-Dorme contemplam"));
            t.add(Component.literal("§8§o tudo. Para vê-Lo, sua mente cede.\""));
            t.add(Component.literal("§c§o-3HP + Náusea. CD 60s."));
        }
    }

    /**
     * <b>Berço de Lilith</b> — invoca uma SOMBRA PROTETORA (zumbi neutro
     * renomeado "Filho de Lilith") que segue você 5 min e ataca quem te ataca.
     * Referência: Lilith = primeira esposa de Adão (folclore judaico), mãe de
     * demônios noturnos.
     */
    public static class CursedCradle extends EffectOnUseItem {
        public CursedCradle(Properties p) { super(p, 6000); } // 5min
        @Override
        protected boolean doUse(ServerPlayer sp, Level level, ItemStack s) {
            ServerLevel sl = (ServerLevel) level;
            // Spawna zombie passive (sem AI hostil) renomeado
            var z = net.minecraft.world.entity.EntityType.ZOMBIE.create(sl);
            if (z == null) return false;
            Vec3 forward = sp.getLookAngle();
            z.moveTo(sp.getX() + forward.x * 2, sp.getY(), sp.getZ() + forward.z * 2,
                    sp.getYRot(), 0);
            z.setCustomName(Component.literal("§5Filho de Lilith"));
            z.setCustomNameVisible(true);
            // r31: tag pra HurtHandler identificar e proteger o caster (futuro)
            z.getPersistentData().putString("liberthia.lilith_owner", sp.getUUID().toString());
            z.getPersistentData().putLong("liberthia.lilith_expire",
                    sl.getGameTime() + 6000);
            // Despawn em 5min via persistentData (não pickup vanilla)
            z.setPersistenceRequired();
            // Buff: speed II + invisibility breve pra dramatic spawn
            z.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6000, 1, false, false));
            z.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 1, false, false));
            // FIX (#46): fire res pra não queimar no sol (aliado de longa duração).
            // O comportamento de ALIADO (não atacar o dono + atacar quem te ataca)
            // é garantido por LilithAllyEvents.
            z.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0, false, false));
            z.setTarget(null); // nasce sem mirar em ninguém
            // Heal: 20 HP
            z.setHealth(20.0F);
            sl.addFreshEntity(z);
            sl.sendParticles(ParticleTypes.SOUL,
                    z.getX(), z.getY() + 1, z.getZ(), 30, 0.3, 1.0, 0.3, 0.1);
            sl.playSound(null, sp.blockPosition(), SoundEvents.WITHER_SPAWN,
                    SoundSource.PLAYERS, 0.5F, 1.8F);
            sp.displayClientMessage(Component.literal(
                    "§5✦ §dFilho de Lilith desperta e te segue por 5min."), true);
            sp.hurt(sp.damageSources().magic(), 2.0F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§4§oBerço de Lilith").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Invoca um §dFilho de Lilith§r§7 — sombra que"));
            t.add(Component.literal("§7te §aprotege§r§7 por 5min antes de se desfazer."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Lilith, mãe da noite, parteja seu rebanho"));
            t.add(Component.literal("§8§o de cabelos negros e olhos vermelhos.\""));
            t.add(Component.literal("§c§o-2HP. CD 5min."));
        }
    }

    /**
     * <b>Pêndulo de Foucault</b> — passive: a cada 1s mostra particula
     * ÂMBAR onde o próximo ataque do mob hostil mais próximo cairá (predição).
     * Referência: pêndulo de Foucault registra a rotação da Terra — você vê o
     * tempo passar em tempo real.
     */
    public static class PendulumOfDread extends Item {
        public PendulumOfDread(Properties p) { super(p); }
        @Override public boolean isFoil(ItemStack s) { return true; }
        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean sel) {
            if (level.isClientSide || !(entity instanceof ServerPlayer holder)) return;
            // r164 FIX (bug #41): só tickar se item está na MAIN HAND, e mais frequente.
            if (!sel) return;  // só quando selecionado na hotbar
            if (level.getGameTime() % 10 != 0) return; // 2x/s pra ser mais responsivo
            ServerLevel sl = (ServerLevel) level;
            Monster nearest = null;
            double bestDist = 16 * 16; // raio 16
            for (Monster m : sl.getEntitiesOfClass(Monster.class,
                    holder.getBoundingBox().inflate(16))) {
                double d = m.distanceToSqr(holder);
                if (d < bestDist) {
                    bestDist = d;
                    nearest = m;
                }
            }
            if (nearest == null) {
                // r164: feedback visual mesmo quando NÃO tem mob — partícula
                // sutil em cima do player pra confirmar que o item ESTÁ ativo
                if (level.getGameTime() % 60 == 0) {
                    sl.sendParticles(ParticleTypes.ENCHANT,
                            holder.getX(), holder.getY() + 2.2, holder.getZ(),
                            3, 0.2, 0.1, 0.2, 0.01);
                }
                return;
            }
            // Prediz posição em 1s baseado em velocity atual
            Vec3 vel = nearest.getDeltaMovement();
            Vec3 predicted = nearest.position().add(vel.scale(20));
            // r164 FIX: WAX_OFF era quase invisível — usa FLAME + REDSTONE (brilhante)
            sl.sendParticles(ParticleTypes.FLAME,
                    predicted.x, predicted.y + 1.0, predicted.z,
                    12, 0.3, 0.5, 0.3, 0.02);
            sl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(1.0f, 0.2f, 0.8f), 1.2f),
                    predicted.x, predicted.y + 0.5, predicted.z,
                    8, 0.4, 0.4, 0.4, 0.0);
            // Ghost ring marker no chão — bem mais largo + brilhante
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        predicted.x + Math.cos(a) * 1.2,
                        predicted.y + 0.05,
                        predicted.z + Math.sin(a) * 1.2,
                        1, 0, 0, 0, 0);
            }
            // Linha do player até o predicted (pra player saber pra ONDE olhar)
            Vec3 from = holder.position().add(0, 1.4, 0);
            double dist = from.distanceTo(predicted);
            for (double d = 1; d < dist; d += 0.7) {
                double t = d / dist;
                Vec3 step = from.add(predicted.subtract(from).scale(t));
                sl.sendParticles(ParticleTypes.END_ROD, step.x, step.y, step.z, 1, 0, 0, 0, 0);
            }
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oPêndulo de Foucault").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Mostra onde o §chostil mais próximo§r§7 estará"));
            t.add(Component.literal("§7em §e1 segundo§r§7. Ajude a esquivar."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"O pêndulo registra a rotação. Em segundos,"));
            t.add(Component.literal("§8§o a faca já saiu da bainha.\""));
        }
    }

    /**
     * <b>Lanterna da Hauntologia</b> — active: por 5min, mostra trilhas
     * fantasmas (SOUL particles) das entidades que passaram pela área.
     */
    public static class LanternOfFalseMemory extends EffectOnUseItem {
        public LanternOfFalseMemory(Properties p) { super(p, 1200); }
        @Override
        protected boolean doUse(ServerPlayer sp, Level level, ItemStack s) {
            ServerLevel sl = (ServerLevel) level;
            int radius = 24;
            // Pulse 1: revela todas entities no raio (fantasmas)
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                    sp.getBoundingBox().inflate(radius))) {
                if (e == sp) continue;
                // Trilha de partículas SOUL ao redor
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6;
                    sl.sendParticles(ParticleTypes.SOUL,
                            e.getX() + Math.cos(a) * 1.2,
                            e.getY() + 0.2,
                            e.getZ() + Math.sin(a) * 1.2,
                            1, 0, 0.5, 0, 0.02);
                }
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, true, false));
            }
            // Trilha vertical onde mortes recentes ocorreram (event-driven seria ideal)
            sp.displayClientMessage(Component.literal(
                    "§b§l✦ §r§bA hauntologia revela todos espíritos próximos..."), true);
            sl.playSound(null, sp.blockPosition(), SoundEvents.SOUL_ESCAPE,
                    SoundSource.PLAYERS, 1.5F, 0.5F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§b§oLanterna da Hauntologia").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Revela §dtrilhas espectrais§r§7 num raio 24b"));
            t.add(Component.literal("§7+ aplica Glowing por 10s nas entidades."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"O passado nunca está morto. Não está sequer"));
            t.add(Component.literal("§8§o passado.\" — Mark Fisher"));
        }
    }

    /**
     * <b>Língua de Glossolália</b> — quando segurar, sua mensagem no chat
     * vira "Z̸̢̛͕̩̗A̵̧͔̬͉͐̕Ļ̴̛̥͕̌͒G̷̛͚̲͓Ó̸͙͕͙" pra todos que NÃO têm a língua
     * tambem.
     * Handler em {@code MadnessEvents.onServerChat} (separado).
     */
    public static class TongueOfOldOnes extends Item {
        public TongueOfOldOnes(Properties p) { super(p); }
        @Override public boolean isFoil(ItemStack s) { return true; }
        public static boolean hasTongue(LivingEntity entity) {
            if (!(entity instanceof Player p)) return false;
            var v = br.com.murilo.liberthia.registry.ModItems.TONGUE_OF_OLD_ONES.get();
            if (p.getMainHandItem().is(v)) return true;
            if (p.getOffhandItem().is(v)) return true;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).is(v)) return true;
            }
            return false;
        }
        /**
         * r34 FIX: Embaralha texto em "Enoquiano". Antes usava caracteres Unicode
         * raros (∆◊◉҂Ψ) que NÃO renderizavam na vanilla Minecraft font (apareciam
         * como □ quadrados vazios). Agora usa um alfabeto enoquiano legível +
         * permutações consistentes do alfabeto latino para que o texto pareça
         * "linguagem estranha" mas renderize bonito.
         *
         * <p>Substitui cada letra por um caractere pseudo-runic (alfabeto que A
         * vira "Ω", B vira "ξ", etc) com mapeamento determinístico. Players podem
         * ver que É uma linguagem específica, não lixo random.
         */
        public static String scramble(String s) {
            // 26 chars rúnicos/gregos selecionados — TODOS renderizam na font padrão
            char[] runeAlpha = {
                'A','B','C','D','E','F','G','H','I','J','K','L','M',
                'N','O','P','Q','R','S','T','U','V','W','X','Y','Z'
            };
            // Mapeamento enoquiano (alfabeto Enoquiano de John Dee aproximado):
            // A→Z, B→Y, C→X... (espelhamento) + alguns símbolos
            char[] runeMap = {
                'Z','Y','X','W','V','U','T','S','R','Q','P','O','N',
                'M','L','K','J','I','H','G','F','E','D','C','B','A'
            };
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (Character.isWhitespace(c)) {
                    sb.append(c);
                } else if (c >= 'a' && c <= 'z') {
                    sb.append(Character.toLowerCase(runeMap[c - 'a']));
                } else if (c >= 'A' && c <= 'Z') {
                    sb.append(runeMap[c - 'A']);
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§4§oLíngua de Glossolália").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Suas mensagens viram §dgibberish enoquiano§r§7 pra"));
            t.add(Component.literal("§7quem §lnão§r§7 também segura uma Língua."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"E na presença d'Eles, falamos línguas"));
            t.add(Component.literal("§8§o que não temos.\" — Atos 2 ressignificado"));
        }
    }

    /**
     * <b>Ampulheta de Cronos</b> — recua sua posição 10s + deixa um rastro
     * de "fantasmas" do caminho.
     */
    public static class HourglassOfRegression extends Item {
        public HourglassOfRegression(Properties p) { super(p); }
        @Override public boolean isFoil(ItemStack s) { return true; }
        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean sel) {
            if (level.isClientSide || !(entity instanceof ServerPlayer sp)) return;
            if (level.getGameTime() % 20 != 0) return;
            CompoundTag tag = stack.getOrCreateTag();
            int idx = (int) ((level.getGameTime() / 20) % 10);
            tag.putDouble("X" + idx, sp.getX());
            tag.putDouble("Y" + idx, sp.getY());
            tag.putDouble("Z" + idx, sp.getZ());
        }
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
            CompoundTag tag = stack.getOrCreateTag();
            int oldestIdx = (int) (((level.getGameTime() / 20) + 1) % 10);
            if (!tag.contains("X" + oldestIdx)) {
                sp.displayClientMessage(Component.literal("§7Cronos ainda recolhe a areia..."), true);
                return InteractionResultHolder.fail(stack);
            }
            // r31: cria rastro de ghosts pelo caminho
            ServerLevel sl = (ServerLevel) level;
            for (int i = 0; i < 10; i++) {
                int ii = (oldestIdx + i) % 10;
                if (!tag.contains("X" + ii)) continue;
                double gx = tag.getDouble("X" + ii);
                double gy = tag.getDouble("Y" + ii);
                double gz = tag.getDouble("Z" + ii);
                sl.sendParticles(ParticleTypes.SOUL,
                        gx, gy + 1, gz, 8, 0.2, 0.5, 0.2, 0.02);
            }
            double x = tag.getDouble("X" + oldestIdx);
            double y = tag.getDouble("Y" + oldestIdx);
            double z = tag.getDouble("Z" + oldestIdx);
            sp.teleportTo(x, y, z);
            sl.playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.0F, 0.4F);
            sp.getCooldowns().addCooldown(this, 600);
            sp.displayClientMessage(Component.literal(
                    "§5✦ §dCronos volta a areia. Você revive os 10s."), true);
            return InteractionResultHolder.consume(stack);
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oAmpulheta de Cronos").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Volta sua posição pra §d10 segundos atrás§r§7"));
            t.add(Component.literal("§7+ deixa §brastros de almas§r§7 pelo caminho."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Cronos devora seus filhos, mas Reia"));
            t.add(Component.literal("§8§o esconde Zeus. O ciclo retrocede.\""));
        }
    }

    /**
     * <b>Orbe de Nyarlathotep</b> — revela "the thing in the corner".
     * Active: 30s glowing em hostis num raio 64b + cria 3 sombras peripherais
     * que apontam pras 3 maiores ameaças (Glowing colorido vermelho).
     */
    public static class VoidSeerOrb extends EffectOnUseItem {
        public VoidSeerOrb(Properties p) { super(p, 1800); }
        @Override
        protected boolean doUse(ServerPlayer sp, Level level, ItemStack s) {
            ServerLevel sl = (ServerLevel) level;
            int counted = 0;
            for (Monster m : sl.getEntitiesOfClass(Monster.class,
                    sp.getBoundingBox().inflate(64))) {
                m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0, true, false));
                // Spawna particles "tentáculo" entre o player e o mob
                Vec3 to = m.position().subtract(sp.position()).normalize();
                for (int i = 1; i <= 5; i++) {
                    Vec3 trail = sp.position().add(to.scale(i));
                    sl.sendParticles(ParticleTypes.DRAGON_BREATH,
                            trail.x, trail.y + 1, trail.z, 1, 0.05, 0.05, 0.05, 0);
                }
                counted++;
            }
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5O Caos Rastejante revela " + counted + " ameaças."), true);
            sl.playSound(null, sp.blockPosition(), SoundEvents.PORTAL_TRIGGER,
                    SoundSource.PLAYERS, 0.7F, 0.3F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oOrbe de Nyarlathotep").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Revela §chostis em 64b§r§7 + trilhas-tentáculo"));
            t.add(Component.literal("§7que apontam pra cada ameaça."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"O Caos Rastejante caminha entre mundos."));
            t.add(Component.literal("§8§o Toda sombra é Sua emanação.\""));
            t.add(Component.literal("§c§oCD 90s."));
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  ANGEL / SPIRIT / CEREMONIAL MAGIC
    // ═════════════════════════════════════════════════════════════

    /**
     * <b>Sigilo de Metatron</b> — Curio. Banishing passivo: hostis num raio
     * 8b tomam 1 dmg/sec; undead 2 dmg/sec. Right-click = Greater Banishing,
     * empurra TODOS hostis 16b longe.
     * Referência: Metatron = arcanjo escriba, voz de Deus; Cubo de Metatron
     * (geometria sagrada).
     */
    public static class HaloOfLight extends Item {
        public HaloOfLight(Properties p) { super(p); }
        @Override public boolean isFoil(ItemStack s) { return true; }
        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean sel) {
            if (level.isClientSide || !(entity instanceof ServerPlayer sp)) return;
            if (level.getGameTime() % 20 != 0) return;
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 50, 0, true, false));
            ServerLevel sl = (ServerLevel) level;
            for (Monster m : sl.getEntitiesOfClass(Monster.class,
                    sp.getBoundingBox().inflate(8))) {
                float dmg = m.getMobType() == net.minecraft.world.entity.MobType.UNDEAD ? 2 : 1;
                m.hurt(m.damageSources().magic(), dmg);
            }
        }
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
            ServerLevel sl = (ServerLevel) level;
            int pushed = 0;
            for (Monster m : sl.getEntitiesOfClass(Monster.class,
                    sp.getBoundingBox().inflate(16))) {
                Vec3 away = m.position().subtract(sp.position()).normalize();
                m.teleportTo(
                        m.getX() + away.x * 20,
                        m.getY(),
                        m.getZ() + away.z * 20);
                sl.sendParticles(ParticleTypes.END_ROD,
                        m.getX(), m.getY() + 1, m.getZ(), 15, 0.3, 0.5, 0.3, 0.1);
                pushed++;
            }
            sl.playSound(null, sp.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS, 2.0F, 1.5F);
            sp.displayClientMessage(Component.literal(
                    "§e§l✦ §r§eGREATER BANISHING — §a" + pushed + " hostis banidos."), true);
            sp.getCooldowns().addCooldown(this, 1200);
            return InteractionResultHolder.consume(stack);
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§e§oSigilo de Metatron").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Hostis num raio §e8b§r§7 tomam dmg/sec contínuo"));
            t.add(Component.literal("§7(undead = §cdobro§r§7). Visão noturna passiva."));
            t.add(Component.literal("§7§eRight-click§r§7: §lGreater Banishing§r§7 — TP 20b"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Metatron, o Escriba, registra cada"));
            t.add(Component.literal("§8§o nome do Livro da Vida.\""));
            t.add(Component.literal("§7Equipável como §6colar§r§7 (Curios)."));
        }
    }

    /**
     * <b>Asas de Lúcifer Caído</b> — active: 8s de Levitation + Slow Falling +
     * Speed II + invulnerability brief 1 tick. Custo: 4 HP.
     */
    public static class WingsOfAscension extends EffectOnUseItem {
        public WingsOfAscension(Properties p) { super(p, 800); }
        @Override
        protected boolean doUse(ServerPlayer sp, Level level, ItemStack s) {
            sp.hurt(sp.damageSources().magic(), 4.0F); // custo
            sp.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 160, 1, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, true, false));
            ServerLevel sl = (ServerLevel) level;
            for (int i = 0; i < 30; i++) {
                double a = i * Math.PI * 2 / 30;
                sl.sendParticles(ParticleTypes.SMOKE,
                        sp.getX() + Math.cos(a) * 1.5,
                        sp.getY() + 0.5,
                        sp.getZ() + Math.sin(a) * 1.5,
                        1, 0, 0.3, 0, 0.05);
            }
            sl.playSound(null, sp.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oAsas de Lúcifer Caído").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7§lLevitation II§r§7 8s + §lSlow Falling§r§7 30s + §lSpeed II§r§7."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Como caíste do céu, ó estrela da manhã!\""));
            t.add(Component.literal("§8§o — Isaías 14:12"));
            t.add(Component.literal("§c§o-4HP. CD 40s."));
        }
    }

    /**
     * <b>Lágrima de Sandalphon</b> — Curio. Previne MORTE uma vez: ao receber
     * dano letal, é consumido e te dá 50% HP + Resistance V por 10s.
     * Handler em {@link br.com.murilo.liberthia.event.AngelTearHandler}.
     */
    public static class AngelTearAmulet extends Item {
        public AngelTearAmulet(Properties p) { super(p); }
        @Override public boolean isFoil(ItemStack s) { return true; }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§e§oLágrima de Sandalphon").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Ao receber §cdano fatal§r§7 uma vez: previne"));
            t.add(Component.literal("§7morte, cura §a50% HP§r§7 + Resistência V 10s,"));
            t.add(Component.literal("§7e a lágrima §evaporiza§r§7."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Sandalphon, anjo das orações, transforma"));
            t.add(Component.literal("§8§o súplicas em coroas.\""));
            t.add(Component.literal("§7Equipável como §6colar§r§7 (Curios)."));
        }
    }

    /**
     * <b>Cordão de Prata Astral</b> — projeção astral. Right-click: 30s de
     * Invisibility + Slow Falling + Speed III + intangível (passa por mobs).
     * Cordão = HUD não implementado mas effect é o astral travel.
     */
    public static class SpiritAnchor extends EffectOnUseItem {
        public SpiritAnchor(Properties p) { super(p, 2400); }
        @Override
        protected boolean doUse(ServerPlayer sp, Level level, ItemStack s) {
            sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, true, false));
            ServerLevel sl = (ServerLevel) level;
            sl.sendParticles(ParticleTypes.END_ROD,
                    sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.3, 0.5, 0.3, 0.1);
            sl.playSound(null, sp.blockPosition(), SoundEvents.PORTAL_TRIGGER,
                    SoundSource.PLAYERS, 0.8F, 1.8F);
            sp.displayClientMessage(Component.literal(
                    "§b§l✦ §r§bProjeção Astral ativada — 30 segundos."), true);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§b§oCordão de Prata Astral").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7§lProjeção Astral§r§7 30s — Invisibilidade,"));
            t.add(Component.literal("§7Speed III, Night Vision, Slow Falling."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"O cordão de prata liga o corpo ao espírito"));
            t.add(Component.literal("§8§o errante. Não o rompa.\" — Eclesiastes 12:6"));
            t.add(Component.literal("§c§oCD 2min."));
        }
    }

    /**
     * <b>Sino dos Ophanim</b> — beam vertical de luz + Regen II + Sat + Resist
     * a todos players num raio 16b.
     * Referência: Ophanim = "rodas de fogo com muitos olhos" (Ezequiel 1:16).
     */
    public static class ChoirBell extends EffectOnUseItem {
        public ChoirBell(Properties p) { super(p, 1600); }
        @Override
        protected boolean doUse(ServerPlayer sp, Level level, ItemStack s) {
            ServerLevel sl = (ServerLevel) level;
            int healed = 0;
            for (Player p : sl.getEntitiesOfClass(Player.class,
                    sp.getBoundingBox().inflate(16))) {
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1, true, true));
                p.addEffect(new MobEffectInstance(MobEffects.SATURATION, 200, 0, true, true));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0, true, true));
                healed++;
            }
            // Beam vertical (Ophanim wheel of fire)
            for (int y = 0; y < 30; y++) {
                sl.sendParticles(ParticleTypes.END_ROD,
                        sp.getX(), sp.getY() + y, sp.getZ(), 3, 0.3, 0, 0.3, 0);
            }
            sl.playSound(null, sp.blockPosition(), SoundEvents.BELL_RESONATE,
                    SoundSource.PLAYERS, 3.0F, 1.5F);
            sp.displayClientMessage(Component.literal(
                    "§e§l✦ §r§eOs Ophanim abençoam " + healed + " aliados."), true);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§e§oSino dos Ophanim").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Aliados num raio §e16b§r§7: §aRegen II§r§7 + §aSat§r§7 + §aResist§r§7."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"E o aspecto das rodas era como o brilho do crisólito;"));
            t.add(Component.literal("§8§o quatro tinham a mesma semelhança...\" — Ezequiel 1:16"));
            t.add(Component.literal("§c§oCD 80s."));
        }
    }

    /**
     * <b>Vara de Salomão</b> — coloca PENTAGRAMA no chão. Hostis dentro por
     * 15s ficam paralisados (Slowness V + Weakness IV + Mining Fatigue V).
     */
    public static class DivineSmiteRod extends EffectOnUseItem {
        public DivineSmiteRod(Properties p) { super(p, 1200); }
        @Override
        protected boolean doUse(ServerPlayer sp, Level level, ItemStack s) {
            HitResult hit = sp.pick(20.0, 0, false);
            ServerLevel sl = (ServerLevel) level;
            Vec3 center;
            if (hit.getType() == HitResult.Type.BLOCK) {
                center = hit.getLocation();
            } else {
                center = sp.position().add(sp.getLookAngle().scale(8));
            }
            // Desenha pentagrama particles
            for (int i = 0; i <= 5; i++) {
                double a = Math.toRadians(-90 + 144 * i);
                double x = center.x + Math.cos(a) * 3;
                double z = center.z + Math.sin(a) * 3;
                for (int k = 0; k < 15; k++) {
                    sl.sendParticles(ParticleTypes.END_ROD,
                            x, center.y + 0.2, z, 1, 0, 0, 0, 0);
                }
            }
            // Aplica paralisia
            int paralyzed = 0;
            for (Monster m : sl.getEntitiesOfClass(Monster.class,
                    new AABB(center.x - 3, center.y - 1, center.z - 3,
                            center.x + 3, center.y + 3, center.z + 3))) {
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 4, false, false));
                m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 3, false, false));
                m.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 300, 4, false, false));
                m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, false));
                paralyzed++;
            }
            sl.playSound(null, BlockPos.containing(center), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN,
                    SoundSource.PLAYERS, 1.5F, 0.8F);
            sp.displayClientMessage(Component.literal(
                    "§e§l✦ §r§eSelo de Salomão — " + paralyzed + " demônios trancados 15s."), true);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§e§oVara de Salomão").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Coloca §dPentagrama§r§7 no chão (raio 3b). Hostis"));
            t.add(Component.literal("§7dentro: §lparalisados§r§7 (Slow V + Weak IV) por 15s."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Salomão domou 72 demônios com seu selo. A vara"));
            t.add(Component.literal("§8§o invocava e o triângulo confinava.\""));
            t.add(Component.literal("§c§oCD 60s."));
        }
    }

    /** <b>Espelho Hermético</b> — Curio. Reflete 25% dmg + endermen nunca enragem. */
    public static class SoulMirror extends Item {
        public SoulMirror(Properties p) { super(p); }
        @Override public boolean isFoil(ItemStack s) { return true; }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§e§oEspelho Hermético").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Reflete §c25% dmg§r§7 ao atacante."));
            t.add(Component.literal("§7Endermen §lnunca§r§7 ficam hostis."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Como em cima, assim embaixo. Como dentro,"));
            t.add(Component.literal("§8§o assim fora.\" — Tábua de Esmeralda"));
            t.add(Component.literal("§7Equipável como §6colar§r§7 (Curios)."));
        }
        public static float reflectFactor(LivingEntity entity) {
            try {
                return top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                        .findFirstCurio(entity, s -> s.is(
                                br.com.murilo.liberthia.registry.ModItems.SOUL_MIRROR.get()))
                        .isPresent() ? 0.25F : 0.0F;
            } catch (Throwable t) { return 0; }
        }
    }

    /** <b>Pêndulo de Radiestesia</b> — Curio. Bookmark + sentido espiritual. */
    public static class SpiritCompass extends Item {
        public SpiritCompass(Properties p) { super(p); }
        @Override public boolean isFoil(ItemStack s) { return true; }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§e§oPêndulo de Radiestesia").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Sente direção espiritual — guia divinatório."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"O pêndulo balança onde a alma chama. A"));
            t.add(Component.literal("§8§o radiestesia lê o invisível.\""));
            t.add(Component.literal("§7Equipável como §6colar§r§7 (Curios)."));
        }
    }

    /** <b>Água Lustral</b> — consagra zona 8×8. */
    public static class HolyWaterBucket extends EffectOnUseItem {
        public HolyWaterBucket(Properties p) { super(p, 800); }
        @Override
        protected boolean doUse(ServerPlayer sp, Level level, ItemStack s) {
            ServerLevel sl = (ServerLevel) level;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                    sp.getBoundingBox().inflate(8))) {
                if (e.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) {
                    e.hurt(sp.damageSources().magic(), 12.0F);
                    e.setSecondsOnFire(8);
                } else if (e instanceof Player p) {
                    p.heal(6.0F);
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
                }
            }
            for (int i = 0; i < 60; i++) {
                double a = i * Math.PI * 2 / 60;
                sl.sendParticles(ParticleTypes.SPLASH,
                        sp.getX() + Math.cos(a) * 4, sp.getY() + 1,
                        sp.getZ() + Math.sin(a) * 4, 1, 0, 0.2, 0, 0.05);
            }
            sl.playSound(null, sp.blockPosition(), SoundEvents.BOTTLE_FILL,
                    SoundSource.PLAYERS, 1.5F, 2.0F);
            s.shrink(1);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§b§oÁgua Lustral").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Consagra zona 8b: cura §aplayers +6HP§r§7,"));
            t.add(Component.literal("§7queima §cundead 12HP + fogo 8s§r§7. Consome 1."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Aspergir-me-ás com hissopo e ficarei puro;"));
            t.add(Component.literal("§8§o lavar-me-ás e me tornarei mais branco que a neve.\" — Salmo 51"));
        }
    }

    /** <b>Pena de Cherub</b> — slow falling 30s + speed boost. */
    public static class AngelWingFeather extends EffectOnUseItem {
        public AngelWingFeather(Properties p) { super(p, 200); }
        @Override
        protected boolean doUse(ServerPlayer sp, Level level, ItemStack s) {
            sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, true, false));
            level.playSound(null, sp.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER,
                    SoundSource.PLAYERS, 0.5F, 2.0F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§f§oPena de Cherub").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Queda lenta 30s + Speed II 10s. CD 10s."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Os Cherubim guardam o caminho da Árvore"));
            t.add(Component.literal("§8§o da Vida, espada flamejante em mãos.\" — Gn 3:24"));
        }
    }

    /**
     * <b>Lâmina Serafim</b> — espada dourada com Holy Fire vs undead (queima
     * 8s ignorando fire resistance).
     */
    public static class SeraphBlade extends SwordItem {
        public SeraphBlade(Properties p) { super(MakeTier(), 4, -2.4F, p); }
        @Override public boolean isFoil(ItemStack s) { return true; }
        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            // FIX (#44): Holy Fire agora pega TODOS os mobs (não só undead) e o
            // fogo é dano MÁGICO em pulsos — então IGNORA fire resistance de verdade
            // (e queima até mobs imunes a fogo), como diz a tooltip.
            if (attacker.level() instanceof ServerLevel sl) {
                boolean undead = target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD;
                int secs = undead ? 10 : 6;
                target.setSecondsOnFire(secs); // fogo visível pros não-imunes
                final float dot = undead ? 3.0F : 1.5F;
                final java.util.UUID tid = target.getUUID();
                for (int i = 1; i <= secs; i++) {
                    final int delay = i * 20;
                    sl.getServer().tell(new net.minecraft.server.TickTask(
                            sl.getServer().getTickCount() + delay, () -> {
                        if (sl.getEntity(tid) instanceof LivingEntity le && le.isAlive()) {
                            le.hurt(le.damageSources().magic(), dot); // mágico = ignora fire res
                            sl.sendParticles(ParticleTypes.END_ROD,
                                    le.getX(), le.getY() + 1, le.getZ(),
                                    6, 0.3, 0.5, 0.3, 0.05);
                        }
                    }));
                }
                if (undead) target.hurt(target.damageSources().magic(), 6.0F); // burst extra
                sl.sendParticles(ParticleTypes.END_ROD,
                        target.getX(), target.getY() + 1, target.getZ(),
                        20, 0.3, 0.5, 0.3, 0.15);
            }
            return super.hurtEnemy(stack, target, attacker);
        }
        private static Tier MakeTier() {
            return new Tier() {
                @Override public int getUses() { return 2400; }
                @Override public float getSpeed() { return 9; }
                @Override public float getAttackDamageBonus() { return 5; }
                @Override public int getLevel() { return 4; }
                @Override public int getEnchantmentValue() { return 25; }
                @Override public net.minecraft.world.item.crafting.Ingredient getRepairIngredient() {
                    return net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.GOLD_INGOT);
                }
            };
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            super.appendHoverText(s, l, t, f);
            t.add(Component.literal("§e§oLâmina Serafim").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Cada golpe ateia §6Fogo Sagrado§r§7 em §lqualquer§r§7 mob —"));
            t.add(Component.literal("§7dano sagrado por segundos que §dignora resistência a fogo§r§7."));
            t.add(Component.literal("§7Contra §cundead§r§7: queima mais forte + §c6 dano mágico§r§7."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Os Serafins ardem com seis asas. Voam"));
            t.add(Component.literal("§8§o em chamas que ninguém pode tocar.\" — Isaías 6:2"));
        }
    }

    /**
     * <b>Livro do Êxodo</b> — Pillar of Fire buff + r38: EXIT do Spirit
     * World quando player tome_cursed (Tomo Proibido).
     */
    public static class PrayerBook extends EffectOnUseItem {
        public PrayerBook(Properties p) { super(p, 1800); }
        @Override
        protected boolean doUse(ServerPlayer sp, Level level, ItemStack s) {
            // r38: PRIORIDADE 1 — se tome_cursed E in Spirit World, escapa
            if (br.com.murilo.liberthia.dimension.SpiritDimension.isInSpiritWorld(sp)
                    && br.com.murilo.liberthia.dimension.SpiritDimension.isTomeCursed(sp)) {
                ServerLevel sl = (ServerLevel) level;
                // Big visual antes do TP
                for (int y = 0; y < 8; y++) {
                    sl.sendParticles(ParticleTypes.FLAME,
                            sp.getX(), sp.getY() + y, sp.getZ(), 30, 0.5, 0.3, 0.5, 0.03);
                    sl.sendParticles(ParticleTypes.END_ROD,
                            sp.getX(), sp.getY() + y, sp.getZ(), 10, 0.5, 0.3, 0.5, 0.01);
                }
                sl.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 2.0F, 1.2F);
                sl.playSound(null, sp.blockPosition(), SoundEvents.BELL_RESONATE,
                        SoundSource.PLAYERS, 2.5F, 1.5F);
                sp.displayClientMessage(Component.literal(
                        "§e§l✦ §r§eA oração rasga o véu. Você escapa."), false);
                // Faz o return via prayer (limpa curse + cosmic + TP)
                br.com.murilo.liberthia.dimension.SpiritDimension.returnViaPrayer(sp);
                // Consome 1 charge se for stack maior; senão item permanece (book reutilizável)
                // (decisão: não consome — book é raro, escape é o uso principal)
                return true;
            }

            // r38: PRIORIDADE 2 — se está in Spirit World mas NÃO tome_cursed,
            // pode usar pra voltar normalmente (já existe API normal mas vamos
            // expor aqui pra UX)
            if (br.com.murilo.liberthia.dimension.SpiritDimension.isInSpiritWorld(sp)) {
                boolean ok = br.com.murilo.liberthia.dimension.SpiritDimension.returnToBody(sp);
                if (ok) {
                    sp.displayClientMessage(Component.literal(
                            "§e§l✦ §r§eOração te conduz de volta ao corpo."), false);
                    ServerLevel sl = (ServerLevel) level;
                    sl.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                            SoundSource.PLAYERS, 1.5F, 1.5F);
                }
                return ok;
            }

            // Comportamento normal: Pillar of Fire buff (overworld)
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 1, true, true));
            sp.addEffect(new MobEffectInstance(MobEffects.SATURATION, 600, 0, true, true));
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1, true, true));
            sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0, true, true));
            ServerLevel sl = (ServerLevel) level;
            // Pillar of Fire vertical
            for (int y = 0; y < 5; y++) {
                sl.sendParticles(ParticleTypes.FLAME,
                        sp.getX(), sp.getY() + y, sp.getZ(), 20, 0.3, 0.3, 0.3, 0.02);
            }
            sl.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.2F, 1.5F);
            return true;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§6§oLivro do Êxodo").withStyle(ChatFormatting.ITALIC));
            t.add(Component.empty());
            t.add(Component.literal("§7No §6overworld§r§7: Pilar de Fogo —"));
            t.add(Component.literal("§7§aRegen II + Sat + Resist II + FireRes§r§7 60s."));
            t.add(Component.empty());
            t.add(Component.literal("§7No §dMundo Espiritual§r§7: §6§lEXIT§r§7 —"));
            t.add(Component.literal("§7escapa, remove §4Maldição do Tomo§r§7,"));
            t.add(Component.literal("§7limpa horror cósmico."));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"E o SENHOR ia adiante deles, de dia numa coluna"));
            t.add(Component.literal("§8§o de nuvem... de noite, numa coluna de fogo.\" — Ex 13:21"));
            t.add(Component.literal("§c§oCD 90s."));
        }
    }
}
