#!/usr/bin/env python3
"""
Gera `tools/wiki-update.json` — entradas completas da Feature Wiki
para TODOS os items/blocos do mod, com texto markdown rico baseado em:

  - ModItems.java + ModBlocks.java       (todos IDs registrados)
  - assets/liberthia/lang/pt_br.json     (display name + descrição curta)
  - data/liberthia/recipes/*.json        (recipes shaped/shapeless)
  - manual/ManualContent.java            (páginas in-game do Liberthia Manual)
  - gradle.properties                    (versão atual = addedInVersion)

Estratégia:
  1. Pra cada item/bloco, monta uma `WikiEntry` com slug, título, categoria,
     summary, contentMd (markdown completo) e recipeJson.
  2. ContentMd inclui:
       - Descrição curta do .lang
       - Como usar (extraído das páginas do ManualContent que tem `itemIcon=<id>`)
       - Recipe ASCII rendering
       - Categoria + tags
  3. Items SEM página no manual ainda recebem stub razoável (nome + descrição
     + recipe se houver).

O JSON resultante é importado via "Importar wiki JSON" no painel admin.
Backend faz UPSERT por slug — items já existentes são atualizados, novos
items aparecem automaticamente. Items removidos do JSON NÃO são deletados
(preserva histórico). Isso significa que TODO push de update do mod pode
disparar esse generator + o bulk-import pra manter a wiki sincronizada.

Uso:
  python tools/generate-wiki-update.py
  python tools/generate-wiki-update.py --version 0.1.15
  python tools/generate-wiki-update.py --output tools/wiki-update.json
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
LANG_PT = ROOT / "src/main/resources/assets/liberthia/lang/pt_br.json"
LANG_EN = ROOT / "src/main/resources/assets/liberthia/lang/en_us.json"
MANUAL = ROOT / "src/main/java/br/com/murilo/liberthia/manual/ManualContent.java"
GRADLE_PROPS = ROOT / "gradle.properties"


# ============================================================
# Utilities
# ============================================================

def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def read_mod_version() -> str:
    if not GRADLE_PROPS.exists():
        return "0.0.0"
    for line in GRADLE_PROPS.read_text(encoding="utf-8").splitlines():
        if line.startswith("mod_version="):
            return line.split("=", 1)[1].strip()
    return "0.0.0"


def slugify(s: str) -> str:
    """Converte ID/título em slug — bate com FeatureWikiEntry.slugify() no backend."""
    s = s.strip().lower()
    s = re.sub(r"[^a-z0-9]+", "-", s)
    s = re.sub(r"^-+|-+$", "", s)
    return s


def humanize_id(item_id: str) -> str:
    """`liberthia:matter_cure` → `Matter Cure`."""
    name = item_id.split(":", 1)[-1]
    return " ".join(w.capitalize() for w in name.split("_"))


def strip_minecraft_codes(text: str) -> str:
    """Remove §-codes do Minecraft (§5, §l, §r, etc) pra ficar markdown limpo."""
    return re.sub(r"§[0-9a-zA-Zk-or]", "", text)


# ============================================================
# Sources
# ============================================================

def extract_registered_ids(java_file: Path) -> list[dict]:
    """Extrai (const, id, classname) de ModItems.java / ModBlocks.java.

    Suporta 2 estilos de registro (igual generate-items-update.py):
      - `BLOCKS.register("name", () -> new FooBlock(...))`   namespaceado
      - `registerBlock("name", () -> new FooBlock(...))`     helper standalone
    """
    if not java_file.exists():
        return []
    text = java_file.read_text(encoding="utf-8", errors="ignore")
    pattern = re.compile(
        r'(?:RegistryObject<\w+>\s+)?(\w+)\s*=\s*(?:\w+\.)?register(?:Block)?\(\s*"([^"]+)"\s*,\s*\(\)\s*->\s*new\s+(?:[\w.]+\.)?(\w+)',
        re.MULTILINE,
    )
    out = []
    seen = set()
    for m in pattern.finditer(text):
        const_name, item_id, class_name = m.group(1), m.group(2), m.group(3)
        if item_id in seen:
            continue
        seen.add(item_id)
        out.append({"const": const_name, "id": item_id, "class": class_name})
    return out


def load_lang(path: Path) -> dict:
    if not path.exists():
        return {}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as e:
        print(f"WARN: falha lendo {path}: {e}", file=sys.stderr)
        return {}


def load_recipe(item_id: str) -> dict | None:
    """Procura recipe pelo nome do item (sem namespace)."""
    if ":" not in item_id:
        return None
    name = item_id.split(":", 1)[1]
    candidate = RECIPES / f"{name}.json"
    if not candidate.exists():
        # Algumas recipes têm sufixos: name + "_recipe", "_alt", etc.
        # Tenta um wildcard simples.
        alt = list(RECIPES.glob(f"{name}*.json"))
        if alt:
            candidate = alt[0]
        else:
            return None
    try:
        return json.loads(candidate.read_text(encoding="utf-8"))
    except Exception:
        return None


# ============================================================
# Manual content extraction
# ============================================================

# Regex pra detectar Pages dentro de Chapters.
# Exemplo de page no source:
#   new Page("Frasco de Sangue", "§7Coletado..."),
# Ou com itemIcon (3-arg):
#   new Page("Auto Farmer", "§7Coloca no chão...", "liberthia:auto_farmer"),

PAGE_ANY_RE = re.compile(
    r'new\s+Page\s*\(\s*'
    r'"((?:[^"\\]|\\.)*)"'        # title (group 1)
    r'\s*,\s*'
    r'"((?:[^"\\]|\\.)*(?:"\s*\+\s*"(?:[^"\\]|\\.)*)*)"'  # body (group 2) - allows string concat
    r'(?:\s*,\s*"((?:[^"\\]|\\.)*)")?'  # optional itemIcon (group 3)
    r'\s*\)',
    re.DOTALL,
)


def _undouble_java_string(s: str) -> str:
    """Lida com strings concatenadas Java ("foo" + "bar") e escapes."""
    # Remove `" + "` (com whitespace/newlines) entre fragmentos
    s = re.sub(r'"\s*\+\s*"', "", s)
    # Unescape básico
    s = s.replace('\\"', '"').replace('\\n', '\n').replace('\\\\', '\\')
    return s


def extract_manual_pages() -> dict[str, list[dict]]:
    """
    Lê ManualContent.java e retorna mapa {itemId → [pages]}.
    Pages sem itemIcon vão pro bucket especial '__no_icon__'.

    Estratégia: regex iterativa que pega TODA chamada `new Page(...)`.
    Tenta deduzir o chapter (heuristica simples — usa o capítulo cujo `new Chapter(`
    aparece mais perto antes da página).
    """
    if not MANUAL.exists():
        return {}
    src = MANUAL.read_text(encoding="utf-8")

    # Captura chapters pra inferir capítulo
    chapter_re = re.compile(r'new\s+Chapter\s*\(\s*"([^"]+)"', re.DOTALL)
    chapter_positions: list[tuple[int, str]] = []
    for m in chapter_re.finditer(src):
        chapter_positions.append((m.start(), strip_minecraft_codes(m.group(1))))

    def chapter_at(pos: int) -> str:
        last = "Geral"
        for cpos, cname in chapter_positions:
            if cpos > pos:
                break
            last = cname
        return last

    by_item: dict[str, list[dict]] = {}
    for m in PAGE_ANY_RE.finditer(src):
        title = strip_minecraft_codes(_undouble_java_string(m.group(1)))
        body = strip_minecraft_codes(_undouble_java_string(m.group(2)))
        icon = m.group(3) or ""
        icon = icon.strip()
        if not icon:
            key = "__no_icon__"
        else:
            # Normaliza com namespace
            if ":" not in icon:
                icon = f"liberthia:{icon}"
            key = icon
        ch = chapter_at(m.start())
        by_item.setdefault(key, []).append({
            "title": title.strip(),
            "body": body.strip(),
            "chapter": ch,
        })
    return by_item


# ============================================================
# Recipe rendering
# ============================================================

def _key_to_label(key_def: dict | list) -> str:
    """Converte {item: 'minecraft:iron_ingot'} ou {tag: 'forge:ingots/iron'} em label."""
    if isinstance(key_def, list):
        # Tag alternative — pega o primeiro
        return " ou ".join(_key_to_label(k) for k in key_def)
    if not isinstance(key_def, dict):
        return str(key_def)
    if "item" in key_def:
        return humanize_id(key_def["item"])
    if "tag" in key_def:
        return f"#{key_def['tag']}"
    return "?"


def render_recipe_md(recipe: dict | None) -> str:
    if not recipe:
        return ""
    rtype = recipe.get("type", "")
    if "crafting_shaped" in rtype:
        return _render_shaped(recipe)
    if "crafting_shapeless" in rtype:
        return _render_shapeless(recipe)
    if "smelting" in rtype or "blasting" in rtype or "smoking" in rtype:
        return _render_smelting(recipe, rtype)
    # Custom recipes do mod (ex: blood_pact_amulet pode ter type liberthia:ritual)
    return f"**Tipo de recipe customizado:** `{rtype}`\n\n```json\n{json.dumps(recipe, indent=2, ensure_ascii=False)}\n```"


def _render_shaped(recipe: dict) -> str:
    pattern = recipe.get("pattern", [])
    keys = recipe.get("key", {})
    result = recipe.get("result", {})
    result_item = result.get("item", "?")
    result_count = result.get("count", 1)
    # ASCII grid 3x3 com letras
    lines = []
    lines.append("### Receita (Crafting Mesa)\n")
    lines.append("```")
    for row in pattern:
        # cada char na linha, espaçado
        chars = []
        for ch in row.ljust(3):
            chars.append(ch if ch != " " else ".")
        lines.append(" " + " | ".join(chars))
    lines.append("```\n")
    lines.append("**Ingredientes:**\n")
    for letter, kd in keys.items():
        lines.append(f"- `{letter}` = {_key_to_label(kd)}")
    lines.append(f"\n**Resultado:** {humanize_id(result_item)} × {result_count}")
    return "\n".join(lines)


def _render_shapeless(recipe: dict) -> str:
    ingredients = recipe.get("ingredients", [])
    result = recipe.get("result", {})
    result_item = result.get("item", "?")
    result_count = result.get("count", 1)
    lines = ["### Receita (Shapeless)\n",
             "**Ingredientes (qualquer disposição):**\n"]
    for ing in ingredients:
        lines.append(f"- {_key_to_label(ing)}")
    lines.append(f"\n**Resultado:** {humanize_id(result_item)} × {result_count}")
    return "\n".join(lines)


def _render_smelting(recipe: dict, rtype: str) -> str:
    ing = recipe.get("ingredient", {})
    result_item = recipe.get("result", "?")
    if isinstance(result_item, dict):
        result_item = result_item.get("item", "?")
    cook_time = recipe.get("cookingtime", 200)
    xp = recipe.get("experience", 0)
    method = "Fornalha"
    if "blasting" in rtype:
        method = "Alto Forno (Blast Furnace)"
    elif "smoking" in rtype:
        method = "Defumador (Smoker)"
    return (f"### Receita ({method})\n\n"
            f"- **Ingrediente:** {_key_to_label(ing)}\n"
            f"- **Resultado:** {humanize_id(result_item)}\n"
            f"- **Tempo:** {cook_time} ticks ({cook_time / 20:.1f}s)\n"
            f"- **XP:** {xp}")


# ============================================================
# Category inference
# ============================================================

def detect_category(item_id: str, class_name: str, is_block: bool) -> str:
    """Mapeia pra FeatureWikiEntry.Category {ITEM, BLOCK, ARTIFACT, MECHANIC, MOB,
    RITUAL, TOOL, ARMOR, WEAPON, OTHER}."""
    name = item_id.split(":", 1)[-1].lower()
    cl = class_name.lower()
    if is_block:
        return "BLOCK"
    if any(s in name for s in ["sword", "axe", "pickaxe", "shovel", "hoe", "dagger", "bow", "shield", "staff"]):
        return "WEAPON" if any(s in name for s in ["sword", "dagger", "bow", "shield", "staff"]) else "TOOL"
    if any(s in name for s in ["helmet", "chestplate", "leggings", "boots"]):
        return "ARMOR"
    if any(s in name for s in ["pendant", "amulet", "charm", "orb", "totem", "ring", "necklace", "tome", "codex"]):
        return "ARTIFACT"
    if any(s in name for s in ["pedestal", "ritual", "altar"]):
        return "RITUAL"
    if any(s in cl for s in ["mob", "entity"]):
        return "MOB"
    return "ITEM"


# ============================================================
# Tag inference (free-text comma list)
# ============================================================

def infer_tags(item_id: str, class_name: str, is_block: bool, has_recipe: bool, has_manual: bool) -> str:
    name = item_id.split(":", 1)[-1].lower()
    tags = set()
    if "dark_matter" in name: tags.add("dark-matter")
    if "clear_matter" in name or "white_matter" in name: tags.add("clear-matter")
    if "yellow_matter" in name: tags.add("yellow-matter")
    if "blood" in name or "sanguine" in name: tags.add("blood")
    if "infect" in name or "scarred" in name or "corrupted" in name: tags.add("infection")
    if "order" in name or "holy" in name or "sanctify" in name: tags.add("order")
    if "ritual" in name or "pedestal" in name or "altar" in name: tags.add("ritual")
    if any(s in name for s in ["pill", "cure", "grenade", "vial", "potion", "flask"]): tags.add("consumable")
    if any(s in name for s in ["ore", "ingot", "shard", "fragment", "crystal"]): tags.add("material")
    if "upgrade" in name: tags.add("upgrade")
    if any(s in name for s in ["sword", "axe", "pickaxe", "bow", "dagger", "shield", "staff"]): tags.add("weapon")
    if any(s in name for s in ["helmet", "chestplate", "leggings", "boots", "armor"]): tags.add("armor")
    if is_block: tags.add("block")
    if has_recipe: tags.add("craftable")
    if has_manual: tags.add("documented")
    return ",".join(sorted(tags))


# ============================================================
# Wiki entry builder
# ============================================================

def build_content_md(
    item_id: str,
    display_name: str,
    desc_short: str,
    desc_long: str,
    manual_pages: list[dict],
    recipe: dict | None,
    category: str,
    is_block: bool,
) -> str:
    """Monta o markdown completo de uma entrada wiki."""
    parts: list[str] = []
    parts.append(f"# {display_name}\n")
    parts.append(f"> **ID:** `{item_id}` · **Tipo:** {'Bloco' if is_block else 'Item'} · **Categoria:** {category}\n")

    if desc_short:
        parts.append(f"## Resumo\n\n{desc_short}\n")

    if desc_long and desc_long != desc_short:
        parts.append(f"## Descrição completa\n\n{desc_long}\n")

    # Manual pages (descrição rica feita à mão, lore + uso)
    if manual_pages:
        parts.append(f"## Como utilizar\n")
        for pg in manual_pages:
            parts.append(f"### {pg['title']}")
            parts.append(f"*(do capítulo: {pg['chapter']})*\n")
            # Preserva quebras de linha — markdown precisa de \n duplo pra parágrafo
            body = pg['body'].replace('\n\n\n', '\n\n').strip()
            parts.append(body)
            parts.append("")  # blank line

    # Recipe
    recipe_md = render_recipe_md(recipe)
    if recipe_md:
        parts.append(recipe_md)
        parts.append("")

    # Footer
    parts.append("---")
    parts.append(f"\n**Comando de give:** `/give @p {item_id} 1`")

    return "\n".join(parts).strip() + "\n"


def make_wiki_entry(
    item_id: str,
    class_name: str,
    is_block: bool,
    lang_pt: dict,
    manual_index: dict,
    version: str,
) -> dict:
    namespace, name = item_id.split(":", 1)
    key_prefix = ("block" if is_block else "item") + ".liberthia." + name
    display_name = lang_pt.get(key_prefix) or humanize_id(item_id)
    desc_short = lang_pt.get(key_prefix + ".desc") or ""
    desc_long = lang_pt.get(key_prefix + ".desc.long") or lang_pt.get(key_prefix + ".lore") or desc_short

    # Recipe
    recipe = load_recipe(item_id)
    recipe_json_str = json.dumps(recipe, ensure_ascii=False) if recipe else ""

    # Manual pages que tem este item como icon
    manual_pages = manual_index.get(item_id, [])

    category = detect_category(item_id, class_name, is_block)
    tags = infer_tags(item_id, class_name, is_block, recipe is not None, len(manual_pages) > 0)

    content_md = build_content_md(
        item_id=item_id,
        display_name=display_name,
        desc_short=desc_short,
        desc_long=desc_long,
        manual_pages=manual_pages,
        recipe=recipe,
        category=category,
        is_block=is_block,
    )

    summary = desc_short or f"{display_name} — {category.lower()} do Liberthia."
    if len(summary) > 480:
        summary = summary[:477] + "..."

    return {
        "slug": slugify(name),
        "title": display_name,
        "category": category,
        "itemId": item_id,
        "summary": summary,
        "contentMd": content_md,
        "recipeJson": recipe_json_str,
        "addedInVersion": version,
        "tags": tags,
        "published": True,
    }


# ============================================================
# Main builder
# ============================================================

def build_wiki(version: str) -> dict:
    print(f"[wiki-gen] versão {version}")
    items = extract_registered_ids(MOD_ITEMS)
    blocks = extract_registered_ids(MOD_BLOCKS)
    print(f"[wiki-gen] {len(items)} items, {len(blocks)} blocks descobertos")

    lang_pt = load_lang(LANG_PT)
    print(f"[wiki-gen] pt_br.json: {len(lang_pt)} keys")

    manual_index = extract_manual_pages()
    print(f"[wiki-gen] ManualContent: {sum(len(v) for v in manual_index.values())} páginas em "
          f"{len(manual_index)} buckets ({len([k for k in manual_index if k != '__no_icon__'])} linkadas a items)")

    entries = []
    seen_slugs = set()

    # IMPORTANTE: processa blocos PRIMEIRO. Quando um BlockItem é registrado em
    # ModItems.java com o MESMO ID do bloco em ModBlocks.java (~110 casos), a
    # entrada BLOCK é semanticamente mais útil pro usuário (é uma coisa
    # colocável no mundo, não só um stack no inventário). Item gets skipped.
    block_ids = {b['id'] for b in blocks}
    for bl in blocks:
        item_id = f"liberthia:{bl['id']}"
        e = make_wiki_entry(item_id, bl['class'], True, lang_pt, manual_index, version)
        if e['slug'] in seen_slugs:
            continue
        seen_slugs.add(e['slug'])
        entries.append(e)

    # Items que NÃO têm um bloco com mesmo ID
    for it in items:
        if it['id'] in block_ids:
            continue  # já capturado pelo loop de blocos
        item_id = f"liberthia:{it['id']}"
        e = make_wiki_entry(item_id, it['class'], False, lang_pt, manual_index, version)
        if e['slug'] in seen_slugs:
            continue
        seen_slugs.add(e['slug'])
        entries.append(e)

    # MECHANIC entries — páginas do manual sem itemIcon viram entradas de
    # mecânica/lore (matérias, mutações compostas, F8, comandos admin, etc).
    no_icon_pages = manual_index.get("__no_icon__", [])
    grouped_by_chapter: dict[str, list[dict]] = {}
    for pg in no_icon_pages:
        grouped_by_chapter.setdefault(pg['chapter'], []).append(pg)

    for chapter, pages in grouped_by_chapter.items():
        slug = slugify("mechanic-" + chapter)
        if slug in seen_slugs:
            continue
        seen_slugs.add(slug)
        title = chapter
        body_parts = [f"# {title}\n", f"> Mecânica / lore do mod (capítulo do Liberthia Manual).\n"]
        for pg in pages:
            body_parts.append(f"## {pg['title']}\n")
            body_parts.append(pg['body'].strip())
            body_parts.append("")
        content = "\n".join(body_parts).strip() + "\n"
        summary = pages[0]['body'][:240].strip().replace('\n', ' ')
        if len(summary) >= 240:
            summary = summary[:237] + "..."
        entries.append({
            "slug": slug,
            "title": title,
            "category": "MECHANIC",
            "itemId": "",
            "summary": summary,
            "contentMd": content,
            "recipeJson": "",
            "addedInVersion": version,
            "tags": "mechanic,lore,manual",
            "published": True,
        })

    print(f"[wiki-gen] {len(entries)} wiki entries totais")
    return {
        "version": version,
        "generatedAt": now_iso(),
        "schema": "feature-wiki-v1",
        "entries": entries,
    }


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--version", help="Override versão (default: lê gradle.properties)")
    p.add_argument("--output", default=str(TOOLS / "wiki-update.json"),
                   help="Caminho do JSON de saída")
    args = p.parse_args()

    version = args.version or read_mod_version()
    out = build_wiki(version)

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"[wiki-gen] -> {output_path}  ({len(out['entries'])} entries)")


if __name__ == "__main__":
    main()
