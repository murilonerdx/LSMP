package br.com.murilo.liberthia.magic.perk;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * v0.1.24 r90: <b>Perks registry</b> — 15 perks implementados.
 *
 * <h2>Perks</h2>
 * <ol>
 *   <li>JUMP — +1 jump boost por stack</li>
 *   <li>STEP_HEIGHT — +0.5 step height</li>
 *   <li>REPAIRING — items perdem dano lentamente (mending lite)</li>
 *   <li>VAMPIRIC — heal 5% do damage dealt</li>
 *   <li>LOOTING — +1 looting effective level</li>
 *   <li>MAGIC_RESIST — -10% magic damage</li>
 *   <li>TOUGHNESS — +1 absorption per stack</li>
 *   <li>FEATHER — no fall damage</li>
 *   <li>GLIDING — slow fall passivo quando agachado</li>
 *   <li>MAGIC_CAPACITY — +50 max Source (TODO: integrar)</li>
 *   <li>SPELL_DAMAGE — +15% spell damage</li>
 *   <li>SATURATION — fome regen passiva</li>
 *   <li>KNOCKBACK_RESIST — +0.5 knockback resistance</li>
 *   <li>POTION_DURATION — duração +25% pra potions</li>
 *   <li>BONDED — bound a 1 spell preset</li>
 * </ol>
 */
public final class Perks {

    public static final Map<String, Perk> REGISTRY = new HashMap<>();

    static {
        register(new Perk("jump", "Jump", "+1 nível de Jump Boost", 3) {
            @Override public void onTick(Player p, int lvl) {
                p.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, lvl - 1, true, false));
            }
        });
        register(new Perk("step_height", "Step Height", "+0.5 step por stack", 2) {
            @Override public void onTick(Player p, int lvl) {
                p.setMaxUpStep(0.6F + lvl * 0.5F);
            }
            @Override public void onRemove(Player p) { p.setMaxUpStep(0.6F); }
        });
        register(new Perk("repairing", "Repairing", "Repara armor lentamente", 1) {
            @Override public void onTick(Player p, int lvl) {
                if (p.tickCount % 200 != 0) return;
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                    ItemStack s = p.getInventory().getItem(i);
                    if (s.isDamageableItem() && s.getDamageValue() > 0) {
                        s.setDamageValue(s.getDamageValue() - 1);
                    }
                }
            }
        });
        register(new Perk("vampiric", "Vampiric", "Heal 5% do damage dealt", 3) {
            // Hook real em LivingHurtEvent — flag NBT
            @Override public void onTick(Player p, int lvl) {
                p.getPersistentData().putInt("liberthia.perk_vampiric", lvl);
            }
            @Override public void onRemove(Player p) {
                p.getPersistentData().remove("liberthia.perk_vampiric");
            }
        });
        register(new Perk("magic_resist", "Magic Resist", "-10% magic damage", 3) {
            @Override public void onTick(Player p, int lvl) {
                p.getPersistentData().putInt("liberthia.perk_magic_resist", lvl * 10);
            }
            @Override public void onRemove(Player p) {
                p.getPersistentData().remove("liberthia.perk_magic_resist");
            }
        });
        register(new Perk("toughness", "Toughness", "+1 absorption por stack", 3) {
            @Override public void onTick(Player p, int lvl) {
                if (p.tickCount % 40 != 0) return;
                if (p.getAbsorptionAmount() < lvl * 2) {
                    p.setAbsorptionAmount(Math.min(p.getAbsorptionAmount() + 0.5F, lvl * 2));
                }
            }
        });
        register(new Perk("feather", "Feather", "Sem dano de queda", 1) {
            @Override public void onTick(Player p, int lvl) {
                p.fallDistance = 0;
            }
        });
        register(new Perk("gliding", "Gliding", "Slow Fall quando agachado", 1) {
            @Override public void onTick(Player p, int lvl) {
                if (p.isShiftKeyDown() && !p.onGround()) {
                    p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false));
                }
            }
        });
        register(new Perk("magic_capacity", "Magic Capacity", "+50 max Source", 3) {
            @Override public void onTick(Player p, int lvl) {
                p.getPersistentData().putInt("liberthia.perk_capacity_bonus", lvl * 50);
            }
            @Override public void onRemove(Player p) {
                p.getPersistentData().remove("liberthia.perk_capacity_bonus");
            }
        });
        register(new Perk("spell_damage", "Spell Damage", "+15% spell damage", 3) {
            @Override public void onTick(Player p, int lvl) {
                p.getPersistentData().putInt("liberthia.perk_spell_damage", lvl * 15);
            }
            @Override public void onRemove(Player p) {
                p.getPersistentData().remove("liberthia.perk_spell_damage");
            }
        });
        register(new Perk("saturation", "Saturation", "Fome regen passiva", 1) {
            @Override public void onTick(Player p, int lvl) {
                if (p.tickCount % 200 == 0 && p.getFoodData().getFoodLevel() < 20) {
                    p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel() + 1);
                }
            }
        });
        register(new Perk("knockback_resist", "Knockback Resist", "+0.5 KB resistance", 2) {
            @Override public void onTick(Player p, int lvl) {
                var attr = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
                if (attr != null) attr.setBaseValue(lvl * 0.5);
            }
            @Override public void onRemove(Player p) {
                var attr = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
                if (attr != null) attr.setBaseValue(0);
            }
        });
        register(new Perk("potion_duration", "Potion Duration", "+25% potion duration", 2) {
            @Override public void onTick(Player p, int lvl) {
                p.getPersistentData().putInt("liberthia.perk_potion_duration", lvl * 25);
            }
        });
        register(new Perk("looting", "Looting", "+1 looting level", 3) {
            @Override public void onTick(Player p, int lvl) {
                p.getPersistentData().putInt("liberthia.perk_looting", lvl);
            }
        });
        register(new Perk("bonded", "Bonded", "Bound a 1 spell preset", 1) {
            // Placeholder — futuro: marca preset bonded em NBT
            @Override public void onTick(Player p, int lvl) {}
        });
    }

    public static void register(Perk perk) {
        REGISTRY.put(perk.getId(), perk);
    }

    public static Perk get(String id) {
        return REGISTRY.get(id);
    }

    private Perks() {}
}
