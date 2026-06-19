package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Aplica efeitos especiais conforme o tipo + quantidade de peças de matter armor.
 *
 * <p>3 tipos: DARK, CLEAR, YELLOW. Cada tipo tem 4 peças (helmet/chest/legs/boots).
 *
 * <p>Efeitos escalados:
 * <ul>
 *   <li><b>1 peça</b>: 1 efeito leve relacionado ao tipo (Speed I / Strength I / etc).</li>
 *   <li><b>2 peças</b>: 2 efeitos leves.</li>
 *   <li><b>3 peças</b>: efeitos médios (II).</li>
 *   <li><b>4 peças (set bonus)</b>: bonus FORTE permanente — corações amarelos
 *       (absorption), Resistance II, e bonus único do tipo.</li>
 * </ul>
 *
 * <p>Efeitos por tipo:
 * <ul>
 *   <li><b>DARK</b>: Strength (força) + Night Vision + (set) Resistance II +
 *       Absorption 4 corações amarelos.</li>
 *   <li><b>CLEAR</b>: Speed (velocidade) + Jump Boost + (set) Regeneration II +
 *       Water Breathing + Absorption 3 corações amarelos.</li>
 *   <li><b>YELLOW</b>: Haste (mining speed) + Luck + (set) Hero of the Village +
 *       Saturation passiva + Absorption 5 corações amarelos.</li>
 * </ul>
 *
 * <p>Aplicado a cada 20t (1s). Efeitos não-permanentes (Speed/Strength/etc) são
 * refrescados constantemente — pra parecer "passivo enquanto vestindo". Sair
 * da peça remove o efeito no próximo tick (não aplica mais).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MatterArmorEffectsHandler {

    private MatterArmorEffectsHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        // Aplica a cada 20 ticks pra não floodar packets de status
        if (sp.tickCount % 20 != 0) return;

        int dark = countMatterArmor(sp, MatterType.DARK);
        int clear = countMatterArmor(sp, MatterType.CLEAR);
        int yellow = countMatterArmor(sp, MatterType.YELLOW);

        if (dark > 0) applyDarkEffects(sp, dark);
        if (clear > 0) applyClearEffects(sp, clear);
        if (yellow > 0) applyYellowEffects(sp, yellow);
    }

    private static void applyDarkEffects(ServerPlayer sp, int count) {
        // v0.1.22: duration 400 ticks (20s) com refresh a cada 20t — antes era
        // 220t. Vanilla pisca NIGHT_VISION quando duration < 200t (warning de
        // expiração). Com 220→ 200t exato no refresh = piscava. Agora 400→380
        // nunca cai abaixo de 200 entre refreshes.
        addEffect(sp, MobEffects.DAMAGE_BOOST, 100, count >= 3 ? 1 : 0);
        addEffect(sp, MobEffects.NIGHT_VISION, 400, 0);

        if (count >= 4) {
            // Set bonus: Resistance II + Absorption 4 corações (8 HP amarelos)
            addEffect(sp, MobEffects.DAMAGE_RESISTANCE, 100, 1);
            addEffect(sp, MobEffects.ABSORPTION, 100, 3); // amplifier 3 = 8 HP extras
        }
    }

    private static void applyClearEffects(ServerPlayer sp, int count) {
        // Speed (velocidade) + Jump Boost (salto)
        addEffect(sp, MobEffects.MOVEMENT_SPEED, 100, count >= 3 ? 1 : 0);
        addEffect(sp, MobEffects.JUMP, 100, count >= 3 ? 1 : 0);

        if (count >= 4) {
            // Set bonus: Regen II + Water Breathing + Absorption 3 corações
            addEffect(sp, MobEffects.REGENERATION, 100, 1);
            addEffect(sp, MobEffects.WATER_BREATHING, 400, 0); // visual ambient — duration alta evita flicker
            addEffect(sp, MobEffects.ABSORPTION, 100, 2);
        }
    }

    private static void applyYellowEffects(ServerPlayer sp, int count) {
        // Haste (mining speed) + Luck — útil pra exploração/mineração
        addEffect(sp, MobEffects.DIG_SPEED, 100, count >= 3 ? 1 : 0);
        addEffect(sp, MobEffects.LUCK, 100, 0);

        if (count >= 4) {
            // Set bonus: Hero of the Village + Saturation + Absorption 5 corações
            addEffect(sp, MobEffects.HERO_OF_THE_VILLAGE, 400, 0); // visual ambient
            addEffect(sp, MobEffects.SATURATION, 100, 0);
            addEffect(sp, MobEffects.ABSORPTION, 100, 4);
        }
    }

    /** Helper pra refrescar effect com particles=false (invisível mas ativo). */
    private static void addEffect(ServerPlayer sp, net.minecraft.world.effect.MobEffect effect,
                                   int duration, int amplifier) {
        // ambient=true, visible=false, showIcon=true — ícone aparece pro player
        // ver que o set bonus tá ativo, mas sem particles distraindo.
        sp.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }

    /** Conta peças de armor do tipo equipadas. */
    private static int countMatterArmor(ServerPlayer sp, MatterType type) {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = sp.getItemBySlot(slot);
            if (matches(stack.getItem(), type)) count++;
        }
        return count;
    }

    private static boolean matches(Item item, MatterType type) {
        return switch (type) {
            case DARK -> item == ModItems.DARK_MATTER_HELMET.get()
                    || item == ModItems.DARK_MATTER_CHESTPLATE.get()
                    || item == ModItems.DARK_MATTER_LEGGINGS.get()
                    || item == ModItems.DARK_MATTER_BOOTS.get();
            case CLEAR -> item == ModItems.CLEAR_MATTER_HELMET.get()
                    || item == ModItems.CLEAR_MATTER_CHESTPLATE.get()
                    || item == ModItems.CLEAR_MATTER_LEGGINGS.get()
                    || item == ModItems.CLEAR_MATTER_BOOTS.get();
            case YELLOW -> item == ModItems.YELLOW_MATTER_HELMET.get()
                    || item == ModItems.YELLOW_MATTER_CHESTPLATE.get()
                    || item == ModItems.YELLOW_MATTER_LEGGINGS.get()
                    || item == ModItems.YELLOW_MATTER_BOOTS.get();
        };
    }

    private enum MatterType { DARK, CLEAR, YELLOW }
}
