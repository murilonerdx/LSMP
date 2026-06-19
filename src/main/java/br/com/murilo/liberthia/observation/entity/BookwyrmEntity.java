package br.com.murilo.liberthia.observation.entity;

import br.com.murilo.liberthia.observation.api.ObservationPart;
import br.com.murilo.liberthia.observation.api.ObservationRegistry;
import br.com.murilo.liberthia.observation.api.ObservationResolver;
import br.com.murilo.liberthia.observation.api.ObservationSpell;
import br.com.murilo.liberthia.observation.item.SpellParchmentItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * v0.1.22 r74: <b>Bookwyrm Familiar</b> — pet voador que segue o owner e
 * automaticamente casta um spell salvo em mob hostil próximo.
 *
 * <p>Pattern AN's Bookwyrm.
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>Segue o owner (FollowMobGoal)</li>
 *   <li>Right-click com Parchment: imprinta spell no Bookwyrm</li>
 *   <li>A cada 200t (10s), se hostile mob &lt; 16b, casta spell nele</li>
 *   <li>Spell custo é pago pelo OWNER, não por ele mesmo</li>
 * </ul>
 */
public class BookwyrmEntity extends PathfinderMob {

    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(
        BookwyrmEntity.class, EntityDataSerializers.INT);

    private List<String> spellRecipe = new ArrayList<>();
    private String spellName = "Tendril";
    private int spellColor = 0x9d4dd6;
    @Nullable private UUID ownerUuid;
    private int castCooldown = 0;

    public BookwyrmEntity(EntityType<? extends BookwyrmEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 8.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COLOR, 0x9d4dd6);
    }

    public int getColor() { return entityData.get(COLOR); }
    public void setOwnerUuid(UUID id) { this.ownerUuid = id; }
    public @Nullable UUID getOwnerUuid() { return ownerUuid; }

    /** Imprintar spell de um parchment. */
    public void imprintFromParchment(ItemStack parchment) {
        var recipe = SpellParchmentItem.getRecipe(parchment);
        if (recipe.isEmpty()) return;
        this.spellRecipe = new ArrayList<>(recipe);
        var spell = SpellParchmentItem.buildSpell(parchment);
        if (spell != null) {
            this.spellName = spell.name();
            this.spellColor = spell.color();
            entityData.set(COLOR, spellColor);
        }
    }

    /** Right-click — se Parchment, imprinta. Senão tooltip mostra o spell. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof SpellParchmentItem) {
            var recipe = SpellParchmentItem.getRecipe(held);
            if (recipe.isEmpty()) {
                sp.displayClientMessage(Component.literal("§c⚠ Pergaminho vazio."), true);
                return InteractionResult.FAIL;
            }
            imprintFromParchment(held);
            if (this.ownerUuid == null) this.ownerUuid = sp.getUUID();
            sp.displayClientMessage(Component.literal(
                "§5§l✦ Bookwyrm imprintado: §r§e" + spellName), false);
            return InteractionResult.CONSUME;
        }
        // Just show info
        sp.displayClientMessage(Component.literal(
            "§5§l✦ Bookwyrm §r§7| spell: §e" + spellName
            + " §7| owner: §f" + (ownerUuid != null ? "set" : "none")), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (castCooldown > 0) castCooldown--;

        // Try to auto-cast every 10s
        if (castCooldown == 0 && !spellRecipe.isEmpty() && ownerUuid != null) {
            ServerPlayer owner = level().getServer().getPlayerList().getPlayer(ownerUuid);
            if (owner != null && owner.isAlive() && this.distanceToSqr(owner) < 256) {
                // Find hostile mob within 16b
                var hostile = level().getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(16),
                    e -> e instanceof Monster && e.isAlive());
                if (!hostile.isEmpty()) {
                    LivingEntity target = hostile.get(0);
                    // Build spell + cast it as owner
                    ObservationSpell spell = buildSpell();
                    if (spell != null) {
                        // Aim at target (set look angle)
                        owner.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                        ObservationResolver.cast(owner, spell);
                        owner.displayClientMessage(Component.literal(
                            "§5§l✦ Bookwyrm cast: §e" + spell.name()), true);
                    }
                    castCooldown = 200; // 10s
                }
            }
        }
    }

    @Nullable
    public ObservationSpell buildSpell() {
        if (spellRecipe.isEmpty()) return null;
        ObservationSpell.Builder b = ObservationSpell.builder(spellName, spellColor);
        for (String id : spellRecipe) {
            ObservationPart p = ObservationRegistry.get(new ResourceLocation(id));
            if (p != null) b.add(p);
        }
        return b.build();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag list = new ListTag();
        for (String id : spellRecipe) list.add(StringTag.valueOf(id));
        tag.put("spell_recipe", list);
        tag.putString("spell_name", spellName);
        tag.putInt("spell_color", spellColor);
        if (ownerUuid != null) tag.putUUID("owner", ownerUuid);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spellRecipe.clear();
        if (tag.contains("spell_recipe")) {
            ListTag list = tag.getList("spell_recipe", 8);
            for (int i = 0; i < list.size(); i++) spellRecipe.add(list.getString(i));
        }
        if (tag.contains("spell_name")) spellName = tag.getString("spell_name");
        if (tag.contains("spell_color")) {
            spellColor = tag.getInt("spell_color");
            entityData.set(COLOR, spellColor);
        }
        if (tag.hasUUID("owner")) ownerUuid = tag.getUUID("owner");
    }
}
