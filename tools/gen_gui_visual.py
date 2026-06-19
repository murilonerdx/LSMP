#!/usr/bin/env python3
"""
gen_gui_visual.py — Liberthia GUI/Block Studio.

Editor visual completo pra criar blocos+GUIs sem precisar abrir Blockbench
ou editor de imagem. Roda com Python 3 padrão (Tkinter já incluso).

Recursos:
    - Selector de pasta do mod (auto-detecta ModBlocks/ModItems/etc)
    - Canvas click-and-drag pra posicionar slots, barras, setas
    - Color picker com RGBA (transparência)
    - Templates de máquina: NoEnergy / Furnace / Crafting / Transformation
    - Auto-patch: insere registros nos arquivos do mod existentes
    - Preview live com tema customizável
"""
import tkinter as tk
from tkinter import ttk, filedialog, colorchooser, messagebox
import json, sys, os, re, subprocess
from pathlib import Path

try:
    from item_list import ALL_ITEMS
except ImportError:
    sys.path.insert(0, str(Path(__file__).parent))
    from item_list import ALL_ITEMS

# -------------------- Config --------------------
SCALE = 2
DEFAULT_MOD_PKG = "br.com.murilo.liberthia"


# -------------------- Helpers --------------------
def to_snake(name):
    return re.sub(r'([a-z])([A-Z])', r'\1_\2', name).lower()


def to_pascal(snake):
    return ''.join(p.capitalize() for p in snake.split('_'))


def hex_to_rgb(c):
    if c.startswith('0x'): c = c[2:]
    if len(c) == 8: c = c[2:]  # drop alpha
    return f"#{c}"


def rgb_to_argb(rgb_hex, alpha=255):
    if rgb_hex.startswith('#'): rgb_hex = rgb_hex[1:]
    return f"0x{alpha:02X}{rgb_hex.upper()}"


# -------------------- Templates --------------------
TEMPLATE_KINDS = {
    "no_energy":      ("Sem Energia",       "Bloco simples com inventário, sem FE"),
    "furnace":        ("Fornalha",          "Combustível + input → output ao longo de tempo"),
    "crafting":       ("Crafting",          "Múltiplos inputs → 1 output (instantâneo)"),
    "transformation": ("Transformação",     "Input + FE + tempo → output (mais comum)"),
    "generator":      ("Gerador",           "Input combustível → produz FE pra rede"),
}


