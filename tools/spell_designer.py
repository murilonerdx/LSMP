#!/usr/bin/env python3
"""r158: Liberthia Spell Designer — GUI para criação visual de feitiços.

Sem código. Sem escrita. Só click, arrastar e selecionar.

Workflow:
  1. Preencha identidade (ID, nome, lore)
  2. Selecione escola, tipo, categoria, raridade
  3. Ajuste sliders de stats
  4. Pinte cor primária/secundária, escolha preset de partícula
  5. Adicione efeitos da lista (mod-native + vanilla)
  6. Arraste sprites da galeria pro canvas (camadas, ordem play)
  7. Click "Save Spell" → cria JSON + textura + model + atualiza index

Requisitos: Python 3.8+ com Pillow (`pip install pillow`).

Uso: `python tools/spell_designer.py` (na raiz do projeto)
"""
from __future__ import annotations

import json
import math
import os
import re
import sys
import tkinter as tk
from pathlib import Path
from tkinter import colorchooser, filedialog, messagebox, ttk
from typing import Dict, List, Optional, Tuple

try:
    from PIL import Image, ImageDraw, ImageTk
except ImportError:
    sys.exit("ERROR: Pillow required. Run: pip install pillow")

# ─── Constants ────────────────────────────────────────────────────────────
ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "src" / "main" / "resources"
SPELLS_DIR = RESOURCES / "data" / "liberthia" / "spells"
SPELL_TEX_DIR = RESOURCES / "assets" / "liberthia" / "textures" / "item"
SPELL_MODEL_DIR = RESOURCES / "assets" / "liberthia" / "models" / "item"
DATA_DIR = RESOURCES / "data" / "liberthia"
SCHOOLS_DIR = DATA_DIR / "schools"
RUNES_DIR = DATA_DIR / "runes"
ELEMENTS_DIR = DATA_DIR / "elements"
THREADS_DIR = DATA_DIR / "threads"
FOCUSES_DIR = DATA_DIR / "focuses"
PARTICLE_DIR = DATA_DIR / "particle_config"

JAVA_INDEX = ROOT / "src" / "main" / "java" / "br" / "com" / "murilo" / "liberthia" / "magic" / "factory" / "FactorySpellIndex.java"
LANG_EN = RESOURCES / "assets" / "liberthia" / "lang" / "en_us.json"
LANG_PT = RESOURCES / "assets" / "liberthia" / "lang" / "pt_br.json"

SCHOOLS = ["FIRE", "ICE", "LIGHTNING", "BLOOD", "ELDRITCH", "HOLY", "NATURE"]
SCHOOL_COLORS = {
    "FIRE": "#ff6633", "ICE": "#66ccff", "LIGHTNING": "#ffff44",
    "BLOOD": "#990033", "ELDRITCH": "#7a3dff", "HOLY": "#ffeeaa", "NATURE": "#33aa33",
}
RARITIES = ["COMMON", "UNCOMMON", "RARE", "EPIC"]
TYPES = [
    "PROJECTILE", "BEAM", "AOE_BURST", "NOVA", "CONE", "RAIN",
    "SUMMON", "DASH", "SELF_BUFF", "TARGETED", "AURA", "EXPLOSION", "CHANNELED_BEAM"
]
CATEGORIES = [
    "EXPLOSION", "DESTRUCTION", "DASH", "RAY", "CRITICAL", "UTILITY",
    "SUMMON", "AOE", "CHANNELED", "INSTANT", "PROJECTILE", "MIXED"
]

# Mod-native effects (from CustomEffectsR156 + others)
MOD_EFFECTS = [
    "dimensional_blindness", "devil_footsteps", "blood_moon_aura", "shadow_double",
    "cosmic_whispers", "vertigo", "hungry_void", "ghost_touch", "soul_link",
    "haunted_inventory", "mirror_walk", "time_dilation", "reverse_gravity",
    "magnet_fist", "pox_swarm", "nightmare", "liberthia_blessing",
    "crystal_bloom", "ominous_aura", "stardust",
    # r151 effects
    "void_touch", "static_vision", "time_fracture", "hollow_hunger",
    "aetheric_shift", "soul_bleed", "mana_surge", "arcane_ward", "astral_sight",
    "elemental_resonance", "mind_fortress", "chronosurge", "void_armor",
    # legacy
    "blood_infection", "void_infection", "obsession", "madness",
]
EFFECT_TYPES = ["DAMAGE", "HEAL", "IGNITE", "KNOCKBACK", "TELEPORT", "CUSTOM_EFFECT",
                "EFFECT", "DRAIN_MANA", "LIFESTEAL", "PURIFY", "DISPELL"]

# ─── Helpers ──────────────────────────────────────────────────────────────
def hex_to_rgb(h: str) -> Tuple[int, int, int]:
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def rgb_to_hex(rgb: Tuple[int, int, int]) -> str:
    return "#{:02x}{:02x}{:02x}".format(*rgb)


def list_png(folder: Path) -> List[str]:
    if not folder.exists():
        return []
    return sorted([f.stem for f in folder.glob("*.png")])


