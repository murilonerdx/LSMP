import json, os

BASES = [
    "src/main/resources/data/liberthia/patchouli_books/liberthia_manual/en_us",
    "src/main/resources/assets/liberthia/patchouli_books/liberthia_manual/en_us",
]

def write(rel, obj):
    for b in BASES:
        p = os.path.join(b, rel)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "w", encoding="utf-8") as f:
            json.dump(obj, f, ensure_ascii=False, indent=2)

def txt(title, text): return {"type": "patchouli:text", "title": title, "text": text}
def txt2(text): return {"type": "patchouli:text", "text": text}
def craft(recipe, text): return {"type": "patchouli:crafting", "recipe": "liberthia:" + recipe, "text": text}
def spot(item, text): return {"type": "patchouli:spotlight", "item": "liberthia:" + item, "text": text}
def entry(rel, name, icon, category, sortnum, pages):
    write("entries/" + rel + ".json", {"name": name, "icon": "liberthia:" + icon, "category": "liberthia:" + category, "sortnum": sortnum, "pages": pages})

# ───────── CATEGORIAS NOVAS ─────────
write("categories/tech.json", {
    "name": "Tecnologia (Liberthia Tech)",
    "description": "Linha de tecnologia estilo Powah/Mekanism: energia (FE), geradores, máquinas, ferramentas e armaduras energizadas. Aba criativa própria.",
    "icon": "liberthia:matter_reactor", "sortnum": 20})
write("categories/antimagic.json", {
    "name": "Tecno-Arcano (Anti-Magia)",
    "description": "Tecnologia que contém magos: armadura e escudo que aguentam magia, selos que suprimem mana e punem conjuradores. Tudo com matéria escura.",
    "icon": "liberthia:warded_helmet", "sortnum": 21})

# ───────── TECNOLOGIA ─────────
entry("tech/tech_overview", "Liberthia Tech", "steel_ingot", "tech", 1, [
    txt("Energia (FE)", "A linha $(l)Liberthia Tech$() roda em $(item)Forge Energy (FE)$() — estilo Powah/Mekanism.\n\n$(li)$(item)Geradores$() produzem FE\n$(li)$(item)Células$() armazenam\n$(li)$(item)Máquinas/Ferramentas$() consomem\n\nLigue tudo com $(item)cabos de energia$() ou por adjacência. Procure a aba criativa $(l)Liberthia Tech$()."),
    txt("Como usar qualquer bloco", "$(item)Clique direito$() em qualquer bloco tech abre a $(l)GUI$():\n\n$(li)Barra de energia (passe o mouse = FE exato)\n$(li)Slots de entrada/saída ou combustível\n$(li)Barra de progresso/queima\n\nGeradores e máquinas precisam de FE — ligue-os à rede pra funcionar."),
])
entry("tech/tech_energy", "Energia & Geradores", "matter_reactor", "tech", 2, [
    txt("Geradores", "$(li)$(item)Painel Solar$(): FE de dia, a céu aberto\n$(li)$(item)Gerador Térmico$(): lava adjacente\n$(li)$(item)Furnator$(): coloque combustível (carvão/madeira/blaze)\n$(li)$(item)Magmator$(): balde de lava (devolve o balde)\n$(li)$(item)Reator de Matéria Escura$(): queima matéria escura — orbe flutuante girando"),
    txt("Armazenar & Distribuir", "$(item)Células de Energia$() — Básica (1M), Avançada (4M), Ultimate (16M FE) — guardam e empurram FE pela rede.\n\n$(item)Carregador$(): põe um item FE no slot pra encher, e ainda $(item)carrega o inventário$() de quem chega perto (raio 5)."),
    craft("matter_reactor", "$(item)Reator de Matéria Escura$(): o gerador supremo — abastece toda a rede queimando matéria escura."),
])
entry("tech/tech_machines", "Máquinas", "crusher", "tech", 3, [
    txt("Processamento", "Todas têm GUI: $(item)entrada à esquerda$(), $(item)saída à direita$(), consomem FE.\n\n$(li)$(item)Triturador$(): minério → 2 pós (ore doubling)\n$(li)$(item)Fundidor Energizado$(): funde qualquer coisa\n$(li)$(item)Compressor$(): lingote → placa\n$(li)$(item)Forja de Liga$(): ferro → aço\n$(li)$(item)Serraria$(): tronco → 6 tábuas"),
    craft("crusher", "$(item)Triturador$() — dobre seus minérios. Ligue à rede FE."),
])
entry("tech/tech_tools", "Ferramentas & Armaduras", "energized_pickaxe", "tech", 4, [
    txt("Ferramentas Energizadas", "Picareta, Machado, Pá, Espada, Furadeira e $(item)Paxel$() movidas a FE.\n\n$(good)Carregadas$(): mineram +rápido e $(l)SEM gastar durabilidade$().\n$(warn)Sem FE$(): viram netherite normal.\n\nRecarregue num $(item)Carregador$() ou $(item)Reator$(). Barra verde = energia."),
    txt("Armadura & Utilidade", "$(li)$(item)Power Armor$() (4 peças): absorve dano gastando FE\n$(li)$(item)Jetpack$() (peitoral): segure $(l)PULAR$() pra voar\n$(li)$(item)Bateria$()/$(item)Carregador Portátil$(): enchem seus itens no inventário\n$(li)$(item)Ímã de Itens$(): clique direito liga/desliga (puxa drops)\n$(li)$(item)Chave Tech$(): rotaciona / desmonta máquinas"),
])

