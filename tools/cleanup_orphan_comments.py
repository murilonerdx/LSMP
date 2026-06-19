"""r144b: Limpa comentarios orfaos no ModCreativeTabs.java
(comentarios cuja secao ficou vazia depois da migracao para ABBADON tab)
"""
from pathlib import Path

FILE = Path("src/main/java/br/com/murilo/liberthia/registry/ModCreativeTabs.java")
src = FILE.read_text(encoding="utf-8")

# Lista de comentarios orfaos que deve sumir (linha inteira)
ORPHAN_COMMENTS = [
    "                        // --- Blood Fountain ---",
    "                        // --- Blood Ritual / Proliferation ---",
    "                        // --- Blood terrain variants ---",
    "                        // --- Blood Armor ---",
    "                        // --- A Mae (Fase 2) ---",
    "                        // --- A Mãe (Fase 2) ---",
    "                        // --- Ordem x Sangue (Fase 5) ---",
    "                        // --- Ordem × Sangue (Fase 5) ---",
    "                        // --- Armas & Magia (Fase 4) ---",
    "                        // --- Alquimia (Fase 3) ---",
    "                        // --- Culto do Sangue (Fase 1) ---",
    "                        // --- Spawn Eggs ---",
    "                        // --- Seringa (T5b) ---",
    "                        // --- T6: EvilCraft ports ---",
    "                        // --- Sanguine Ward (anti Blood Infection) ---",
    "                        // --- Ritual blocks ---",
    "                        // --- Blood staves + attacking blood blocks ---",
    "                        // --- Occultism ports ---",
    "                        // --- Sanguine Wood set ---",
    "                        // --- Blood Tree set ---",
    "                        // Blood",
]

removed = 0
for c in ORPHAN_COMMENTS:
    # Remove a linha inteira (comentario + newline)
    target = c + "\n"
    if target in src:
        src = src.replace(target, "")
        removed += 1
        print(f"  - removed: {c.strip()}")

# Recoloca alguns comentarios uteis para manter contexto
# T6 EvilCraft ports ainda tem PURGING_PENDANT (linha 238 antes do cleanup)
# nao precisamos recolocar nada

FILE.write_text(src, encoding="utf-8")
print(f"\nDone. {removed} orphan comments removed.")
