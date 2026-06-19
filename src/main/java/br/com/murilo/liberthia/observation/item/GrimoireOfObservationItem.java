package br.com.murilo.liberthia.observation.item;

import br.com.murilo.liberthia.observation.ObservationParts;
import br.com.murilo.liberthia.observation.api.ObservationResolver;
import br.com.murilo.liberthia.observation.api.ObservationSpell;
import br.com.murilo.liberthia.observation.entity.EntityObservationProjectile;
import br.com.murilo.liberthia.observation.source.SourceData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r64: <b>Grimório de Observação</b> — item final do sistema mágico.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>Right-click: spawna {@link EntityObservationProjectile} na direção do
 *       olhar — projétil voador real, com trail particles, hit detection</li>
 *   <li>Shift+Right-click: casta spell direto (sem projétil) usando o preset</li>
 * </ul>
 *
 * <h2>Cycle presets</h2>
 * NBT {@code grimoire.preset} 0-5 → 6 spells diferentes (Tendril, Silence,
 * Mirror, Decay, Whisper, Memory).
 */
public class GrimoireOfObservationItem extends Item {

    public static final String NBT_PRESET = "grimoire.preset";

    /** NBT key pra spell custom carregada via parchment (recipe completo). */
    public static final String NBT_CUSTOM_RECIPE = "grimoire.custom_recipe";
    public static final String NBT_CUSTOM_NAME = "grimoire.custom_name";
    public static final String NBT_CUSTOM_COLOR = "grimoire.custom_color";
    /** r78: cast type ordinal — 0=PROJECTILE, 1=BEAM, 2=BURST, 3=RAY, 4=SELF. */
    public static final String NBT_CAST_TYPE = "grimoire.cast_type";

    public GrimoireOfObservationItem(Properties p) {
        // r74 fix: durability() já implica stacksTo(1) — não chamamos stacksTo
        // depois pra evitar "Unable to have damage AND stack" quando subclass
        // (GrimoireTierItem) já setou durability primeiro.
        super(p.rarity(Rarity.EPIC).durability(500));
    }

    @Override public boolean isFoil(ItemStack s) { return true; }

    /** Carrega a spell ativa — prefere custom recipe (parchment-imprintada) se existir. */
    public static ObservationSpell loadActiveSpell(ItemStack stack) {
        var tag = stack.getOrCreateTag();
        if (tag.contains(NBT_CUSTOM_RECIPE)) {
            // Custom spell from parchment imprint
            var list = tag.getList(NBT_CUSTOM_RECIPE, 8); // StringTag
            String name = tag.contains(NBT_CUSTOM_NAME) ? tag.getString(NBT_CUSTOM_NAME) : "Custom";
            int color = tag.contains(NBT_CUSTOM_COLOR) ? tag.getInt(NBT_CUSTOM_COLOR) : 0x9d4dd6;
            var builder = ObservationSpell.builder(name, color);
            for (int i = 0; i < list.size(); i++) {
                var part = br.com.murilo.liberthia.observation.api.ObservationRegistry.get(
                    new net.minecraft.resources.ResourceLocation(list.getString(i)));
                if (part != null) builder.add(part);
            }
            return builder.build();
        }
        // Fallback: preset hardcoded
        int preset = tag.getInt(NBT_PRESET);
        return ObservationTomeItem.buildPreset(preset);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        // r68: Se tem Parchment na offhand E está fazendo rclick, IMPRINTA o parchment
        ItemStack offhand = sp.getOffhandItem();
        if (offhand.getItem() instanceof SpellParchmentItem) {
            var recipe = SpellParchmentItem.getRecipe(offhand);
            if (recipe.isEmpty()) {
                sp.displayClientMessage(Component.literal(
                    "§c⚠ Pergaminho vazio."), true);
                return InteractionResultHolder.fail(stack);
            }
            ObservationSpell parchSpell = SpellParchmentItem.buildSpell(offhand);
            if (parchSpell == null) {
                sp.displayClientMessage(Component.literal(
                    "§c⚠ Recipe inválido."), true);
                return InteractionResultHolder.fail(stack);
            }
            String err = parchSpell.validate();
            if (err != null) {
                sp.displayClientMessage(Component.literal(
                    "§c⚠ " + err), true);
                return InteractionResultHolder.fail(stack);
            }
            // Imprint
            var tag = stack.getOrCreateTag();
            var list = new net.minecraft.nbt.ListTag();
            for (String id : recipe) list.add(net.minecraft.nbt.StringTag.valueOf(id));
            tag.put(NBT_CUSTOM_RECIPE, list);
            tag.putString(NBT_CUSTOM_NAME, parchSpell.name());
            tag.putInt(NBT_CUSTOM_COLOR, parchSpell.color());
            sp.displayClientMessage(Component.literal(
                "§a✓ §rGrimório imprintado com: §d" + parchSpell.name()
                + " §7(" + parchSpell.totalSourceCost() + " Source)"), true);
            return InteractionResultHolder.success(stack);
        }

        int preset = stack.getOrCreateTag().getInt(NBT_PRESET);

        // r78: Shift+rclick agora cicla CAST TYPE (Projectile → Beam → Burst → Ray → Self)
        if (sp.isShiftKeyDown()) {
            var current = br.com.murilo.liberthia.observation.api.CastType.fromOrdinal(
                stack.getOrCreateTag().getInt(NBT_CAST_TYPE));
            var next = current.next();
            stack.getOrCreateTag().putInt(NBT_CAST_TYPE, next.ordinal());
            sp.displayClientMessage(Component.literal(
                "§5§l✦ §rFormato: ").append(next.component())
                .append(Component.literal(" §7— " + next.hint)), true);
            return InteractionResultHolder.success(stack);
        }

        // Right-click: CASTA com o cast type atual
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        ObservationSpell spell = loadActiveSpell(stack);
        var castType = br.com.murilo.liberthia.observation.api.CastType.fromOrdinal(
            stack.getOrCreateTag().getInt(NBT_CAST_TYPE));
        int sourceCost = Math.max(5, spell.totalSourceCost() / 2);
        if (!SourceData.consume(sp, sourceCost)) {
            sp.displayClientMessage(Component.literal(
                "§c⚠ Source insuficiente §7(" + SourceData.get(sp) + "/" + sourceCost + ")"), true);
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel sl = (ServerLevel) level;
        Vec3 look = sp.getLookAngle();

        // Execute por cast type
        switch (castType) {
            case PROJECTILE -> castProjectile(sl, sp, spell, look, 1);
            case BEAM -> castBeam(sl, sp, spell);
            case BURST -> castBurst(sl, sp, spell);
            case RAY -> castRayDispersion(sl, sp, spell, look);
            case SELF -> castSelf(sl, sp, spell);
        }

        // VFX + cast animation packet
        br.com.murilo.liberthia.observation.vfx.CastVfx.spiralIn(sp, sp.position(), spell.color());
        br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
            new br.com.murilo.liberthia.observation.network.CastAnimationS2CPacket(spell.color()));
        sl.playSound(null, sp.blockPosition(),
            net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.4F);

        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        sp.getCooldowns().addCooldown(this, 12);
        return InteractionResultHolder.consume(stack);
    }