# -------------------- Java codegen --------------------
class CodeGen:
    """Gera arquivos Java baseado em config + template kind."""

    def __init__(self, cfg, mod_root):
        self.cfg = cfg
        self.mod_root = Path(mod_root)
        self.pkg = cfg.get('package', DEFAULT_MOD_PKG)
        self.modid = cfg.get('modid', 'liberthia')
        self.name = cfg['name']
        self.snake = cfg['snake']
        self.const = cfg['block_const']
        self.kind = cfg.get('kind', 'transformation')

    def src_dir(self):
        # Encontra a pasta src/main/java/<pkg>/
        pkg_path = self.pkg.replace('.', '/')
        return self.mod_root / "src/main/java" / pkg_path

    def res_dir(self):
        return self.mod_root / "src/main/resources/assets" / self.modid

    def generate_all(self):
        results = []
        results.append(self.gen_menu())
        results.append(self.gen_screen())
        results.append(self.gen_be())
        results.append(self.gen_block())
        results.append(self.gen_blockstate())
        results.append(self.gen_block_model())
        results.append(self.gen_item_model())
        registrations = self.gen_registration_lines()
        return results, registrations

    def gen_menu(self):
        be_class = self.cfg['be_class']
        block_const = self.const
        data_slots = self.cfg.get('data_slots', 8)
        slots = self.cfg['slots']
        pinv = self.cfg['player_inv']
        hb = self.cfg['hotbar']

        slot_lines = "\n".join(
            f'        addSlot(new SlotItemHandler((IItemHandler) be.getInventory(), '
            f'{be_class}.SLOT_{s["name"]}, {s["x"]}, {s["y"]}));'
            for s in slots
        )

        getters = []
        if 'energy_bar' in self.cfg:
            eb = self.cfg['energy_bar']
            getters.extend([
                f'    public int rawEnergy()    {{ return read32({eb["energy_data_hi"]}, {eb["energy_data_lo"]}); }}',
                f'    public int rawEnergyMax() {{ return read32({eb["max_data_hi"]}, {eb["max_data_lo"]}); }}',
                '    public float energyFrac() { return Math.min(1f, rawEnergy() / (float) Math.max(1, rawEnergyMax())); }',
            ])
        if 'progress_arrow' in self.cfg:
            pa = self.cfg['progress_arrow']
            getters.extend([
                f'    public int progress()    {{ return data.get({pa["progress_data"]}); }}',
                f'    public int progressMax() {{ return Math.max(1, data.get({pa["progress_max_data"]})); }}',
                '    public float progressFrac() { return Math.min(1f, progress() / (float) progressMax()); }',
            ])
        getters_code = "\n".join(getters)

        code = f'''package {self.pkg}.menu;

import {self.pkg}.block.entity.{be_class};
import {self.pkg}.registry.ModBlocks;
import {self.pkg}.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class {self.name}Menu extends AbstractContainerMenu {{
    private final {be_class} be;
    private final Level level;
    private final ContainerData data;

    public {self.name}Menu(int id, Inventory inv, FriendlyByteBuf buf) {{
        this(id, inv,
                ({be_class}) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData({data_slots}));
    }}

    public {self.name}Menu(int id, Inventory inv, {be_class} be, ContainerData data) {{
        super(ModMenuTypes.{block_const}.get(), id);
        this.be = be;
        this.level = inv.player.level();
        this.data = data;
        addPlayer(inv);
{slot_lines}
        addDataSlots(data);
    }}

    private void addPlayer(Inventory inv) {{
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, {pinv["x"]} + col * 18, {pinv["y"]} + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, {hb["x"]} + col * 18, {hb["y"]}));
    }}

    private int read32(int hi, int lo) {{ return ((data.get(hi) & 0xFFFF) << 16) | (data.get(lo) & 0xFFFF); }}
{getters_code}

    public {be_class} getBlockEntity() {{ return be; }}

    @Override public ItemStack quickMoveStack(Player p, int idx) {{ return ItemStack.EMPTY; }}

    @Override public boolean stillValid(Player p) {{
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), p, ModBlocks.{block_const}.get());
    }}
}}
'''
        path = self.src_dir() / "menu" / f"{self.name}Menu.java"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(code, encoding='utf-8')
        return path

    def gen_screen(self):
        cfg = self.cfg
        theme = cfg['theme']
        slots_draws = "\n".join(
            f'        slot(g, x + {s["x"]} - 1, y + {s["y"]} - 1, {s.get("color", theme["accent"])});'
            for s in cfg['slots']
        )
        energy_draw = ""
        if 'energy_bar' in cfg:
            e = cfg['energy_bar']
            energy_draw = f'''
        // Energy bar
        int bx = x + {e["x"]}, by = y + {e["y"]}, bw = {e["width"]}, bh = {e["height"]};
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, {theme["primary"]});
        g.fill(bx, by, bx + bw, by + bh, 0xFF080012);
        float ef = menu.energyFrac();
        int efill = (int) (bh * ef);
        if (efill > 0) {{
            int top = by + bh - efill;
            for (int i = 0; i < efill; i++) {{
                float t = i / (float) Math.max(1, efill);
                int r = (int) (90 + 165 * t);
                int gC = (int) (10 + 50 * t);
                int b = (int) (160 + 95 * t);
                g.fill(bx, top + i, bx + bw, top + i + 1, 0xFF000000 | (r << 16) | (gC << 8) | b);
            }}
        }}'''
        progress_draw = ""
        if 'progress_arrow' in cfg:
            p = cfg['progress_arrow']
            progress_draw = f'''
        // Progress arrow
        int arrowX = x + {p["x"]}, arrowY = y + {p["y"]}, arrowW = {p["width"]}, arrowH = {p["height"]};
        g.fill(arrowX, arrowY, arrowX + arrowW, arrowY + arrowH, 0xFF1A0830);
        g.fill(arrowX, arrowY, arrowX + arrowW, arrowY + 1, {theme["primary"]});
        g.fill(arrowX, arrowY + arrowH - 1, arrowX + arrowW, arrowY + arrowH, {theme["primary"]});
        float pf = menu.progressFrac();
        int filled = (int) (arrowW * pf);
        if (filled > 0) {{
            for (int i = 0; i < filled; i++) {{
                float t = i / (float) Math.max(1, arrowW);
                int r = (int) (110 + 100 * t);
                int gC = (int) (40 + 80 * t);
                int b = (int) (180 + 50 * t);
                g.fill(arrowX + i, arrowY + 4, arrowX + i + 1, arrowY + arrowH - 4,
                        0xFF000000 | (r << 16) | (gC << 8) | b);
            }}
        }}
        for (int i = 0; i < 6; i++) {{
            int xx = arrowX + arrowW + i;
            g.fill(xx, arrowY + 6 - i, xx + 1, arrowY + arrowH - 6 + i, {theme["accent"]});
        }}'''

        code = f'''package {self.pkg}.client.screen;

import {self.pkg}.menu.{self.name}Menu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class {self.name}Screen extends AbstractContainerScreen<{self.name}Menu> {{

    public {self.name}Screen({self.name}Menu m, Inventory inv, Component title) {{
        super(m, inv, title);
        this.imageWidth = {cfg["image_width"]};
        this.imageHeight = {cfg["image_height"]};
        this.titleLabelY = {cfg.get("title_y", 6)};
        this.inventoryLabelY = {cfg.get("inventory_label_y", 72)};
    }}

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {{
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0E0212);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, {theme["background"]});
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, {theme["primary"]});
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + {cfg.get("inventory_label_y", 72)} - 4, {theme["panel"]});

{slots_draws}
{energy_draw}
{progress_draw}

        g.fill(x + 6, y + {cfg["player_inv"]["y"]} - 4, x + imageWidth - 6, y + imageHeight - 6, {theme["panel"]});
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + {cfg["player_inv"]["x"]} + col * 18 - 1, y + {cfg["player_inv"]["y"]} + row * 18 - 1, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            slot(g, x + {cfg["hotbar"]["x"]} + col * 18 - 1, y + {cfg["hotbar"]["y"]} - 1, {theme["accent"]});
    }}

    private void slot(GuiGraphics g, int x, int y, int hi) {{
        g.fill(x, y, x + 18, y + 1, 0xFF2A0D44);
        g.fill(x, y, x + 1, y + 18, 0xFF2A0D44);
        g.fill(x, y + 17, x + 18, y + 18, hi);
        g.fill(x + 17, y, x + 18, y + 18, hi);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1B0830);
    }}

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {{
        g.drawString(font,
                Component.translatable("container.{self.modid}.{self.snake}")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9966CC, false);
    }}

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {{
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }}
}}
'''
        path = self.src_dir() / "client/screen" / f"{self.name}Screen.java"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(code, encoding='utf-8')
        return path

    def gen_be(self):
        slots = self.cfg['slots']
        slot_consts = "\n".join(
            f'    public static final int SLOT_{s["name"]} = {s["index"]};'
            for s in slots
        )
        n_slots = len(slots)

        has_energy = 'energy_bar' in self.cfg
        has_progress = 'progress_arrow' in self.cfg
        kind = self.kind

        # FE imports + buffer
        energy_field = ""
        energy_caps = ""
        if has_energy:
            cap = self.cfg.get('fe_capacity', 100_000)
            rate = self.cfg.get('fe_per_tick', 1_000)
            energy_field = f'''
    public static final int FE_BUFFER = {cap};
    public static final int FE_PER_TICK = {rate};
    public final EnergyStorage energy = new EnergyStorage(FE_BUFFER, FE_PER_TICK * 4, 0);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();'''
            energy_caps = '''
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();'''

        # ContainerData (energy 4 slots + progress 2 slots if present)
        data_size = 0
        data_cases = []
        if has_energy:
            data_cases.append("                case 0, 1 -> energy.getEnergyStored();")
            data_cases.append("                case 2, 3 -> energy.getMaxEnergyStored();")
            data_size = 4
        if has_progress:
            data_cases.append(f"                case {data_size} -> progress;")
            data_cases.append(f"                case {data_size+1} -> processTicks;")
            data_size += 2

        progress_field = ""
        if has_progress:
            ticks = self.cfg.get('process_ticks', 200)
            progress_field = f'''
    public static final int BASE_PROCESS_TICKS = {ticks};
    private int progress = 0;
    private int processTicks = BASE_PROCESS_TICKS;'''

        data_code = '''
    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            int v = switch (i) {
''' + "\n".join(data_cases) + f'''
                default -> 0;
            }};
            return switch (i) {{
                case 0, 2 -> (v >> 16) & 0xFFFF;
                case 1, 3 -> v & 0xFFFF;
                default -> v;
            }};
        }}
        @Override public void set(int i, int v) {{}}
        @Override public int getCount() {{ return {data_size if data_size else 1}; }}
    }};''' if data_cases else ""

        # Tick template per kind
        tick_body = self._tick_body(kind, has_energy, has_progress)

        # Snippets pra evitar backslashes em f-string
        be_class_name = self.cfg["be_class"]
        onload_energy = "        lazyEnergy = LazyOptional.of(() -> energy);" if has_energy else ""
        invalidate_energy = "        lazyEnergy.invalidate();" if has_energy else ""
        save_energy = '        tag.put("energy", energy.serializeNBT());' if has_energy else ""
        save_progress = '        tag.putInt("progress", progress);' if has_progress else ""
        load_energy = '        if (tag.contains("energy")) energy.deserializeNBT(tag.get("energy"));' if has_energy else ""
        load_progress = '        progress = tag.getInt("progress");' if has_progress else ""

        code = f'''package {self.pkg}.block.entity;

import {self.pkg}.menu.{self.name}Menu;
import {self.pkg}.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Auto-gerado pelo Liberthia GUI Studio. Template: {kind}.
 */
public class {self.cfg["be_class"]} extends BlockEntity implements MenuProvider {{

{slot_consts}

{energy_field}
{progress_field}

    private final ItemStackHandler inventory = new ItemStackHandler({n_slots}) {{
        @Override protected void onContentsChanged(int slot) {{ setChanged(); }}
    }};
    private LazyOptional<IItemHandler> lazyItem = LazyOptional.empty();
{data_code}

    public {self.cfg["be_class"]}(BlockPos pos, BlockState state) {{
        super(ModBlockEntities.{self.const}.get(), pos, state);
    }}

    public IItemHandler getInventory() {{ return inventory; }}

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {{
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItem.cast();{energy_caps}
        return super.getCapability(cap, side);
    }}
    @Override public void onLoad() {{
        super.onLoad();
        lazyItem = LazyOptional.of(() -> inventory);
{onload_energy}
    }}
    @Override public void invalidateCaps() {{
        super.invalidateCaps();
        lazyItem.invalidate();
{invalidate_energy}
    }}

    public void drops() {{
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, c);
    }}

    @Override public @NotNull Component getDisplayName() {{
        return Component.translatable("container.{self.modid}.{self.snake}");
    }}
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {{
        return new {self.name}Menu(id, inv, this, this.data);
    }}

    public static void tick(Level level, BlockPos pos, BlockState state, {be_class_name} be) {{
        if (level.isClientSide) return;
{tick_body}
    }}

    @Override protected void saveAdditional(CompoundTag tag) {{
        tag.put("inv", inventory.serializeNBT());
{save_energy}
{save_progress}
        super.saveAdditional(tag);
    }}
    @Override public void load(CompoundTag tag) {{
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
{load_energy}
{load_progress}
    }}
}}
'''
        path = self.src_dir() / "block/entity" / f"{self.cfg['be_class']}.java"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(code, encoding='utf-8')
        return path

    def _tick_body(self, kind, has_energy, has_progress):
        recipes = self.cfg.get('recipes', [])

        if kind == "no_energy":
            return "        // Sem ticking — só armazena items"

        if kind == "transformation":
            return self._gen_transformation_tick(recipes, has_energy)

        if kind == "furnace":
            return self._gen_furnace_tick(recipes)

        if kind == "crafting":
            return self._gen_crafting_tick(recipes)

        if kind == "generator":
            return self._gen_generator_tick(recipes)

        return "        // template desconhecido"

    def _gen_transformation_tick(self, recipes, has_energy):
        """Tick que itera receitas: match inputs → drena FE → processa → produz output."""
        if not recipes:
            return '''        // Sem receitas configuradas — adicione na aba Lógica do editor.
        if (be.progress > 0) be.progress = 0;'''
        body = ['        // Itera receitas configuradas, processa a primeira que matcha.',
                '        int matchedRecipe = -1;']
        for i, r in enumerate(recipes):
            checks = []
            for inp in r.get('inputs', []):
                checks.append(f'be.inventory.getStackInSlot(SLOT_{inp["slot"]}).is(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("{inp["item"]}"))) && be.inventory.getStackInSlot(SLOT_{inp["slot"]}).getCount() >= {inp["count"]}')
            cond = " && ".join(checks) if checks else "true"
            body.append(f'        if ({cond}) {{ matchedRecipe = {i}; }}')

        body.append('        if (matchedRecipe < 0) { be.progress = 0; return; }')
        if has_energy:
            body.append('        if (be.energy.getEnergyStored() < FE_PER_TICK) return;')
            body.append('        be.energy.extractEnergy(FE_PER_TICK, false);')
        body.append('        be.progress++;')
        body.append('        be.setChanged();')

        # Gera o switch das receitas
        body.append('        if (be.progress >= be.processTicks) {')
        body.append('            switch (matchedRecipe) {')
        for i, r in enumerate(recipes):
            body.append(f'                case {i} -> {{')
            for inp in r.get('inputs', []):
                body.append(f'                    be.inventory.extractItem(SLOT_{inp["slot"]}, {inp["count"]}, false);')
            out = r.get('output', {})
            if out.get('item'):
                body.append(f'                    be.inventory.insertItem(SLOT_{out["slot"]}, new net.minecraft.world.item.ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("{out["item"]}")), {out.get("count", 1)}), false);')
            body.append('                }')
        body.append('            }')
        body.append('            be.progress = 0;')
        body.append('        }')
        return "\n".join(body)

    def _gen_furnace_tick(self, recipes):
        """Burn fuel time + smelting progress baseado em receitas."""
        body = ['        // Furnace tick — gerencia burn time + processa receitas.',
                '        ItemStack fuel = be.inventory.getStackInSlot(SLOT_FUEL);',
                '        ItemStack input = be.inventory.getStackInSlot(SLOT_INPUT);',
                '        // Burn time persistente em "feSpent" (cada bloco no NBT)',
                '        if (be.progress > 0 || (!fuel.isEmpty() && !input.isEmpty())) {',
                '            be.progress++;',
                '            be.setChanged();',
                '        }',
                '        if (be.progress >= be.processTicks && !input.isEmpty()) {']
        if recipes:
            body.append('            // Match input contra receitas')
            for i, r in enumerate(recipes):
                inp = r.get('inputs', [{}])[0] if r.get('inputs') else {}
                out = r.get('output', {})
                if inp.get('item') and out.get('item'):
                    body.append(f'            if (input.is(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("{inp["item"]}")))) {{')
                    body.append(f'                be.inventory.extractItem(SLOT_INPUT, {inp.get("count", 1)}, false);')
                    body.append('                if (!fuel.isEmpty()) be.inventory.extractItem(SLOT_FUEL, 1, false);')
                    body.append(f'                be.inventory.insertItem(SLOT_OUTPUT, new net.minecraft.world.item.ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("{out["item"]}")), {out.get("count", 1)}), false);')
                    body.append('                be.progress = 0;')
                    body.append('                return;')
                    body.append('            }')
        body.append('            be.progress = 0;')
        body.append('        }')
        return "\n".join(body)

    def _gen_crafting_tick(self, recipes):
        """Crafting INSTANTÂNEO — match em qualquer mudança de inventário."""
        if not recipes:
            return '        // Sem receitas — adicione na aba Lógica.'
        body = ['        // Crafting tick — verifica receitas e produz se output vazio.',
                '        ItemStack out = be.inventory.getStackInSlot(SLOT_OUTPUT);',
                '        if (!out.isEmpty()) return; // output ocupado, espera']
        for i, r in enumerate(recipes):
            checks = []
            for inp in r.get('inputs', []):
                checks.append(f'be.inventory.getStackInSlot(SLOT_{inp["slot"]}).is(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("{inp["item"]}"))) && be.inventory.getStackInSlot(SLOT_{inp["slot"]}).getCount() >= {inp["count"]}')
            cond = " && ".join(checks) if checks else "true"
            body.append(f'        if ({cond}) {{')
            for inp in r.get('inputs', []):
                body.append(f'            be.inventory.extractItem(SLOT_{inp["slot"]}, {inp["count"]}, false);')
            o = r.get('output', {})
            if o.get('item'):
                body.append(f'            be.inventory.insertItem(SLOT_{o["slot"]}, new net.minecraft.world.item.ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("{o["item"]}")), {o.get("count", 1)}), false);')
            body.append('            be.setChanged();')
            body.append('            return;')
            body.append('        }')
        return "\n".join(body)

    def _gen_generator_tick(self, recipes):
        """Generator — queima fuel produzindo FE/tick."""
        body = ['        // Generator tick — consome fuel e gera FE.',
                '        if (be.energy.getEnergyStored() >= be.energy.getMaxEnergyStored()) return;',
                '        ItemStack fuel = be.inventory.getStackInSlot(SLOT_FUEL);',
                '        if (fuel.isEmpty()) return;']
        if recipes:
            body.append('        // Cada receita: input fuel → FE total (count × ticks)')
            for r in recipes:
                inp = r.get('inputs', [{}])[0] if r.get('inputs') else {}
                if inp.get('item'):
                    fe_total = r.get('fe_per_op', 100000)
                    body.append(f'        if (fuel.is(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("{inp["item"]}")))) {{')
                    body.append(f'            be.inventory.extractItem(SLOT_FUEL, 1, false);')
                    body.append(f'            be.energy.receiveEnergy({fe_total}, false);')
                    body.append('            be.setChanged();')
                    body.append('            return;')
                    body.append('        }')
        else:
            body.append('        // Sem receitas — gera FE/tick base')
            body.append('        be.energy.receiveEnergy(FE_PER_TICK, false);')
            body.append('        be.setChanged();')
        return "\n".join(body)

    def gen_block(self):
        code = f'''package {self.pkg}.block;

import {self.pkg}.block.entity.{self.cfg["be_class"]};
import {self.pkg}.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class {self.name}Block extends BaseEntityBlock {{

    public {self.name}Block(Properties p) {{ super(p); }}

    @Override public RenderShape getRenderShape(BlockState s) {{ return RenderShape.MODEL; }}

    @Override
    public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player,
                                 InteractionHand h, BlockHitResult hit) {{
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider mp && player instanceof net.minecraft.server.level.ServerPlayer sp) {{
            net.minecraftforge.network.NetworkHooks.openScreen(sp, mp, pos);
        }}
        return InteractionResult.CONSUME;
    }}

    @Override
    public void onRemove(BlockState s, Level level, BlockPos pos, BlockState ns, boolean moved) {{
        if (!s.is(ns.getBlock()) && level.getBlockEntity(pos) instanceof {self.cfg["be_class"]} be) {{
            be.drops();
        }}
        super.onRemove(s, level, pos, ns, moved);
    }}

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) {{
        return new {self.cfg["be_class"]}(pos, s);
    }}

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {{
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.{self.const}.get(),
                {self.cfg["be_class"]}::tick);
    }}
}}
'''
        path = self.src_dir() / "block" / f"{self.name}Block.java"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(code, encoding='utf-8')
        return path

    def gen_blockstate(self):
        path = self.res_dir() / "blockstates" / f"{self.snake}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({
            "variants": {"": {"model": f"{self.modid}:block/{self.snake}"}}
        }, indent=2), encoding='utf-8')
        return path

    def gen_block_model(self):
        base = self.cfg.get('block_texture', 'minecraft:block/iron_block')
        path = self.res_dir() / "models/block" / f"{self.snake}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({
            "parent": "block/cube_all",
            "textures": {"all": base}
        }, indent=2), encoding='utf-8')
        return path

    def gen_item_model(self):
        path = self.res_dir() / "models/item" / f"{self.snake}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({
            "parent": f"{self.modid}:block/{self.snake}"
        }, indent=2), encoding='utf-8')
        return path

    def gen_registration_lines(self):
        title_pt = self.cfg.get('title_pt', self.name)
        title_en = self.cfg.get('title_en', self.name)
        return {
            "ModBlocks.java": f'''
    public static final RegistryObject<Block> {self.const} = BLOCKS.register("{self.snake}",
            () -> new {self.pkg}.block.{self.name}Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)));''',
            "ModItems.java": f'''
    public static final RegistryObject<Item> {self.const}_ITEM = ITEMS.register("{self.snake}",
            () -> new BlockItem(ModBlocks.{self.const}.get(), new Item.Properties()));''',
            "ModBlockEntities.java": f'''
    public static final RegistryObject<BlockEntityType<{self.cfg["be_class"]}>> {self.const} =
            BLOCK_ENTITIES.register("{self.snake}",
                    () -> BlockEntityType.Builder.of({self.cfg["be_class"]}::new,
                            ModBlocks.{self.const}.get()).build(null));''',
            "ModMenuTypes.java": f'''
    public static final RegistryObject<MenuType<{self.name}Menu>> {self.const} = registerMenuType(
            "{self.snake}", {self.name}Menu::new);''',
            "ClientModEvents.java": f'''
            MenuScreens.register(ModMenuTypes.{self.const}.get(),
                    {self.pkg}.client.screen.{self.name}Screen::new);''',
            "ModCreativeTabs.java": f'                        output.accept(ModItems.{self.const}_ITEM.get());',
            "pt_br.json": f'''  "block.{self.modid}.{self.snake}": "{title_pt}",
  "item.{self.modid}.{self.snake}": "{title_pt}",
  "container.{self.modid}.{self.snake}": "{title_pt}",''',
            "en_us.json": f'''  "block.{self.modid}.{self.snake}": "{title_en}",
  "item.{self.modid}.{self.snake}": "{title_en}",
  "container.{self.modid}.{self.snake}": "{title_en}",''',
        }


