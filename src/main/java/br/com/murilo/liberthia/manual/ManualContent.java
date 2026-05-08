package br.com.murilo.liberthia.manual;

import java.util.List;

/**
 * Conteúdo estático do Liberthia Manual — capítulos + páginas.
 * Cada página é texto puro; o Screen renderiza e quebra linhas automaticamente.
 *
 * <p>Cores Minecraft inline com §-codes:
 * §5 = roxo, §d = lilás, §f = branco, §e = amarelo, §c = vermelho,
 * §6 = dourado, §a = verde, §b = ciano, §7 = cinza, §o = itálico, §l = bold
 */
public final class ManualContent {

    private ManualContent() {}

    public record Chapter(String title, List<Page> pages) {}

    /**
     * Página do manual. {@code itemIcon} (opcional) é o ID de um item que
     * será renderizado como ícone ao lado do título — útil pra páginas de
     * "como usar este bloco/item".
     */
    public record Page(String title, String body, String itemIcon) {
        public Page(String title, String body) { this(title, body, null); }
    }

    public static final List<Chapter> CHAPTERS = List.of(

            // ============================================================
            new Chapter("§5Bem-vindo", List.of(
                    new Page("Liberthia",
                            "§dLiberthia§7 abriga as §5três matérias§7 simultaneamente — "
                                    + "Escura, Clara e Amarela.\n\n"
                                    + "§7Use os §dcapítulos§7 à esquerda pra navegar. As §dsetas§7 do rodapé "
                                    + "passam páginas dentro do capítulo.\n\n"
                                    + "§l§5Foco deste manual:§r§7 como usar cada item/bloco do mod. "
                                    + "Para lore completa, leia o §dCodex do Pesquisador§r§7."),
                    new Page("Tecla útil — F8",
                            "§dF8§7 cicla a posição do HUD de matéria entre os 4 cantos da tela.\n\n"
                                    + "§7Configurável em §lOptions → Controls → Liberthia§r§7."),
                    new Page("Comandos de admin",
                            "§7Para testar com perfis específicos:\n\n"
                                    + "§o/liberthia matter set <jogador> dark|white|yellow <0-100>§r\n\n"
                                    + "§o/liberthia matter add <jogador> dark|white|yellow <-100..100>§r\n\n"
                                    + "§o/liberthia matter get <jogador>§r\n\n"
                                    + "§o/liberthia matter clear <jogador>§r")
            )),

            // ============================================================
            new Chapter("§5As Três Matérias", List.of(
                    new Page("Matéria Escura",
                            "§5A mais poderosa das três§r§7. Distorce a realidade ao redor, criando ambientes "
                                    + "hostis onde o caos predomina.\n\n"
                                    + "§7Em pequenas escalas — como em Liberthia — infecta hospedeiros, "
                                    + "transformando-os em §omarionetes§r§7 de uma Entidade que possivelmente a "
                                    + "controla.\n\n"
                                    + "§dManipulada com precisão§7, gera vida do nada. Pesquisadores a chamam de "
                                    + "§oanti-criação§r§7."),
                    new Page("Matéria Clara",
                            "§fÀ primeira vista parece inofensiva§r§7. Cobaias expostas isoladamente não mostram "
                                    + "reações imediatas.\n\n"
                                    + "§7Mas ela §oalimenta-se de memórias§r§7. Sujeitos demonstram lapsos cognitivos. "
                                    + "Esquecem nomes. Teleportam involuntariamente.\n\n"
                                    + "§7Combinada à §5Matéria Escura§r§7, desperta uma §oconsciência maligna§r§7 — "
                                    + "o hospedeiro se torna inteligente, motivado, e§l perigoso de outra maneira§r§7."),
                    new Page("Matéria Amarela",
                            "§eA menos investigada§r§7. Repele §5Matéria Escura§r§7 completamente.\n\n"
                                    + "§7Sozinha provoca §odescontrole emocional§r§7: alucinações, crises súbitas, "
                                    + "risadas e prantos alternados. §lNão é necessariamente agressiva§r§7.\n\n"
                                    + "§7Combinada à §fMatéria Clara§r§7 dentro de um hospedeiro, o caos é "
                                    + "estabilizado. Surge um ser §6frio, calculista§r§7, capaz de planos elaborados. "
                                    + "Não muda as motivações originais — §lapenas as intensifica§r§7.")
            )),

            // ============================================================
            new Chapter("§5Mutações Compostas", List.of(
                    new Page("Selvagem",
                            "§5DM puro§r §8(>30 DM, <15 WM, <15 YM)§r\n\n"
                                    + "§dO infectado vira §oselvagem§r§d. Agressividade alta. Mudanças físicas — "
                                    + "tamanho oscila. Coceira nos primeiros estágios.\n\n"
                                    + "§7Manipulado corretamente, pode §lclonar seres vivos§r§7. Familiares e "
                                    + "trabalhadores de baixo nível tendem a esse perfil.\n\n"
                                    + "§oEvite contato sem proteção.§r"),
                    new Page("Cognitiva",
                            "§fWM puro§r §8(>30 WM, <15 DM, <15 YM)§r\n\n"
                                    + "§fO infectado fica §omais inteligente§r§f, mas a custos altos. Memórias "
                                    + "são consumidas. Perde XP aleatoriamente.\n\n"
                                    + "§7Responsável pelo §lteleporte involuntário§r§7 e pelo apagamento de memórias "
                                    + "em outros sujeitos.\n\n"
                                    + "§7Trabalhadores de pesquisa que ficaram tempo demais nos labs apresentam "
                                    + "esse perfil."),
                    new Page("Errática",
                            "§eYM puro§r §8(>30 YM, <15 DM, <15 WM)§r\n\n"
                                    + "§eDescontrole emocional total. O sujeito alterna §oeufórico§r§e, §ofurioso§r§e, "
                                    + "§otriste§r§e, §oaterrorizado§r§e em ciclos curtos.\n\n"
                                    + "§7Alucinações constantes — vê mobs que não existem, ouve sons fantasmas.\n\n"
                                    + "§lNão é hostil por padrão§r§7 — mas é §oimprevisível§r§7."),
                    new Page("Simbiótica",
                            "§dDM + WM§r §8(≥20 ambos, <15 YM)§r\n\n"
                                    + "§dO §oAnfitrião§r§d e os §otrabalhadores§r§d caem nesse perfil.\n\n"
                                    + "§7Manipulador. Caótico mas inteligente. Agressivo mas contido pela WM. "
                                    + "§lFacilmente controlado§r§7 por outro ser com DM mais elevada.\n\n"
                                    + "§7É como um soldado que sabe pensar — mas obedece quem grita mais alto."),
                    new Page("Estrategista",
                            "§6YM + WM§r §8(≥20 ambos, <15 DM)§r\n\n"
                                    + "§6O descontrole da YM é §lsuprimido§r§6 pela WM.\n\n"
                                    + "§7Resultado: pessoa §oestável§r§7, §olúcida§r§7, §lnão muda motivações§r§7 — "
                                    + "apenas as intensifica e dá ferramentas pra realizá-las.\n\n"
                                    + "§7Bola planos, executa estratégias. Abomina a §5Matéria Escura§r§7 e a "
                                    + "combate com brutal precisão racional."),
                    new Page("Instável",
                            "§cDM + YM§r §8(>20 ambos)§r\n\n"
                                    + "§4As duas se §lrepelem§r§4 quimicamente. O hospedeiro entra em colapso.\n\n"
                                    + "§7Pesquisadores nunca documentaram com sucesso esse estado em humanos. "
                                    + "Os poucos casos que existem terminaram em §oexplosão biológica§r§7.\n\n"
                                    + "§lEvite a todo custo.§r")
            )),

            // ============================================================
            new Chapter("§5Blocos & Máquinas", List.of(
                    new Page("Gerador de Matéria Escura",
                            "§dQueima §oblocos de matéria escura§r§d como combustível, gerando §oFE§r§d (Forge Energy).\n\n"
                                    + "§71 bloco = 500.000 FE de reserva, queima a 1.000 FE/tick.\n\n"
                                    + "§7§lUpgrades suportados:§r§7\n"
                                    + "§e• Velocidade§r — +100% FE/tick por unidade\n"
                                    + "§b• Eficiência§r — +50% FE/bloco\n"
                                    + "§d• Capacidade§r — +100% buffer\n\n"
                                    + "§7Compatível com §lAE2§r§7, §lMekanism§r§7 e qualquer mod FE.",
                            "liberthia:dark_matter_generator"),
                    new Page("Cabos & Tubos",
                            "§dCabo de Energia§r — relay transparente para FE.\n\n"
                                    + "§7§lInteração:§r§7\n"
                                    + "§7• §dRight-click numa face§7 = liga/desliga aquela face (visual + funcional)\n\n"
                                    + "§dItem Pipe§r — 4 modos por face: §7Default / §6Extract / §aInsert / §cDisabled§r.\n\n"
                                    + "§7§lInteração:§r§7\n"
                                    + "§7• §dRight-click§7 mão vazia = ciclar modo da face\n"
                                    + "§7• §dRight-click§7 com item = adicionar ao filtro\n"
                                    + "§7• §dShift+Right-click§7 com item = ciclar velocidade\n"
                                    + "§7• §dShift+Right-click§7 mão vazia = ciclar §lTipo§r§7 (Universal/Items/Blocks/Fluids)",
                            "liberthia:item_pipe"),
                    new Page("Fragmented Generator",
                            "§dPrimeiro estágio da refinação.§r\n\n"
                                    + "§7Recebe §obucket de matéria escura§r§7 + §odiamante§r§7 + §o50.000 FE§r§7. "
                                    + "Produz §oMatéria Escura Inativa§r§7 em 200 ticks (-50t por upgrade).\n\n"
                                    + "§7Slot único de upgrade: §eVelocidade§7 (até 4).",
                            "liberthia:fragmented_generator"),
                    new Page("Crystallizer + Lasers",
                            "§dSegundo estágio. Cristaliza inativa em ATIVA → §oblock dark matter§r§d.\n\n"
                                    + "§7Coloque o §dCrystallizer§7 no centro. Aponte §c≥2 Laser Emitters§7 "
                                    + "para ele. Cada laser consome §c4.000 FE/tick§7.\n\n"
                                    + "§7§lLaser controles:§r§7\n"
                                    + "§7• §dShift+right-click§7 numa face = liga/desliga aquela direção\n"
                                    + "§7• §dRight-click§7 sem shift = mostra status\n\n"
                                    + "§4§lAVISO:§r§4 destruir um laser ativo causa §lEXPLOSÃO§r§4. "
                                    + "Sempre desligue antes de quebrar.",
                            "liberthia:crystallizer"),
                    new Page("Dimensional Extractor",
                            "§dGera matéria escura passivamente perto de §lrifts dimensionais§r§d.\n\n"
                                    + "§7§lNÃO precisa de FE§r§7 — alimentado pelo próprio rift.\n\n"
                                    + "§7Use a §dBússola Dimensional§r§7 pra achar um rift. Coloque o extractor "
                                    + "perto. Quanto mais perto, mais rápido produz.\n\n"
                                    + "§7Right-click abre GUI: insira §obalde vazio§r§7 → vira §odark_matter_bucket§r§7.",
                            "liberthia:dimensional_extractor"),
                    new Page("Auto Farmer",
                            "§dColocado sobre §olava§r§d, consome §ocatalisador§r§d + §o20.000 FE§r§d e produz "
                                    + "§oblocos de matéria escura§r§d automaticamente.\n\n"
                                    + "§7Cooldown 200t por operação. Bom pra autosuficiência mid-game.",
                            "liberthia:auto_farmer"),
                    new Page("Matter Analyzer",
                            "§dO computador da pesquisa.§r\n\n"
                                    + "§7Insira qualquer item ou §dFrasco de Amostra§r§7 e veja:\n"
                                    + "§7• Conteúdo de DM/WM/YM em barras\n"
                                    + "§7• Energia equivalente em FE\n"
                                    + "§7• §lMutação composta§r§7 dominante\n\n"
                                    + "§7Hover na tela mostra descrição completa da mutação.",
                            "liberthia:matter_analyzer"),
                    new Page("Bau de Matéria Escura",
                            "§dArmazenamento 9×6 = 54 slots.§r\n\n"
                                    + "§7Tema dark matter. Não é mais radioativo que um baú comum, mas blocos "
                                    + "armazenados §c§lainda emitem radiação se você abrir o baú§r§7 — porque "
                                    + "passam pelo seu inventário durante a transferência.",
                            "liberthia:dark_matter_chest"),
                    new Page("Baterias FE",
                            "§dArmazenamento mass de FE em 3 tiers:§r\n\n"
                                    + "§7• §dBasic§7 — 1.000.000 FE / 10k transferência\n"
                                    + "§7• §dAdvanced§7 — 100.000.000 FE / 100k transferência\n"
                                    + "§7• §dQuantum§7 — 2.147.483.647 FE / 1M transferência\n\n"
                                    + "§7Empurram FE ATIVAMENTE pela rede de cabos. Right-click mostra %.\n\n"
                                    + "§7§oFE é int 32-bit, max ~2.1B. Não há tier maior por limitação técnica.§r",
                            "liberthia:dm_battery_quantum")
            )),

            // ============================================================
            new Chapter("§5Itens & Ferramentas", List.of(
                    new Page("Bússola Dimensional",
                            "§dRight-click§r§7 escaneia rifts da dimensão atual e aponta para o mais próximo.\n\n"
                                    + "§7Mostra coords + distância em metros no chat. NBT armazenado — brilha "
                                    + "quando tem alvo trancado.\n\n"
                                    + "§7Rifts são §lpontos dimensionais§r§7. Existem 8 iniciais por mundo, mais "
                                    + "geram com o tempo (a cada 10 min se um jogador estiver online).",
                            "liberthia:dimensional_compass"),
                    new Page("Frasco de Amostra",
                            "§dRight-click num bloco§r§7 com matéria → coleta amostra no NBT.\n\n"
                                    + "§7Insira o frasco preenchido no §dMatter Analyzer§r§7 pra leitura completa.\n\n"
                                    + "§7Vidro vazio quando sem amostra; rosto cheio com líquido roxo quando preenchido. "
                                    + "Tooltip mostra valores brutos.",
                            "liberthia:sample_vial"),
                    new Page("Luva de Contenção",
                            "§dProteção contra radiação de §lblocos de matéria escura§r§d.§r\n\n"
                                    + "§7Em qualquer slot do inventário (não precisa equipar). Suprime o dano "
                                    + "automaticamente.\n\n"
                                    + "§7Consome 1 durabilidade a cada §o~6 segundos§r§7 de exposição. 500 dura = "
                                    + "~50 minutos de uso.\n\n"
                                    + "§c§lAVISO:§r§7 a luva NÃO previne acúmulo de DM no perfil — só dano físico.",
                            "liberthia:containment_glove"),
                    new Page("Medidor de Energia",
                            "§dRight-click em qualquer bloco§r§7 com capability FE pra ler:\n\n"
                                    + "§7§l⚡ 12.345 / 100.000 FE (12%)§r\n\n"
                                    + "§7Funciona com TUDO que aceita FE — máquinas Liberthia, AE2, Mekanism, "
                                    + "Thermal, etc.",
                            "liberthia:energy_meter"),
                    new Page("Catalisador",
                            "§dUsado pelo §oAuto Farmer§r§d.§r\n\n"
                                    + "§7Cada bloco produzido consome 1 catalisador.\n\n"
                                    + "§7Stacks até 16. Consigne nos slots de input."),
                    new Page("Codex do Pesquisador",
                            "§dLivro lore em formato §ochat-page§r§d.§r\n\n"
                                    + "§7Right-click avança página. 8 páginas no total.\n\n"
                                    + "§7Cobre o mesmo conteúdo deste manual mas em §oprosa narrativa§r§7. "
                                    + "Bom pra imersão."),
                    new Page("Itens da Lore",
                            "§dFragmento do Olho de Horus§r §8— DM:90§r\n"
                                    + "§7Fragmento da Ilha de Horus, perdida no Nether selado. Caos puro.\n\n"
                                    + "§dCristal de Equilibrium§r §8— WM:50, YM:50§r\n"
                                    + "§7Pedaço da entidade-sol da Ilha Equilibrium, no Twilight. "
                                    + "Mutação Estrategista direto na palma da mão.")
            )),

            // ============================================================
            new Chapter("§5Efeitos & Sintomas", List.of(
                    new Page("Como funciona o perfil",
                            "§dCada jogador acumula 3 valores 0-100§r§7: §dDM§r§7, §fWM§r§7, §eYM§r§7.\n\n"
                                    + "§7Acumula por exposição (radiação ambiente, blocos no inventário, etc). "
                                    + "Decai 1 ponto/min sem exposição.\n\n"
                                    + "§7O HUD no canto da tela mostra as 3 barras. Tecla §dF8§r§7 cicla a posição "
                                    + "do HUD entre os 4 cantos.\n\n"
                                    + "§7Comando: §o/liberthia matter set @s dark 50§r"),
                    new Page("Fúria Selvagem (DM)",
                            "§dStrength§r§7 + §dDig Speed§r§7 (em níveis altos)\n\n"
                                    + "§dPartículas de raiva§r§7 em volta. Tamanho do jogador pode oscilar (futuro).\n\n"
                                    + "§7Bom pra combate corpo-a-corpo. Ruim pra socializar — mobs neutros podem "
                                    + "te atacar antes."),
                    new Page("Mente Contida (DM+WM)",
                            "§dDig Speed§r§7 + §dStrength§r§7\n\n"
                                    + "§dPartículas de encantamento§r§7 em volta.\n\n"
                                    + "§7Ainda agressivo, mas com cabeça pra planejar. Bom pra mineração+luta.\n\n"
                                    + "§c§lFraqueza:§r§7 outro jogador com DM puro alto pode te dominar (mecânica futura)."),
                    new Page("Lapsos de Memória (WM)",
                            "§dSpeed§r§7 + §dDig Speed§r§7 + §oTeleport§r§7 ao tomar dano (25%, perde XP)\n\n"
                                    + "§dPartículas end_rod§r§7 em rastro.\n\n"
                                    + "§lO jogador WHITE perde XP§r§7 quando teleportado pelo blink — a matéria "
                                    + "branca consome lembranças. É o preço da inteligência."),
                    new Page("Tempestade Emocional (YM)",
                            "§cHunger§r§7 + §dSlow Falling§r§7\n\n"
                                    + "§dPartículas de nota musical§r§7 em volta.\n\n"
                                    + "§7Mood swings aleatórios — sons espontâneos, jumps, slows.\n\n"
                                    + "§7Não bom pra exploração tranquila. Você nunca sabe o que vai sentir nos "
                                    + "próximos 60 ticks."),
                    new Page("Foco Frio (YM+WM)",
                            "§6Strength§r§7 + §dResistance§r§7\n\n"
                                    + "§dPartículas soul fire§r§7 em volta.\n\n"
                                    + "§7§lO efeito mais poderoso defensivo§r§7. Bom em multidão. A WM bloqueia "
                                    + "o caos da YM, deixando só o foco e a frieza."),
                    new Page("Como evitar/ganhar perfil",
                            "§a§lPara EVITAR ganhar matéria:§r§7\n"
                                    + "§7• Use §dLuva de Contenção§r§7 (mas só protege HP, não perfil)\n"
                                    + "§7• Não fique perto de minérios DM\n"
                                    + "§7• Saia de §oradiation hotspots§r§7 detectados pelo Geiger\n\n"
                                    + "§a§lPara GANHAR de propósito:§r§7\n"
                                    + "§7• Carregue blocos no inventário\n"
                                    + "§7• Beba poções específicas (futuro)\n"
                                    + "§7• Use comandos: §o/liberthia matter add @s yellow 30§r")
            )),

            // ============================================================
            new Chapter("§5Catálogo — Materiais", List.of(
                    new Page("Bloco de Matéria Escura",
                            "§dForma sólida da §lMatéria Escura§r§d.§r\n\n"
                                    + "§7Combustível primário do §dGerador de Matéria Escura§r§7. Pode ser usado "
                                    + "como bloco de construção, mas §c§lemite radiação§r§7 — use §dLuva de Contenção§r§7.\n\n"
                                    + "§7Drop principal do §dCrystallizer§7 ao processar matéria ativa.",
                            "liberthia:dark_matter_block"),
                    new Page("Bloco de Matéria Clara",
                            "§fForma sólida da §lMatéria Clara§r§f.§r\n\n"
                                    + "§7Emissivo (luz nível 11). Random ticks geram efeitos. Não é radioativo "
                                    + "fisicamente — mas exposição prolongada começa a §oconsumir suas memórias§r§7 (XP).",
                            "liberthia:clear_matter_block"),
                    new Page("Bloco de Matéria Amarela",
                            "§eForma sólida da §lMatéria Amarela§r§e.§r\n\n"
                                    + "§7Estável visualmente, mas perto dele você sente §omood swings§r§7 — risadas, "
                                    + "pranto, jumps espontâneos. Combina com WM no §dMatter Forge§r§7 pra criar a §lLiga "
                                    + "Estrategista§r§7.",
                            "liberthia:yellow_matter_block"),
                    new Page("Minério de Matéria Escura",
                            "§dMinério raro encontrado em camadas profundas.§r\n\n"
                                    + "§7Solta §dShards§r§7 (matéria escura cru) ao quebrar com pickaxe ferro+. "
                                    + "Variante deepslate solta o mesmo, mais lentamente.\n\n"
                                    + "§c§lAVISO:§r§7 minerar sem luva acumula DM no perfil §orápido§r§7.",
                            "liberthia:dark_matter_ore"),
                    new Page("Deepslate Dark Matter Ore",
                            "§8Variante profunda do minério.§r\n\n"
                                    + "§7Mesmo drop, harder hardness. Perfila acumula DM mais devagar (rocha "
                                    + "deepslate isola parcialmente).",
                            "liberthia:deepslate_dark_matter_ore"),
                    new Page("Minério de Matéria Clara",
                            "§fEncontrado em formações §obrancas§r§f, perto de rifts dimensionais.§r\n\n"
                                    + "§7Solta §oclear matter shards§r§7 (raros) e §oXP§r§7 — mas a presença dele "
                                    + "consome XP do jogador próximo lentamente. §lEquilibre o ganho com a perda.§r",
                            "liberthia:white_matter_ore"),
                    new Page("Bucket de Matéria Escura",
                            "§dFluido pesado, líquido viscoso roxo-escuro.§r\n\n"
                                    + "§7Coletado com §obalde vazio§r§7 do §dDimensional Extractor§r§7. "
                                    + "Insumo do §dFragmented Generator§r§7 e §dDark Matter Forge§r§7.",
                            "liberthia:dark_matter_bucket"),
                    new Page("Bucket de Matéria Clara",
                            "§fLíquido leitoso, quase translúcido.§r\n\n"
                                    + "§7Coletado em §opoças cristalinas§r§7 perto de rifts. Insumo do §dPurification "
                                    + "Bench§r§7 e §dMatter Infuser§r§7. Beber direto = §oWither II + perda de XP§r§7.",
                            "liberthia:clear_matter_bucket"),
                    new Page("Bucket de Matéria Amarela",
                            "§eLíquido dourado luminoso.§r\n\n"
                                    + "§7Mais raro dos 3. Encontrado só em estruturas Equilibrium. Beber dá "
                                    + "§oRegeneração III por 30s§r§7 mas adiciona §e+15 YM§r§7 ao perfil.",
                            "liberthia:yellow_matter_bucket"),
                    new Page("Solo Corrompido",
                            "§4Terra envenenada por DM.§r\n\n"
                                    + "§7Spawna naturalmente em zonas de infecção. Random ticks espalham infecção. "
                                    + "Quebrar dropa nada — mas o bloco em si é um indicador de §lhotspot§r§7.",
                            "liberthia:corrupted_soil"),
                    new Page("Tronco / Pedra Corrompida",
                            "§4Materiais §lcorrompidos§r§4 da floresta infectada.§r\n\n"
                                    + "§7Decorativos. Mantêm a estética da zona dark. Não emitem radiação.",
                            "liberthia:corrupted_log")
            )),

            // ============================================================
            new Chapter("§5Catálogo — Refinação", List.of(
                    new Page("Shard de Matéria Escura",
                            "§5Pedaço bruto de DM.§r\n\n"
                                    + "§7Drop direto de §dminério dark matter§r§7. 9 shards = 1 §dBloco de DM§r§7 "
                                    + "no crafting normal. Insumo de poções e §dRitual Pedestal§r§7.",
                            "liberthia:dark_matter_shard"),
                    new Page("Matéria Escura Inativa",
                            "§dPrimeiro estágio refinado.§r\n\n"
                                    + "§7Saída do §dFragmented Generator§r§7. Não é radioativa. Insumo do §dCrystallizer§r§7 "
                                    + "(2º estágio).",
                            "liberthia:inactive_dark_matter"),
                    new Page("Matéria Escura Ativa",
                            "§5Segundo estágio refinado.§r\n\n"
                                    + "§7Saída do §dCrystallizer§r§7. Volta a ser radioativa, mas com §opotência "
                                    + "controlável§r§7. Crafta §dDark Matter Cell§r§7 e ferramentas DM.",
                            "liberthia:active_dark_matter"),
                    new Page("Matéria Escura Estabilizada",
                            "§dAlto-tier refinado.§r\n\n"
                                    + "§7Saída do §dDark Matter Alchemizer§r§7. Insumo de §ditens lendários§r§7 e "
                                    + "ferramentas top-tier do mod.",
                            "liberthia:stabilized_dark_matter"),
                    new Page("Matéria Instável",
                            "§4Subproduto §lperigoso§r§4 da refinação.§r\n\n"
                                    + "§7Sai aleatoriamente do Crystallizer. Cair na lava ou tocar com lava = "
                                    + "§oexplosão pequena§r§7. Stack até 16. Use com cuidado.",
                            "liberthia:unstable_matter"),
                    new Page("Catalisador",
                            "§dPequena pedra com sulcos roxos.§r\n\n"
                                    + "§7Insumo do §dAuto Farmer§r§7 — 1 catalisador = 1 bloco de DM produzido. "
                                    + "Crafta com 2 shards + 1 obsidian.",
                            "liberthia:dark_matter_catalyst"),
                    new Page("Dark Matter Cell",
                            "§dBateria FE pessoal portátil.§r\n\n"
                                    + "§7Acumula até 50.000 FE. Carregue no §dWireless Charger§r§7 ou no slot 4 "
                                    + "da §dBateria§r§7. Alimenta ferramentas FE de outros mods quando segurada.",
                            "liberthia:dark_matter_cell"),
                    new Page("Lingote de Matéria Amarela",
                            "§eMetal dourado vibrante.§r\n\n"
                                    + "§7Smelt do §dyellow_matter_shard§r§7 na fornalha (eventualmente). Crafta as "
                                    + "ferramentas Yellow Matter (espada, picareta, machado) e armadura.",
                            "liberthia:yellow_matter_ingot"),
                    new Page("Matter Core",
                            "§dNúcleo refinado §lmulti-matéria§r§d.§r\n\n"
                                    + "§7Crafta misturando os 3 tipos de matter shards no §dMatter Forge§r§7. "
                                    + "Insumo de §lblocos top-tier§r§7 (Bateria Quantum, Wireless Charger).",
                            "liberthia:matter_core"),
                    new Page("Matter Ampoule",
                            "§dFrasco lacrado com mistura concentrada.§r\n\n"
                                    + "§7Right-click consome 1 ampoule e dá §obuff temporário§r§7 baseado no perfil "
                                    + "atual. Crafta no §dMatter Infuser§r§7.",
                            "liberthia:matter_ampoule"),
                    new Page("Speed Upgrade",
                            "§eUpgrade de velocidade pra máquinas.§r\n\n"
                                    + "§7Aumenta FE/tick ou velocidade de processamento em +100% por unidade. "
                                    + "Stack até 4 no slot de upgrade. Crafta com 1 redstone + 1 sugar + 1 "
                                    + "shard.",
                            "liberthia:speed_upgrade"),
                    new Page("Efficiency Upgrade",
                            "§bUpgrade de eficiência.§r\n\n"
                                    + "§7Aumenta FE/bloco em +50% por unidade. Custo menor por output. Stack até 4.",
                            "liberthia:efficiency_upgrade"),
                    new Page("Capacity Upgrade",
                            "§dUpgrade de capacidade.§r\n\n"
                                    + "§7Multiplica capacidade do buffer em +100% por unidade. Stack até 4.",
                            "liberthia:capacity_upgrade")
            )),

            // ============================================================
            new Chapter("§5Catálogo — Combate", List.of(
                    new Page("Espada de Matéria Escura",
                            "§5+12 dano + §lFire Aspect II§r§5.§r\n\n"
                                    + "§7Crafta com 2 active_dark_matter + 1 stick. §c+15 DM§r§7 ao perfil quando "
                                    + "segurada >30s. Use com luva.",
                            "liberthia:dark_matter_sword"),
                    new Page("Espada de Matéria Clara",
                            "§f+9 dano + §lknockback II§r§f.§r\n\n"
                                    + "§7Cada hit consome 1 XP do alvo (player ou mob com XP). §oTeleporta o alvo§r§7 "
                                    + "aleatoriamente em distâncias curtas (5%).",
                            "liberthia:clear_matter_sword"),
                    new Page("Espada de Matéria Amarela",
                            "§e+10 dano + §lLooting II§r§e.§r\n\n"
                                    + "§7Inflige §oconfusão§r§7 (Nausea I) por 5s ao alvo. Mood-swing visual no "
                                    + "wielder.",
                            "liberthia:yellow_matter_sword"),
                    new Page("Holy Blade",
                            "§6+10 dano, §l+50% vs undead/infected§r§6.§r\n\n"
                                    + "§7Drop dos §dOrder Paladins§r§7. Não pode ser craftada. §lNão durabilidade§r§7 — "
                                    + "ferramenta espiritual.",
                            "liberthia:holy_blade"),
                    new Page("Holy Hammer",
                            "§6Martelo radiante. AOE em 3×3.§r\n\n"
                                    + "§7Right-click cria §londa de luz§r§7 que purifica blocos infectados num raio "
                                    + "de 5. Cooldown 200t.",
                            "liberthia:holy_hammer"),
                    new Page("Holy Smite Staff",
                            "§6Cajado de raios sagrados.§r\n\n"
                                    + "§7Right-click invoca §oraio holy§r§7 onde você está olhando. Causa 12 dano "
                                    + "+ ignora armor de inimigos infected/undead. Cooldown 100t.",
                            "liberthia:holy_smite_staff"),
                    new Page("Blood Scythe",
                            "§4Foice ritual. §l+8 dano§r§4.§r\n\n"
                                    + "§7Cada kill consome 1 saúde do wielder, mas dá §oRegen II§r§7 por 5s e drop "
                                    + "extra. Drop de §dBlood Mage§r§7.",
                            "liberthia:blood_scythe"),
                    new Page("Blood Bow",
                            "§4Arco que dispara §lflechas sangrentas§r§4.§r\n\n"
                                    + "§7Não consome flechas — usa 1 HP por shot. Causa §oBleed§r§7 (sangramento, "
                                    + "1 dano/s por 5s).",
                            "liberthia:blood_bow"),
                    new Page("Blood Ritual Dagger",
                            "§4Adaga curta de ritual.§r\n\n"
                                    + "§7Right-click no ar = sacrificar 4 HP pra +30 §dDM§r§7 no perfil. Right-click "
                                    + "em mob = §obleed§r§7 brutal por 10s.",
                            "liberthia:blood_ritual_dagger"),
                    new Page("Hemomancer Staff",
                            "§4Cajado de magia de sangue.§r\n\n"
                                    + "§7Right-click consome §c4 HP§r§7 e lança §lblood bolt§r§7 (10 dano). Tem 4 "
                                    + "modos: dispara, AOE, heal-self, summon.",
                            "liberthia:hemomancer_staff"),
                    new Page("Sword Brum",
                            "§dEspada lendária — referência meta.§r\n\n"
                                    + "§7Crafta com §lSanguine Core§r§7 + ferro + leather. §l+18 dano§r§7 + custom "
                                    + "swing animation. Drop ultra-raro de event-related.",
                            "liberthia:sword_brum"),
                    new Page("Escudo de Matéria Clara",
                            "§fEscudo translúcido.§r\n\n"
                                    + "§7Bloqueia 100% do dano vindo de WM/YM. Bloqueia 50% de DM. Permanece em "
                                    + "guard maior tempo que vanilla.",
                            "liberthia:clear_matter_shield"),
                    new Page("Escudo de Matéria Amarela",
                            "§eEscudo dourado §lreflexivo§r§e.§r\n\n"
                                    + "§7Reflete 50% de projeteis no atacante. Cooldown menor que vanilla. Consome "
                                    + "1 dura por reflexo.",
                            "liberthia:yellow_matter_shield"),
                    new Page("Summon Staff",
                            "§dCajado de invocação.§r\n\n"
                                    + "§7Right-click invoca §oclone temporário§r§7 (worker_clone) que luta por "
                                    + "você por 30s. Cooldown 600t.",
                            "liberthia:summon_staff"),
                    new Page("Freeze Staff",
                            "§bCajado de gelo.§r\n\n"
                                    + "§7Right-click congela alvo onde olha por 5s (Slowness V + impossível pular). "
                                    + "Custo: 1 §dfrost_flask§r§7 por uso.",
                            "liberthia:freeze_staff"),
                    new Page("White Light Wand",
                            "§fVarinha de luz branca §lpura§r§f.§r\n\n"
                                    + "§7Right-click cria §obarreira de luz§r§7 que repele mobs infected num raio "
                                    + "de 8 por 30s. Bom em emergência.",
                            "liberthia:white_light_wand")
            )),

            // ============================================================
            new Chapter("§5Catálogo — Mineração", List.of(
                    new Page("Picareta de Matéria Escura",
                            "§5+8 dano, §lFortune III§r§5 implícita em DM ores.§r\n\n"
                                    + "§7Mina blocos quase tão rápido quanto netherite. Drop bonus em minérios "
                                    + "DM. §c+10 DM§r§7 ao perfil em uso prolongado.",
                            "liberthia:dark_matter_pickaxe"),
                    new Page("Machado de Matéria Escura",
                            "§5+9 dano, §lvein-cut§r§5 (3×3 madeira).§r\n\n"
                                    + "§7Right-click derruba a árvore inteira. Consome 5 dura por árvore. "
                                    + "Combina bem com §dAuto Farmer§r§7.",
                            "liberthia:dark_matter_axe"),
                    new Page("Picareta de Matéria Clara",
                            "§fEficiente §lcontra mobs infected§r§f.§r\n\n"
                                    + "§7Speed alto. Cada bloco minerado tem 5% de chance de teleportar você 8 "
                                    + "blocos pra cima — útil em quedas, ruim em túneis fechados.",
                            "liberthia:clear_matter_pickaxe"),
                    new Page("Machado de Matéria Clara",
                            "§fMachado §lleve§r§f.§r\n\n"
                                    + "§7Bom dano + Sharpness II. Cada hit reduz armadura do alvo em 5% por 3s "
                                    + "(stackable).",
                            "liberthia:clear_matter_axe"),
                    new Page("Picareta de Matéria Amarela",
                            "§eDrops dobrados§r §o(50% chance)§r §epor bloco minerado§r§e.§r\n\n"
                                    + "§7Aumenta YM no perfil em uso prolongado.",
                            "liberthia:yellow_matter_pickaxe"),
                    new Page("Machado de Matéria Amarela",
                            "§eMachado §lirresistível§r§e.§r\n\n"
                                    + "§7Hits têm 25% de chance de causar §oconfusão§r§7 no alvo. Mobs neutros "
                                    + "podem virar agressivos pelo efeito.",
                            "liberthia:yellow_matter_axe")
            )),

            // ============================================================
            new Chapter("§5Catálogo — Engenharia", List.of(
                    new Page("Liberthia Wrench",
                            "§dChave-inglesa do mod.§r\n\n"
                                    + "§7§lUso em cabos:§r§7 §dright-click numa face§r§7 = liga/desliga aquela face. "
                                    + "Visual + funcional.\n\n"
                                    + "§7§lUso em pipes:§r§7 §dshift+right-click§r§7 = cicla §oTipo§r§7 do pipe "
                                    + "(Universal/Items/Blocks/Fluids).",
                            "liberthia:liberthia_wrench"),
                    new Page("Energy Meter",
                            "§dMedidor portátil de FE.§r\n\n"
                                    + "§7Right-click em qualquer bloco com capability ENERGY pra ler:\n"
                                    + "§e⚡ X / Y FE (Z%)§r\n\n"
                                    + "§7Funciona com §lTUDO§r§7 — máquinas Liberthia, AE2, Mekanism, Thermal, "
                                    + "Industrial Foregoing, etc.",
                            "liberthia:energy_meter"),
                    new Page("Pylon Remote",
                            "§dControle remoto do §lCommand Pylon§r§d.§r\n\n"
                                    + "§7§dShift+right-click§r§7 num pylon = liga ele ao remote. Right-click "
                                    + "em qualquer lugar = aciona o pylon remotamente. Multiple pylons por remote (lista).",
                            "liberthia:pylon_remote"),
                    new Page("Marking Stick",
                            "§dPincel de área.§r\n\n"
                                    + "§7Right-click em 2 blocos pra definir área retangular. Útil pra demarcar "
                                    + "construção. Mostra outline visual quando segurado.",
                            "liberthia:marking_stick"),
                    new Page("Chalk",
                            "§7Giz de ritual.§r\n\n"
                                    + "§7Right-click em chão liso desenha §dsímbolos rituais§r§7. Cada símbolo "
                                    + "tem efeito quando ativado por DM/blood. 8 cargas por giz.",
                            "liberthia:chalk"),
                    new Page("Chalk Symbol (placed)",
                            "§dSímbolo de ritual desenhado.§r\n\n"
                                    + "§7Bloco passivo. Quando posicionado em pattern correto + sangue/DM próximo, "
                                    + "ativa rituais. Veja §dResearcher Codex§r§7 pra patterns.",
                            "liberthia:chalk_symbol"),
                    new Page("Safe Siphon",
                            "§dSifão de extração §lsafe§r§d.§r\n\n"
                                    + "§7Tira matéria escura de um inventário sem causar dano de radiação ao "
                                    + "manipulador. Insumo de pesquisa avançada.",
                            "liberthia:safe_siphon")
            )),

            // ============================================================
            new Chapter("§5Catálogo — Detecção", List.of(
                    new Page("Bússola Dimensional",
                            "§dRight-click§r§7 escaneia rifts da dimensão atual e aponta pro mais próximo.\n\n"
                                    + "§7Mostra coords + distância em metros no chat. NBT armazenado — brilha "
                                    + "quando tem alvo trancado. Update a cada uso.",
                            "liberthia:dimensional_compass"),
                    new Page("White Matter Finder",
                            "§fDetector de §lmatéria clara§r§f no terreno.§r\n\n"
                                    + "§7Right-click escaneia 32 blocos em todas direções. Aponta o §ominério WM§r§7 "
                                    + "mais próximo. Útil em deserts e tundras.",
                            "liberthia:white_matter_finder"),
                    new Page("Geiger Counter",
                            "§4Detector de §lradiação§r§4.§r\n\n"
                                    + "§7Right-click ou só segurar mostra: nível de radiação atual + previsão de "
                                    + "acúmulo de DM/min. Beep audível em hotspots.",
                            "liberthia:geiger_counter"),
                    new Page("Expedition Tracker",
                            "§eRastreador de expedições.§r\n\n"
                                    + "§7Marca §oposições importantes§r§7 (até 16). Right-click em bloco = save. "
                                    + "Right-click no ar = ciclar entre alvos. Mostra distância + direção em HUD.",
                            "liberthia:expedition_tracker"),
                    new Page("Sample Vial (Frasco de Amostra)",
                            "§dRight-click num bloco com matéria§r§7 → coleta amostra no NBT.\n\n"
                                    + "§7Insira frasco preenchido no §dMatter Analyzer§r§7. Vidro vazio quando "
                                    + "sem amostra; rosto cheio com líquido roxo quando preenchido.",
                            "liberthia:sample_vial"),
                    new Page("Revelation Lens",
                            "§dLente reveladora.§r\n\n"
                                    + "§7Segurando: blocos invisíveis (glitch_block, wormhole_block) ficam "
                                    + "§ovisíveis§r§7 com outline. Mobs disguised mostram identidade real.",
                            "liberthia:revelation_lens")
            )),

            // ============================================================
            new Chapter("§5Catálogo — Defensivos", List.of(
                    new Page("Luva de Contenção",
                            "§dProteção contra radiação de §lblocos de matéria escura§r§d.§r\n\n"
                                    + "§7Em qualquer slot do inventário (não precisa equipar). Suprime o dano "
                                    + "automaticamente.\n\n"
                                    + "§7Consome 1 dura a cada §o~6s§r§7 de exposição. 500 dura = ~50 min de uso. "
                                    + "§c§lAVISO:§r§7 NÃO previne acúmulo de DM no perfil — só dano físico.",
                            "liberthia:containment_glove"),
                    new Page("Protection Ruby",
                            "§cRuby de proteção mágica.§r\n\n"
                                    + "§7Equipado em qualquer slot, reduz dano físico em 15%. Consome 1 dura por hit "
                                    + "absorvido. 200 dura.",
                            "liberthia:protection_ruby"),
                    new Page("Sanctify Orb",
                            "§6Orbe sagrado.§r\n\n"
                                    + "§7Right-click consome 1 holy_essence + cria §oaura sagrada§r§7 ao redor "
                                    + "(raio 6) por 60s. Repele mobs infected/dark.",
                            "liberthia:sanctify_orb"),
                    new Page("Purity Beacon",
                            "§fFarol de pureza colocável.§r\n\n"
                                    + "§7Bloco em formato cone. Em raio de 16 blocos: drena §dDM§r§7 do perfil "
                                    + "dos jogadores em -1/seg. Custo: 1 holy_essence por minuto ativo.",
                            "liberthia:purity_beacon"),
                    new Page("Withered Totem",
                            "§8Totem da §lmurchidão§r§8.§r\n\n"
                                    + "§7No inventário: ativa-se quando você ia morrer, gasta 1 dura, te dá "
                                    + "§oRegen V§r§7 por 5s + 1 HP. Stack até 8. Não precisa segurar.",
                            "liberthia:withered_totem"),
                    new Page("Blood Pact Amulet",
                            "§4Amuleto de §lpacto sangrento§r§4.§r\n\n"
                                    + "§7Equipado: ao tomar dano fatal, consome 1 amuleto + 50% HP cap por 60s. "
                                    + "Você sobrevive. Risco real de corromper o perfil pra blood-aligned.",
                            "liberthia:blood_pact_amulet"),
                    new Page("Player Lock",
                            "§dItem de admin — tranca um player no spawn.§r\n\n"
                                    + "§7§lApenas OPs.§r§7 Right-click no player = ele não pode quebrar/colocar "
                                    + "blocos por 5 min. Útil em events.",
                            "liberthia:player_lock")
            )),

            // ============================================================
            new Chapter("§5Catálogo — Arremesso & Bombas", List.of(
                    new Page("Cleansing Grenade",
                            "§fGranada de §llimpeza§r§f.§r\n\n"
                                    + "§7Right-click pra arremessar. Explode em raio 5 — purifica blocos "
                                    + "corrupted_soil/log voltando pra grass/oak. Não causa dano.",
                            "liberthia:cleansing_grenade"),
                    new Page("Lightning Grenade",
                            "§eGranada de §lraio§r§e.§r\n\n"
                                    + "§7Arremessar invoca raio no ponto de impacto. 3x3 dano elétrico (8). Não "
                                    + "queima blocos. Cooldown curto.",
                            "liberthia:lightning_grenade"),
                    new Page("White Matter Bomb",
                            "§fBomba de Matéria Clara.§r\n\n"
                                    + "§7Right-click pra arremessar. Explode em raio 6: drena §oXP§r§7 dos "
                                    + "jogadores+mobs no raio (até 30 niveis cada). Não destrói blocos.",
                            "liberthia:white_matter_bomb"),
                    new Page("White Matter TNT",
                            "§fTNT branca §lcolocável§r§f.§r\n\n"
                                    + "§7Coloca como bloco, ativa com flint+steel. Explosão sem dano físico, mas "
                                    + "§ovaporiza blocos§r§7 num raio 4 (não dropa). Bom pra clearing.",
                            "liberthia:white_matter_tnt"),
                    new Page("Frost Flask",
                            "§bFrasco de gelo.§r\n\n"
                                    + "§7Right-click pra arremessar. Cria patch de §oice spike§r§7 onde aterra. "
                                    + "Slowness IV em mobs próximos por 8s. Insumo do §dFreeze Staff§r§7.",
                            "liberthia:frost_flask"),
                    new Page("Clear Matter Injector",
                            "§fSeringa de Matéria Clara.§r\n\n"
                                    + "§7Right-click em si mesmo: §o+15 WM§r§7 ao perfil + Speed II por 60s. "
                                    + "Right-click em outro player: aplica neles. Stack até 8.",
                            "liberthia:clear_matter_injector"),
                    new Page("White Matter Syringe",
                            "§fSeringa concentrada de WM.§r\n\n"
                                    + "§7Versão alto-tier do injector. §o+30 WM§r§7 + Speed III. Consume XP do "
                                    + "alvo. Stack até 4.",
                            "liberthia:white_matter_syringe")
            )),

            // ============================================================
            new Chapter("§5Catálogo — Workers", List.of(
                    new Page("Worker Lightning",
                            "§eItem de admin de OP — eletrocuta um worker.§r\n\n"
                                    + "§7Right-click num NPC trabalhador (ou player na lista) = §oraio direcionado§r§7 "
                                    + "no alvo. Útil pra disciplina narrativa.",
                            "liberthia:worker_lightning"),
                    new Page("Worker Teleporter",
                            "§dTeleporta um worker pra coords salvas.§r\n\n"
                                    + "§7§dShift+right-click§r§7 abre tela pra setar destino. §dRight-click§r§7 "
                                    + "num worker o move pra lá.",
                            "liberthia:worker_teleporter"),
                    new Page("Worker Voice Box",
                            "§dCaixa de voz — faz um worker falar uma fala pré-gravada.§r\n\n"
                                    + "§7§dRight-click§r§7 abre lista de falas. Selecione e o worker mais próximo "
                                    + "diz no chat (radius 16).",
                            "liberthia:worker_voice_box"),
                    new Page("Worker Inventory Viewer",
                            "§dVisualiza inventário de um worker.§r\n\n"
                                    + "§7§dRight-click§r§7 num worker = abre GUI mostrando o que ele tá carregando. "
                                    + "Apenas leitura.",
                            "liberthia:worker_inventory_viewer"),
                    new Page("Worker Badge",
                            "§dCrachá de identificação.§r\n\n"
                                    + "§7Equipado num worker o transforma em §oexpedicionário§r§7 — comportamento "
                                    + "muda pra explorar áreas. Drop dele depois é §oRare§r§7.",
                            "liberthia:worker_badge"),
                    new Page("Worker Clone",
                            "§dClone temporário de worker.§r\n\n"
                                    + "§7Spawned pelo §dSummon Staff§r§7. Vida 20, dano 6, dura 30s. Combatível.",
                            "liberthia:worker_clone"),
                    new Page("Execution Stick",
                            "§4Vara de §lexecução§r§4.§r\n\n"
                                    + "§7§dRight-click§r§7 num worker = mata instantaneamente sem drops. Item "
                                    + "narrativo de admin pra eliminar NPCs.",
                            "liberthia:execution_stick")
            )),

            // ============================================================
            new Chapter("§5Catálogo — Lore & Livros", List.of(
                    new Page("Liberthia Manual",
                            "§dEste livro.§r\n\n"
                                    + "§7Seu guia mecânico. Right-click pra abrir. Capítulos navegáveis com setas/"
                                    + "lista lateral. F8 cicla HUD.",
                            "liberthia:liberthia_manual"),
                    new Page("Researcher Codex",
                            "§dCodex narrativo.§r\n\n"
                                    + "§7Right-click avança página. 8 páginas em prosa de pesquisador-Anfitrião. "
                                    + "Mesmo conteúdo deste manual mas em §olore form§r§7.",
                            "liberthia:researcher_codex"),
                    new Page("Field Journal",
                            "§dDiário de campo §leditável§r§d.§r\n\n"
                                    + "§7Right-click abre editor — você escreve livremente. Salva em NBT do item. "
                                    + "Útil pra anotações de exploração.",
                            "liberthia:field_journal"),
                    new Page("Host Journal",
                            "§dDiário do §lAnfitrião-Chefe§r§d.§r\n\n"
                                    + "§7Drop raro de loot da Ilha de Horus / Equilibrium. 8 páginas com lore "
                                    + "exclusiva sobre origem das matérias.",
                            "liberthia:host_journal"),
                    new Page("Research Notes",
                            "§dNotas de pesquisa §lsoltas§r§d.§r\n\n"
                                    + "§7Drop comum em containment chambers e zonas de pesquisa. 1 página cada. "
                                    + "Coleta o conjunto pra montar narrativa.",
                            "liberthia:research_notes"),
                    new Page("Magic Book",
                            "§5Tomo arcano.§r\n\n"
                                    + "§7Right-click ensina um §ofeitiço§r§7 aleatório (dura limitada). Usado com "
                                    + "varinha/cajado depois. Drop de §dBlood Mage§r§7.",
                            "liberthia:magic_book"),
                    new Page("Image Frame Book Builder",
                            "§dCriador de livros §lcom imagens§r§d.§r\n\n"
                                    + "§7Right-click abre editor que aceita upload/paste de imagens. Salva pra "
                                    + "§dimage_frame_book§r§7 distribuível.",
                            "liberthia:image_frame_book_builder"),
                    new Page("Image Frame Book",
                            "§dLivro com imagens §lpré-feitas§r§d.§r\n\n"
                                    + "§7Right-click pra ler. Mostra páginas que misturam texto + imagens. Útil "
                                    + "pra evento/server-narrative.",
                            "liberthia:image_frame_book"),
                    new Page("Dark Blood Test Item",
                            "§4Item de §lteste§r§4 do sistema sangue.§r\n\n"
                                    + "§7§oUsado em desenvolvimento.§r§7 Right-click no chão spawna entities pra "
                                    + "debug. Não use em servidor live.",
                            "liberthia:dark_blood_test_item"),
                    new Page("Book Red Kiriko",
                            "§4Livro lore-story sangrento.§r\n\n"
                                    + "§7Conta a história do culto Red Kiriko. 12 páginas. Drop raro de blood priests.",
                            "liberthia:book_red_kiriko")
            )),

            // ============================================================
            new Chapter("§5Catálogo — Itens Lendários", List.of(
                    new Page("Eye of Horus",
                            "§4Olho da §lentidade Horus§r§4.§r\n\n"
                                    + "§7§l§o+90 DM puro instantâneo§r§7 ao tocar. Right-click = §lrasga buraco§r§7 "
                                    + "pra ilha de Horus por 30s. Item §lbanido§r§7 do servidor live.",
                            "liberthia:eye_of_horus"),
                    new Page("Horus Eye Shard",
                            "§4Fragmento do Olho de Horus.§r\n\n"
                                    + "§7Versão menos potente. §o+30 DM§r§7 ao segurar. Insumo de rituais top-tier "
                                    + "no §dRitual Pedestal§r§7.",
                            "liberthia:horus_eye_shard"),
                    new Page("Eye of Decay",
                            "§4Olho da §ldecadência§r§4.§r\n\n"
                                    + "§7Right-click acelera infecção em raio 16 (corrupted_soil spread). Drop de "
                                    + "Withered entities.",
                            "liberthia:eye_of_decay"),
                    new Page("Withering Eye",
                            "§8Olho murcho.§r\n\n"
                                    + "§7Carrega: aumenta drop chance de §oloot infected§r§7 em +25%. Custa 1 HP "
                                    + "a cada 30s.",
                            "liberthia:withering_eye"),
                    new Page("Equilibrium Crystal",
                            "§eCristal de Equilibrium.§r\n\n"
                                    + "§7§o+50 WM + 50 YM§r§7 ao perfil §linstantaneamente§r§7. Mutação Estrategista "
                                    + "direto na palma da mão. Drop ultra-raro.",
                            "liberthia:equilibrium_crystal"),
                    new Page("Equilibrium Fragment",
                            "§eFragmento menor.§r\n\n"
                                    + "§o+15 WM + 15 YM§r§7. Drop do crystal acima quando partido. Crafta "
                                    + "ferramentas Equilibrium.",
                            "liberthia:equilibrium_fragment"),
                    new Page("Heart of Flesh",
                            "§4Coração que §lbate§r§4.§r\n\n"
                                    + "§7Carrega: §oRegeneração I§r§7 sempre ativa, mas perde 1 dura/min. Drop de "
                                    + "§dFlesh Mother Boss§r§7.",
                            "liberthia:heart_of_flesh"),
                    new Page("Heart of the Mother",
                            "§4Coração da §lFlesh Mother§r§4.§r\n\n"
                                    + "§7Versão lendária. Regen II + immune to bleed. Drop ÚNICO.",
                            "liberthia:heart_of_the_mother"),
                    new Page("Living Flesh",
                            "§4Carne viva.§r\n\n"
                                    + "§7Material crafting de Heart-tier items. Drop de blood mobs.",
                            "liberthia:living_flesh"),
                    new Page("Attacking Flesh",
                            "§4Carne predatória.§r\n\n"
                                    + "§7Right-click no chão lança §oentidade carne§r§7 que ataca por você por 30s.",
                            "liberthia:attacking_flesh"),
                    new Page("Sanguine Core",
                            "§4Núcleo sangrento.§r\n\n"
                                    + "§7Insumo de §dSword Brum§r§7 e §dHemomancer Staff§r§7. Drop de sacrifício "
                                    + "ritual no §dBlood Altar§r§7.",
                            "liberthia:sanguine_core"),
                    new Page("Sanguine Essence",
                            "§4Essência líquida de sangue.§r\n\n"
                                    + "§7Insumo de potions e ritual amplificators. Drop comum de blood mobs.",
                            "liberthia:sanguine_essence"),
                    new Page("Holy Essence",
                            "§6Essência §lsagrada§r§6.§r\n\n"
                                    + "§7Insumo de holy weapons + sanctify orb. Drop de Order Paladins.",
                            "liberthia:holy_essence"),
                    new Page("Purified Essence",
                            "§fEssência purificada.§r\n\n"
                                    + "§7Saída do §dPurification Bench§r§7. Insumo de cures e cleansing items.",
                            "liberthia:purified_essence"),
                    new Page("Singularity Core",
                            "§5Núcleo de singularidade.§r\n\n"
                                    + "§7Item top-tier. Crafta §dDimensional Chest§r§7 e §dWormhole Block§r§7. "
                                    + "Drop ultra-raro de wormholes naturais.",
                            "liberthia:singularity_core"),
                    new Page("Void Crystal",
                            "§8Cristal do vazio.§r\n\n"
                                    + "§7Storage infinito conceitual. Insumo de wormholes e dim chests.",
                            "liberthia:void_crystal"),
                    new Page("Burning Gem",
                            "§6Gema flamejante.§r\n\n"
                                    + "§7Carrega: §oFire Resistance§r§7 sempre. Drop de Blood Volcano.",
                            "liberthia:burning_gem"),
                    new Page("Screaming Soul",
                            "§5Alma penada.§r\n\n"
                                    + "§7Item lore. Right-click solta som triste + partícula soul_fire. Drop de "
                                    + "soul-bound entities.",
                            "liberthia:screaming_soul"),
                    new Page("Desecrated Holy Relic",
                            "§4Relíquia §lprofanada§r§4.§r\n\n"
                                    + "§7Holy item corrompido. Causa Wither II ao segurar. Drop de Order Paladin "
                                    + "killed by infected.",
                            "liberthia:desecrated_holy_relic"),
                    new Page("Blood Cure Pill",
                            "§4Pílula de §lcura§r§4 sangrenta.§r\n\n"
                                    + "§7Right-click pra consumir. Cura efeitos blood/infection. Custo: -2 HP "
                                    + "máximo permanente. Use só em emergência.",
                            "liberthia:blood_cure_pill"),
                    new Page("Clear Matter Pill",
                            "§fPílula de §llimpeza§r§f.§r\n\n"
                                    + "§7Right-click consome. -20 DM/WM/YM (todos os 3). Side effect: 30s de "
                                    + "Slowness II + perda de 5 níveis XP.",
                            "liberthia:clear_matter_pill")
            )),

            // ============================================================
            new Chapter("§5Catálogo — Selos & Espiritual", List.of(
                    new Page("Bone Seal",
                            "§7Selo de osso §l(tier 1)§r§7.§r\n\n"
                                    + "§7Insumo de runas de proteção básicas. Crafta com 4 ossos + ash.",
                            "liberthia:bone_seal"),
                    new Page("Gold Seal",
                            "§eSelo de ouro §l(tier 2)§r§e.§r\n\n"
                                    + "§7Runa nível médio. Permite linkar 2 spiritual_connection.",
                            "liberthia:gold_seal"),
                    new Page("Diamond Seal",
                            "§bSelo de diamante §l(tier 3)§r§b.§r\n\n"
                                    + "§7Runa avançada. Insumo de spiritual_link entre dimensões.",
                            "liberthia:diamond_seal"),
                    new Page("Netherite Seal",
                            "§8Selo de netherite §l(tier max)§r§8.§r\n\n"
                                    + "§7Runa lendária. Permanente — não quebra.",
                            "liberthia:netherite_seal"),
                    new Page("Spiritual Connection",
                            "§5Item §lpassivo§r§5 de conexão.§r\n\n"
                                    + "§7Equipado: você sente §opresença§r§7 de outros wielders num raio de 64. "
                                    + "Brilha quando perto. Pareado pelo §dRitual Pedestal§r§7.",
                            "liberthia:spiritual_connection"),
                    new Page("Spiritual Link",
                            "§5Liga 2 jogadores §lcross-dim§r§5.§r\n\n"
                                    + "§7Right-click cria copy do item — dê pra outro player. Quando ambos têm, "
                                    + "podem trocar items via §dRitual Pedestal§r§7 ou GUI specifc.",
                            "liberthia:spiritual_link"),
                    new Page("Spawn Eggs (admin)",
                            "§7Spawn eggs disponíveis:\n\n"
                                    + "§7• §4flesh_mother_boss_spawn_egg§r — boss\n"
                                    + "§7• §6order_paladin_spawn_egg§r — friendly NPC\n\n"
                                    + "§oApenas comando admin / creative.§r"),
                    new Page("Glitch Block",
                            "§5Bloco de §lglitch§r§5.§r\n\n"
                                    + "§7Bloco invisível com hitbox sólido. Só §dRevelation Lens§r§7 mostra. "
                                    + "Trap em puzzles e dungeons custom.",
                            "liberthia:glitch_block"),
                    new Page("Wormhole Block",
                            "§5Bloco de §lwormhole§r§5.§r\n\n"
                                    + "§7Conecta dois pontos. Andar nele teleporta pro par. Pareado por NBT — "
                                    + "use §dSingularity Core§r§7 + ritual.",
                            "liberthia:wormhole_block")
            )),

            // ============================================================
            new Chapter("§5As Três Ilhas", List.of(
                    new Page("Liberthia (esta ilha)",
                            "§dA mais habitável.§r §oOnde você está agora.§r\n\n"
                                    + "§7As três matérias coexistem em quantidades pequenas — gerenciáveis. O "
                                    + "Anfitrião-Chefe vive aqui com a família.\n\n"
                                    + "§7§lAcesso:§r§7 spawn natural. Não precisa portal.\n"
                                    + "§7§lDimensão:§r§7 Overworld."),
                    new Page("Ilha de Horus",
                            "§4A mais letal.§r §oNão tente acessar.§r\n\n"
                                    + "§4Acesso:§r§7 Nether (selado).\n"
                                    + "§4Dominância:§r§7 §lapenas Matéria Escura§r§7, em estado caótico extremo.\n\n"
                                    + "§7§l50 expedicionários enviados§r§7. §c10 retornaram§r§7. Poucos coerentes.\n\n"
                                    + "§7Sobreviventes descrevem §ojogos de sobrevivência§r§7, §otortura psicológica§r§7 "
                                    + "e §omortalidade§r§7 sistemática. O ambiente parece ser §lponto de origem do "
                                    + "próprio Nether§r§7 — não um sub-produto dele.\n\n"
                                    + "§4§lPortal selado em definitivo.§r"),
                    new Page("Ilha Equilibrium",
                            "§eA mais enigmática.§r §oTalvez bonita demais.§r\n\n"
                                    + "§e§lAcesso:§r§7 anomalia espacial dentro do Twilight.\n"
                                    + "§e§lDominância:§r§7 §eYM§r§7 alta, §fWM§r§7 moderada, §0§lzero DM§r§7.\n\n"
                                    + "§7§l30 expedicionários§r§7. §a5 retornaram§r§7. §c25 ESCOLHERAM ficar§r§7 — "
                                    + "destruíram seus rastreadores voluntariamente.\n\n"
                                    + "§7Habitada por uma §6entidade solar§r§7: forma de sol, amarela e branca, "
                                    + "constelações orbitando. Persuasiva. Cuidadosa. §oSerena.§r\n\n"
                                    + "§7Outras criaturas, §oalongadas e curvas como bananas§r§7, parecem ser "
                                    + "subjugadas por essa entidade.")
            )),

            // ============================================================
            new Chapter("§5Notas Finais", List.of(
                    new Page("Boas práticas",
                            "§a§l1.§r§7 §dNunca§r§7 carregue blocos de matéria escura sem luva.\n\n"
                                    + "§a§l2.§r§7 Use o §dAnalyzer§r§7 antes de consumir qualquer item desconhecido.\n\n"
                                    + "§a§l3.§r§7 Lasers ativos = §lbomba ambulante§r§7. Desligue antes de quebrar.\n\n"
                                    + "§a§l4.§r§7 Monitore o §dHUD§r§7. Se uma das barras cresce sem você querer, "
                                    + "saia da exposição §oimediatamente§r§7.\n\n"
                                    + "§a§l5.§r§7 §dRifts dimensionais§r§7 são instáveis. Não fique nele por horas."),
                    new Page("Última nota do Anfitrião-Chefe",
                            "§o§7Nem sempre uma cobaia volta. Nem sempre um pesquisador escolhe voltar.§r\n\n"
                                    + "§o§7Mantenha o controle, registre tudo, e §lnunca§r§o confie em nada que "
                                    + "sussurra de trás de seus olhos.§r\n\n"
                                    + "§7§7— §dAnfitrião-Chefe§r§7, último registro antes do encerramento da expedição "
                                    + "à Ilha de Horus.\n\n"
                                    + "§8§oAssine: ____________________§r")
            ))
    );
}