    // ════════════════════════════════════════════════════════════════
    // r78: 5 CAST TYPE IMPLEMENTATIONS
    // ════════════════════════════════════════════════════════════════

    /** PROJECTILE — projétil voador rápido, plano (sem gravidade). */
    private static void castProjectile(ServerLevel sl, ServerPlayer sp, ObservationSpell spell, Vec3 look, int count) {
        for (int i = 0; i < count; i++) {
            EntityObservationProjectile proj = new EntityObservationProjectile(sl, sp, spell.color(), 0.55F);
            proj.setDamage(5.0F);
            proj.setMaxAge(120);
            proj.setPiercing(0);
            proj.setPos(sp.getX() + look.x * 0.5, sp.getEyeY() - 0.1, sp.getZ() + look.z * 0.5);
            // r78: velocidade DOBRADA + scatter pequeno pra múltiplos
            double scatter = count > 1 ? (Math.random() - 0.5) * 0.15 : 0;
            Vec3 v = look.scale(2.4).add(scatter, scatter * 0.3, scatter);
            proj.setDeltaMovement(v);
            sl.addFreshEntity(proj);
        }
    }

    /** BEAM — raycast instantâneo 32b, aplica spell direto no que olhar. */
    private static void castBeam(ServerLevel sl, ServerPlayer sp, ObservationSpell spell) {
        Vec3 start = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        Vec3 end = start.add(look.scale(32.0));
        var hit = sl.clip(new net.minecraft.world.level.ClipContext(start, end,
            net.minecraft.world.level.ClipContext.Block.OUTLINE,
            net.minecraft.world.level.ClipContext.Fluid.NONE, sp));
        // Particles ao longo do beam
        int steps = 32;
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            Vec3 p = start.lerp(hit.getLocation(), t);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        // Dano em entities no caminho (raycast manual)
        var hitEntity = sp.level().getEntities(sp,
            sp.getBoundingBox().expandTowards(look.scale(32)).inflate(1.0),
            e -> e instanceof net.minecraft.world.entity.LivingEntity le && e != sp && le.isAlive());
        for (var e : hitEntity) {
            // Verifica se entity está no caminho do beam (cone tight)
            Vec3 toEnt = e.position().subtract(start).normalize();
            if (toEnt.dot(look) > 0.95) {
                if (e instanceof net.minecraft.world.entity.LivingEntity le) {
                    le.hurt(sl.damageSources().magic(), 6.0F);
                }
            }
        }
        // Resolve spell no hit point
        br.com.murilo.liberthia.observation.api.ObservationResolver.cast(sp, spell);
    }