# -------------------- Auto-patcher --------------------
class AutoPatcher:
    """Insere as linhas geradas automaticamente nos arquivos do mod."""

    def __init__(self, mod_root, pkg=DEFAULT_MOD_PKG, modid='liberthia'):
        self.root = Path(mod_root)
        pkg_path = pkg.replace('.', '/')
        self.registry_dir = self.root / "src/main/java" / pkg_path / "registry"
        self.client_dir = self.root / "src/main/java" / pkg_path / "client"
        self.lang_dir = self.root / "src/main/resources/assets" / modid / "lang"

    def patch(self, registrations):
        results = {}
        results["ModBlocks"] = self._insert_in_registry("ModBlocks.java", registrations["ModBlocks.java"])
        results["ModItems"] = self._insert_in_registry("ModItems.java", registrations["ModItems.java"])
        results["ModBlockEntities"] = self._insert_in_registry("ModBlockEntities.java", registrations["ModBlockEntities.java"])
        results["ModMenuTypes"] = self._insert_before_method("ModMenuTypes.java", "private static <T extends",
                                                              registrations["ModMenuTypes.java"])
        results["ClientModEvents"] = self._insert_in_client_setup(registrations["ClientModEvents.java"])
        results["ModCreativeTabs"] = self._insert_in_creative_tab(registrations["ModCreativeTabs.java"])
        results["pt_br.json"] = self._patch_lang("pt_br.json", registrations["pt_br.json"])
        results["en_us.json"] = self._patch_lang("en_us.json", registrations["en_us.json"])
        return results

    def _insert_in_registry(self, fname, snippet):
        path = self.registry_dir / fname
        if not path.exists(): return f"NOT FOUND: {path}"
        text = path.read_text(encoding='utf-8')
        if snippet.strip() in text:
            return "JÁ EXISTE"
        # Insere antes da linha "private ...() {}" ou no final da classe
        lines = text.splitlines(keepends=True)
        for i in range(len(lines) - 1, -1, -1):
            if lines[i].strip().startswith("private ") and lines[i].strip().endswith("() {}"):
                lines.insert(i, snippet + "\n")
                path.write_text("".join(lines), encoding='utf-8')
                return f"OK ({path.name})"
        # fallback: insere antes do último '}' do arquivo
        idx = text.rfind('}')
        if idx > 0:
            new_text = text[:idx] + snippet + "\n" + text[idx:]
            path.write_text(new_text, encoding='utf-8')
            return f"OK ({path.name}, fim)"
        return "FALHOU"

    def _insert_before_method(self, fname, marker, snippet):
        path = self.registry_dir / fname
        if not path.exists(): return f"NOT FOUND: {path}"
        text = path.read_text(encoding='utf-8')
        if snippet.strip() in text: return "JÁ EXISTE"
        idx = text.find(marker)
        if idx < 0: return self._insert_in_registry(fname, snippet)
        new_text = text[:idx] + snippet + "\n    " + text[idx:]
        path.write_text(new_text, encoding='utf-8')
        return f"OK ({path.name})"

    def _insert_in_client_setup(self, snippet):
        # Procura por ClientModEvents.java
        for p in self.client_dir.rglob("ClientModEvents.java"):
            text = p.read_text(encoding='utf-8')
            if snippet.strip() in text: return "JÁ EXISTE"
            # Insere depois do último MenuScreens.register
            matches = list(re.finditer(r'MenuScreens\.register\([^;]+;', text, re.DOTALL))
            if matches:
                last = matches[-1]
                idx = last.end()
                new_text = text[:idx] + "\n" + snippet + text[idx:]
                p.write_text(new_text, encoding='utf-8')
                return f"OK ({p.name})"
        return "ClientModEvents não encontrado"

    def _insert_in_creative_tab(self, snippet):
        path = self.registry_dir / "ModCreativeTabs.java"
        if not path.exists(): return "NOT FOUND"
        text = path.read_text(encoding='utf-8')
        if snippet.strip() in text: return "JÁ EXISTE"
        # Insere antes do .build()) do tab principal
        idx = text.rfind(".build())")
        if idx < 0: return "Padrão não achado"
        # Volta até a }) anterior
        end_brace = text.rfind("})", 0, idx)
        if end_brace < 0: return "FALHOU"
        new_text = text[:end_brace] + snippet + "\n                    " + text[end_brace:]
        path.write_text(new_text, encoding='utf-8')
        return f"OK"

    def _patch_lang(self, fname, snippet):
        path = self.lang_dir / fname
        if not path.exists(): return "NOT FOUND"
        text = path.read_text(encoding='utf-8')
        # Lang JSON: precisa ser keys válidas. Insere antes do último '}'
        snippet_stripped = snippet.strip()
        if snippet_stripped in text: return "JÁ EXISTE"
        # encontra o último }
        idx = text.rfind('}')
        if idx < 0: return "FALHOU"
        # checa se a linha anterior tem vírgula, senão adiciona
        before = text[:idx].rstrip()
        if not before.endswith(','):
            before = before + ','
        new_text = before + "\n" + snippet + "\n}\n"
        path.write_text(new_text, encoding='utf-8')
        return f"OK ({path.name})"


