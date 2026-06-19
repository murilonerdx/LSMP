package br.com.murilo.liberthia.observation.perk;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * v0.1.22 r62: <b>Perk</b> — passive item modifier registered as singleton.
 *
 * <p>Inspired by Ars Nouveau's {@code IPerk}. Cada perk é um singleton
 * registrado por ResourceLocation, aplicado a items que implementam
 * {@link PerkHolder}.
 *
 * <h2>Tipos</h2>
 * <ul>
 *   <li>{@link TickablePerk} — tick lógico per-tick enquanto equipped</li>
 *   <li>{@link DamageMultPerk} — multiplica damage do owner</li>
 *   <li>{@link SourceCostPerk} — reduz Source cost dos spells</li>
 * </ul>
 *
 * <h2>Slots</h2>
 * Items têm múltiplos slots (1-3) de tier diferentes. Cada perk vai num slot.
 * O slot value (1, 2, ou 3) determina a intensidade do perk.
 */
public abstract class Perk {

    private final ResourceLocation id;
    private final String displayName;

    protected Perk(ResourceLocation id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public final ResourceLocation id() { return id; }
    public final String displayName() { return displayName; }
    public Component nameComponent() {
        return Component.literal(displayName);
    }
    public abstract List<Component> lore();

    /** Tick perk per-frame se for TickablePerk. Default no-op. */
    public interface TickablePerk {
        void tickPerk(ItemStack stack, ServerPlayer player, int slotValue);
    }

    /** Modifica damage do owner ao atacar. */
    public interface DamageMultPerk {
        float modifyDamage(float baseDamage, int slotValue);
    }

    /** Reduz custo de Source dos spells. */
    public interface SourceCostPerk {
        int reduceCost(int baseCost, int slotValue);
    }

    /** Boosta Sanidade ao usar magic. */
    public interface SanityShieldPerk {
        int reduceSanityCost(int baseCost, int slotValue);
    }
}