    /** BURST — AOE radial 6b em volta do caster. */
    private static void castBurst(ServerLevel sl, ServerPlayer sp, ObservationSpell spell) {
        // VFX explosion ring
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            double r = 3.0;
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                sp.getX() + Math.cos(a) * r, sp.getY() + 0.5, sp.getZ() + Math.sin(a) * r,
                1, 0.05, 0.05, 0.05, 0.02);
        }
        // Dano + spell em todos os mobs em 6b
        var targets = sl.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
            sp.getBoundingBox().inflate(6.0), e -> e != sp && e.isAlive());
        for (var t : targets) {
            t.hurt(sl.damageSources().magic(), 4.0F);
            Vec3 push = t.position().subtract(sp.position()).normalize().scale(1.0);
            t.setDeltaMovement(push.x, 0.3, push.z);
            t.hurtMarked = true;
        }
        br.com.murilo.liberthia.observation.api.ObservationResolver.cast(sp, spell);
    }

    /** RAY — 5 projéteis em cone 30° (dispersion). */
    private static void castRayDispersion(ServerLevel sl, ServerPlayer sp, ObservationSpell spell, Vec3 look) {
        // 5 projéteis com leve scatter angular
        for (int i = 0; i < 5; i++) {
            double offset = (i - 2) * 0.15; // -0.3, -0.15, 0, +0.15, +0.3
            Vec3 perp = new Vec3(-look.z, 0, look.x).normalize();
            Vec3 dir = look.add(perp.scale(offset)).normalize();
            EntityObservationProjectile proj = new EntityObservationProjectile(sl, sp, spell.color(), 0.4F);
            proj.setDamage(3.0F);
            proj.setMaxAge(80);
            proj.setPos(sp.getX() + dir.x * 0.5, sp.getEyeY() - 0.1, sp.getZ() + dir.z * 0.5);
            proj.setDeltaMovement(dir.scale(2.2));
            sl.addFreshEntity(proj);
        }
    }

    /** SELF — aplica spell em si mesmo (buff). */
    private static void castSelf(ServerLevel sl, ServerPlayer sp, ObservationSpell spell) {
        // VFX self-cast (spiral up)
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12;
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                sp.getX() + Math.cos(a) * 1.0,
                sp.getY() + i * 0.1,
                sp.getZ() + Math.sin(a) * 1.0,
                1, 0.02, 0.02, 0.02, 0.0);
        }
        br.com.murilo.liberthia.observation.api.ObservationResolver.cast(sp, spell);
    }

    /** r70: Cycle preset on shift+left-click. Agora cicla entre 16 presets. */
    public static void cyclePreset(ItemStack stack, ServerPlayer sp) {
        int current = stack.getOrCreateTag().getInt(NBT_PRESET);
        int next = (current + 1) % ObservationTomeItem.PRESET_COUNT;
        stack.getOrCreateTag().putInt(NBT_PRESET, next);
        // Limpa custom recipe quando cicla preset
        stack.getOrCreateTag().remove(NBT_CUSTOM_RECIPE);
        stack.getOrCreateTag().remove(NBT_CUSTOM_NAME);
        stack.getOrCreateTag().remove(NBT_CUSTOM_COLOR);
        ObservationSpell s = ObservationTomeItem.buildPreset(next);
        sp.displayClientMessage(Component.literal(
            "§5§l✦ §rPreset §7#" + next + "§r: §e" + s.name()
            + " §7(custo: §c" + s.totalSourceCost() + " Source§7, §c"
            + s.totalSanityCost() + " Sanity§7)"), true);
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§5§oGrimório de Observação").withStyle(ChatFormatting.ITALIC));
        // r78: mostra cast type ativo
        var castType = br.com.murilo.liberthia.observation.api.CastType.fromOrdinal(
            s.getOrCreateTag().getInt(NBT_CAST_TYPE));
        t.add(Component.literal("§7Formato: ").append(castType.component())
            .append(Component.literal(" §8(" + castType.hint + ")")));
        t.add(Component.literal("§7Right-click: casta com o formato"));
        t.add(Component.literal("§7Shift+rclick: §6cicla formato§7 (5 tipos)"));
        t.add(Component.literal("§7Use §ePedestal de Vínculo§7 pra imprintar Pergaminho"));
        t.add(Component.empty());
        ObservationSpell spell = loadActiveSpell(s);
        boolean isCustom = s.getOrCreateTag().contains(NBT_CUSTOM_RECIPE);
        if (isCustom) {
            t.add(Component.literal("§6§l✦ Custom imprintado: §r§e" + spell.name()));
        } else {
            int preset = s.getOrCreateTag().getInt(NBT_PRESET);
            t.add(Component.literal("§5Preset §7#" + preset + ": §e" + spell.name()));
        }
        t.add(Component.literal("§7Recipe: §f" + spell.recipe().size() + " glyphs"));
        t.add(Component.literal("§7Custo: §c" + spell.totalSourceCost() + " Source"));
        t.add(Component.empty());
        t.add(Component.literal("§8§o\"O olhar pesa. Cada palavra cobra.\""));
    }
}
