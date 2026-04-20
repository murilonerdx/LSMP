package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.faction.Faction;
import br.com.murilo.liberthia.faction.FactionTag;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Passive mob found in abandoned cult camps. Right-click gives a Blood Cure
 * Pill + Tome of the Pilgrim once per pilgrim (NBT flag).
 */
public class WoundedPilgrimEntity extends PathfinderMob {

    private static final String TAG_GIVEN = "pilgrim_given";

    public WoundedPilgrimEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Mob.class, 10.0F, 1.2D, 1.3D,
                e -> FactionTag.get(e) == Faction.BLOOD && !(e instanceof WoundedPilgrimEntity)));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            CompoundTag tag = getPersistentData();
            if (!tag.getBoolean(TAG_GIVEN)) {
                tag.putBoolean(TAG_GIVEN, true);
                player.displayClientMessage(Component.literal(
                        "§4[Peregrino] §7\"Eu já fui da Ordem. Eles me chamaram de herege...\"")
                        .withStyle(ChatFormatting.ITALIC), false);
                player.displayClientMessage(Component.literal(
                        "§4[Peregrino] §7\"Se encontrares a Mãe, não olhes nos seus olhos.\"")
                        .withStyle(ChatFormatting.ITALIC), false);
                player.displayClientMessage(Component.literal(
                        "§4[Peregrino] §7\"Toma. Isto pode te salvar, se ainda não estiver perdido.\"")
                        .withStyle(ChatFormatting.ITALIC), false);
                if (!player.getInventory().add(new ItemStack(ModItems.BLOOD_CURE_PILL.get(), 1))) {
                    spawnAtLocation(new ItemStack(ModItems.BLOOD_CURE_PILL.get(), 1));
                }
                if (!player.getInventory().add(new ItemStack(ModItems.TOME_OF_THE_PILGRIM.get(), 1))) {
                    spawnAtLocation(new ItemStack(ModItems.TOME_OF_THE_PILGRIM.get(), 1));
                }
                return InteractionResult.CONSUME;
            } else {
                player.displayClientMessage(Component.literal(
                        "§4[Peregrino] §7\"...já te dei tudo que tinha. Vai.\"")
                        .withStyle(ChatFormatting.ITALIC), false);
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.BLOODY_RAG.get(), 1));
        if (!getPersistentData().getBoolean(TAG_GIVEN)) {
            spawnAtLocation(new ItemStack(ModItems.TOME_OF_THE_PILGRIM.get(), 1));
        }
    }
}