# ───────── TECNO-ARCANO ─────────
entry("antimagic/antimagic_overview", "Tecno-Arcano", "warded_core", "antimagic", 1, [
    txt("Contra a Magia", "O $(l)Tecno-Arcano$() é tecnologia feita pra $(item)conter magos$(). Tudo usa $(matter)matéria escura$(), em $(l)níveis$() (um precisa do anterior):\n\n$(li)$(item)Núcleo Bastião$() ← fragmento de matéria escura\n$(li)$(item)Disruptor Arcano$() ← lingote de matéria escura\n$(li)$(item)Matriz Nula$() ← catalisador de matéria escura"),
    spot("warded_core", "$(item)Núcleo Bastião$() — o componente-base de toda a linha anti-magia."),
])
entry("antimagic/antimagic_gear", "Armadura & Escudo", "warded_chestplate", "antimagic", 2, [
    txt("Armadura Bastião (FE)", "4 peças movidas a FE. Resiste a $(l)TODO dano mágico$() — $(good)−15% por peça$().\n\nCom o $(l)conjunto completo$(), aplica $(item)Antimagia$() nos magos que te atacam (enfraquece eles).\n\n$(warn)Sem FE$() = netherite comum. Recarregue num Carregador."),
    txt("Escudo Dissonante (FE)", "$(l)Bloqueando$(), anula $(good)90% de QUALQUER dano mágico$() — qualquer escola, vanilla ou Mana & Artifice — consumindo FE."),
    craft("warded_chestplate", "$(item)Peitoral Bastião$() — feito de Núcleos Bastião."),
])
entry("antimagic/antimagic_seals", "Selos", "mana_suppressor", "antimagic", 3, [
    txt("Selo Supressor de Mana", "Ligado à rede FE, cria um $(item)campo (raio 8)$() que $(l)IMPEDE conjuração$(): quem entra $(warn)não consegue gastar mana$() — o feitiço falha.\n\nÓtimo pra proteger bases de magos."),
    txt("Selo Sentinela", "Detecta quem $(l)conjura$() no raio (12) e $(warn)PUNE$(): dano + Lentidão + Cegueira + Antimagia.\n\nAmbos consomem FE e abrem GUI de energia no clique direito."),
    craft("mana_suppressor", "$(item)Selo Supressor$() — precisa de Disruptor Arcano (T2)."),
])

