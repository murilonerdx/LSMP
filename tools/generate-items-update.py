#!/usr/bin/env python3
"""
Gera os 4 JSONs canônicos do mod a partir do estado atual:

  tools/beta-items-update.json    — todos os items registrados em ModItems.java
  tools/rewards-update.json       — template vazio (admin preenche manualmente)
  tools/changelog-update.json     — entry da versão atual (placeholder)
  tools/roadmap-update.json       — template vazio

Uso:
  python tools/generate-items-update.py                # gera os 4
  python tools/generate-items-update.py --only items   # só beta-items
  python tools/generate-items-update.py --version 0.1.12  # override versão

Esses JSONs são importados depois no painel admin via "Importar JSON".
"""
import argparse
import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).parent.parent
TOOLS = ROOT / "tools"
MOD_ITEMS = ROOT / "src/main/java/br/com/murilo/liberthia/registry/ModItems.java"
MOD_BLOCKS = ROOT / "src/main/java/br/com/murilo/liberthia/registry/ModBlocks.java"
RECIPES = ROOT / "src/main/resources/data/liberthia/recipes"
GRADLE_PROPS = ROOT / "gradle.properties"


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def read_mod_version() -> str:
    if not GRADLE_PROPS.exists():
        return "0.0.0"
    for line in GRADLE_PROPS.read_text(encoding="utf-8").splitlines():
        if line.startswith("mod_version="):
            return line.split("=", 1)[1].strip()
    return "0.0.0"


def extract_items_from_java(java_file: Path) -> list[dict]:
    """Extrai (constant, id, registry-class) de ModItems.java/ModBlocks.java.

    Suporta 2 estilos de registro:
      - `BLOCKS.register("name", () -> new FooBlock(...))`   (estilo namespaceado)
      - `registerBlock("name", () -> new FooBlock(...))`     (estilo helper standalone)
    """
    if not java_file.exists():
        return []
    text = java_file.read_text(encoding="utf-8", errors="ignore")
    pattern = re.compile(
        r'public static final RegistryObject<\w+>\s+(\w+)\s*=\s*(?:\w+\.)?register(?:Block)?\(\s*"([^"]+)"\s*,\s*\(\)\s*->\s*new\s+(?:[\w.]+\.)?(\w+)',
        re.MULTILINE,
    )
    out = []
    for m in pattern.finditer(text):
        const_name, item_id, class_name = m.group(1), m.group(2), m.group(3)
        out.append({"const": const_name, "id": item_id, "class": class_name})
    return out


def detect_kind(class_name: str, registry_id: str) -> str:
    """Heurística pra inferir kind a partir do nome da classe/id."""
    n = class_name.lower()
    if "block" in n or "blockitem" in n:
        return "BLOCK"
    # Items de armadura, ferramenta, espada, etc → ITEM
    if any(k in n for k in ["sword", "axe", "pickaxe", "shield", "bow", "staff", "tool", "armor",
                              "helmet", "chestplate", "leggings", "boots"]):
        return "ITEM"
    # Items lore (livros, relíquias, etc) → ARTIFACT
    if any(k in n for k in ["tome", "codex", "journal", "totem", "orb", "amulet", "charm", "sigil", "relic"]):
        return "ARTIFACT"
    return "ITEM"


def detect_category(item_id: str) -> str:
    """Categoria por prefixo do ID."""
    namespace, name = item_id.split(":", 1) if ":" in item_id else ("", item_id)
    if name.startswith("blood_"):
        return "blood"
    if name.startswith("sanguine_"):
        return "sanguine"
    if name.startswith("dark_matter_"):
        return "dark-matter"
    if name.startswith("clear_matter_"):
        return "clear-matter"
    if name.startswith("yellow_matter_"):
        return "yellow-matter"
    if name.startswith("white_matter_"):
        return "white-matter"
    if name.startswith("infection_") or name.startswith("scarred_") or name.startswith("corrupted_"):
        return "infection"
    if name.startswith("order_") or "holy" in name or "sanctify" in name:
        return "order"
    if name.endswith("_upgrade"):
        return "upgrade"
    if any(s in name for s in ["pickaxe", "axe", "sword", "shield", "bow", "staff", "dagger"]):
        return "weapon"
    if any(s in name for s in ["helmet", "chestplate", "leggings", "boots", "armor"]):
        return "armor"
    if any(s in name for s in ["pill", "cure", "grenade", "potion", "vial", "flask"]):
        return "consumable"
    if "ore" in name:
        return "ore"
    return "misc"


def find_recipe_for(item_id: str) -> str | None:
    """Carrega o JSON da recipe se existe pra esse item."""
    if not ":" in item_id:
        return None
    name = item_id.split(":", 1)[1]
    candidate = RECIPES / f"{name}.json"
    if candidate.exists():
        try:
            return candidate.read_text(encoding="utf-8")
        except Exception:
            return None
    return None


def humanize_id(item_id: str) -> str:
    """`liberthia:matter_cure` → `Matter Cure`."""
    name = item_id.split(":", 1)[-1]
    return " ".join(w.capitalize() for w in name.split("_"))


