#!/usr/bin/env python3
"""
gen_gui.py — gerador de GUI customizada pro Liberthia mod.

Uso:
  python tools/gen_gui.py minha_gui.json
  python tools/gen_gui.py --interactive

Com --interactive, abre prompts pra você ir configurando.
Com config JSON, lê e gera os arquivos diretamente.

Saída: 3 arquivos Java + registros pra colar nos arquivos do mod:
  - src/main/java/br/com/murilo/liberthia/menu/<Name>Menu.java
  - src/main/java/br/com/murilo/liberthia/client/screen/<Name>Screen.java
  - imprime no terminal as linhas pra adicionar em ModMenuTypes + ClientModEvents
"""
import argparse, json, os, sys, re
from pathlib import Path
# -------------------- Config schema --------------------
# {
#   "name": "MyMachine",                     // PascalCase, vira <Name>Menu / <Name>Screen
#   "snake": "my_machine",                   // pra IDs / lang
#   "be_class": "MyMachineBlockEntity",      // BE class simple name
#   "block_const": "MY_MACHINE",             // ModBlocks constant name
#   "title_pt": "Minha Máquina",
#   "title_en": "My Machine",
#   "image_width": 176,
#   "image_height": 166,
#   "theme": {
#     "primary": "0xFF6B2A8C",       // bordas/destaques
#     "background": "0xFF120420",     // fundo do GUI
#     "panel": "0xFF1B0830",          // painéis internos
#     "accent": "0xFFAA40E8"          // pontas de seta etc
#   },
#   "data_slots": 8,                  // tamanho do ContainerData
#   "energy_bar": {                   // omitir se não tem energia
#     "x": 8, "y": 18, "width": 8, "height": 50,
#     "energy_data_hi": 0, "energy_data_lo": 1,
#     "max_data_hi": 2, "max_data_lo": 3
#   },
#   "progress_arrow": {               // omitir se não tem progresso
#     "x": 70, "y": 40, "width": 48, "height": 18,
#     "progress_data": 4, "progress_max_data": 5
#   },
#   "slots": [                        // slots do BE — coords da GUI (em pixels)
#     {"name": "INPUT", "index": 0, "x": 44, "y": 30, "color": "0xFF553090"},
#     {"name": "CATALYST", "index": 1, "x": 44, "y": 52, "color": "0xFFE0C040"},
#     {"name": "OUTPUT", "index": 2, "x": 122, "y": 40, "color": "0xFFAA40E8"}
#   ],
#   "player_inv": { "x": 8, "y": 84 },     // top-left do inv 3x9
#   "hotbar": { "x": 8, "y": 142 },        // top-left da hotbar
#   "title_y": 6,
#   "inventory_label_y": 72
# }

JAVA_PKG = "br.com.murilo.liberthia"
PROJECT_ROOT = Path(__file__).parent.parent
MENU_DIR = PROJECT_ROOT / "src/main/java/br/com/murilo/liberthia/menu"
SCREEN_DIR = PROJECT_ROOT / "src/main/java/br/com/murilo/liberthia/client/screen"


# -------------------- Templates --------------------
MENU_TEMPLATE = '''package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.{be_class};
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
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

/**
 * Menu auto-gerado por tools/gen_gui.py — não edite manualmente, regenere.
 */
public class {name}Menu extends AbstractContainerMenu {{
    private final {be_class} be;
    private final Level level;
    private final ContainerData data;

    public {name}Menu(int id, Inventory inv, FriendlyByteBuf buf) {{
        this(id, inv,
                ({be_class}) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData({data_slots}));
    }}

    public {name}Menu(int id, Inventory inv, {be_class} be, ContainerData data) {{
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
                addSlot(new Slot(inv, col + row * 9 + 9, {pinv_x} + col * 18, {pinv_y} + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, {hotbar_x} + col * 18, {hotbar_y}));
    }}

    private int read32(int hi, int lo) {{ return ((data.get(hi) & 0xFFFF) << 16) | (data.get(lo) & 0xFFFF); }}
{getter_methods}

    public {be_class} getBlockEntity() {{ return be; }}

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {{ return ItemStack.EMPTY; }}

    @Override
    public boolean stillValid(Player p) {{
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), p, ModBlocks.{block_const}.get());
    }}
}}
'''

