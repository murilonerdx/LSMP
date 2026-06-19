package br.com.murilo.liberthia.automation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * v0.1.24 r100: <b>Mage Cauldron</b> — auto-crafter simplificado.
 *
 * <p>10 slots: 9 input + 1 output. Right-click com item armazena no primeiro
 * slot vazio. A cada 60t, se houver pelo menos 4 itens diferentes, "magicka"
 * combina pra produzir o item indicado no slot 9 (configurado via shift+right-click
 * com item desejado).
 *
 * <p>Simplificação: não usa recipe registry — só converte stacks aleatoriamente
 * em items "alquímicos" (placeholder). Pode ser extendido.
 */
public class MageCauldronBlock extends Block implements EntityBlock {

    public MageCauldronBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof Tile t) Tile.tick(lvl, pos, st, t);
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof Tile t)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // Set target output
            if (!held.isEmpty()) {
                t.targetOutput = held.copy();
                t.targetOutput.setCount(1);
                t.setChanged();
                player.displayClientMessage(Component.literal(
                        "§5✦ Output configurado: §b" + held.getHoverName().getString()), false);
            }
            return InteractionResult.CONSUME;
        }

        // r164: mão vazia agora abre INFO SCREEN em vez de só take output silencioso.
        // Take output continua se houver, mas o player também vê a UI.
        if (held.isEmpty()) {
            // Take output if any
            if (!t.items.get(9).isEmpty()) {
                player.getInventory().placeItemBackInInventory(t.items.get(9).copy());
                t.items.set(9, ItemStack.EMPTY);
                t.setChanged();
                player.displayClientMessage(Component.literal("§a✓ Output retirado"), true);
                return InteractionResult.CONSUME;
            }
            // Sem output → abre info screen
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                openInfoScreen(sp, t);
            }
            return InteractionResult.CONSUME;
        }

        // Insert item
        for (int i = 0; i < 9; i++) {
            if (t.items.get(i).isEmpty()) {
                t.items.set(i, held.split(1));
                t.setChanged();
                return InteractionResult.CONSUME;
            }
        }
        player.displayClientMessage(Component.literal("§c⚠ Caldeirão cheio (9/9 slots)"), true);
        return InteractionResult.CONSUME;
    }

    /** r164: abre a info screen mostrando status do caldeirão. */
    private void openInfoScreen(net.minecraft.server.level.ServerPlayer sp, Tile t) {
        int filled = 0;
        for (int i = 0; i < 9; i++) if (!t.items.get(i).isEmpty()) filled++;
        java.util.List<String> stats = new java.util.ArrayList<>();
        stats.add("§7Slots de entrada: §e" + filled + "§7/§e9");
        stats.add("§7Output configurado: " + (t.targetOutput.isEmpty()
                ? "§c§o(nenhum — configure com Shift+RClick)"
                : "§a" + t.targetOutput.getHoverName().getString()));
        stats.add("§7Output pronto: " + (t.items.get(9).isEmpty() ? "§8não" : "§a§lSIM"));
        stats.add("§7Tick rate: §b60t (3s)");
        stats.add("§7Receita: §74 items → §a1 output");

        java.util.List<String> instr = new java.util.ArrayList<>();
        instr.add("§7• §eRClick com item§7 = inserir nos slots de entrada");
        instr.add("§7• §eShift+RClick com item§7 = configurar output target");
        instr.add("§7• §eRClick com mão vazia§7 = retirar output (ou ver isto)");
        instr.add("§7• A cada §b3s§7, se houver §a4+ items§7, converte em output");

        br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                new br.com.murilo.liberthia.network.packet.OpenBlockInfoS2CPacket(
                        "✦ Mage Cauldron", stats, instr, 0xAA66FF));
    }

    public static class Tile extends BlockEntity implements WorldlyContainer {
        public NonNullList<ItemStack> items = NonNullList.withSize(10, ItemStack.EMPTY);
        public ItemStack targetOutput = ItemStack.EMPTY;

        public Tile(BlockPos pos, BlockState state) {
            super(br.com.murilo.liberthia.registry.ModBlockEntities.MAGE_CAULDRON.get(), pos, state);
        }

        public static void tick(Level level, BlockPos pos, BlockState state, Tile t) {
            if (!(level instanceof ServerLevel sl)) return;
            if (level.getGameTime() % 60 != 0) return;
            if (t.targetOutput.isEmpty()) return;
            if (!t.items.get(9).isEmpty()) return;

            // Conta items not-empty nos 9 input slots
            int filled = 0;
            for (int i = 0; i < 9; i++) if (!t.items.get(i).isEmpty()) filled++;
            if (filled < 4) return;

            // Consume 4 items + produce output
            int consumed = 0;
            for (int i = 0; i < 9 && consumed < 4; i++) {
                if (!t.items.get(i).isEmpty()) {
                    t.items.get(i).shrink(1);
                    if (t.items.get(i).isEmpty()) t.items.set(i, ItemStack.EMPTY);
                    consumed++;
                }
            }
            t.items.set(9, t.targetOutput.copy());
            t.setChanged();

            // Visual
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    15, 0.3, 0.3, 0.3, 0.05);
        }

        @Override public int getContainerSize() { return items.size(); }
        @Override public boolean isEmpty() {
            for (ItemStack s : items) if (!s.isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot) { return items.get(slot); }
        @Override public ItemStack removeItem(int slot, int amount) {
            return ContainerHelper.removeItem(items, slot, amount);
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack s = items.get(slot);
            items.set(slot, ItemStack.EMPTY);
            return s;
        }
        @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); }
        @Override public boolean stillValid(Player p) { return true; }
        @Override public void clearContent() { items.clear(); }

        @Override public int[] getSlotsForFace(net.minecraft.core.Direction side) {
            return new int[]{0,1,2,3,4,5,6,7,8,9};
        }
        @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction direction) {
            return slot < 9;
        }
        @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction direction) {
            return slot == 9;
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            ContainerHelper.saveAllItems(tag, items);
            if (!targetOutput.isEmpty()) {
                CompoundTag t = new CompoundTag();
                targetOutput.save(t);
                tag.put("TargetOutput", t);
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            ContainerHelper.loadAllItems(tag, items);
            if (tag.contains("TargetOutput")) {
                targetOutput = ItemStack.of(tag.getCompound("TargetOutput"));
            }
        }
    }
}
