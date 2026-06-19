package br.com.murilo.liberthia.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * v0.1.24 r92: <b>Scryer Oculus</b> — bloco que permite ver através de paredes
 * num raio extenso (256 blocos).
 *
 * <h2>Uso</h2>
 * <ul>
 *   <li>Right-click com Scrying Lens → ativa visão remota (GLOWING em todas
 *       entities num raio de 256)</li>
 *   <li>Funciona por 30s</li>
 *   <li>Cooldown 90s</li>
 * </ul>
 *
 * <p>Complementa o ScryingLens — Lens é portátil (raio 32), Oculus é fixo
 * (raio 256 + activates aura por 30s).
 */
public class ScryerOculusBlock extends Block {

    public ScryerOculusBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof br.com.murilo.liberthia.magic.scrying.ScryingLensItem)) {
            // r164: sem lens → abre info screen explicando como usar
            java.util.List<String> stats = new java.util.ArrayList<>();
            stats.add("§7Função: §dvisão remota através de paredes");
            stats.add("§7Raio: §a256 blocos");
            stats.add("§7Duração: §b30 segundos");
            stats.add("§7Cooldown: §690 segundos");
            stats.add("§7Status: " + (sp.getCooldowns().isOnCooldown(this.asItem())
                    ? "§c§oRepousando..." : "§a§lPRONTO"));
            java.util.List<String> instr = new java.util.ArrayList<>();
            instr.add("§71. Crafte uma §dScrying Lens§7 (item portátil)");
            instr.add("§72. §eRClick no Oculus com a Lens na mão§7 → ativa");
            instr.add("§73. Todas entidades num raio §a256§7 ficam §6GLOWING§7");
            instr.add("§74. Você vê elas através das paredes por 30s");
            instr.add("§75. Lens portátil tem raio menor (§a32§7) — Oculus é §lfixo + maior§r");
            br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                    new br.com.murilo.liberthia.network.packet.OpenBlockInfoS2CPacket(
                            "🔮 Scryer Oculus", stats, instr, 0x66CCFF));
            return InteractionResult.CONSUME;
        }
        if (sp.getCooldowns().isOnCooldown(this.asItem())) {
            sp.displayClientMessage(Component.literal("§5✦ Oculus repousa..."), true);
            return InteractionResult.FAIL;
        }

        // GLOWING em todas entities num raio de 256
        int radius = 256;
        var entities = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(radius));
        int count = 0;
        for (var e : entities) {
            if (e == sp) continue;
            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING, 600, 0, true, false));
            count++;
        }

        // Particles aura ao redor do oculus
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            for (int i = 0; i < 30; i++) {
                double a = (i / 30.0) * Math.PI * 2;
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        pos.getX() + 0.5 + Math.cos(a) * 2,
                        pos.getY() + 1.5,
                        pos.getZ() + 0.5 + Math.sin(a) * 2,
                        1, 0, 0, 0, 0);
            }
        }

        sp.displayClientMessage(Component.literal(
                "§5§l✦ §r§5Oculus ativo: §b" + count + " §5entities reveladas em §b" + radius + " §5blocos"), false);
        sp.level().playSound(null, pos,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.5F, 1.0F);
        sp.getCooldowns().addCooldown(this.asItem(), 1800);
        return InteractionResult.CONSUME;
    }
}