SCREEN_TEMPLATE = '''package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.{name}Menu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Screen auto-gerado por tools/gen_gui.py.
 * Tema: primary={primary} bg={background}.
 */
public class {name}Screen extends AbstractContainerScreen<{name}Menu> {{

    public {name}Screen({name}Menu m, Inventory inv, Component title) {{
        super(m, inv, title);
        this.imageWidth = {image_width};
        this.imageHeight = {image_height};
        this.titleLabelY = {title_y};
        this.inventoryLabelY = {inv_label_y};
    }}

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {{
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        // Frame
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0E0212);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, {background});
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, {primary});
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + {inv_label_y} - 4, {panel});

{slot_draws}
{energy_draw}
{progress_draw}

        // Player inv panel
        g.fill(x + 6, y + {pinv_y} - 4, x + imageWidth - 6, y + imageHeight - 6, {panel});
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + {pinv_x} + col * 18 - 1, y + {pinv_y} + row * 18 - 1, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            slot(g, x + {hotbar_x} + col * 18 - 1, y + {hotbar_y} - 1, {accent});
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
                Component.translatable("container.liberthia.{snake}")
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


# -------------------- Generation --------------------
def gen_slot_lines(cfg):
    lines = []
    for s in cfg['slots']:
        lines.append(f'        addSlot(new SlotItemHandler((IItemHandler) be.getInventory(), '
                     f'{cfg["be_class"]}.SLOT_{s["name"]}, {s["x"]}, {s["y"]}));')
    return "\n".join(lines)


def gen_getter_methods(cfg):
    methods = []
    if 'energy_bar' in cfg:
        eb = cfg['energy_bar']
        methods.append(f'    public int rawEnergy()    {{ return read32({eb["energy_data_hi"]}, {eb["energy_data_lo"]}); }}')
        methods.append(f'    public int rawEnergyMax() {{ return read32({eb["max_data_hi"]}, {eb["max_data_lo"]}); }}')
        methods.append('    public float energyFrac() { return Math.min(1f, rawEnergy() / (float) Math.max(1, rawEnergyMax())); }')
    if 'progress_arrow' in cfg:
        pa = cfg['progress_arrow']
        methods.append(f'    public int progress()    {{ return data.get({pa["progress_data"]}); }}')
        methods.append(f'    public int progressMax() {{ return Math.max(1, data.get({pa["progress_max_data"]})); }}')
        methods.append('    public float progressFrac() { return Math.min(1f, progress() / (float) progressMax()); }')
    return "\n".join(methods)


def gen_slot_draws(cfg):
    lines = []
    for s in cfg['slots']:
        c = s.get('color', '0xFF553090')
        lines.append(f'        slot(g, x + {s["x"]} - 1, y + {s["y"]} - 1, {c});')
    return "\n".join(lines)


def gen_energy_draw(cfg):
    if 'energy_bar' not in cfg:
        return ""
    e = cfg['energy_bar']
    return f'''
        // Energy bar
        int bx = x + {e["x"]}, by = y + {e["y"]}, bw = {e["width"]}, bh = {e["height"]};
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, {cfg["theme"]["primary"]});
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


def gen_progress_draw(cfg):
    if 'progress_arrow' not in cfg:
        return ""
    p = cfg['progress_arrow']
    return f'''
        // Progress arrow
        int arrowX = x + {p["x"]}, arrowY = y + {p["y"]}, arrowW = {p["width"]}, arrowH = {p["height"]};
        g.fill(arrowX, arrowY, arrowX + arrowW, arrowY + arrowH, 0xFF1A0830);
        g.fill(arrowX, arrowY, arrowX + arrowW, arrowY + 1, {cfg["theme"]["primary"]});
        g.fill(arrowX, arrowY + arrowH - 1, arrowX + arrowW, arrowY + arrowH, {cfg["theme"]["primary"]});
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
            g.fill(xx, arrowY + 6 - i, xx + 1, arrowY + arrowH - 6 + i, {cfg["theme"]["accent"]});
        }}'''


