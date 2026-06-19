package br.com.murilo.liberthia.observation.block;

import br.com.murilo.liberthia.observation.api.ObservationPart;
import br.com.murilo.liberthia.observation.api.ObservationRegistry;
import br.com.murilo.liberthia.observation.api.ObservationSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * v0.1.22 r72: <b>Rune Block Entity</b> — armazena spell recipe + cooldown
 * per-entity pra prevenir trigger duplicado.
 */
public class RuneBlockEntity extends BlockEntity {

    private List<String> spellRecipe = new ArrayList<>();
    private String spellName = "";
    private int spellColor = 0x9d4dd6;
    /** Cooldown map: entityUUID → next valid trigger gameTime. */
    private final Map<UUID, Long> triggerCooldowns = new HashMap<>();

    public RuneBlockEntity(BlockPos pos, BlockState state) {
        super(br.com.murilo.liberthia.registry.ModBlockEntities.RUNE_BLOCK.get(), pos, state);
    }

    public boolean hasSpell() { return !spellRecipe.isEmpty(); }
    public List<String> getSpellRecipe() { return spellRecipe; }
    public String getSpellName() { return spellName.isEmpty() ? "Spell" : spellName; }
    public int getSpellColor() { return spellColor; }

    public void setSpellRecipe(List<String> recipe) {
        this.spellRecipe = new ArrayList<>(recipe);
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    public void setSpellName(String name) {
        this.spellName = name;
        setChanged();
    }
    public void setSpellColor(int color) {
        this.spellColor = color;
        setChanged();
    }

    /** True se o entity ainda está em cooldown. */
    public boolean isOnCooldownFor(UUID entityId, long gameTime) {
        Long until = triggerCooldowns.get(entityId);
        return until != null && gameTime < until;
    }

    public void setTriggeredBy(UUID entityId, long gameTime) {
        triggerCooldowns.put(entityId, gameTime + RuneBlock.TRIGGER_COOLDOWN);
        // Cleanup old entries
        triggerCooldowns.entrySet().removeIf(e -> gameTime > e.getValue() + 200);
    }

    @Nullable
    public ObservationSpell buildSpell() {
        if (spellRecipe.isEmpty()) return null;
        ObservationSpell.Builder b = ObservationSpell.builder(
            spellName.isEmpty() ? "Rune" : spellName, spellColor);
        for (String id : spellRecipe) {
            ObservationPart p = ObservationRegistry.get(new ResourceLocation(id));
            if (p != null) b.add(p);
        }
        return b.build();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (String id : spellRecipe) list.add(StringTag.valueOf(id));
        tag.put("recipe", list);
        tag.putString("name", spellName);
        tag.putInt("color", spellColor);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        spellRecipe.clear();
        if (tag.contains("recipe")) {
            ListTag list = tag.getList("recipe", 8);
            for (int i = 0; i < list.size(); i++) spellRecipe.add(list.getString(i));
        }
        spellName = tag.getString("name");
        if (tag.contains("color")) spellColor = tag.getInt("color");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }
}
