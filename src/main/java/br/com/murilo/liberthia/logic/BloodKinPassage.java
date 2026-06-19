package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.entity.BloodCultistEntity;
import br.com.murilo.liberthia.entity.BloodPriestEntity;
import br.com.murilo.liberthia.entity.BloodWardenBossEntity;
import br.com.murilo.liberthia.entity.DisarmerEntity;
import br.com.murilo.liberthia.entity.FleshCrawlerEntity;
import br.com.murilo.liberthia.entity.FleshMotherBossEntity;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22: helper + event handlers do Selo de Passagem.
 *
 * <p>{@link #hasSigil(LivingEntity)} — true se o entity é {@link Player} e tem
 * {@code passage_sigil} em qualquer slot do inventário (hotbar/main/offhand/armor).
 *
 * <p>Eventos:
 * <ul>
 *   <li>{@link LivingChangeTargetEvent}: cancela quando uma criatura de sangue
 *       hostil tenta target um player com sigil → setNewTarget(null).</li>
 *   <li>{@link LivingAttackEvent}: cancela ataque direto de criatura de sangue
 *       contra player com sigil (defesa em profundidade — pega ataques que
 *       passam por bypass de target).</li>
 * </ul>
 *
 * <p>Os blocos atacantes (WitheringEye, VenomGeyser, etc.) chamam
 * {@link #hasSigil(LivingEntity)} diretamente no método attack pra skip o player.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class BloodKinPassage {

    private BloodKinPassage() {}

    /**
     * True se o entity é um {@link Player} carregando o passage_sigil em
     * qualquer slot do inventário (mainhand, offhand, hotbar, main inv ou
     * armor). Performance: 41 slots max por player, scan early-exit.
     */
    public static boolean hasSigil(LivingEntity entity) {
        if (!(entity instanceof Player p)) return false;
        var sigil = ModItems.PASSAGE_SIGIL.get();
        if (p.getMainHandItem().is(sigil)) return true;
        if (p.getOffhandItem().is(sigil)) return true;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.is(sigil)) return true;
        }
        return false;
    }

    /**
     * v0.1.22: true se o player tem {@code boss_crown ATIVA} no inventário.
     * Proteção paralela ao Sigil — quem tem a coroa ativa é tratado igual.
     */
    public static boolean hasActiveCrown(LivingEntity entity) {
        if (!(entity instanceof Player p)) return false;
        var crown = ModItems.BOSS_CROWN.get();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.is(crown) && br.com.murilo.liberthia.item.BossCrownItem.isActive(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * v0.1.22 r18: REVERTIDO. User pediu (textual): "QUEM ESTIVER COM A
     * PORRA DO SIGIL OU A PORRA DO CROWN OS MONSTROS IGNORAM SÓ ELE,
     * OUTROS PLAYERS SEGUE NORMAL".
     *
     * <p>Comportamento correto:
     * <ul>
     *   <li>Monstros/blocos do mod fazem target selection PULANDO quem tem
     *       Sigil/Crown/creative → atacam só players SEM proteção</li>
     *   <li>Outros players (sem proteção) seguem sendo atacados normalmente</li>
     *   <li>Sigil = mobs ignoram + AOE pula</li>
     *   <li>Crown = mobs ignoram + AOE pula + cancela dano direto (imunidade)</li>
     * </ul>
     */
    public static boolean isProtected(LivingEntity entity) {
        if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return true;
        }
        return hasSigil(entity) || hasActiveCrown(entity);
    }


    /**
     * v0.1.22 REFAtorado: agora considera QUALQUER mob/entity do mod Liberthia
     * (não só blood). User pediu: "todos do mod liberthia nenhum me ve como
     * ameaça". Cobre blood, flesh, corruptos, ordem, etc — tudo via namespace.
     */
    public static boolean isBloodCreatureStatic(LivingEntity entity) {
        return br.com.murilo.liberthia.logic.LiberthiaMob.isMob(entity);
    }

    private static boolean isBloodCreature(LivingEntity entity) {
        return isBloodCreatureStatic(entity);
    }

    /**
     * Hook 1: criatura de sangue tenta selecionar alvo → cancela se o alvo é
     * player com sigil.
     */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity attacker = event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (target == null) return;
        if (!isBloodCreature(attacker)) return;
        boolean protectedByGameMode = target instanceof Player p
                && (p.isCreative() || p.isSpectator());
        // v0.1.22: Boss Crown ativa também protege
        if (hasSigil(target) || hasActiveCrown(target) || protectedByGameMode) {
            event.setNewTarget(null);
            if (attacker instanceof Mob mob) mob.setTarget(null);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!isProtected(event.getEntity())) return;
        var src = event.getSource().getEntity();
        if (src instanceof LivingEntity attacker && isBloodCreature(attacker)) {
            if (attacker instanceof Mob mob) mob.setTarget(null);
            event.setCanceled(true);
            return;
        }
        // v0.1.22 r4: também cancela damage source MAGIC/WITHER/SONIC_BOOM/MOB
        // sem entidade source identificável SE veio de blood creature/block
        // (proxy attacks como sonic_boom do warden, beams de WitheringEye)
        var damageType = event.getSource().typeHolder().value().msgId();
        if (("sonic_boom".equals(damageType) || "wither".equals(damageType))
                && src instanceof LivingEntity le && isBloodCreature(le)) {
            event.setCanceled(true);
        }
    }

    /**
     * v0.1.22 r6: bloqueia COMPLETAMENTE qualquer MobEffect HARMFUL
     * (categoria vanilla HARMFUL) em players protegidos. User pediu: "não de
     * Wither nem efeito em mim por estar perto dos monstros". Cobre TODOS os
     * caminhos — Wither, Slowness, Weakness, Blindness, Poison, Hunger,
     * Nausea, Darkness, MiningFatigue, BLOOD_INFECTION, etc.
     *
     * <p>NÃO bloqueia efeitos BENEFICIAL ou NEUTRAL (Regeneration, Resistance,
     * Speed, etc.) — o player continua podendo tomar buffs de outros mods/items.
     */
    @SubscribeEvent
    public static void onEffectApply(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        if (!isProtected(event.getEntity())) return;
        var effect = event.getEffectInstance().getEffect();
        if (effect.getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }
}
