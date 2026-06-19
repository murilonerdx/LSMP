package br.com.murilo.liberthia.cosmic.observatory;

import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r48: <b>The Reflection Seed</b> — admin artifact.
 *
 * <h2>Uso</h2>
 * Right-click num player target → cria uma {@link ReflectionEntity} com a
 * skin/profile do target. O clone aparece em um chunk próximo, anda
 * naturalmente, e desaparece quando observado.
 *
 * <p>Cooldown 5 min entre usos. Apenas admins (op level ≥ 2) podem usar.
 *
 * <p>Quando ativado:
 * <ul>
 *   <li>Spawn ReflectionEntity 16-32 blocos atrás do target em direção aleatória</li>
 *   <li>Inicia CloneSession (chat simulator + tablist fake)</li>
 *   <li>Target NÃO recebe notificação — pode descobrir só pelas manifestações</li>
 *   <li>Outros players próximos podem ver/interagir</li>
 * </ul>
 */
public class ReflectionSeedItem extends Item {

    public ReflectionSeedItem(Properties p) {
        super(p.rarity(Rarity.EPIC).stacksTo(1));
    }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user,
                                                   LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(user instanceof ServerPlayer sp)) return InteractionResult.PASS;

        // Admin check
        if (!sp.hasPermissions(2) && !sp.isCreative()) {
            sp.displayClientMessage(Component.literal(
                    "§cVocê não é um Caretaker."), true);
            return InteractionResult.FAIL;
        }
        if (sp.getCooldowns().isOnCooldown(this)) {
            sp.displayClientMessage(Component.literal(
                    "§7A semente ainda está germinando."), true);
            return InteractionResult.FAIL;
        }
        if (!(target instanceof ServerPlayer victim)) {
            sp.displayClientMessage(Component.literal(
                    "§7A semente exige um §dplayer§r§7 como modelo."), true);
            return InteractionResult.FAIL;
        }

        // Spawn position: 16-32 blocos atrás do target em direção aleatória
        double angle = Math.random() * Math.PI * 2;
        double dist = 16 + Math.random() * 16;
        double spawnX = victim.getX() + Math.cos(angle) * dist;
        double spawnZ = victim.getZ() + Math.sin(angle) * dist;
        ServerLevel sl = victim.serverLevel();
        // Adjust Y pra estar no chão
        int spawnY = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                (int) spawnX, (int) spawnZ);

        ReflectionEntity clone = ModEntities.REFLECTION_ENTITY.get().create(sl);
        if (clone == null) return InteractionResult.FAIL;
        clone.setOwnerUuid(victim.getUUID());
        clone.setOwnerName(victim.getName().getString());
        // r49: COPIA EQUIPAMENTO COMPLETO (armor + main + offhand + skin)
        clone.copyEquipmentFrom(victim);
        clone.spawnedTick = sl.getGameTime();
        // Visible to ALL players (não apenas victim)
        clone.targetViewerId = null;
        clone.moveTo(spawnX + 0.5, spawnY, spawnZ + 0.5,
                (float) (Math.random() * 360), 0);
        sl.addFreshEntity(clone);

        // Spawn particles dramáticos na pos do clone (mesh black)
        for (int i = 0; i < 30; i++) {
            sl.sendParticles(ParticleTypes.PORTAL,
                    spawnX + (Math.random() - 0.5) * 2,
                    spawnY + Math.random() * 2,
                    spawnZ + (Math.random() - 0.5) * 2,
                    1, 0, 0.1, 0, 0.05);
        }
        sl.playSound(null, BlockPos.containing(spawnX, spawnY, spawnZ),
                SoundEvents.PORTAL_TRIGGER, SoundSource.MASTER, 0.5F, 0.3F);

        // Inicia CloneChatSimulator pro target
        CloneChatSimulator.startSession(victim, clone);

        // Caretaker feedback
        sp.displayClientMessage(Component.literal(
                "§4§l✦ §r§4§oReflexo plantado. Cresce em §o"
                        + victim.getName().getString() + "§r§4§o ("
                        + (int)dist + "b longe, " + (int)spawnX + ", "
                        + spawnY + ", " + (int)spawnZ + ")"), false);

        // Hallucination sutil pro target (não revela mas levanta paranoia)
        HallucinationManager.force(victim, HallucinationType.FAKE_WHISPER, 0.5F, 30, "");

        sp.getCooldowns().addCooldown(this, 6000); // 5min
        return InteractionResult.CONSUME;
    }

    @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§4§l§oSemente do Reflexo").withStyle(ChatFormatting.DARK_RED));
        t.add(Component.literal("§7§oArtifact dos Caretakers."));
        t.add(Component.empty());
        t.add(Component.literal("§7Right-click num §dplayer§r§7:"));
        t.add(Component.literal("§7• Cria um §lreflexo§r§7 dele 16-32b longe"));
        t.add(Component.literal("§7• Skin + nome + animações idênticos"));
        t.add(Component.literal("§7• Anda naturalmente, olha em volta"));
        t.add(Component.literal("§7• §c§lDESAPARECE§r§7 quando observado direto"));
        t.add(Component.literal("§7• Fake chat messages do clone aleatoriamente"));
        t.add(Component.literal("§7• Lifetime: 5 minutos"));
        t.add(Component.empty());
        t.add(Component.literal("§c§oADMIN ONLY (op level 2+)"));
        t.add(Component.literal("§c§oCD 5 min"));
        t.add(Component.empty());
        t.add(Component.literal("§8§o\"A semente cresce no que o alvo deseja esquecer.\""));
    }
}
