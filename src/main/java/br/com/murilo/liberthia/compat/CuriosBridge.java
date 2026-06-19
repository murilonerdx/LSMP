package br.com.murilo.liberthia.compat;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

/**
 * Bridge concreto com a API Curios. Esta classe SÓ deve ser carregada quando
 * Curios estiver presente no classpath (verificado por {@link CuriosCompat#isCuriosLoaded()}).
 */
final class CuriosBridge {

    private CuriosBridge() {}

    static void attach(AttachCapabilitiesEvent<ItemStack> event, ItemStack stack) {
        event.addCapability(
                new ResourceLocation(LiberthiaMod.MODID, "containment_glove_curio"),
                new Provider(new GenericCurio(stack))
        );
    }

    static void attachPendant(AttachCapabilitiesEvent<ItemStack> event, ItemStack stack) {
        event.addCapability(
                new ResourceLocation(LiberthiaMod.MODID, "white_matter_pendant_curio"),
                new Provider(new GenericCurio(stack))
        );
    }

    static void attachRefinedPendant(AttachCapabilitiesEvent<ItemStack> event, ItemStack stack) {
        event.addCapability(
                new ResourceLocation(LiberthiaMod.MODID, "refined_containment_pendant_curio"),
                new Provider(new GenericCurio(stack))
        );
    }

    static void attachRefinedGlove(AttachCapabilitiesEvent<ItemStack> event, ItemStack stack) {
        event.addCapability(
                new ResourceLocation(LiberthiaMod.MODID, "refined_containment_glove_curio"),
                new Provider(new GenericCurio(stack))
        );
    }

    // ── v1 — matter pendants ──
    static void attachDarkMatterPendant(AttachCapabilitiesEvent<ItemStack> event, ItemStack stack) {
        event.addCapability(
                new ResourceLocation(LiberthiaMod.MODID, "dark_matter_pendant_curio"),
                new Provider(new GenericCurio(stack))
        );
    }
    static void attachClearMatterPendant(AttachCapabilitiesEvent<ItemStack> event, ItemStack stack) {
        event.addCapability(
                new ResourceLocation(LiberthiaMod.MODID, "clear_matter_pendant_curio"),
                new Provider(new GenericCurio(stack))
        );
    }
    static void attachYellowMatterPendant(AttachCapabilitiesEvent<ItemStack> event, ItemStack stack) {
        event.addCapability(
                new ResourceLocation(LiberthiaMod.MODID, "yellow_matter_pendant_curio"),
                new Provider(new GenericCurio(stack))
        );
    }

    // ── r164: Magic Accessories (rings/gloves/belts pro sistema novo) ──
    static void attachMagicAccessory(AttachCapabilitiesEvent<ItemStack> event, ItemStack stack) {
        String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();
        event.addCapability(
                new ResourceLocation(LiberthiaMod.MODID, "accessory_" + id),
                new Provider(new GenericCurio(stack))
        );
    }

    // ── r179: Blood Pact Amulet — curio de necklace que dá +3 ATK / -4 MAX_HEALTH ──
    static void attachBloodPact(AttachCapabilitiesEvent<ItemStack> event, ItemStack stack) {
        event.addCapability(
                new ResourceLocation(LiberthiaMod.MODID, "blood_pact_curio"),
                new Provider(new BloodPactCurio(stack))
        );
    }

    // ── v1 — Astaron items ──
    static void attachReliquiaAstaron(AttachCapabilitiesEvent<ItemStack> event, ItemStack stack) {
        event.addCapability(
                new ResourceLocation(LiberthiaMod.MODID, "reliquia_astaron_curio"),
                new Provider(new GenericCurio(stack))
        );
    }
    static void attachPesQueimantes(AttachCapabilitiesEvent<ItemStack> event, ItemStack stack) {
        event.addCapability(
                new ResourceLocation(LiberthiaMod.MODID, "pes_queimantes_curio"),
                new EquipAwareProvider(new PesQueimantesCurio(stack))
        );
    }
    static void attachBotasMercuriais(AttachCapabilitiesEvent<ItemStack> event, ItemStack stack) {
        event.addCapability(
                new ResourceLocation(LiberthiaMod.MODID, "botas_mercuriais_curio"),
                new Provider(new GenericCurio(stack))
        );
    }