def render_files(cfg):
    name = cfg['name']
    snake = cfg['snake']

    menu_code = MENU_TEMPLATE.format(
        name=name,
        be_class=cfg['be_class'],
        block_const=cfg['block_const'],
        data_slots=cfg['data_slots'],
        slot_lines=gen_slot_lines(cfg),
        getter_methods=gen_getter_methods(cfg),
        pinv_x=cfg['player_inv']['x'],
        pinv_y=cfg['player_inv']['y'],
        hotbar_x=cfg['hotbar']['x'],
        hotbar_y=cfg['hotbar']['y'],
    )

    screen_code = SCREEN_TEMPLATE.format(
        name=name,
        snake=snake,
        image_width=cfg['image_width'],
        image_height=cfg['image_height'],
        title_y=cfg.get('title_y', 6),
        inv_label_y=cfg.get('inventory_label_y', 72),
        primary=cfg['theme']['primary'],
        background=cfg['theme']['background'],
        panel=cfg['theme']['panel'],
        accent=cfg['theme']['accent'],
        pinv_x=cfg['player_inv']['x'],
        pinv_y=cfg['player_inv']['y'],
        hotbar_x=cfg['hotbar']['x'],
        hotbar_y=cfg['hotbar']['y'],
        slot_draws=gen_slot_draws(cfg),
        energy_draw=gen_energy_draw(cfg),
        progress_draw=gen_progress_draw(cfg),
    )

    menu_path = MENU_DIR / f"{name}Menu.java"
    screen_path = SCREEN_DIR / f"{name}Screen.java"
    menu_path.parent.mkdir(parents=True, exist_ok=True)
    screen_path.parent.mkdir(parents=True, exist_ok=True)
    menu_path.write_text(menu_code, encoding='utf-8')
    screen_path.write_text(screen_code, encoding='utf-8')
    return menu_path, screen_path


def print_registration_lines(cfg):
    name = cfg['name']
    snake = cfg['snake']
    block_const = cfg['block_const']
    print()
    print("=" * 70)
    print("Adicione em ModMenuTypes.java:")
    print("=" * 70)
    print(f'    public static final RegistryObject<MenuType<{name}Menu>> {block_const} = registerMenuType(')
    print(f'            "{snake}", {name}Menu::new);')
    print()
    print("=" * 70)
    print("Adicione em ClientModEvents.onClientSetup() no enqueueWork:")
    print("=" * 70)
    print(f'            MenuScreens.register(ModMenuTypes.{block_const}.get(),')
    print(f'                    br.com.murilo.liberthia.client.screen.{name}Screen::new);')
    print()
    print("=" * 70)
    print("Adicione em pt_br.json e en_us.json:")
    print("=" * 70)
    print(f'  "container.liberthia.{snake}": "{cfg["title_pt"]}",')