# -------------------- App --------------------
class GuiStudio(tk.Tk):
    def __init__(self, initial_config=None):
        super().__init__()
        self.title("🎨 Liberthia GUI/Block Studio")
        self.geometry("1400x820")
        self.config_data = initial_config or self.default_config()
        self.mod_root = self.config_data.get('_mod_root') or self.guess_mod_root()
        self.selected_kind = None
        self.selected_index = None
        self.drag_data = None
        self.canvas_items = {}

        self.configure(bg="#0d0d12")
        self.build_ui()
        self.redraw_canvas()

    def guess_mod_root(self):
        # Se script tá em <mod>/tools/, mod root é parent
        return str(Path(__file__).parent.parent)

    def default_config(self):
        return {
            "name": "MyMachine", "snake": "my_machine",
            "be_class": "MyMachineBlockEntity", "block_const": "MY_MACHINE",
            "title_pt": "Minha Máquina", "title_en": "My Machine",
            "modid": "liberthia",
            "package": DEFAULT_MOD_PKG,
            "kind": "transformation",
            "block_texture": "minecraft:block/iron_block",
            "image_width": 176, "image_height": 166,
            "data_slots": 6,
            "fe_capacity": 100000, "fe_per_tick": 1000, "process_ticks": 200,
            "theme": {
                "primary": "0xFF6B2A8C",
                "background": "0xFF120420",
                "panel": "0xFF1B0830",
                "accent": "0xFFAA40E8"
            },
            "slots": [
                {"name": "INPUT", "index": 0, "x": 44, "y": 30, "color": "0xFF553090"},
                {"name": "OUTPUT", "index": 1, "x": 122, "y": 40, "color": "0xFFAA40E8"}
            ],
            "energy_bar": {
                "x": 8, "y": 18, "width": 8, "height": 50,
                "energy_data_hi": 0, "energy_data_lo": 1,
                "max_data_hi": 2, "max_data_lo": 3
            },
            "progress_arrow": {
                "x": 70, "y": 40, "width": 48, "height": 18,
                "progress_data": 4, "progress_max_data": 5
            },
            "player_inv": {"x": 8, "y": 84},
            "hotbar": {"x": 8, "y": 142},
            "title_y": 6, "inventory_label_y": 72,
            "recipes": []
        }

    def _make_scrollable(self, parent, width=320):
        """Cria um frame com scrollbar vertical. Retorna {outer, inner}.
        outer vai dentro do paned/parent, inner é onde adiciona widgets."""
        outer = tk.Frame(parent, bg="#1a1a26")
        canvas = tk.Canvas(outer, bg="#1a1a26", highlightthickness=0,
                           width=width, borderwidth=0)
        scrollbar = ttk.Scrollbar(outer, orient="vertical", command=canvas.yview)
        inner = tk.Frame(canvas, bg="#1a1a26", padx=8, pady=8)

        inner_window = canvas.create_window((0, 0), window=inner, anchor="nw")

        def _on_inner_config(_=None):
            canvas.configure(scrollregion=canvas.bbox("all"))
            canvas.itemconfig(inner_window, width=canvas.winfo_width())
        def _on_canvas_config(e):
            canvas.itemconfig(inner_window, width=e.width)

        inner.bind("<Configure>", _on_inner_config)
        canvas.bind("<Configure>", _on_canvas_config)
        canvas.configure(yscrollcommand=scrollbar.set)

        def _on_mousewheel(e):
            try: canvas.yview_scroll(int(-1 * (e.delta / 120)), "units")
            except Exception: pass
        canvas.bind("<Enter>", lambda _: canvas.bind_all("<MouseWheel>", _on_mousewheel))
        canvas.bind("<Leave>", lambda _: canvas.unbind_all("<MouseWheel>"))

        canvas.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")
        return {"outer": outer, "inner": inner, "canvas": canvas}

    def build_ui(self):
        # Top bar — mod folder
        topbar = tk.Frame(self, bg="#1a1a26", padx=10, pady=6)
        topbar.pack(fill=tk.X)
        tk.Label(topbar, text="📁 Mod:", bg="#1a1a26", fg="#aaf",
                 font=("Arial", 10, "bold")).pack(side=tk.LEFT)
        self.mod_root_var = tk.StringVar(value=self.mod_root)
        tk.Entry(topbar, textvariable=self.mod_root_var, bg="#0a0a0a", fg="#fff",
                 insertbackground="#fff", relief=tk.FLAT, width=70).pack(side=tk.LEFT, padx=6)
        tk.Button(topbar, text="...", command=self.pick_mod_root,
                  bg="#444", fg="white", relief=tk.FLAT, padx=8).pack(side=tk.LEFT)
        tk.Label(topbar, text="modid:", bg="#1a1a26", fg="#aaf",
                 font=("Arial", 10)).pack(side=tk.LEFT, padx=(20, 4))
        self.modid_var = tk.StringVar(value=self.config_data.get('modid', 'liberthia'))
        tk.Entry(topbar, textvariable=self.modid_var, bg="#0a0a0a", fg="#fff",
                 insertbackground="#fff", relief=tk.FLAT, width=14).pack(side=tk.LEFT)
        self.modid_var.trace_add('write', lambda *a: self.update_modid())

        # Main 3-pane
        paned = tk.PanedWindow(self, orient=tk.HORIZONTAL, sashwidth=4, bg="#222")
        paned.pack(fill=tk.BOTH, expand=True)

        left = self._make_scrollable(paned, width=320)
        paned.add(left['outer'], minsize=300)
        self.build_form(left['inner'])

        center = tk.Frame(paned, bg="#0a0a0f")
        paned.add(center, minsize=400)
        self.canvas = tk.Canvas(center, bg="#000", highlightthickness=0)
        self.canvas.pack(fill=tk.BOTH, expand=True, padx=20, pady=20)
        self.canvas.bind("<Button-1>", self.on_canvas_click)
        self.canvas.bind("<B1-Motion>", self.on_canvas_drag)
        self.canvas.bind("<ButtonRelease-1>", self.on_canvas_release)
        self.canvas.bind("<Button-3>", self.on_canvas_rclick)

        right = self._make_scrollable(paned, width=300)
        paned.add(right['outer'], minsize=280)
        self.build_props(right['inner'])

        # Bottom toolbar
        bottom = tk.Frame(self, bg="#262630", padx=10, pady=6)
        bottom.pack(fill=tk.X, side=tk.BOTTOM)
        tk.Button(bottom, text="+ Slot", command=self.add_slot,
                  bg="#553090", fg="white", relief=tk.FLAT, padx=10, pady=4).pack(side=tk.LEFT, padx=3)
        tk.Button(bottom, text="⚡ Energy", command=self.toggle_energy,
                  bg="#3B1A5C", fg="white", relief=tk.FLAT, padx=10, pady=4).pack(side=tk.LEFT, padx=3)
        tk.Button(bottom, text="➡ Progress", command=self.toggle_progress,
                  bg="#AA40E8", fg="white", relief=tk.FLAT, padx=10, pady=4).pack(side=tk.LEFT, padx=3)
        tk.Button(bottom, text="📂 Load", command=self.load_json,
                  bg="#444", fg="white", relief=tk.FLAT, padx=10, pady=4).pack(side=tk.LEFT, padx=20)
        tk.Button(bottom, text="💾 Save", command=self.save_json,
                  bg="#444", fg="white", relief=tk.FLAT, padx=10, pady=4).pack(side=tk.LEFT, padx=3)
        tk.Button(bottom, text="🔍 Preview Code", command=self.preview_code,
                  bg="#268", fg="white", relief=tk.FLAT, padx=10, pady=4).pack(side=tk.LEFT, padx=20)
        tk.Button(bottom, text="⚙ Generate + AutoPatch", command=self.generate_and_patch,
                  bg="#0a8", fg="white", relief=tk.FLAT, padx=14, pady=6,
                  font=("Arial", 10, "bold")).pack(side=tk.RIGHT, padx=3)

    def update_modid(self):
        self.config_data['modid'] = self.modid_var.get()

    def pick_mod_root(self):
        d = filedialog.askdirectory(initialdir=self.mod_root_var.get(),
                                     title="Selecione a pasta raíz do mod")
        if d:
            self.mod_root_var.set(d)
            self.mod_root = d
            self.config_data['_mod_root'] = d

    def build_form(self, parent):
        tk.Label(parent, text="🧩 Identidade", bg="#1a1a26", fg="#aaf",
                 font=("Arial", 12, "bold")).pack(anchor='w')

        self.form_vars = {}
        for key, label, kind in [
            ("name", "Nome (PascalCase)", str),
            ("snake", "snake_case", str),
            ("be_class", "BE class", str),
            ("block_const", "Const ModBlocks", str),
            ("title_pt", "Título PT", str),
            ("title_en", "Título EN", str),
        ]:
            row = tk.Frame(parent, bg="#1a1a26")
            row.pack(fill=tk.X, pady=2)
            tk.Label(row, text=label, bg="#1a1a26", fg="#ddd",
                     font=("Arial", 9), anchor='w', width=18).pack(side=tk.LEFT)
            v = tk.StringVar(value=str(self.config_data.get(key, "")))
            self.form_vars[key] = (v, kind)
            tk.Entry(row, textvariable=v, bg="#0a0a0a", fg="#fff",
                     insertbackground="#fff", relief=tk.FLAT, width=18).pack(side=tk.RIGHT)
            v.trace_add('write', lambda *a, k=key: self.on_form_change(k))
        self.form_vars['name'][0].trace_add('write', lambda *a: self.auto_derive())

        # Kind selector
        tk.Label(parent, text="", bg="#1a1a26").pack()
        tk.Label(parent, text="🔧 Tipo de Máquina", bg="#1a1a26", fg="#aaf",
                 font=("Arial", 11, "bold")).pack(anchor='w')
        self.kind_var = tk.StringVar(value=self.config_data.get('kind', 'transformation'))
        for key, (label, desc) in TEMPLATE_KINDS.items():
            row = tk.Frame(parent, bg="#1a1a26")
            row.pack(fill=tk.X, pady=1)
            rb = tk.Radiobutton(row, text=f"{label}", variable=self.kind_var, value=key,
                                bg="#1a1a26", fg="#fff", selectcolor="#3a2a4a",
                                activebackground="#1a1a26", activeforeground="#aaf",
                                font=("Arial", 9, "bold"), command=self.on_kind_change)
            rb.pack(side=tk.LEFT)
            tk.Label(row, text=f" — {desc}", bg="#1a1a26", fg="#888",
                     font=("Arial", 8)).pack(side=tk.LEFT)

        # Block texture
        tk.Label(parent, text="", bg="#1a1a26").pack()
        tk.Label(parent, text="🧱 Aparência do Bloco", bg="#1a1a26", fg="#aaf",
                 font=("Arial", 11, "bold")).pack(anchor='w')
        row = tk.Frame(parent, bg="#1a1a26")
        row.pack(fill=tk.X, pady=2)
        tk.Label(row, text="Textura base", bg="#1a1a26", fg="#ddd",
                 font=("Arial", 9), anchor='w', width=14).pack(side=tk.LEFT)
        self.block_tex_var = tk.StringVar(value=self.config_data.get('block_texture', 'minecraft:block/iron_block'))
        tk.Entry(row, textvariable=self.block_tex_var, bg="#0a0a0a", fg="#fff",
                 insertbackground="#fff", relief=tk.FLAT, width=22).pack(side=tk.RIGHT)
        self.block_tex_var.trace_add('write', lambda *a: self.config_data.update({'block_texture': self.block_tex_var.get()}))

        tk.Label(parent, text="(ex: minecraft:block/diamond_block, minecraft:block/amethyst_block)",
                 bg="#1a1a26", fg="#666", font=("Arial", 8)).pack(anchor='w')

        # Numerics
        tk.Label(parent, text="", bg="#1a1a26").pack()
        tk.Label(parent, text="🔢 Dimensões & Energia", bg="#1a1a26", fg="#aaf",
                 font=("Arial", 11, "bold")).pack(anchor='w')
        for key, label, kind in [
            ("image_width", "GUI largura", int),
            ("image_height", "GUI altura", int),
            ("data_slots", "ContainerData size", int),
            ("fe_capacity", "FE Capacity", int),
            ("fe_per_tick", "FE / tick", int),
            ("process_ticks", "Ticks por processo", int),
        ]:
            row = tk.Frame(parent, bg="#1a1a26")
            row.pack(fill=tk.X, pady=1)
            tk.Label(row, text=label, bg="#1a1a26", fg="#ddd",
                     font=("Arial", 9), anchor='w', width=18).pack(side=tk.LEFT)
            v = tk.StringVar(value=str(self.config_data.get(key, "")))
            self.form_vars[key] = (v, kind)
            tk.Entry(row, textvariable=v, bg="#0a0a0a", fg="#fff",
                     insertbackground="#fff", relief=tk.FLAT, width=18).pack(side=tk.RIGHT)
            v.trace_add('write', lambda *a, k=key: self.on_form_change(k))

        # ==================== Recipe Editor (Programação Visual) ====================
        tk.Label(parent, text="", bg="#1a1a26").pack()
        recipe_header = tk.Frame(parent, bg="#1a1a26")
        recipe_header.pack(fill=tk.X)
        tk.Label(recipe_header, text="🔬 Lógica / Receitas", bg="#1a1a26", fg="#aaf",
                 font=("Arial", 11, "bold")).pack(side=tk.LEFT)
        tk.Button(recipe_header, text="+ Add", command=self.add_recipe,
                  bg="#0a8", fg="white", relief=tk.FLAT, padx=8,
                  font=("Arial", 8, "bold")).pack(side=tk.RIGHT)

        self.recipe_list_frame = tk.Frame(parent, bg="#1a1a26")
        self.recipe_list_frame.pack(fill=tk.X, pady=4)
        self.refresh_recipe_list()

        # Theme colors
        tk.Label(parent, text="", bg="#1a1a26").pack()
        tk.Label(parent, text="🎨 Tema (RGBA)", bg="#1a1a26", fg="#aaf",
                 font=("Arial", 11, "bold")).pack(anchor='w')
        for key, label in [("primary", "Primary"), ("background", "Background"),
                           ("panel", "Panel"), ("accent", "Accent")]:
            row = tk.Frame(parent, bg="#1a1a26")
            row.pack(fill=tk.X, pady=2)
            tk.Label(row, text=label, bg="#1a1a26", fg="#ddd",
                     font=("Arial", 9), anchor='w', width=11).pack(side=tk.LEFT)
            sw = tk.Frame(row, bg=hex_to_rgb(self.config_data['theme'][key]),
                          width=30, height=20, cursor="hand2", relief=tk.RAISED, bd=1)
            sw.pack(side=tk.LEFT, padx=4)
            sw.bind("<Button-1>", lambda e, k=key, s=sw: self.pick_theme_color(k, s))
            lbl = tk.Label(row, text=self.config_data['theme'][key], bg="#1a1a26", fg="#888",
                           font=("Courier", 8))
            lbl.pack(side=tk.LEFT)

    # ==================== Recipe System ====================
    def refresh_recipe_list(self):
        for w in self.recipe_list_frame.winfo_children():
            w.destroy()
        recipes = self.config_data.get('recipes', [])
        if not recipes:
            tk.Label(self.recipe_list_frame, text="Nenhuma receita.\nClick + Add",
                     bg="#1a1a26", fg="#666", font=("Arial", 8),
                     justify=tk.LEFT).pack(anchor='w', padx=4)
            return
        for i, r in enumerate(recipes):
            row = tk.Frame(self.recipe_list_frame, bg="#22223a", padx=4, pady=2)
            row.pack(fill=tk.X, pady=1)
            ins = ", ".join(f"{inp.get('count', 1)}× {inp.get('item', '?').split(':')[-1][:8]}"
                            for inp in r.get('inputs', []))
            out = r.get('output', {})
            out_str = f"{out.get('count', 1)}× {out.get('item', '?').split(':')[-1][:10]}" if out.get('item') else "?"
            txt = f"#{i}: {ins} → {out_str}"
            if 'ticks' in r: txt += f" ({r['ticks']}t"
            if 'fe_per_op' in r: txt += f" {r['fe_per_op']}FE)"
            elif 'ticks' in r: txt += ")"
            tk.Label(row, text=txt, bg="#22223a", fg="#dfd",
                     font=("Arial", 8), anchor='w').pack(side=tk.LEFT, fill=tk.X, expand=True)
            tk.Button(row, text="✏", command=lambda idx=i: self.edit_recipe(idx),
                      bg="#444", fg="white", relief=tk.FLAT, font=("Arial", 7),
                      padx=4).pack(side=tk.RIGHT, padx=1)
            tk.Button(row, text="✕", command=lambda idx=i: self.delete_recipe(idx),
                      bg="#822", fg="white", relief=tk.FLAT, font=("Arial", 7),
                      padx=4).pack(side=tk.RIGHT, padx=1)

    def add_recipe(self):
        r = {
            "inputs": [{"slot": "INPUT", "item": "minecraft:diamond", "count": 1}],
            "output": {"slot": "OUTPUT", "item": "minecraft:diamond_block", "count": 1},
            "ticks": 200, "fe_per_op": 50000
        }
        self.config_data.setdefault('recipes', []).append(r)
        self.edit_recipe(len(self.config_data['recipes']) - 1)

    def edit_recipe(self, idx):
        RecipeDialog(self, idx).wait_window()
        self.refresh_recipe_list()

    def delete_recipe(self, idx):
        if messagebox.askyesno("Deletar", f"Deletar receita #{idx}?"):
            self.config_data['recipes'].pop(idx)
            self.refresh_recipe_list()

    def slot_names(self):
        return [s['name'] for s in self.config_data.get('slots', [])] or ["INPUT", "OUTPUT"]

    def build_props(self, parent):
        self.props_frame = parent
        tk.Label(parent, text="📋 Propriedades", bg="#1a1a26", fg="#aaf",
                 font=("Arial", 12, "bold")).pack(anchor='w')
        self.props_content = tk.Frame(parent, bg="#1a1a26")
        self.props_content.pack(fill=tk.BOTH, expand=True, pady=8)
        tk.Label(self.props_content, text="Selecione um elemento\nno canvas pra editar",
                 bg="#1a1a26", fg="#888", font=("Arial", 9), justify=tk.CENTER).pack(pady=20)

        tk.Label(parent, text="", bg="#1a1a26").pack()
        tk.Label(parent, text="ℹ Atalhos", bg="#1a1a26", fg="#fa0",
                 font=("Arial", 10, "bold")).pack(anchor='w')
        tk.Label(parent, text="• Click & arraste pra mover\n"
                              "• Right-click pra deletar\n"
                              "• Ctrl+S salva JSON\n"
                              "• Ctrl+G gera+aplica patches",
                 bg="#1a1a26", fg="#aaa", font=("Arial", 8),
                 justify=tk.LEFT).pack(anchor='w')

    def auto_derive(self):
        name = self.form_vars['name'][0].get()
        if not name: return
        snake = to_snake(name)
        for k, v in [('snake', snake), ('be_class', f"{name}BlockEntity"),
                     ('block_const', snake.upper())]:
            var, _ = self.form_vars[k]
            if var.get() != v: var.set(v)

    def on_form_change(self, key):
        v, kind = self.form_vars[key]
        try:
            self.config_data[key] = kind(v.get()) if kind == int else v.get()
            if key in ('image_width', 'image_height'):
                self.redraw_canvas()
        except ValueError:
            pass

    def on_kind_change(self):
        self.config_data['kind'] = self.kind_var.get()

    def pick_theme_color(self, key, swatch):
        cur = hex_to_rgb(self.config_data['theme'][key])
        result = colorchooser.askcolor(initialcolor=cur, title=f"Escolher {key}")
        if result and result[1]:
            self.config_data['theme'][key] = rgb_to_argb(result[1])
            swatch.config(bg=result[1])
            for child in swatch.master.winfo_children():
                if isinstance(child, tk.Label) and child['text'].startswith('0x'):
                    child.config(text=self.config_data['theme'][key])
            self.redraw_canvas()

    # -------------------- Canvas (mesma lógica anterior) --------------------
    def redraw_canvas(self):
        self.canvas.delete("all")
        self.canvas_items.clear()
        cfg = self.config_data
        w, h = cfg['image_width'] * SCALE, cfg['image_height'] * SCALE
        self.canvas.create_rectangle(0, 0, w, h, fill=hex_to_rgb(cfg['theme']['background']), outline="")
        self.canvas.create_rectangle(0, 0, w, 4, fill=hex_to_rgb(cfg['theme']['primary']), outline="")
        self.canvas.create_rectangle(0, h - 4, w, h, fill=hex_to_rgb(cfg['theme']['primary']), outline="")
        self.canvas.create_rectangle(0, 0, 4, h, fill=hex_to_rgb(cfg['theme']['primary']), outline="")
        self.canvas.create_rectangle(w - 4, 0, w, h, fill=hex_to_rgb(cfg['theme']['primary']), outline="")
        self.canvas.create_text(w / 2, cfg.get('title_y', 6) * SCALE + 6,
                                text=cfg.get('title_pt', '?'), fill="#ffe", font=("Arial", 10, "bold"))

        if 'energy_bar' in cfg:
            e = cfg['energy_bar']
            x1, y1 = e['x'] * SCALE, e['y'] * SCALE
            x2, y2 = (e['x'] + e['width']) * SCALE, (e['y'] + e['height']) * SCALE
            r = self.canvas.create_rectangle(x1 - 2, y1 - 2, x2 + 2, y2 + 2,
                                             fill=hex_to_rgb(cfg['theme']['primary']), outline="")
            inner = self.canvas.create_rectangle(x1, y1, x2, y2, fill="#080012", outline="")
            self.canvas.create_rectangle(x1, y2 - (y2 - y1) * 0.6, x2, y2, fill="#aa40e8", outline="")
            self.canvas_items[r] = ('energy', None)
            self.canvas_items[inner] = ('energy', None)

        if 'progress_arrow' in cfg:
            p = cfg['progress_arrow']
            x1, y1 = p['x'] * SCALE, p['y'] * SCALE
            x2, y2 = (p['x'] + p['width']) * SCALE, (p['y'] + p['height']) * SCALE
            r = self.canvas.create_rectangle(x1, y1, x2, y2, fill="#1a0830",
                                             outline=hex_to_rgb(cfg['theme']['primary']))
            self.canvas.create_rectangle(x1, y1 + 8, x1 + (x2 - x1) * 0.5, y2 - 8,
                                         fill=hex_to_rgb(cfg['theme']['accent']), outline="")
            self.canvas.create_polygon(x2, y1 + 4, x2 + 12, (y1 + y2) / 2, x2, y2 - 4,
                                       fill=hex_to_rgb(cfg['theme']['accent']), outline="")
            self.canvas_items[r] = ('progress', None)

        for i, s in enumerate(cfg['slots']):
            x1, y1 = s['x'] * SCALE, s['y'] * SCALE
            x2, y2 = x1 + 18 * SCALE, y1 + 18 * SCALE
            r = self.canvas.create_rectangle(x1, y1, x2, y2,
                                             fill=hex_to_rgb(s.get('color', '0xFF553090')),
                                             outline="#fff", width=2)
            t = self.canvas.create_text((x1 + x2) / 2, (y1 + y2) / 2,
                                        text=s['name'], fill="#fff", font=("Arial", 7, "bold"))
            self.canvas_items[r] = ('slot', i)
            self.canvas_items[t] = ('slot', i)

        pi = cfg['player_inv']
        for row in range(3):
            for col in range(9):
                x1 = (pi['x'] + col * 18) * SCALE
                y1 = (pi['y'] + row * 18) * SCALE
                self.canvas.create_rectangle(x1, y1, x1 + 18 * SCALE, y1 + 18 * SCALE,
                                             outline="#55208a", fill="#1b0830")
        x1, y1 = pi['x'] * SCALE, pi['y'] * SCALE
        x2 = x1 + 18 * SCALE * 9
        y2 = y1 + 18 * SCALE * 3
        r = self.canvas.create_rectangle(x1, y1, x2, y2, outline="#5599ff", width=1, dash=(2, 2))
        self.canvas_items[r] = ('pinv', None)

        hb = cfg['hotbar']
        for col in range(9):
            x1 = (hb['x'] + col * 18) * SCALE
            y1 = hb['y'] * SCALE
            self.canvas.create_rectangle(x1, y1, x1 + 18 * SCALE, y1 + 18 * SCALE,
                                         outline=hex_to_rgb(cfg['theme']['accent']), fill="#1b0830", width=2)
        x1, y1 = hb['x'] * SCALE, hb['y'] * SCALE
        x2 = x1 + 18 * SCALE * 9
        y2 = y1 + 18 * SCALE
        r = self.canvas.create_rectangle(x1, y1, x2, y2, outline="#5599ff", width=1, dash=(2, 2))
        self.canvas_items[r] = ('hotbar', None)

    def on_canvas_click(self, e):
        item = self.canvas.find_closest(e.x, e.y)
        if item and item[0] in self.canvas_items:
            kind, idx = self.canvas_items[item[0]]
            self.selected_kind = kind
            self.selected_index = idx
            self.drag_data = {"x": e.x, "y": e.y}
            self.show_props()
            return
        self.selected_kind = None
        self.selected_index = None
        self.show_props()

    def on_canvas_drag(self, e):
        if not self.drag_data or not self.selected_kind: return
        dx = (e.x - self.drag_data["x"]) // SCALE
        dy = (e.y - self.drag_data["y"]) // SCALE
        if dx == 0 and dy == 0: return
        cfg = self.config_data
        kind = self.selected_kind
        if kind == 'slot':
            s = cfg['slots'][self.selected_index]
            s['x'] = max(0, min(cfg['image_width'] - 18, s['x'] + dx))
            s['y'] = max(0, min(cfg['image_height'] - 18, s['y'] + dy))
        elif kind == 'energy':
            e_ = cfg['energy_bar']
            e_['x'] = max(0, min(cfg['image_width'] - e_['width'], e_['x'] + dx))
            e_['y'] = max(0, min(cfg['image_height'] - e_['height'], e_['y'] + dy))
        elif kind == 'progress':
            p = cfg['progress_arrow']
            p['x'] = max(0, min(cfg['image_width'] - p['width'], p['x'] + dx))
            p['y'] = max(0, min(cfg['image_height'] - p['height'], p['y'] + dy))
        elif kind == 'pinv':
            pi = cfg['player_inv']
            pi['x'] = max(0, min(cfg['image_width'] - 18 * 9, pi['x'] + dx))
            pi['y'] = max(0, min(cfg['image_height'] - 18 * 3, pi['y'] + dy))
        elif kind == 'hotbar':
            hb = cfg['hotbar']
            hb['x'] = max(0, min(cfg['image_width'] - 18 * 9, hb['x'] + dx))
            hb['y'] = max(0, min(cfg['image_height'] - 18, hb['y'] + dy))
        self.drag_data["x"] = e.x
        self.drag_data["y"] = e.y
        self.redraw_canvas()
        self.show_props()

    def on_canvas_release(self, e): self.drag_data = None

    def on_canvas_rclick(self, e):
        item = self.canvas.find_closest(e.x, e.y)
        if not item or item[0] not in self.canvas_items: return
        kind, idx = self.canvas_items[item[0]]
        if kind == 'slot':
            if messagebox.askyesno("Deletar", f"Deletar slot '{self.config_data['slots'][idx]['name']}'?"):
                self.config_data['slots'].pop(idx)
                self.redraw_canvas(); self.show_props()
        elif kind == 'energy':
            if messagebox.askyesno("Deletar", "Remover barra de energia?"):
                del self.config_data['energy_bar']
                self.redraw_canvas()
        elif kind == 'progress':
            if messagebox.askyesno("Deletar", "Remover seta de progresso?"):
                del self.config_data['progress_arrow']
                self.redraw_canvas()

    def show_props(self):
        for w in self.props_content.winfo_children(): w.destroy()
        kind = self.selected_kind
        if kind == 'slot': self._props_slot()
        elif kind == 'energy': self._props_simple('energy_bar', "⚡ Energy Bar",
                                                  ['x', 'y', 'width', 'height',
                                                   'energy_data_hi', 'energy_data_lo',
                                                   'max_data_hi', 'max_data_lo'])
        elif kind == 'progress': self._props_simple('progress_arrow', "➡ Progress",
                                                    ['x', 'y', 'width', 'height',
                                                     'progress_data', 'progress_max_data'])
        elif kind == 'pinv': self._props_simple('player_inv', "🎒 Player Inv", ['x', 'y'])
        elif kind == 'hotbar': self._props_simple('hotbar', "🔥 Hotbar", ['x', 'y'])
        else:
            tk.Label(self.props_content, text="Selecione um elemento\nno canvas pra editar",
                     bg="#1a1a26", fg="#888", font=("Arial", 9), justify=tk.CENTER).pack(pady=20)

    def _props_slot(self):
        s = self.config_data['slots'][self.selected_index]
        tk.Label(self.props_content, text=f"🔲 Slot #{self.selected_index}", bg="#1a1a26",
                 fg="#fff", font=("Arial", 11, "bold")).pack(anchor='w')
        for key, label, kind in [("name", "Nome", str), ("index", "Index", int),
                                 ("x", "X", int), ("y", "Y", int)]:
            row = tk.Frame(self.props_content, bg="#1a1a26")
            row.pack(fill=tk.X, pady=2)
            tk.Label(row, text=label, bg="#1a1a26", fg="#ddd", font=("Arial", 9),
                     anchor='w', width=10).pack(side=tk.LEFT)
            v = tk.StringVar(value=str(s[key]))
            v.trace_add('write', lambda *a, k=key, kn=kind, var=v: self._update_slot(k, kn, var))
            tk.Entry(row, textvariable=v, bg="#0a0a0a", fg="#fff",
                     insertbackground="#fff", relief=tk.FLAT, width=14).pack(side=tk.RIGHT)
        row = tk.Frame(self.props_content, bg="#1a1a26")
        row.pack(fill=tk.X, pady=4)
        tk.Label(row, text="Cor", bg="#1a1a26", fg="#ddd", font=("Arial", 9),
                 anchor='w', width=10).pack(side=tk.LEFT)
        sw = tk.Frame(row, bg=hex_to_rgb(s.get('color', '0xFF553090')),
                      width=40, height=20, cursor="hand2", relief=tk.RAISED, bd=1)
        sw.pack(side=tk.LEFT, padx=4)
        sw.bind("<Button-1>", lambda e: self._pick_slot_color(sw))

    def _update_slot(self, key, kind, var):
        try:
            v = kind(var.get()) if kind == int else var.get()
            self.config_data['slots'][self.selected_index][key] = v
            self.redraw_canvas()
        except (ValueError, IndexError): pass

    def _pick_slot_color(self, sw):
        s = self.config_data['slots'][self.selected_index]
        cur = hex_to_rgb(s.get('color', '0xFF553090'))
        result = colorchooser.askcolor(initialcolor=cur, title="Cor do slot")
        if result and result[1]:
            s['color'] = rgb_to_argb(result[1])
            sw.config(bg=result[1])
            self.redraw_canvas()

    def _props_simple(self, parent_key, label, keys):
        d = self.config_data[parent_key]
        tk.Label(self.props_content, text=label, bg="#1a1a26",
                 fg="#fff", font=("Arial", 11, "bold")).pack(anchor='w')
        for key in keys:
            row = tk.Frame(self.props_content, bg="#1a1a26")
            row.pack(fill=tk.X, pady=2)
            tk.Label(row, text=key, bg="#1a1a26", fg="#ddd", font=("Arial", 9),
                     anchor='w', width=18).pack(side=tk.LEFT)
            v = tk.StringVar(value=str(d[key]))
            v.trace_add('write', lambda *a, pk=parent_key, k=key, var=v: self._update_simple(pk, k, var))
            tk.Entry(row, textvariable=v, bg="#0a0a0a", fg="#fff",
                     insertbackground="#fff", relief=tk.FLAT, width=8).pack(side=tk.RIGHT)

    def _update_simple(self, parent_key, key, var):
        try:
            self.config_data[parent_key][key] = int(var.get())
            self.redraw_canvas()
        except ValueError: pass

    # -------------------- Toolbar actions --------------------
    def add_slot(self):
        i = len(self.config_data['slots'])
        self.config_data['slots'].append({
            "name": f"SLOT_{i}", "index": i,
            "x": 80, "y": 30 + (i * 22) % 60,
            "color": "0xFF553090"
        })
        self.redraw_canvas()

    def toggle_energy(self):
        if 'energy_bar' in self.config_data:
            del self.config_data['energy_bar']
        else:
            self.config_data['energy_bar'] = {
                "x": 8, "y": 18, "width": 8, "height": 50,
                "energy_data_hi": 0, "energy_data_lo": 1,
                "max_data_hi": 2, "max_data_lo": 3
            }
        self.redraw_canvas()

    def toggle_progress(self):
        if 'progress_arrow' in self.config_data:
            del self.config_data['progress_arrow']
        else:
            self.config_data['progress_arrow'] = {
                "x": 70, "y": 40, "width": 48, "height": 18,
                "progress_data": 4, "progress_max_data": 5
            }
        self.redraw_canvas()

    def save_json(self):
        path = filedialog.asksaveasfilename(
            initialdir=str(Path(self.mod_root_var.get()) / "tools"),
            initialfile=f"{self.config_data.get('snake', 'gui')}.json",
            defaultextension=".json",
            filetypes=[("JSON", "*.json")])
        if not path: return
        Path(path).write_text(json.dumps(self.config_data, indent=2), encoding='utf-8')
        messagebox.showinfo("Salvo", f"Config salvo em\n{path}")

    def load_json(self):
        path = filedialog.askopenfilename(filetypes=[("JSON", "*.json")])
        if not path: return
        try:
            self.config_data = json.loads(Path(path).read_text(encoding='utf-8'))
        except Exception as ex:
            messagebox.showerror("Erro", f"Falha:\n{ex}"); return
        for k, (v, _) in self.form_vars.items():
            v.set(str(self.config_data.get(k, "")))
        self.redraw_canvas()

    def preview_code(self):
        gen = CodeGen(self.config_data, self.mod_root_var.get())
        try:
            menu = gen.gen_menu()
            screen = gen.gen_screen()
            be = gen.gen_be()
            block = gen.gen_block()
            regs = gen.gen_registration_lines()
            msg = "=== ARQUIVOS GERADOS ===\n"
            for p in [menu, screen, be, block]: msg += f"  {p}\n"
            msg += "\n=== LINHAS PRA REGISTRO ===\n"
            for k, v in regs.items(): msg += f"\n--- {k} ---\n{v}\n"
            self._show_text("Preview de Código", msg)
        except Exception as ex:
            messagebox.showerror("Erro", str(ex))

    def generate_and_patch(self):
        if not Path(self.mod_root_var.get()).exists():
            messagebox.showerror("Erro", "Pasta do mod não existe")
            return
        if not messagebox.askyesno("Confirmar",
                f"Vai gerar arquivos Java + assets E modificar:\n"
                f"  ModBlocks.java, ModItems.java, ModBlockEntities.java\n"
                f"  ModMenuTypes.java, ModCreativeTabs.java\n"
                f"  ClientModEvents.java, lang/*.json\n\n"
                f"Faz backup antes? Continuar?"):
            return
        try:
            cfg = self.config_data
            cfg['kind'] = self.kind_var.get()
            cfg['block_texture'] = self.block_tex_var.get()
            cfg['modid'] = self.modid_var.get()
            gen = CodeGen(cfg, self.mod_root_var.get())
            files, regs = gen.generate_all()
            patcher = AutoPatcher(self.mod_root_var.get(), cfg.get('package', DEFAULT_MOD_PKG),
                                  cfg.get('modid', 'liberthia'))
            patch_results = patcher.patch(regs)

            msg = "=== ARQUIVOS CRIADOS ===\n"
            for p in files: msg += f"  + {p}\n"
            msg += "\n=== AUTOPATCH ===\n"
            for k, v in patch_results.items(): msg += f"  {k}: {v}\n"
            msg += "\n✓ Pronto! Roda ./gradlew build pra compilar."
            self._show_text("Resultado", msg)
        except Exception as ex:
            import traceback
            messagebox.showerror("Erro", traceback.format_exc())

    def _show_text(self, title, text):
        win = tk.Toplevel(self)
        win.title(title)
        win.geometry("800x600")
        win.configure(bg="#1a1a26")
        txt = tk.Text(win, bg="#0a0a0a", fg="#fff", font=("Courier", 9),
                      insertbackground="#fff")
        txt.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
        txt.insert("1.0", text)
        txt.configure(state="disabled")


