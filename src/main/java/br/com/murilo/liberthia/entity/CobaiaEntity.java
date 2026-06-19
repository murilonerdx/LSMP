package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * r180: <b>Cobaia</b> — sujeito de testes de matéria. Injeta-se matéria nela (clique direito
 * com pílula/injetor de matéria → sobe o nível de infecção), e tira-se <b>amostras</b>
 * (clique direito com Frasco de Vidro → recebe uma Amostra de Matéria pra estudar).
 * Ao chegar a <b>100%</b>, a matéria toma o corpo: a Cobaia muta (vira um Zumbi Corrompido)
 * numa explosão de matéria.
 */
public class CobaiaEntity extends PathfinderMob {

    private int matterLevel = 0;          // 0..100
    private String matterType = "none";   // dark / clear / yellow / mixed

    public CobaiaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    public int getMatterLevel() { return matterLevel; }
    public String getMatterType() { return matterType; }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide) {
            String id = key(held);
            boolean matter = id.contains("matter") && (id.contains("pill") || id.contains("injector") || id.contains("dose"));
            return (matter || held.is(Items.GLASS_BOTTLE)) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        String id = key(held);
        // 1) INJETAR matéria
        if (id.contains("matter") && (id.contains("pill") || id.contains("injector") || id.contains("dose"))) {
            inject(player, held, id);
            return InteractionResult.CONSUME;
        }
        // 2) TIRAR AMOSTRA (frasco de vidro → amostra)
        if (held.is(Items.GLASS_BOTTLE)) {
            takeSample(player, held);
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    private void inject(Player player, ItemStack pill, String id) {
        String t = id.contains("dark") ? "dark" : id.contains("clear") ? "clear" : id.contains("yellow") ? "yellow" : "mixed";
        if (!matterType.equals(t) && !matterType.equals("none")) t = "mixed";
        matterType = t;
        matterLevel = Math.min(100, matterLevel + 20);
        if (!player.getAbilities().instabuild) pill.shrink(1);

        if (level() instanceof ServerLevel sl) {
            typeParticles(sl, 20);
            sl.playSound(null, blockPosition(), SoundEvents.HONEY_BLOCK_BREAK, SoundSource.NEUTRAL, 0.8F, 1.4F);
        }
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
        player.displayClientMessage(Component.literal("§aMatéria injetada — §f" + matterType + " §a(" + matterLevel + "%)"), true);
        if (matterLevel >= 100) transform();
    }

    private void takeSample(Player player, ItemStack bottle) {
        if (!player.getAbilities().instabuild) bottle.shrink(1);
        ItemStack sample = new ItemStack(ModItems.MATTER_SAMPLE.get());
        CompoundTag tag = sample.getOrCreateTag();
        tag.putString("MatterType", matterType);
        tag.putInt("MatterLevel", matterLevel);
        if (!player.getInventory().add(sample)) player.drop(sample, false);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SNEEZE, getX(), getY() + 1.0, getZ(), 6, 0.2, 0.3, 0.2, 0.0);
            sl.playSound(null, blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 0.8F, 1.0F);
        }
        player.displayClientMessage(Component.literal("§bAmostra coletada — §f" + matterType + " §b(" + matterLevel + "%)"), true);
    }

    /** A matéria toma o corpo (100%): muta numa explosão. */
    private void transform() {
        if (!(level() instanceof ServerLevel sl)) return;
        typeParticles(sl, 80);
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.NEUTRAL, 1.2F, 0.6F);
        CorruptedZombieEntity z = ModEntities.CORRUPTED_ZOMBIE.get().create(sl);
        if (z != null) {
            z.moveTo(getX(), getY(), getZ(), getYRot(), 0);
            sl.addFreshEntity(z);
        }
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        // ganho ambiente leve se estiver em chunk infectado (laboratório vivo)
        if (this.tickCount % 100 == 0 && matterLevel < 100) {
            float density = br.com.murilo.liberthia.logic.InfectionLogic.getChunkInfectionDensity(sl, blockPosition());
            if (density > 0.3F) {
                if (matterType.equals("none")) matterType = "mixed";
                matterLevel = Math.min(100, matterLevel + 1);
                if (matterLevel >= 100) transform();
            }
        }
        // partículas conforme o nível
        if (matterLevel > 0 && this.tickCount % 12 == 0) {
            typeParticles(sl, 1 + matterLevel / 25);
        }
    }

    private void typeParticles(ServerLevel sl, int count) {
        var p = switch (matterType) {
            case "dark" -> ParticleTypes.SQUID_INK;
            case "clear" -> ParticleTypes.END_ROD;
            case "yellow" -> ParticleTypes.FLAME;
            default -> ParticleTypes.WITCH;
        };
        sl.sendParticles(p, getX(), getY() + getBbHeight() * 0.6, getZ(), count, 0.3, 0.4, 0.3, 0.02);
    }

    private static String key(ItemStack s) {
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem()).getPath();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MatterLevel", matterLevel);
        tag.putString("MatterType", matterType);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        matterLevel = tag.getInt("MatterLevel");
        matterType = tag.contains("MatterType") ? tag.getString("MatterType") : "none";
    }
}
