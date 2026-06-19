package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.entity.SoulBodyEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r24: Soul Sever — Cortador de Almas.
 *
 * <h2>Função</h2>
 * <p>Right-click no AR: separa a consciência do player do corpo.
 * <ul>
 *   <li><b>Overworld → Spirit World</b>:
 *     <ol>
 *       <li>Spawna {@link SoulBodyEntity} na posição atual (mesma skin/nome)</li>
 *       <li>Salva NBT com UUID do body + coords de retorno</li>
 *       <li>Teleporta player pra Spirit World (mesmas coords)</li>
 *       <li>Aplica Resistance 5s + partículas SOUL massivas</li>
 *     </ol>
 *   </li>
 *   <li><b>Spirit World → Overworld</b>: chama
 *       {@link SpiritDimension#returnToBody(ServerPlayer)} + remove body
 *   </li>
 * </ul>
 *
 * <h2>Custo</h2>
 * Cooldown 60s. Reduz 20 pontos de sanidade por uso. Durabilidade infinita
 * (item permanente).
 */
public class SoulSeverItem extends Item {

    public static final int COOLDOWN_TICKS = 1200; // 60s
    public static final int SANITY_COST = 20;

    public SoulSeverItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        // v0.1.22 r25 BUGFIX: retorno NÃO checa cooldown — sempre permitido.
        // Antes o cooldown de 60s do enter bloqueava o return durante quase
        // todo o tempo no spirit. Agora player pode voltar a qualquer hora.
        if (SpiritDimension.isInSpiritWorld(sp)) {
            return returnFromSpirit(sp, stack);
        }
        // Entry checa cooldown
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        return enterSpirit(sp, stack);
    }

    private InteractionResultHolder<ItemStack> enterSpirit(ServerPlayer sp, ItemStack stack) {
        if (!(sp.level() instanceof ServerLevel sl)) return InteractionResultHolder.fail(stack);

        // v0.1.22 r25 BUGFIX: se já tem body órfão (NBT preservado de
        // sessão anterior que crashou), despawna primeiro pra evitar
        // 2 bodies do mesmo player no mundo.
        // r30 EXTRA: também faz BROAD SCAN — busca QUALQUER SoulBodyEntity com
        // owner=player UUID em todos os levels e despawna. Isso cobre o caso
        // do usuário "tem um bug ... gera mais um corpo" — bodies fantasmas
        // de sessões crashadas que não tinham UUID gravado mais.
        var oldData = sp.getPersistentData();
        if (oldData.hasUUID(SpiritDimension.NBT_BODY_UUID)) {
            java.util.UUID oldUuid = oldData.getUUID(SpiritDimension.NBT_BODY_UUID);
            for (var lvl : sp.server.getAllLevels()) {
                var e = lvl.getEntity(oldUuid);
                if (e instanceof SoulBodyEntity oldBody) {
                    oldBody.discard();
                    break;
                }
            }
            oldData.remove(SpiritDimension.NBT_BODY_UUID);
        }
        // r30: cleanup CATCH-ALL — todos os SoulBody com owner=sp são órfãos
        for (var lvl : sp.server.getAllLevels()) {
            for (var entity : lvl.getEntities().getAll()) {
                if (entity instanceof SoulBodyEntity body
                        && sp.getUUID().equals(body.getOwnerUuid())) {
                    body.discard();
                }
            }
        }

        // Spawna SoulBody na pos atual ANTES de teleportar
        SoulBodyEntity body = ModEntities.SOUL_BODY.get().create(sl);
        if (body == null) return InteractionResultHolder.fail(stack);
        body.moveTo(sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), sp.getXRot());
        body.setOwnerUuid(sp.getUUID());
        body.setOwnerName(sp.getName().getString());
        body.setCustomName(Component.literal(sp.getName().getString()));
        body.setCustomNameVisible(true);
        body.setHealth(sp.getHealth());
        // r32 BUG FIX: força body persistente (não despawna mesmo em chunks
        // não carregados / sem player próximo). Antes, body sumia em ~3s →
        // body-link check interpretava como "morto" → auto-return.
        body.setPersistenceRequired();
        sl.addFreshEntity(body);

        // Salva UUID do body no player + coords de retorno
        sp.getPersistentData().putUUID(SpiritDimension.NBT_BODY_UUID, body.getUUID());

        // VFX antes de teleportar (vai pra todo mundo perto)
        sl.sendParticles(ParticleTypes.SOUL,
                sp.getX(), sp.getY() + 1.0, sp.getZ(),
                40, 0.5, 1.0, 0.5, 0.15);
        sl.sendParticles(ParticleTypes.PORTAL,
                sp.getX(), sp.getY() + 1.0, sp.getZ(),
                30, 0.5, 1.0, 0.5, 0.5);
        sl.playSound(null, sp.blockPosition(),
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5F, 0.5F);
        sl.playSound(null, sp.blockPosition(),
                SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.8F, 0.7F);

        // Teleporta consciência pra Spirit World
        boolean ok = SpiritDimension.enterSpiritWorld(sp);
        if (!ok) {
            // Falhou — limpa body
            body.discard();
            sp.displayClientMessage(Component.literal(
                    "§cFalha ao acessar o mundo espiritual.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        // Custo de sanidade + sync inicial pro client renderizar HUD bar
        SpiritDimension.addSanity(sp, -SANITY_COST);
        br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                new br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket(
                        SpiritDimension.getSanity(sp)));

        // r29 BUGFIX: cooldown longo de 60s adicionado AQUI bloqueava o uso
        // do próprio item no spirit world (vanilla bloqueia right-click no
        // client enquanto item está em cooldown). Player ficava preso até
        // expirar. Agora só um cooldown pequeno de 2s pra evitar spam.
        sp.getCooldowns().addCooldown(this, 40);
        sp.displayClientMessage(Component.literal(
                "§5§l✦ §r§5Sua alma se separa do corpo...").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return InteractionResultHolder.consume(stack);
    }

    private InteractionResultHolder<ItemStack> returnFromSpirit(ServerPlayer sp, ItemStack stack) {
        // r26 BUGFIX: despawna body buscando em TODOS os levels (não só
        // originLevel). Cobre edge case de body movido por admin/teleport.
        // Também force-load do chunk via getChunkAt(x,z) se necessário.
        var data = sp.getPersistentData();
        if (data.hasUUID(SpiritDimension.NBT_BODY_UUID)) {
            java.util.UUID bodyUuid = data.getUUID(SpiritDimension.NBT_BODY_UUID);
            double bodyX = data.getDouble(SpiritDimension.NBT_RETURN_X);
            double bodyZ = data.getDouble(SpiritDimension.NBT_RETURN_Z);

            // Tenta achar body em algum level. Se NBT_RETURN_DIM existe,
            // tenta esse PRIMEIRO + force-load do chunk pra garantir que
            // body não está "dormindo" em chunk descarregado.
            String returnDim = data.getString(SpiritDimension.NBT_RETURN_DIM);
            net.minecraft.server.level.ServerLevel originLevel = null;
            if (!returnDim.isEmpty()) {
                originLevel = sp.server.getLevel(
                        net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION,
                                new net.minecraft.resources.ResourceLocation(returnDim)));
                if (originLevel != null) {
                    // Force-load do chunk onde o body foi salvo
                    originLevel.getChunkAt(new net.minecraft.core.BlockPos(
                            (int) bodyX, 64, (int) bodyZ));
                }
            }

            net.minecraft.world.entity.Entity foundBody = null;
            // Origin first
            if (originLevel != null) {
                foundBody = originLevel.getEntity(bodyUuid);
            }
            // Fallback: iterate ALL levels
            if (foundBody == null) {
                for (var lvl : sp.server.getAllLevels()) {
                    var e = lvl.getEntity(bodyUuid);
                    if (e != null) { foundBody = e; break; }
                }
            }

            if (foundBody instanceof SoulBodyEntity body
                    && body.level() instanceof ServerLevel bsl) {
                bsl.sendParticles(ParticleTypes.PORTAL,
                        body.getX(), body.getY() + 1.0, body.getZ(),
                        20, 0.4, 1.0, 0.4, 0.3);
                body.discard();
            }
        }
        // r30: cleanup CATCH-ALL — despawna QUALQUER SoulBody com owner=sp
        // (mesmo se o UUID NBT estiver corrompido/perdido). Previne bodies
        // fantasmas de sessões anteriores ficarem no mundo.
        for (var lvl : sp.server.getAllLevels()) {
            for (var entity : lvl.getEntities().getAll()) {
                if (entity instanceof SoulBodyEntity body
                        && sp.getUUID().equals(body.getOwnerUuid())) {
                    body.discard();
                }
            }
        }

        boolean ok = SpiritDimension.returnToBody(sp);
        if (!ok) {
            sp.displayClientMessage(Component.literal(
                    "§cVocê não tem corpo pra voltar.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        // r29: cooldown de 60s aplicado AGORA (após retorno) — antes era no enter,
        // o que travava o item dentro do spirit world.
        sp.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        sp.displayClientMessage(Component.literal(
                "§a✦ Você retorna ao seu corpo.").withStyle(ChatFormatting.GREEN), true);
        sp.level().playSound(null, sp.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.5F);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§5§oCortador de Almas").withStyle(ChatFormatting.ITALIC));
        tip.add(Component.literal("§7Right-click: §dseparar consciência do corpo§r§7"));
        tip.add(Component.literal("§7e acessar o §5Mundo Espiritual§r§7."));
        tip.add(Component.empty());
        tip.add(Component.literal("§7• Seu corpo fica visível pra outros"));
        tip.add(Component.literal("§7• No spirit world só você se vê"));
        tip.add(Component.literal("§7• Sanidade decresce com o tempo"));
        tip.add(Component.literal("§7• Right-click no spirit pra voltar"));
        tip.add(Component.empty());
        tip.add(Component.literal("§8§oCusto: §c-20 sanidade§8§o por uso. Cooldown 60s."));
    }
}