# -------------------- Recipe Dialog --------------------
class RecipeDialog(tk.Toplevel):
    """Dialog visual pra editar uma receita: inputs (multi-slot), output, FE, ticks."""

    def __init__(self, parent, recipe_idx):
        super().__init__(parent)
        self.parent = parent
        self.recipe_idx = recipe_idx
        self.recipe = parent.config_data['recipes'][recipe_idx]
        self.title(f"Editor de Receita #{recipe_idx}")
        self.geometry("680x520")
        self.configure(bg="#1a1a26")
        self.input_rows = []  # list of (slot_var, item_var, count_var)
        self.build_ui()

    def build_ui(self):
        # Header
        tk.Label(self, text=f"🔬 Receita #{self.recipe_idx}",
                 bg="#1a1a26", fg="#aaf", font=("Arial", 14, "bold")).pack(pady=10)

        # ============ Visual flow representation ============
        flow = tk.Frame(self, bg="#1a1a26")
        flow.pack(pady=8)
        tk.Label(flow, text="📥 INPUTS", bg="#1a1a26", fg="#0fa",
                 font=("Arial", 10, "bold")).pack(side=tk.LEFT, padx=20)
        tk.Label(flow, text="→ ⚙ →", bg="#1a1a26", fg="#fa0",
                 font=("Arial", 14, "bold")).pack(side=tk.LEFT, padx=20)
        tk.Label(flow, text="📤 OUTPUT", bg="#1a1a26", fg="#fa0",
                 font=("Arial", 10, "bold")).pack(side=tk.LEFT, padx=20)

        # ============ Inputs section ============
        inputs_box = tk.LabelFrame(self, text=" 📥 Inputs (slots de entrada) ",
                                    bg="#1a1a26", fg="#0fa", font=("Arial", 10, "bold"),
                                    padx=8, pady=6)
        inputs_box.pack(fill=tk.X, padx=10, pady=4)

        self.inputs_frame = tk.Frame(inputs_box, bg="#1a1a26")
        self.inputs_frame.pack(fill=tk.X)
        for inp in self.recipe.get('inputs', []):
            self._add_input_row(inp)

        tk.Button(inputs_box, text="+ Adicionar Input", command=self._add_input,
                  bg="#0a8", fg="white", relief=tk.FLAT, padx=10).pack(pady=4)

        # ============ Output section ============
        out_box = tk.LabelFrame(self, text=" 📤 Output (resultado) ",
                                bg="#1a1a26", fg="#fa0", font=("Arial", 10, "bold"),
                                padx=8, pady=6)
        out_box.pack(fill=tk.X, padx=10, pady=4)

        out = self.recipe.get('output', {"slot": "OUTPUT", "item": "", "count": 1})
        row = tk.Frame(out_box, bg="#1a1a26")
        row.pack(fill=tk.X, pady=2)
        tk.Label(row, text="Slot:", bg="#1a1a26", fg="#ddd",
                 font=("Arial", 9), width=6).pack(side=tk.LEFT)
        self.out_slot_var = tk.StringVar(value=out.get('slot', 'OUTPUT'))
        slot_menu = ttk.Combobox(row, textvariable=self.out_slot_var,
                                  values=self.parent.slot_names(), width=10)
        slot_menu.pack(side=tk.LEFT, padx=4)
        tk.Label(row, text="Item:", bg="#1a1a26", fg="#ddd",
                 font=("Arial", 9), width=6).pack(side=tk.LEFT, padx=(10, 2))
        self.out_item_var = tk.StringVar(value=out.get('item', ''))
        item_picker = AutocompleteCombobox(row, textvariable=self.out_item_var,
                                            values=ALL_ITEMS, width=35)
        item_picker.pack(side=tk.LEFT, padx=4)
        tk.Label(row, text="Qtd:", bg="#1a1a26", fg="#ddd",
                 font=("Arial", 9), width=4).pack(side=tk.LEFT, padx=(10, 2))
        self.out_count_var = tk.StringVar(value=str(out.get('count', 1)))
        tk.Entry(row, textvariable=self.out_count_var, bg="#0a0a0a", fg="#fff",
                 insertbackground="#fff", relief=tk.FLAT, width=4).pack(side=tk.LEFT)

        # ============ Process settings ============
        proc_box = tk.LabelFrame(self, text=" ⚙ Processamento ",
                                  bg="#1a1a26", fg="#fff", font=("Arial", 10, "bold"),
                                  padx=8, pady=6)
        proc_box.pack(fill=tk.X, padx=10, pady=4)
        row = tk.Frame(proc_box, bg="#1a1a26")
        row.pack(fill=tk.X)
        tk.Label(row, text="Ticks por op:", bg="#1a1a26", fg="#ddd",
                 font=("Arial", 9)).pack(side=tk.LEFT)
        self.ticks_var = tk.StringVar(value=str(self.recipe.get('ticks', 200)))
        tk.Entry(row, textvariable=self.ticks_var, bg="#0a0a0a", fg="#fff",
                 insertbackground="#fff", relief=tk.FLAT, width=8).pack(side=tk.LEFT, padx=4)
        tk.Label(row, text="(20t = 1s)", bg="#1a1a26", fg="#888",
                 font=("Arial", 8)).pack(side=tk.LEFT)

        tk.Label(row, text="  FE total:", bg="#1a1a26", fg="#ddd",
                 font=("Arial", 9)).pack(side=tk.LEFT, padx=(20, 0))
        self.fe_var = tk.StringVar(value=str(self.recipe.get('fe_per_op', 50000)))
        tk.Entry(row, textvariable=self.fe_var, bg="#0a0a0a", fg="#fff",
                 insertbackground="#fff", relief=tk.FLAT, width=10).pack(side=tk.LEFT, padx=4)
        tk.Label(row, text="(0 = sem energia)", bg="#1a1a26", fg="#888",
                 font=("Arial", 8)).pack(side=tk.LEFT)

        # ============ Buttons ============
        btn_row = tk.Frame(self, bg="#1a1a26")
        btn_row.pack(side=tk.BOTTOM, pady=10)
        tk.Button(btn_row, text="💾 Salvar", command=self._save,
                  bg="#0a8", fg="white", relief=tk.FLAT, padx=20, pady=4,
                  font=("Arial", 10, "bold")).pack(side=tk.LEFT, padx=5)
        tk.Button(btn_row, text="✕ Cancelar", command=self.destroy,
                  bg="#444", fg="white", relief=tk.FLAT, padx=20, pady=4).pack(side=tk.LEFT, padx=5)

    def _add_input(self):
        self._add_input_row({"slot": "INPUT", "item": "", "count": 1})

    def _add_input_row(self, inp):
        row = tk.Frame(self.inputs_frame, bg="#22223a", padx=4, pady=2)
        row.pack(fill=tk.X, pady=1)
        tk.Label(row, text="if", bg="#22223a", fg="#fa0", font=("Arial", 9, "bold")).pack(side=tk.LEFT)
        slot_var = tk.StringVar(value=inp.get('slot', 'INPUT'))
        ttk.Combobox(row, textvariable=slot_var, values=self.parent.slot_names(),
                     width=10).pack(side=tk.LEFT, padx=2)
        tk.Label(row, text="contém", bg="#22223a", fg="#ddd", font=("Arial", 8)).pack(side=tk.LEFT)
        item_var = tk.StringVar(value=inp.get('item', ''))
        AutocompleteCombobox(row, textvariable=item_var, values=ALL_ITEMS, width=32).pack(side=tk.LEFT, padx=2)
        tk.Label(row, text="×", bg="#22223a", fg="#fa0", font=("Arial", 9, "bold")).pack(side=tk.LEFT)
        count_var = tk.StringVar(value=str(inp.get('count', 1)))
        tk.Entry(row, textvariable=count_var, bg="#0a0a0a", fg="#fff",
                 insertbackground="#fff", relief=tk.FLAT, width=4).pack(side=tk.LEFT, padx=2)
        tk.Button(row, text="✕", command=lambda r=row: self._remove_input(r),
                  bg="#822", fg="white", relief=tk.FLAT, font=("Arial", 7),
                  padx=4).pack(side=tk.RIGHT)
        self.input_rows.append((row, slot_var, item_var, count_var))

    def _remove_input(self, row):
        for i, (r, *_) in enumerate(self.input_rows):
            if r == row:
                self.input_rows.pop(i)
                row.destroy()
                return

    def _save(self):
        try:
            inputs = []
            for _, sv, iv, cv in self.input_rows:
                if not iv.get().strip(): continue
                inputs.append({
                    "slot": sv.get(),
                    "item": iv.get().strip(),
                    "count": int(cv.get())
                })
            output = {
                "slot": self.out_slot_var.get(),
                "item": self.out_item_var.get().strip(),
                "count": int(self.out_count_var.get())
            }
            self.recipe['inputs'] = inputs
            self.recipe['output'] = output
            self.recipe['ticks'] = int(self.ticks_var.get())
            self.recipe['fe_per_op'] = int(self.fe_var.get())
            self.destroy()
        except ValueError as e:
            messagebox.showerror("Erro", f"Valores inválidos: {e}")


# -------------------- Autocomplete Combobox --------------------
class AutocompleteCombobox(ttk.Combobox):
    """Combobox que filtra valores enquanto digita."""

    def __init__(self, parent, **kwargs):
        self._all_values = list(kwargs.get('values', []))
        super().__init__(parent, **kwargs)
        self.bind('<KeyRelease>', self._on_keyrelease)

    def _on_keyrelease(self, event):
        if event.keysym in ('Up', 'Down', 'Return', 'Tab'):
            return
        text = self.get().lower()
        if not text:
            self['values'] = self._all_values
            return
        matches = [v for v in self._all_values if text in v.lower()]
        self['values'] = matches[:50]


def main():
    initial = None
    if len(sys.argv) > 1:
        try: initial = json.loads(Path(sys.argv[1]).read_text(encoding='utf-8'))
        except Exception: pass
    app = GuiStudio(initial)
    app.bind("<Control-s>", lambda e: app.save_json())
    app.bind("<Control-g>", lambda e: app.generate_and_patch())
    app.mainloop()


if __name__ == "__main__":
    main()