# ───────── COSMIC (categoria existente) ─────────
entry("cosmic/entropy_engines", "Motores de Entropia", "entropy_core", "cosmic", 50, [
    txt("$(cosmic)Núcleo de Entropia$()", "Converte a região em blocos de $(cosmic)outras dimensões$(): a superfície vira infecção e $(l)4 camadas abaixo$() viram rifts (flux → warped → void → rift residue).\n\nEle $(l)EXPANDE e acelera$() com o tempo; quem fica perto $(warn)perde sanidade$()."),
    txt("$(cosmic)Motor de Matéria Escura$()", "Faz o mesmo nos blocos $(l)abaixo$(), trocando por $(matter)matéria escura$() (dark matter, pedra/solo corrompido, esporos — e $(o)raramente$() $(item)Matéria Escura Cristalizada$())."),
    txt("Reverter tudo", "$(warn)Só para quando o bloco é destruído$() — e aí $(good)REVERTE tudo$() ao original.\n\nCada motor mapeia cada troca num JSON em $(o)<mundo>/liberthia/entropy/$(). Se o servidor cair/bugar:\n$(li)$(item)/liberthia entropy list$()\n$(li)$(item)/liberthia entropy revert <id>$()\n$(li)$(item)/liberthia entropy revert all$()"),
    craft("entropy_core", "$(item)Núcleo de Entropia$() — super premium (Matriz Nula + catalisador + cristal)."),
])
entry("cosmic/cosmic_creatures", "Criaturas Cósmicas", "colossal_eye_spawn_egg", "cosmic", 51, [
    txt("14 Horrores", "Criaturas planas tipo $(item)Olho Parasita$() que soltam infecção e corrompem o chão com flux dimensional.\n\n$(warn)Aparecem só à NOITE / no escuro$() e somente com sua $(l)sanidade abaixo de 40%$() — nunca à luz do dia."),
    txt("Habilidades", "$(li)$(item)Sanguessuga do Olhar$(): ENCARAR a faz crescer e ficar mais forte\n$(li)$(item)Tecelão Cego$(): encarar te cega + invoca brasas de fogo\n$(li)$(item)Hospedeiro$(): infecta mobs → eles te atacam\n$(li)$(item)Olho Colossal$(): o chefe — lento, enorme, tanque\n$(li)$(item)Carrapato do Vazio$(): teleporta pras suas costas"),
    txt("Mais variantes", "$(li)$(item)Larva Gritante$(): grito sônico que empurra\n$(li)$(item)Orbe do Pavor$(): pulsos de Escuridão\n$(li)$(item)Olho Pútrido$(): Veneno + Wither\n$(li)$(item)Mariposa da Penumbra$(), $(item)Ácaro Sussurrante$(), $(item)Rastejante de Bocas$(), $(item)Vigia de Carne$(), $(item)Reflexo$(), $(item)Brasa Parasita$()."),
])
entry("cosmic/sanity_horror", "Sanidade & Horror", "void_eye", "cosmic", 52, [
    txt("Sanidade", "Começa em $(l)100$(). Ela CAI quando você:\n$(li)fica no $(item)escuro$()\n$(li)$(item)minera$() por muito tempo no subsolo\n$(li)$(item)vê criaturas$() cósmicas/horror\n\nRecupere na $(good)luz/sol$(), com $(item)Tônico Lúcido$() e velas."),
    txt("Quando o horror começa", "$(warn)Só abaixo de 40% de sanidade$() começam: vozes, alucinações, o Visitante, sustos e os spawns de horror.\n\nE $(l)pioram$() quanto menor a sanidade. $(good)Acima de 40% você está seguro$() — nada de horror do nada."),
])

print("Manual: 2 categorias + 10 entradas geradas (data/ + assets/)")
