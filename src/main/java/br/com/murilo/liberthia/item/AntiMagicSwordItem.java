package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.compat.mna.MagicDetect;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;

/**
 * r180: <b>Espada Anti-Magia</b> — dá <b>dano EXTRA em quem carrega itens/artefatos de
 * magia</b> ({@link MagicDetect}) e aplica <b>Antimagia 5s</b> no acerto. Lâmina da linha
 * anti-magia (textura ciano). Craft com matéria escura.
 */
public class AntiMagicSwordItem extends SwordItem {

    private static final float BONUS_VS_MAGE = 6.0F;

    public AntiMagicSwordItem(Properties props) {
        super(Tiers.NETHERITE, 4, -2.4F, props);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean ok = super.hurtEnemy(stack, target, attacker);
        if (ok && !target.level().isClientSide) {
            boolean mage = (target instanceof Player tp && MagicDetect.isMagicUser(tp))
                    || target.getClass().getName().startsWith("com.mna");
            if (mage) {
                target.hurt(target.damageSources().magic(), BONUS_VS_MAGE); // dano extra anti-magia
                target.addEffect(new MobEffectInstance(ModEffects.ANTIMAGIA.get(), 100, 0, false, true, true));
                if (target.level() instanceof ServerLevel sl) {
                    var cyan = new DustParticleOptions(new Vector3f(0.17F, 0.84F, 0.84F), 1.2F);
                    sl.sendParticles(cyan, target.getX(), target.getY() + 1.0, target.getZ(), 16, 0.3, 0.4, 0.3, 0.0);
                }
            }
        }
        return ok;
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§3+" + (int) BONUS_VS_MAGE + " de dano em quem porta magia").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("§7Acerto aplica §bAntimagia §75s (sem voo/magia)"));
    }
}