def build_beta_items(version: str) -> dict:
    """Constrói o JSON de beta-items a partir do estado atual do mod."""
    items_java = extract_items_from_java(MOD_ITEMS)
    blocks_java = extract_items_from_java(MOD_BLOCKS)

    items_out = []
    seen_ids = set()

    for entry in items_java:
        item_id = f"liberthia:{entry['id']}"
        if item_id in seen_ids:
            continue
        seen_ids.add(item_id)

        recipe = find_recipe_for(item_id)
        items_out.append({
            "name": humanize_id(item_id),
            "itemId": item_id,
            "kind": detect_kind(entry["class"], entry["id"]),
            "category": detect_category(entry["id"]),
            "description": f"(auto-gerado) {humanize_id(item_id)}. Editar descrição completa no admin.",
            "lore": "",
            "giveCommand": f"/give @p {item_id} 1",
            "propertiesJson": "",
            "effectsJson": "",
            "recipeJson": recipe if recipe else "",
            "imageUrl": "",
            "enabled": True,
        })

    # Blocos também viram items beta com kind=BLOCK
    for entry in blocks_java:
        item_id = f"liberthia:{entry['id']}"
        if item_id in seen_ids:
            continue
        seen_ids.add(item_id)
        recipe = find_recipe_for(item_id)
        items_out.append({
            "name": humanize_id(item_id),
            "itemId": item_id,
            "kind": "BLOCK",
            "category": detect_category(entry["id"]),
            "description": f"(auto-gerado) Bloco {humanize_id(item_id)}.",
            "lore": "",
            "giveCommand": f"/give @p {item_id} 1",
            "propertiesJson": "",
            "effectsJson": "",
            "recipeJson": recipe if recipe else "",
            "imageUrl": "",
            "enabled": True,
        })

    return {
        "version": version,
        "generatedAt": now_iso(),
        "items": items_out,
    }


def build_rewards_template(version: str) -> dict:
    return {
        "version": version,
        "generatedAt": now_iso(),
        "rewards": [
            {
                "name": "Yellow Matter Ingot x4",
                "description": "4 ingots de Yellow Matter — pra craftar armadura completa.",
                "imageUrl": "",
                "category": "item",
                "costPoints": 250,
                "giveCommand": "/give {player} liberthia:yellow_matter_ingot 4",
                "perTesterLimit": 0,
                "enabled": True,
            },
            {
                "name": "Singularity Core",
                "description": "Drop ultra-raro — recompensa de testers ativos.",
                "imageUrl": "",
                "category": "item",
                "costPoints": 1000,
                "giveCommand": "/give {player} liberthia:singularity_core 1",
                "perTesterLimit": 1,
                "enabled": True,
            },
        ],
    }


def build_changelog_template(version: str) -> dict:
    """
    Schema real do backend (ChangelogEntry):
      version, releaseDate, title, summary, itemsAdded, bugsFixed,
      buffs, debuffs, integrations, credits, notes, highlighted.
    """
    return {
        "version": version,
        "generatedAt": now_iso(),
        "entries": [
            {
                "version": version,
                "title": f"v{version} — Atualização inicial",
                "releaseDate": now_iso(),
                "summary": "Atualização inicial — preenche manualmente no admin.",
                "itemsAdded": "",
                "bugsFixed": "",
                "buffs": "",
                "debuffs": "",
                "integrations": "",
                "credits": "",
                "notes": "(edite no admin)",
                "highlighted": False,
            }
        ],
    }


def build_roadmap_template(version: str) -> dict:
    """
    Schema real do backend (RoadmapItem):
      title, description, category (IDEA|PLANNED|IN_DEV|NEXT|DONE|CANCELLED),
      emoji, tag, priority (int), targetVersion.
    """
    return {
        "version": version,
        "generatedAt": now_iso(),
        "items": [
            {
                "title": "Matter Analyzer GUI completa",
                "description": "Tabs internos: análise / curas / histórico de exposição",
                "category": "PLANNED",
                "emoji": "🔬",
                "tag": "tool",
                "priority": 10,
                "targetVersion": "0.2.0",
            }
        ],
    }


def write_json(path: Path, data: dict):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
    # ASCII-only print pra evitar UnicodeEncodeError em consoles cp1252 (Windows)
    print(f"OK  -> {path.relative_to(ROOT)}  ({len(json.dumps(data))} bytes)")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", choices=["items", "rewards", "changelog", "roadmap"],
                    help="Gera só um arquivo (default: todos)")
    ap.add_argument("--version", help="Override versão (default: lê gradle.properties)")
    args = ap.parse_args()

    version = args.version or read_mod_version()
    print(f"Generating for mod version: {version}")
    print(f"Output dir: {TOOLS}")
    print()

    if args.only in (None, "items"):
        write_json(TOOLS / "beta-items-update.json", build_beta_items(version))

    if args.only in (None, "rewards"):
        # Não sobreescreve se já existe — admin pode ter editado
        out = TOOLS / "rewards-update.json"
        if out.exists() and not args.only:
            print(f"SKIP {out.name} (já existe, use --only rewards pra forçar)")
        else:
            write_json(out, build_rewards_template(version))

    if args.only in (None, "changelog"):
        out = TOOLS / "changelog-update.json"
        if out.exists() and not args.only:
            print(f"SKIP {out.name} (já existe, use --only changelog pra forçar)")
        else:
            write_json(out, build_changelog_template(version))

    if args.only in (None, "roadmap"):
        out = TOOLS / "roadmap-update.json"
        if out.exists() and not args.only:
            print(f"SKIP {out.name} (já existe, use --only roadmap pra forçar)")
        else:
            write_json(out, build_roadmap_template(version))

    print()
    print("Done. Os JSONs estao prontos pra serem importados no admin.")
    print()
    print("No painel:")
    print("  /tester-admin -> Items beta -> Importar JSON -> tools/beta-items-update.json")
    print("  /tester-admin -> Rewards    -> Importar JSON -> tools/rewards-update.json")
    print("  /tester-admin -> Changelog  -> Importar JSON -> tools/changelog-update.json")
    print("  /tester-admin -> Roadmap    -> Importar JSON -> tools/roadmap-update.json")


if __name__ == "__main__":
    main()