# -------------------- Interactive mode --------------------
def interactive():
    print("=== Liberthia GUI Generator ===")
    print("Configure passo-a-passo. Enter pra default.\n")
    cfg = {
        "theme": {},
        "player_inv": {},
        "hotbar": {},
        "slots": []
    }
    cfg['name'] = input("Nome PascalCase (ex: MyMachine): ").strip() or "MyMachine"
    cfg['snake'] = input(f"Nome snake_case [my_machine]: ").strip() or to_snake(cfg['name'])
    cfg['be_class'] = input(f"BE class [{cfg['name']}BlockEntity]: ").strip() or f"{cfg['name']}BlockEntity"
    cfg['block_const'] = input(f"Block const ModBlocks.X [{cfg['snake'].upper()}]: ").strip() or cfg['snake'].upper()
    cfg['title_pt'] = input("Título PT-BR: ").strip() or cfg['name']
    cfg['title_en'] = input("Título EN: ").strip() or cfg['name']
    cfg['image_width'] = int(input("Largura GUI [176]: ") or 176)
    cfg['image_height'] = int(input("Altura GUI [166]: ") or 166)
    cfg['data_slots'] = int(input("ContainerData size [8]: ") or 8)
    cfg['theme']['primary'] = input("Primary color [0xFF6B2A8C]: ") or "0xFF6B2A8C"
    cfg['theme']['background'] = input("Background [0xFF120420]: ") or "0xFF120420"
    cfg['theme']['panel'] = input("Panel [0xFF1B0830]: ") or "0xFF1B0830"
    cfg['theme']['accent'] = input("Accent [0xFFAA40E8]: ") or "0xFFAA40E8"

    # Slots
    n = int(input("Quantos slots de máquina? [3]: ") or 3)
    for i in range(n):
        print(f"  Slot #{i}:")
        nm = input(f"    Nome (vai virar SLOT_X) [INPUT_{i}]: ").strip() or f"INPUT_{i}"
        x = int(input(f"    x [44]: ") or 44)
        y = int(input(f"    y [{30 + i*22}]: ") or 30 + i*22)
        col = input(f"    cor [0xFF553090]: ") or "0xFF553090"
        cfg['slots'].append({"name": nm, "index": i, "x": x, "y": y, "color": col})

    if input("Tem barra de energia? [s/N]: ").lower().startswith('s'):
        cfg['energy_bar'] = {
            "x": int(input("  x [8]: ") or 8),
            "y": int(input("  y [18]: ") or 18),
            "width": int(input("  largura [8]: ") or 8),
            "height": int(input("  altura [50]: ") or 50),
            "energy_data_hi": int(input("  data_hi [0]: ") or 0),
            "energy_data_lo": int(input("  data_lo [1]: ") or 1),
            "max_data_hi": int(input("  max_hi [2]: ") or 2),
            "max_data_lo": int(input("  max_lo [3]: ") or 3),
        }
    if input("Tem progress arrow? [s/N]: ").lower().startswith('s'):
        cfg['progress_arrow'] = {
            "x": int(input("  x [70]: ") or 70),
            "y": int(input("  y [40]: ") or 40),
            "width": int(input("  largura [48]: ") or 48),
            "height": int(input("  altura [18]: ") or 18),
            "progress_data": int(input("  progress_data [4]: ") or 4),
            "progress_max_data": int(input("  progress_max_data [5]: ") or 5),
        }

    cfg['player_inv'] = {
        "x": int(input("Player inv x [8]: ") or 8),
        "y": int(input("Player inv y [84]: ") or 84)
    }
    cfg['hotbar'] = {
        "x": int(input("Hotbar x [8]: ") or 8),
        "y": int(input("Hotbar y [142]: ") or 142)
    }
    cfg['title_y'] = int(input("Title y [6]: ") or 6)
    cfg['inventory_label_y'] = int(input("Inv label y [72]: ") or 72)

    out = input(f"Salvar config como? [{cfg['snake']}.json]: ").strip() or f"{cfg['snake']}.json"
    Path(out).write_text(json.dumps(cfg, indent=2), encoding='utf-8')
    print(f"Config salvo em {out}")
    return cfg


def to_snake(name):
    return re.sub(r'([a-z])([A-Z])', r'\1_\2', name).lower()


# -------------------- Entry --------------------
def main():
    p = argparse.ArgumentParser()
    p.add_argument("config", nargs="?", help="caminho pra JSON")
    p.add_argument("--interactive", action="store_true")
    args = p.parse_args()

    if args.interactive:
        cfg = interactive()
    elif args.config:
        cfg = json.loads(Path(args.config).read_text(encoding='utf-8'))
    else:
        p.print_help()
        sys.exit(1)

    menu_path, screen_path = render_files(cfg)
    print(f"\n[OK] Menu gerado: {menu_path}")
    print(f"[OK] Screen gerado: {screen_path}")
    print_registration_lines(cfg)


if __name__ == "__main__":
    main()