def slugify(s: str) -> str:
    s = s.lower().strip()
    s = re.sub(r"[^a-z0-9]+", "_", s)
    s = re.sub(r"_+", "_", s).strip("_")
    return s or "spell"


# ─── Main App ─────────────────────────────────────────────────────────────
class SpellDesigner(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Liberthia Spell Designer — r158")
        self.geometry("1400x900")
        self.configure(bg="#1a0824")
        self._setup_style()

        # State
        self.color_primary = tk.StringVar(value="#ff5500")
        self.color_secondary = tk.StringVar(value="#ffaa00")
        self.var_id = tk.StringVar(value="")
        self.var_name = tk.StringVar(value="Meu Feitiço")
        self.var_lore = tk.StringVar(value="Um feitiço customizado.")
        self.var_school = tk.StringVar(value="FIRE")
        self.var_rarity = tk.StringVar(value="UNCOMMON")
        self.var_type = tk.StringVar(value="PROJECTILE")
        self.var_category = tk.StringVar(value="PROJECTILE")
        self.var_mana = tk.IntVar(value=40)
        self.var_cooldown = tk.IntVar(value=100)
        self.var_damage = tk.IntVar(value=10)
        self.var_range = tk.IntVar(value=24)
        # Particle preset
        self.var_particle_preset = tk.StringVar(value="standard")
        # Effects list — each: {"type":..., "effect":..., "duration":..., "magnitude":...}
        self.effects: List[Dict] = []
        # Sprite layers — each: (folder, name, alpha, scale)
        self.sprite_layers: List[Dict] = []
        self.thumb_cache: Dict[str, ImageTk.PhotoImage] = {}

        self._build_ui()
        self._refresh_preview()

    # ─── Styling ───────────────────────────────────────────────────────
    def _setup_style(self):
        s = ttk.Style(self)
        try:
            s.theme_use("clam")
        except Exception:
            pass
        BG = "#1a0824"; FG = "#e0d0ff"; ACCENT = "#7a3dff"; PANEL = "#2a1244"
        s.configure(".", background=BG, foreground=FG, fieldbackground=PANEL)
        s.configure("TFrame", background=BG)
        s.configure("Panel.TFrame", background=PANEL, relief="flat")
        s.configure("Header.TLabel", background=BG, foreground=ACCENT,
                    font=("Segoe UI", 11, "bold"))
        s.configure("Title.TLabel", background=BG, foreground="#ffd700",
                    font=("Segoe UI", 16, "bold"))
        s.configure("TLabel", background=BG, foreground=FG, font=("Segoe UI", 9))
        s.configure("Pixel.TLabel", background=PANEL, foreground=FG)
        s.configure("TButton", background=ACCENT, foreground="white",
                    borderwidth=0, padding=6, font=("Segoe UI", 9, "bold"))
        s.map("TButton",
              background=[("active", "#aa66ff"), ("pressed", "#5a2dcc")])
        s.configure("Save.TButton", background="#ffd700", foreground="#1a0824",
                    font=("Segoe UI", 11, "bold"), padding=10)
        s.map("Save.TButton",
              background=[("active", "#fff060"), ("pressed", "#ddb000")])
        s.configure("TEntry", fieldbackground=PANEL, foreground=FG, insertcolor=FG)
        s.configure("TCombobox", fieldbackground=PANEL, background=PANEL,
                    foreground=FG, arrowcolor=FG)
        s.configure("Horizontal.TScale", background=BG, troughcolor=PANEL)

    # ─── UI Build ──────────────────────────────────────────────────────
    def _build_ui(self):
        title_bar = ttk.Frame(self)
        title_bar.pack(fill="x", pady=(8, 4))
        ttk.Label(title_bar, text="✦ Liberthia Spell Designer ✦", style="Title.TLabel").pack()
        ttk.Label(title_bar, text="Click. Drag. Compose. — No code, no typing.",
                  style="TLabel").pack()

        main = ttk.Frame(self)
        main.pack(fill="both", expand=True, padx=12, pady=4)

        # Left column — form
        left = ttk.Frame(main, style="Panel.TFrame")
        left.pack(side="left", fill="y", padx=(0, 6))
        self._build_form(left)

        # Center column — sprite composer + preview
        center = ttk.Frame(main, style="Panel.TFrame")
        center.pack(side="left", fill="both", expand=True, padx=6)
        self._build_composer(center)

        # Right column — gallery
        right = ttk.Frame(main, style="Panel.TFrame")
        right.pack(side="left", fill="y", padx=(6, 0))
        self._build_gallery(right)

        # Bottom — save bar
        bottom = ttk.Frame(self)
        bottom.pack(fill="x", padx=12, pady=(4, 12))
        self.status_var = tk.StringVar(value="Pronto. Comece pelo ID do feitiço.")
        ttk.Label(bottom, textvariable=self.status_var, style="TLabel").pack(side="left")
        ttk.Button(bottom, text="💾  SAVE SPELL", style="Save.TButton",
                   command=self.save_spell).pack(side="right")
        ttk.Button(bottom, text="🔄 Reset", command=self.reset).pack(side="right", padx=8)
        ttk.Button(bottom, text="📂 Load…", command=self.load_spell).pack(side="right")

    # ─── Form (left) ───────────────────────────────────────────────────
    def _build_form(self, parent):
        # Scrollable form
        canvas = tk.Canvas(parent, bg="#2a1244", width=360, highlightthickness=0)
        scrollbar = ttk.Scrollbar(parent, orient="vertical", command=canvas.yview)
        canvas.configure(yscrollcommand=scrollbar.set)
        canvas.pack(side="left", fill="y")
        scrollbar.pack(side="left", fill="y")
        frame = ttk.Frame(canvas, style="Panel.TFrame")
        canvas.create_window((0, 0), window=frame, anchor="nw")
        frame.bind("<Configure>", lambda e: canvas.configure(scrollregion=canvas.bbox("all")))
        # Mousewheel scrolling
        canvas.bind_all("<MouseWheel>", lambda e: canvas.yview_scroll(int(-e.delta / 60), "units"))

        # Identity
        ttk.Label(frame, text="● Identity", style="Header.TLabel").pack(anchor="w", padx=10, pady=(10, 2))
        self._add_entry(frame, "ID (auto):", self.var_id, hint="Será derivado do nome se vazio")
        self._add_entry(frame, "Nome:", self.var_name)
        self._add_entry(frame, "Lore:", self.var_lore)
        self._add_combo(frame, "Raridade:", self.var_rarity, RARITIES)

        # Magic taxonomy
        ttk.Label(frame, text="● School & Type", style="Header.TLabel").pack(anchor="w", padx=10, pady=(14, 2))
        self._add_school_picker(frame)
        self._add_combo(frame, "Tipo (behavior):", self.var_type, TYPES, callback=self._on_type_change)
        self._add_combo(frame, "Categoria:", self.var_category, CATEGORIES)

        # Stats
        ttk.Label(frame, text="● Stats", style="Header.TLabel").pack(anchor="w", padx=10, pady=(14, 2))
        self._add_slider(frame, "Mana cost:", self.var_mana, 1, 200)
        self._add_slider(frame, "Cooldown (ticks):", self.var_cooldown, 10, 600)
        self._add_slider(frame, "Damage:", self.var_damage, 0, 80)
        self._add_slider(frame, "Range (blocos):", self.var_range, 4, 64)

        # VFX
        ttk.Label(frame, text="● VFX", style="Header.TLabel").pack(anchor="w", padx=10, pady=(14, 2))
        self._add_color_picker(frame, "Cor primária:", self.color_primary)
        self._add_color_picker(frame, "Cor secundária:", self.color_secondary)
        self._add_combo(frame, "Preset partícula:", self.var_particle_preset,
                        [p.stem for p in PARTICLE_DIR.glob("*.json")] or ["standard"])

        # Effects
        ttk.Label(frame, text="● Effects", style="Header.TLabel").pack(anchor="w", padx=10, pady=(14, 2))
        ef_btn = ttk.Frame(frame, style="Panel.TFrame")
        ef_btn.pack(fill="x", padx=10, pady=2)
        ttk.Button(ef_btn, text="+ Adicionar Effect", command=self._add_effect_dialog).pack(side="left")
        ttk.Button(ef_btn, text="✖ Limpar", command=self._clear_effects).pack(side="left", padx=4)
        self.effects_list = tk.Listbox(frame, height=6, bg="#1a0824", fg="#e0d0ff",
                                       selectbackground="#7a3dff", borderwidth=0, font=("Consolas", 9))
        self.effects_list.pack(fill="x", padx=10, pady=4)
        self.effects_list.bind("<Double-Button-1>", lambda e: self._remove_selected_effect())

    def _add_entry(self, parent, label, var, hint=""):
        row = ttk.Frame(parent, style="Panel.TFrame")
        row.pack(fill="x", padx=10, pady=2)
        ttk.Label(row, text=label, style="TLabel", width=14).pack(side="left")
        e = ttk.Entry(row, textvariable=var, font=("Segoe UI", 9))
        e.pack(side="left", fill="x", expand=True)
        if hint:
            ttk.Label(parent, text=f"   {hint}", style="TLabel",
                      foreground="#998aaa").pack(anchor="w", padx=10)

    def _add_combo(self, parent, label, var, values, callback=None):
        row = ttk.Frame(parent, style="Panel.TFrame")
        row.pack(fill="x", padx=10, pady=2)
        ttk.Label(row, text=label, style="TLabel", width=14).pack(side="left")
        cb = ttk.Combobox(row, textvariable=var, values=values, state="readonly",
                          font=("Segoe UI", 9))
        cb.pack(side="left", fill="x", expand=True)
        if callback:
            cb.bind("<<ComboboxSelected>>", lambda e: callback())
        cb.bind("<<ComboboxSelected>>", lambda e: self._refresh_preview(), add="+")

    def _add_slider(self, parent, label, var, lo, hi):
        row = ttk.Frame(parent, style="Panel.TFrame")
        row.pack(fill="x", padx=10, pady=2)
        ttk.Label(row, text=label, style="TLabel", width=14).pack(side="left")
        scale = ttk.Scale(row, from_=lo, to=hi, variable=var, orient="horizontal",
                          command=lambda v: self._refresh_preview())
        scale.pack(side="left", fill="x", expand=True)
        val_label = ttk.Label(row, text=str(var.get()), style="TLabel", width=4)
        val_label.pack(side="left", padx=4)
        def update_label(*_):
            val_label.config(text=str(int(var.get())))
        var.trace_add("write", update_label)

    def _add_color_picker(self, parent, label, var):
        row = ttk.Frame(parent, style="Panel.TFrame")
        row.pack(fill="x", padx=10, pady=2)
        ttk.Label(row, text=label, style="TLabel", width=14).pack(side="left")
        swatch = tk.Label(row, bg=var.get(), width=4, height=1, relief="raised")
        swatch.pack(side="left", padx=4)
        def pick():
            c = colorchooser.askcolor(initialcolor=var.get(), parent=self)
            if c[1]:
                var.set(c[1])
                swatch.config(bg=c[1])
                self._refresh_preview()
        ttk.Button(row, text="...", width=3, command=pick).pack(side="left")

    def _add_school_picker(self, parent):
        row = ttk.Frame(parent, style="Panel.TFrame")
        row.pack(fill="x", padx=10, pady=4)
        ttk.Label(row, text="Escola:", style="TLabel").pack(anchor="w")
        grid = ttk.Frame(parent, style="Panel.TFrame")
        grid.pack(padx=10, pady=2)
        self._school_buttons = {}
        for i, sch in enumerate(SCHOOLS):
            btn = tk.Button(grid, text=sch[:4], bg=SCHOOL_COLORS[sch], fg="black",
                            font=("Segoe UI", 8, "bold"), width=6, borderwidth=2,
                            command=lambda s=sch: self._set_school(s))
            btn.grid(row=i // 4, column=i % 4, padx=2, pady=2)
            self._school_buttons[sch] = btn
        self._set_school("FIRE")

    def _set_school(self, sch):
        self.var_school.set(sch)
        # Set primary color from school
        self.color_primary.set(SCHOOL_COLORS[sch])
        for s, b in self._school_buttons.items():
            b.config(relief="sunken" if s == sch else "raised",
                     borderwidth=3 if s == sch else 1)
        self._refresh_preview()

    def _on_type_change(self):
        # Auto-fill behavior fields based on type
        t = self.var_type.get()
        if "DASH" in t and self.var_category.get() == "PROJECTILE":
            self.var_category.set("DASH")
        elif "BEAM" in t:
            self.var_category.set("RAY")
        elif "AOE" in t or "NOVA" in t:
            self.var_category.set("AOE")

    # ─── Composer (center) ─────────────────────────────────────────────
    def _build_composer(self, parent):
        ttk.Label(parent, text="● Sprite Composer (16×16 final)",
                  style="Header.TLabel").pack(anchor="w", padx=10, pady=(10, 4))

        # Canvas preview
        prev_frame = ttk.Frame(parent, style="Panel.TFrame")
        prev_frame.pack(padx=10, pady=4)
        # Scaled preview (16×16 → 256×256 = 16× nearest neighbor)
        self.preview_canvas = tk.Canvas(prev_frame, width=256, height=256,
                                         bg="#0a0410", highlightthickness=2,
                                         highlightbackground="#7a3dff")
        self.preview_canvas.pack()
        ttk.Label(prev_frame, text="Preview 16×16 escalado 16×",
                  style="TLabel", foreground="#aaaaaa").pack()

        # Layer list
        ttk.Label(parent, text="● Layers (top→bottom = ordem play)",
                  style="Header.TLabel").pack(anchor="w", padx=10, pady=(12, 2))
        layer_panel = ttk.Frame(parent, style="Panel.TFrame")
        layer_panel.pack(fill="both", expand=True, padx=10, pady=2)
        self.layers_list = tk.Listbox(layer_panel, height=8, bg="#1a0824", fg="#e0d0ff",
                                       selectbackground="#7a3dff", borderwidth=0,
                                       font=("Consolas", 9))
        self.layers_list.pack(side="left", fill="both", expand=True)

        # Layer controls
        ctrls = ttk.Frame(layer_panel, style="Panel.TFrame")
        ctrls.pack(side="left", fill="y", padx=4)
        ttk.Button(ctrls, text="▲", width=3,
                   command=lambda: self._move_layer(-1)).pack(pady=2)
        ttk.Button(ctrls, text="▼", width=3,
                   command=lambda: self._move_layer(+1)).pack(pady=2)
        ttk.Button(ctrls, text="✖", width=3,
                   command=self._remove_layer).pack(pady=2)
        ttk.Button(ctrls, text="▶", width=3,
                   command=self._play_layers).pack(pady=8)

        # Tip
        ttk.Label(parent, text="💡 Duplo-click numa sprite da galeria para adicionar.",
                  style="TLabel", foreground="#aaaaaa").pack(anchor="w", padx=10, pady=2)

    # ─── Gallery (right) ───────────────────────────────────────────────
    def _build_gallery(self, parent):
        ttk.Label(parent, text="● Sprite Gallery", style="Header.TLabel").pack(
            anchor="w", padx=10, pady=(10, 4))
        nb = ttk.Notebook(parent, width=320)
        nb.pack(fill="both", expand=True, padx=4)

        for label, folder in [
            ("Schools",  SCHOOLS_DIR),
            ("Runes",    RUNES_DIR),
            ("Elements", ELEMENTS_DIR),
            ("Threads",  THREADS_DIR),
            ("Focuses",  FOCUSES_DIR),
        ]:
            tab = ttk.Frame(nb, style="Panel.TFrame")
            self._fill_gallery_tab(tab, folder)
            nb.add(tab, text=label)

    def _fill_gallery_tab(self, parent, folder: Path):
        canvas = tk.Canvas(parent, bg="#2a1244", highlightthickness=0)
        scroll = ttk.Scrollbar(parent, orient="vertical", command=canvas.yview)
        canvas.configure(yscrollcommand=scroll.set)
        canvas.pack(side="left", fill="both", expand=True)
        scroll.pack(side="right", fill="y")
        grid = ttk.Frame(canvas, style="Panel.TFrame")
        canvas.create_window((0, 0), window=grid, anchor="nw")
        grid.bind("<Configure>", lambda e: canvas.configure(scrollregion=canvas.bbox("all")))

        names = list_png(folder)
        cols = 4
        for i, name in enumerate(names):
            r, c = divmod(i, cols)
            cell = ttk.Frame(grid, style="Panel.TFrame")
            cell.grid(row=r, column=c, padx=4, pady=4)
            try:
                img = Image.open(folder / f"{name}.png").convert("RGBA")
                # Upscale to 48 for visibility
                img_disp = img.resize((48, 48), Image.NEAREST)
                photo = ImageTk.PhotoImage(img_disp)
                self.thumb_cache[f"{folder.name}_{name}"] = photo
                lbl = tk.Label(cell, image=photo, bg="#1a0824", relief="solid",
                               borderwidth=1, cursor="hand2")
                lbl.pack()
                ttk.Label(cell, text=name[:10], style="Pixel.TLabel",
                          font=("Segoe UI", 7)).pack()
                lbl.bind("<Double-Button-1>",
                         lambda e, f=folder, n=name: self._add_layer(f, n))
                lbl.bind("<Button-1>",
                         lambda e, f=folder, n=name: self.status_var.set(
                             f"Selecionado: {f.name}/{n} — duplo-click pra adicionar"))
            except Exception as e:
                ttk.Label(cell, text=f"err: {name}", style="TLabel").pack()

    # ─── Layer ops ─────────────────────────────────────────────────────
    def _add_layer(self, folder: Path, name: str):
        self.sprite_layers.append({"folder": str(folder), "name": name,
                                     "alpha": 1.0, "scale": 1.0})
        self.layers_list.insert("end", f"  {folder.name}/{name}")
        self._refresh_preview()

    def _remove_layer(self):
        sel = self.layers_list.curselection()
        if not sel:
            return
        idx = sel[0]
        self.layers_list.delete(idx)
        del self.sprite_layers[idx]
        self._refresh_preview()

    def _move_layer(self, delta: int):
        sel = self.layers_list.curselection()
        if not sel:
            return
        idx = sel[0]
        new = idx + delta
        if not 0 <= new < len(self.sprite_layers):
            return
        self.sprite_layers[idx], self.sprite_layers[new] = \
            self.sprite_layers[new], self.sprite_layers[idx]
        # Refresh list
        items = list(self.layers_list.get(0, "end"))
        items[idx], items[new] = items[new], items[idx]
        self.layers_list.delete(0, "end")
        for it in items:
            self.layers_list.insert("end", it)
        self.layers_list.selection_set(new)
        self._refresh_preview()

    def _play_layers(self):
        """Animação: pisca cada layer em sequência por 200ms."""
        if not self.sprite_layers:
            return
        self.status_var.set("▶ Playing layer sequence…")
        for i in range(len(self.sprite_layers)):
            self.after(i * 200, lambda ii=i: self._show_single_layer(ii))
        self.after(len(self.sprite_layers) * 200 + 100, self._refresh_preview)

    def _show_single_layer(self, idx: int):
        layer = self.sprite_layers[idx]
        try:
            img = Image.open(Path(layer["folder"]) / f"{layer['name']}.png").convert("RGBA")
            self._set_preview(img)
        except Exception:
            pass

    # ─── Effects dialog ────────────────────────────────────────────────
    def _add_effect_dialog(self):
        dlg = tk.Toplevel(self)
        dlg.title("Adicionar Effect")
        dlg.geometry("420x320")
        dlg.configure(bg="#1a0824")

        type_var = tk.StringVar(value="CUSTOM_EFFECT")
        eff_var = tk.StringVar(value=MOD_EFFECTS[0] if MOD_EFFECTS else "")
        dur_var = tk.IntVar(value=100)
        mag_var = tk.DoubleVar(value=1.0)

        ttk.Label(dlg, text="Tipo:", style="Header.TLabel").pack(anchor="w", padx=10, pady=4)
        ttk.Combobox(dlg, textvariable=type_var, values=EFFECT_TYPES,
                     state="readonly").pack(fill="x", padx=10)

        ttk.Label(dlg, text="Effect ID (apenas pra CUSTOM_EFFECT/EFFECT):",
                  style="Header.TLabel").pack(anchor="w", padx=10, pady=(8, 2))
        eff_cb = ttk.Combobox(dlg, textvariable=eff_var, values=MOD_EFFECTS)
        eff_cb.pack(fill="x", padx=10)

        ttk.Label(dlg, text="Duration (ticks):",
                  style="Header.TLabel").pack(anchor="w", padx=10, pady=(8, 2))
        ttk.Entry(dlg, textvariable=dur_var).pack(fill="x", padx=10)

        ttk.Label(dlg, text="Magnitude:",
                  style="Header.TLabel").pack(anchor="w", padx=10, pady=(8, 2))
        ttk.Entry(dlg, textvariable=mag_var).pack(fill="x", padx=10)

        def confirm():
            e = {"type": type_var.get()}
            if e["type"] in ("CUSTOM_EFFECT", "EFFECT"):
                e["effect"] = eff_var.get()
            if dur_var.get() > 0:
                e["duration"] = int(dur_var.get())
            if mag_var.get() != 0:
                e["magnitude"] = float(mag_var.get())
            self.effects.append(e)
            self._refresh_effects()
            dlg.destroy()

        ttk.Button(dlg, text="Adicionar", style="Save.TButton",
                   command=confirm).pack(pady=14)

    def _refresh_effects(self):
        if not hasattr(self, "effects_list"):
            return
        self.effects_list.delete(0, "end")
        for e in self.effects:
            txt = e["type"]
            if "effect" in e:
                txt += f"  → {e['effect']}"
            if "duration" in e:
                txt += f"  ({e['duration']}t)"
            if "magnitude" in e:
                txt += f"  ×{e['magnitude']}"
            self.effects_list.insert("end", txt)

    def _remove_selected_effect(self):
        sel = self.effects_list.curselection()
        if not sel:
            return
        del self.effects[sel[0]]
        self._refresh_effects()

    def _clear_effects(self):
        self.effects.clear()
        self._refresh_effects()

    # ─── Preview composer ──────────────────────────────────────────────
    def _refresh_preview(self):
        # Compose layered image 16×16 with school primary color tint background.
        bg = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(bg)
        # Parchment base (subtle)
        for y in range(2, 14):
            for x in range(2, 14):
                v = (math.sin(x * 0.6) + math.cos(y * 0.7)) * 4
                base = (212, 180, 130)
                d.point((x, y), fill=(int(base[0] + v),
                                       int(base[1] + v),
                                       int(base[2] + v), 255))
        # Top/bottom rolls
        for x in range(2, 14):
            d.point((x, 1), fill=(80, 50, 20, 255))
            d.point((x, 14), fill=(80, 50, 20, 255))
            for y in [2, 3, 12, 13]:
                d.point((x, y), fill=(140, 100, 50, 255))

        # Compose each layer scaled to fit 16×16
        for layer in self.sprite_layers:
            try:
                p = Path(layer["folder"]) / f"{layer['name']}.png"
                lyr = Image.open(p).convert("RGBA")
                # Downscale source (likely 32×32) to 12×12 to fit inside scroll
                lyr = lyr.resize((12, 12), Image.LANCZOS)
                # Position centered
                bg.paste(lyr, (2, 2), lyr)
            except Exception:
                pass

        # School color seal at bottom
        try:
            rgb = hex_to_rgb(self.color_primary.get())
            cx, cy = 8, 11
            for x in range(cx - 2, cx + 3):
                for y in range(cy - 1, cy + 2):
                    dx, dy = x - cx, y - cy
                    if dx * dx + dy * dy <= 4:
                        bg.putpixel((x, y), (*rgb, 255))
        except Exception:
            pass

        self._set_preview(bg)

    def _set_preview(self, img16: Image.Image):
        # r158 fix: build_form chama _set_school antes do composer existir
        if not hasattr(self, "preview_canvas"):
            return
        img_up = img16.resize((256, 256), Image.NEAREST)
        self._preview_photo = ImageTk.PhotoImage(img_up)
        self.preview_canvas.delete("all")
        self.preview_canvas.create_image(128, 128, image=self._preview_photo)

    # ─── Save ──────────────────────────────────────────────────────────
    def save_spell(self):
        # Validate id
        sid = self.var_id.get().strip() or slugify(self.var_name.get())
        if not sid.startswith("factory_"):
            sid = "factory_" + sid
        # Make sure no collision
        target_json = SPELLS_DIR / f"{sid}.json"
        if target_json.exists():
            if not messagebox.askyesno("Overwrite?", f"{sid}.json já existe. Sobrescrever?"):
                return

        # Build JSON
        spell = {
            "id": sid,
            "name": self.var_name.get(),
            "school": self.var_school.get(),
            "rarity": self.var_rarity.get(),
            "mana": int(self.var_mana.get()),
            "cooldown": int(self.var_cooldown.get()),
            "damage": int(self.var_damage.get()),
            "range": int(self.var_range.get()),
            "type": self.var_type.get(),
            "lore": self.var_lore.get(),
            "behavior": self._build_behavior(),
            "vfx": self._build_vfx(),
            "effects": list(self.effects),
            "element": self.var_school.get(),
            "category": self.var_category.get(),
        }

        # Compose final 16×16 texture
        try:
            tex = self._compose_texture()
            tex_path = SPELL_TEX_DIR / f"factory_spell_scroll_{sid.replace('factory_', '')}.png"
            tex_path.parent.mkdir(parents=True, exist_ok=True)
            tex.save(tex_path)
        except Exception as e:
            messagebox.showerror("Erro textura", str(e))
            return

        # Write JSON
        target_json.parent.mkdir(parents=True, exist_ok=True)
        target_json.write_text(json.dumps(spell, indent=2, ensure_ascii=False), encoding="utf-8")

        # Write model JSON
        model = {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"liberthia:item/factory_spell_scroll_{sid.replace('factory_', '')}"}
        }
        model_path = SPELL_MODEL_DIR / f"factory_spell_scroll_{sid.replace('factory_', '')}.json"
        model_path.parent.mkdir(parents=True, exist_ok=True)
        model_path.write_text(json.dumps(model, indent=2), encoding="utf-8")

        # Update FactorySpellIndex.java
        self._update_java_index(sid)

        # Update lang
        self._update_lang(sid, self.var_name.get())

        # Update master factory_spell_scroll.json overrides
        self._update_master_overrides(sid)

        self.status_var.set(f"✓ Salvo: {sid} — JSON + textura + model + index + lang atualizados.")
        messagebox.showinfo("Salvo!", f"Feitiço {sid} criado.\n\nRebuild o mod (gradlew build) para ver in-game.")

    def _build_behavior(self):
        t = self.var_type.get()
        b = {}
        # Defaults per type
        if t == "PROJECTILE":
            b = {"speed": 1.6, "lifetime_ticks": 80, "pierce": False, "aoe_radius": 2.0}
        elif t == "BEAM" or t == "CHANNELED_BEAM":
            b = {"beamRange": float(self.var_range.get()), "beamMaxTicks": 30}
        elif t in ("AOE_BURST", "NOVA", "EXPLOSION"):
            b = {"radius": max(3.0, self.var_range.get() / 4), "aoe_radius": max(3.0, self.var_range.get() / 4)}
        elif t == "RAIN":
            b = {"radius": 8.0, "rain_strikes": 12, "rain_duration_ticks": 100}
        elif t == "DASH":
            b = {"dashDistance": 10.0, "speed": 2.5}
        elif t == "CONE":
            b = {"radius": 8.0, "coneAngle": 60.0}
        elif t == "AURA":
            b = {"radius": 6.0, "aoe_radius": 6.0, "lifetime_ticks": 200}
        return b

    def _build_vfx(self):
        # Load particle preset and merge with color
        preset_name = self.var_particle_preset.get()
        preset_file = PARTICLE_DIR / f"{preset_name}.json"
        preset = {}
        if preset_file.exists():
            preset = json.loads(preset_file.read_text(encoding="utf-8"))
        vfx = {
            "color_primary": self.color_primary.get(),
            "color_secondary": self.color_secondary.get(),
            "trail_density": preset.get("trail_density", 10),
            "trail_size": preset.get("trail_size", 1.2),
            "impact_scale": preset.get("impact_scale", 1.0),
            "impact_particles": preset.get("impact_particles", 60),
            "screen_shake": preset.get("screen_shake", 0.3),
            "impact_light": preset.get("impact_light", 10),
        }
        return vfx

    def _compose_texture(self) -> Image.Image:
        """Renderiza a textura final 16×16 com layers + bg parchment + seal."""
        # Match preview compose but on a fresh image
        bg = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(bg)
        # Parchment
        for y in range(2, 14):
            for x in range(2, 14):
                v = (math.sin(x * 0.6) + math.cos(y * 0.7)) * 4
                base = (212, 180, 130)
                d.point((x, y), fill=(int(base[0] + v),
                                       int(base[1] + v),
                                       int(base[2] + v), 255))
        for x in range(2, 14):
            d.point((x, 1), fill=(80, 50, 20, 255))
            d.point((x, 14), fill=(80, 50, 20, 255))
            for y in [2, 3, 12, 13]:
                d.point((x, y), fill=(140, 100, 50, 255))
        # Layers
        for layer in self.sprite_layers:
            try:
                p = Path(layer["folder"]) / f"{layer['name']}.png"
                lyr = Image.open(p).convert("RGBA")
                lyr = lyr.resize((12, 12), Image.LANCZOS)
                bg.paste(lyr, (2, 2), lyr)
            except Exception:
                pass
        # Seal
        try:
            rgb = hex_to_rgb(self.color_primary.get())
            cx, cy = 8, 11
            for x in range(cx - 2, cx + 3):
                for y in range(cy - 1, cy + 2):
                    dx, dy = x - cx, y - cy
                    if dx * dx + dy * dy <= 4:
                        bg.putpixel((x, y), (*rgb, 255))
        except Exception:
            pass
        return bg

    def _update_java_index(self, spell_id: str):
        if not JAVA_INDEX.exists():
            return
        txt = JAVA_INDEX.read_text(encoding="utf-8")
        if f'IDX.put("{spell_id}"' in txt:
            return  # already there
        # Parse existing entries: read all factory_*.json IDs, sort, rewrite
        existing_ids = sorted([
            json.loads(j.read_text(encoding="utf-8"))["id"]
            for j in SPELLS_DIR.glob("*.json")
            if "KITCHEN_SINK" not in j.name
        ])
        # Build new block
        puts = "\n".join(f'        IDX.put("{s}", {i+1});' for i, s in enumerate(existing_ids))
        # Replace between "static {" and "}"
        new_txt = re.sub(
            r'(static\s*\{[^}]*?)(// Generated by gen_spell_textures\.py — do not edit manually\n)(.*?)(\s*\})',
            lambda m: m.group(1) + m.group(2) + puts + "\n" + m.group(4),
            txt, count=1, flags=re.DOTALL
        )
        JAVA_INDEX.write_text(new_txt, encoding="utf-8")

    def _update_lang(self, spell_id: str, display_name: str):
        for path in [LANG_EN, LANG_PT]:
            if not path.exists():
                continue
            try:
                d = json.loads(path.read_text(encoding="utf-8"))
            except Exception:
                continue
            short_id = spell_id.replace("factory_", "")
            key = f"item.liberthia.factory_spell_scroll.{short_id}"
            d[key] = display_name
            path.write_text(json.dumps(d, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    def _update_master_overrides(self, spell_id: str):
        """Atualiza factory_spell_scroll.json com novo override entry."""
        master = SPELL_MODEL_DIR / "factory_spell_scroll.json"
        if not master.exists():
            return
        try:
            data = json.loads(master.read_text(encoding="utf-8"))
        except Exception:
            return
        # Re-read all spells to rebuild overrides
        existing = sorted([
            json.loads(j.read_text(encoding="utf-8"))["id"]
            for j in SPELLS_DIR.glob("*.json")
            if "KITCHEN_SINK" not in j.name
        ])
        overrides = []
        for i, sid in enumerate(existing):
            overrides.append({
                "predicate": {"liberthia:spell_index": float(i + 1)},
                "model": f"liberthia:item/factory_spell_scroll_{sid.replace('factory_', '')}"
            })
        data["overrides"] = overrides
        master.write_text(json.dumps(data, indent=2), encoding="utf-8")

    # ─── Reset / Load ──────────────────────────────────────────────────
    def reset(self):
        if not messagebox.askyesno("Reset?", "Limpar todos os campos?"):
            return
        self.var_id.set("")
        self.var_name.set("Meu Feitiço")
        self.var_lore.set("Um feitiço customizado.")
        self.var_rarity.set("UNCOMMON")
        self.var_type.set("PROJECTILE")
        self.var_category.set("PROJECTILE")
        self.var_mana.set(40)
        self.var_cooldown.set(100)
        self.var_damage.set(10)
        self.var_range.set(24)
        self.color_primary.set("#ff5500")
        self.color_secondary.set("#ffaa00")
        self._set_school("FIRE")
        self.effects.clear()
        self._refresh_effects()
        self.sprite_layers.clear()
        self.layers_list.delete(0, "end")
        self._refresh_preview()
        self.status_var.set("Reset.")

    def load_spell(self):
        path = filedialog.askopenfilename(
            initialdir=str(SPELLS_DIR),
            title="Carregar feitiço",
            filetypes=[("Spell JSON", "*.json")]
        )
        if not path:
            return
        try:
            d = json.loads(Path(path).read_text(encoding="utf-8"))
        except Exception as e:
            messagebox.showerror("Erro", f"Falha ao ler: {e}")
            return
        self.var_id.set(d.get("id", "").replace("factory_", ""))
        self.var_name.set(d.get("name", ""))
        self.var_lore.set(d.get("lore", ""))
        self.var_rarity.set(d.get("rarity", "UNCOMMON"))
        self.var_type.set(d.get("type", "PROJECTILE"))
        self.var_category.set(d.get("category", "PROJECTILE"))
        self.var_mana.set(d.get("mana", 40))
        self.var_cooldown.set(d.get("cooldown", 100))
        self.var_damage.set(d.get("damage", 10))
        self.var_range.set(d.get("range", 24))
        vfx = d.get("vfx", {})
        self.color_primary.set(vfx.get("color_primary", "#ff5500"))
        self.color_secondary.set(vfx.get("color_secondary", "#ffaa00"))
        self._set_school(d.get("school", "FIRE"))
        self.effects = list(d.get("effects", []))
        self._refresh_effects()
        self.sprite_layers.clear()
        self.layers_list.delete(0, "end")
        self._refresh_preview()
        self.status_var.set(f"Carregado: {d.get('id', '?')}")


# ─── Entry point ──────────────────────────────────────────────────────────
if __name__ == "__main__":
    app = SpellDesigner()
    app.mainloop()
