package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.event.CelestialBindManager;
import br.com.murilo.liberthia.util.EntityRaycast;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * <b>Chicote Celestial</b> — estala uma corrente de luz no inimigo mirado e o
 * <b>prende no lugar por 10 segundos</b> (não consegue andar nem fugir). Dano
 * leve no estalo; alcance longo. Quem é preso fica brilhando (glowing).
 *
 * <p>A lógica de prender roda em {@link CelestialBindManager} (server tick).
 */
public class ChicoteCelestialItem extends Item {

    private static final double REACH = 16.0;     // alcance do chicote
    private static final int BIND_TICKS = 200;    // 10s preso
    private static final int COOLDOWN = 30;        // 1.5s entre laços (curto p/ combo laço→feixe)
    private static final float DAMAGE = 4.0F;      // estalo
    private static final int BEAM_COOLDOWN = 20;   // 1s entre feixes
    private static final float BEAM_DAMAGE = 8.0F; // dano do raio celestial

    public ChicoteCelestialItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // r179: se já tem alvo LAÇADO, lança o FEIXE celestial nele (sprite VFX + raio).
        LivingEntity bound = CelestialBindManager.getBoundTargetOf(sp);
        if (bound != null) {
            launchBeam(sl, sp, bound);
            player.getCooldowns().addCooldown(this, BEAM_COOLDOWN);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        LivingEntity target = EntityRaycast.pickLiving(player, REACH);
        if (target == null || target == player) {
            // estalo no vazio
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.4F);
            player.getCooldowns().addCooldown(this, 10);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        // corrente de luz: feixe de partículas do player até o alvo
        drawLightChain(sl, player.getEyePosition(),
                target.position().add(0, target.getBbHeight() * 0.5, 0));

        target.hurt(target.damageSources().playerAttack(player), DAMAGE);
        CelestialBindManager.bind(target, sp, BIND_TICKS);

        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);
        sl.playSound(null, target.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7F, 1.7F);

        sp.displayClientMessage(Component.translatable("item.liberthia.chicote_celestial.bound",
                target.getDisplayName()).withStyle(ChatFormatting.GOLD), true);

        player.getCooldowns().addCooldown(this, COOLDOWN);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /**
     * r179: lança o FEIXE celestial no alvo laçado — beam de sprites animados
     * (SpriteVfxEntity) + raio visual + corrente de luz + dano.
     */
    private void launchBeam(ServerLevel sl, ServerPlayer sp, LivingEntity target) {
        Vec3 from = sp.getEyePosition();
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);

        // Sprite VFX: impacto grande no alvo + rastro animado ao longo do feixe
        spawnBeamVfx(sl, sp, to, 1.5F);
        for (int i = 1; i <= 4; i++) {
            spawnBeamVfx(sl, sp, from.add(to.subtract(from).scale(i / 5.0)), 0.6F);
        }
        drawLightChain(sl, from, to);

        // Raio celestial VISUAL (sem fogo / sem dano de bloco)
        net.minecraft.world.entity.LightningBolt bolt =
                net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.setVisualOnly(true);
            bolt.moveTo(target.getX(), target.getY(), target.getZ());
            sl.addFreshEntity(bolt);
        }

        target.hurt(target.damageSources().playerAttack(sp), BEAM_DAMAGE);

        sl.playSound(null, target.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F, 1.5F);
        sl.playSound(null, target.blockPosition(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7F, 1.9F);
    }

    /** Spawna um sprite VFX animado num ponto (usa o sistema SpriteVfx do mod). */
    private static void spawnBeamVfx(ServerLevel sl, LivingEntity owner, Vec3 pos, float scale) {
        var vfx = new br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxEntity(
                sl, owner, pos,
                br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.MAGIC_SPELL, scale);
        sl.addFreshEntity(vfx);
    }

    /** Desenha um feixe de END_ROD entre dois pontos (a corrente de luz). */
    private static void drawLightChain(ServerLevel sl, Vec3 from, Vec3 to) {
        Vec3 diff = to.subtract(from);
        double dist = diff.length();
        int steps = (int) Math.max(6, dist * 3);
        Vec3 step = diff.scale(1.0 / steps);
        Vec3 p = from;
        for (int i = 0; i <= steps; i++) {
            sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0.0);
            p = p.add(step);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.liberthia.chicote_celestial.desc1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.liberthia.chicote_celestial.desc2")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
