# GLTF → Minecraft Block Model — Workflow Docs

> **Problema:** quando você importa um `.gltf` (gerado por AI, Meshy, Tripo, etc.) no Blockbench,
> aparecem **milhares de cubos minúsculos** porque cada triângulo da mesh vira uma face individual.
> Resultado: modelo ingovernável + Minecraft trava (limite ~384 elements por bloco).
>
> **Solução:** `build/gltf_to_minecraft.py` — voxeliza + greedy meshing automático.

## Como funciona o script

```
GLTF (mesh com 10k+ tris)
    ↓ trimesh.load
Mesh consolidada
    ↓ voxelized(pitch=X)
Grid 3D booleano de voxels ocupados
    ↓ greedy meshing 3D
Lista de boxes (X,Y,Z,W,H,D) — agrupa voxels contíguos
    ↓ converte coords 0..res → 0..16 Minecraft
JSON Block Model (poucos elements)
```

**Resultado típico:** modelo com 5-30 elementos (em vez de centenas).
No teste com cilindro (28 tris): **348 voxels → 5 boxes (98.6% redução)**.

## Uso rápido

```bash
# 1. Instala deps (uma vez)
pip install trimesh numpy pillow

# 2. Roda o conversor
cd C:/Users/T-GAMER/Desktop/liberthia_mod
python build/gltf_to_minecraft.py meu_modelo.gltf --name minha_vela --resolution 16
```

**Parâmetros:**

| Flag | Default | O que faz |
|---|---|---|
| `input` | obrigatório | Caminho do .gltf/.glb/.obj |
| `--resolution` | 16 | Voxel grid size (8/16/24/32). Maior = mais detalhe + mais cubos |
| `--name` | nome do arquivo | Nome interno do bloco no mod |
| `--mod-id` | liberthia | Mod ID pros paths |
| `--out-dir` | `../src/main/resources` | Root assets |

**Saída automática** (4 arquivos):
```
assets/liberthia/
├── models/block/<name>.json       ← modelo 3D do bloco
├── models/item/<name>.json        ← item parented
├── blockstates/<name>.json        ← blockstate single variant
└── textures/block/<name>.png      ← textura placeholder flat-color
```

## Após gerar — registrar no Java

O script só gera os assets. Você precisa registrar o bloco no código Java:

```java
// ModBlocks.java
public static final RegistryObject<Block> MINHA_VELA = BLOCKS.register("minha_vela",
        () -> new Block(BlockBehaviour.Properties.of()
                .strength(2.0F, 4.0F)
                .sound(SoundType.WOOL)
                .noOcclusion()));     // IMPORTANTE: noOcclusion pra não cortar faces

// ModItems.java
public static final RegistryObject<Item> MINHA_VELA_ITEM = ITEMS.register("minha_vela",
        () -> new BlockItem(ModBlocks.MINHA_VELA.get(),
                new Item.Properties()));
```

E lang entries:
```json
// pt_br.json
"block.liberthia.minha_vela": "Minha Vela",
"item.liberthia.minha_vela": "Minha Vela"
```

## Tabela: como escolher --resolution

| Resolution | Voxels max | Elements típicos | Usa quando... |
|---|---|---|---|
| 8 | 512 | 5-15 | Modelos simples (lápide, cubo decorativo) |
| **16** | 4096 | 10-50 | Default — boa qualidade pra blocks/items |
| 24 | 13824 | 50-150 | Detalhes finos (estátua, ferramenta) |
| 32 | 32768 | 200-400+ | Risco de bater limite Minecraft (384) |

**Regra prática:** começa em 16. Se ficar muito blocky, sobe pra 24. Se passar de 300 elements, desce pra 12.

## Quando o GLTF tem MUITAS partes coloridas

O script atual gera **textura flat single-color** baseada no material principal do GLTF. Se você quer
**múltiplas cores** preservadas (ex: vela com cera roxa + pavio preto), tem 2 opções:

### Opção 1 (manual): edita a textura PNG gerada
1. Roda o script normalmente
2. Abre `assets/liberthia/textures/block/<name>.png` no GIMP/Photoshop
3. Pinta as regiões com as cores que quer
4. Salva — recarrega o jogo

### Opção 2 (futura): mode multi-cor
TODO no script — preservar cores per-voxel e gerar atlas com regiões UV separadas.

## Template manual (sem GLTF) — vela 3D bonita

Pra casos simples (vela, lápide, decoração com forma básica), é **mais rápido** escrever o JSON à mão
do que passar por GLTF→voxelize. Use este como base:

📄 `src/main/resources/assets/liberthia/models/block/candle_3d_template.json`

Contém:
- Base derretida (6×1×6)
- Coluna de cera (4×11×4)
- Topo wider melt (6×1×6)
- Pavio (1×2×1)
- Chama X-cross billboard (2 planos perpendiculares)

**6 elementos só**, perfeito Minecraft-friendly.

## Troubleshooting

| Sintoma | Causa | Fix |
|---|---|---|
| `ModuleNotFoundError: trimesh` | Não instalou deps | `pip install trimesh numpy pillow` |
| "voxels ocupados: 0/4096" | Mesh inválido ou GLTF corrompido | Tenta abrir no Blockbench primeiro pra ver se mesh tá OK |
| Texturas brancas in-game | Texture path errado | Confirma `assets/<mod_id>/textures/block/<name>.png` existe |
| Block aparece invisível | `noOcclusion()` faltando OU blockstate JSON path errado | Adiciona `.noOcclusion()` no Properties + checa registry name |
| > 384 elements de erro Minecraft | Resolução muito alta | Roda com `--resolution 12` ou menos |
| Forma diferente do GLTF original | Voxelização perde curvas | Aumenta resolution. Pra curvas suaves, modelagem manual é melhor |

## Pipeline completo (com gerador AI)

```
[Meshy/Tripo/Sloyd]
    ↓ generate 3D model from text prompt "voxel low-poly candle"
modelo.glb
    ↓ python build/gltf_to_minecraft.py modelo.glb --name my_candle --resolution 16
4 arquivos JSON+PNG gerados em assets/
    ↓ edita PNG pra colorir (opcional)
    ↓ adiciona block ao ModBlocks.java + lang
./gradlew build
JAR pronto com bloco 3D in-game
```

## Limitações conhecidas

- **Sem rotação automática**: o GLTF pode ter orientação Y-up vs Z-up. Se aparecer deitado in-game,
  abre o block JSON e adiciona `"rotation": {"angle": 90, "axis": "x", "origin": [8, 8, 8]}` em cada element
- **Sem UV mapping inteligente**: todas faces usam UV 0,0-16,16 (textura tile). Pra textura específica
  por face, edita o JSON manualmente
- **Sem suporte a animação**: GLTF animations são ignoradas. Pra blocks/items animados em MC, precisa
  usar `.mcmeta` ou tile entities
- **Sem suporte a transparência per-face**: tudo opaco. Pra glass-like, marca `"tintindex": 0` no JSON e
  use `RenderType.translucent()` no Java

## Próximos features planejados

- [ ] Multi-color: preservar cor de cada voxel e gerar atlas
- [ ] Auto-rotate: detecta orientação canônica do mesh
- [ ] Greedy meshing 4D (variante mais agressiva pra mesh maiores)
- [ ] Suporte a GLTF com bones (entity models, modelo Java auto-gerado)