    // ── Finders ──
    static ItemStack findGlove(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, s -> s.is(ModItems.CONTAINMENT_GLOVE.get()))
                .map(slotResult -> slotResult.stack())
                .orElse(ItemStack.EMPTY);
    }

    static ItemStack findPendant(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, s -> s.is(ModItems.WHITE_MATTER_PENDANT.get()))
                .map(slotResult -> slotResult.stack())
                .orElse(ItemStack.EMPTY);
    }

    static ItemStack findRefinedPendant(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, s -> s.is(ModItems.REFINED_CONTAINMENT_PENDANT.get()))
                .map(slotResult -> slotResult.stack())
                .orElse(ItemStack.EMPTY);
    }

    static ItemStack findRefinedGlove(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, s -> s.is(ModItems.REFINED_CONTAINMENT_GLOVE.get()))
                .map(slotResult -> slotResult.stack())
                .orElse(ItemStack.EMPTY);
    }

    static ItemStack findDarkMatterPendant(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, s -> s.is(ModItems.DARK_MATTER_PENDANT.get()))
                .map(slotResult -> slotResult.stack())
                .orElse(ItemStack.EMPTY);
    }

    static ItemStack findClearMatterPendant(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, s -> s.is(ModItems.CLEAR_MATTER_PENDANT.get()))
                .map(slotResult -> slotResult.stack())
                .orElse(ItemStack.EMPTY);
    }

    static ItemStack findYellowMatterPendant(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, s -> s.is(ModItems.YELLOW_MATTER_PENDANT.get()))
                .map(slotResult -> slotResult.stack())
                .orElse(ItemStack.EMPTY);
    }

    static ItemStack findReliquiaAstaron(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, s -> s.is(ModItems.RELIQUIA_PROTECAO_ASTARON.get()))
                .map(slotResult -> slotResult.stack())
                .orElse(ItemStack.EMPTY);
    }
    /** r195 — finder genérico por item (10 Relíquias de Astaron). */
    static ItemStack findByItem(Player player, net.minecraft.world.item.Item item) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, s -> s.is(item))
                .map(slotResult -> slotResult.stack())
                .orElse(ItemStack.EMPTY);
    }

    static ItemStack findPesQueimantes(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, s -> s.is(ModItems.PES_QUEIMANTES_ASTARON.get()))
                .map(slotResult -> slotResult.stack())
                .orElse(ItemStack.EMPTY);
    }

    static ItemStack findBotasMercuriais(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, s -> s.is(ModItems.BOTAS_MERCURIAIS.get()))
                .map(slotResult -> slotResult.stack())
                .orElse(ItemStack.EMPTY);
    }

    // ── Damagers ──
    static void damagePendant(Player player, int amount) {
        damageStack(findPendant(player), amount);
    }
    static void damageRefinedPendant(Player player, int amount) {
        damageStack(findRefinedPendant(player), amount);
    }
    static void damageRefinedGlove(Player player, int amount) {
        damageStack(findRefinedGlove(player), amount);
    }
    static void damageDarkMatterPendant(Player player, int amount) {
        damageStack(findDarkMatterPendant(player), amount);
    }
    static void damageClearMatterPendant(Player player, int amount) {
        damageStack(findClearMatterPendant(player), amount);
    }
    static void damageYellowMatterPendant(Player player, int amount) {
        damageStack(findYellowMatterPendant(player), amount);
    }
    static void damageBotasMercuriais(Player player, int amount) {
        damageStack(findBotasMercuriais(player), amount);
    }

    /** v0.1.35: drena durab dos Pés Queimantes (não existia antes). */
    static void damagePesQueimantes(Player player, int amount) {
        damageStack(findPesQueimantes(player), amount);
    }

    /**
     * v0.1.43: SAFETY — items SEM .durability() têm maxDamage = 0.
     * Antes: newDamage(1) >= maxDamage(0) → shrink(1) → ITEM DELETADO no
     * primeiro tick de drain. Bug crítico: botas/cinto/keys sumindo.
     * Agora skipamos drain pra items sem durability.
     */
    private static void damageStack(ItemStack stack, int amount) {
        if (stack.isEmpty()) return;
        if (stack.getMaxDamage() <= 0) return; // não-damageable, skip silencioso
        int newDamage = stack.getDamageValue() + amount;
        if (newDamage >= stack.getMaxDamage()) {
            stack.shrink(1);
        } else {
            stack.setDamageValue(newDamage);
        }
    }

    /**
     * ICurio genérico para items que apenas precisam estar num slot Curios.
     */
    private static class GenericCurio implements ICurio {
        protected final ItemStack stack;
        GenericCurio(ItemStack stack) { this.stack = stack; }

        @Override public ItemStack getStack() { return stack; }

        @Override public boolean canEquipFromUse(SlotContext context) { return true; }

        @Override public DropRule getDropRule(
                SlotContext context, net.minecraft.world.damagesource.DamageSource source,
                int lootingLevel, boolean recentlyHit) {
            return DropRule.DEFAULT;
        }
    }

    /**
     * ICurio dos Pés Queimantes — quando equipado, gera uma FlameKey
     * na mão do player (uma vez).
     */
    private static class PesQueimantesCurio extends GenericCurio {
        PesQueimantesCurio(ItemStack stack) { super(stack); }

        @Override
        public void onEquip(SlotContext context, ItemStack prevStack) {
            super.onEquip(context, prevStack);
            // report #81: delega pro helper idempotente — antes esta fonte dava
            // key SEM dedup, e junto com o PesQueimantesHandler gerava chaves
            // infinitas. Agora ambos passam pelo mesmo guard (flag + hasFlameKey).
            if (context.entity() instanceof Player player) {
                br.com.murilo.liberthia.event.PesQueimantesHandler.giveFlameKeyOnce(player);
            }
        }
    }

    /**
     * r179: Blood Pact Amulet como Curios (necklace). Aplica +3 ATK_DAMAGE e -4 MAX_HEALTH
     * enquanto equipado (Curios aplica os attribute modifiers automaticamente no equip/unequip).
     */
    private static class BloodPactCurio extends GenericCurio {
        BloodPactCurio(ItemStack stack) { super(stack); }

        @Override
        public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,
                net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(
                SlotContext slotContext, java.util.UUID uuid) {
            com.google.common.collect.ImmutableMultimap.Builder<
                    net.minecraft.world.entity.ai.attributes.Attribute,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier> b =
                    com.google.common.collect.ImmutableMultimap.builder();
            // r196: UUIDs DISTINTOS por atributo (com o mesmo uuid do slot, o -4 de vida não aplicava)
            b.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            java.util.UUID.fromString("b10d9ac7-0001-4a11-8a11-000000000001"), "Blood Pact ATK", 3.0D,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
            b.put(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            java.util.UUID.fromString("b10d9ac7-0002-4a11-8a11-000000000002"), "Blood Pact HP", -4.0D,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
            return b.build();
        }
    }

    private static final class Provider implements ICapabilityProvider {
        private final LazyOptional<ICurio> opt;
        Provider(ICurio curio) { this.opt = LazyOptional.of(() -> curio); }

        @Override
        public <T> LazyOptional<T> getCapability(
                net.minecraftforge.common.capabilities.Capability<T> cap,
                net.minecraft.core.Direction side) {
            return CuriosCapability.ITEM.orEmpty(cap, opt.cast());
        }
    }

    /** Variante de provider só pra Pés Queimantes (alias semântico). */
    private static final class EquipAwareProvider implements ICapabilityProvider {
        private final LazyOptional<ICurio> opt;
        EquipAwareProvider(ICurio curio) { this.opt = LazyOptional.of(() -> curio); }

        @Override
        public <T> LazyOptional<T> getCapability(
                net.minecraftforge.common.capabilities.Capability<T> cap,
                net.minecraft.core.Direction side) {
            return CuriosCapability.ITEM.orEmpty(cap, opt.cast());
        }
    }
}
