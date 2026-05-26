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
                            "§8[REMOVIDO v0.1.7]§r §dSifão de extração §lsafe§r§d.§r\n\n"
                                    + "§7Tirava matéria escura de um inventário sem causar dano de radiação ao "
                                    + "manipulador. Insumo de pesquisa avançada.\n\n"
                                    + "§c§lAvisar:§r§7 item removido do mod. Saves antigos com este item perdem "
                                    + "ao logar. Doc mantida pra histórico/referência.")
            )),

            // ============================================================
            new Chapter("§5Catálogo — Detecção", List.of(
                    new Page("Bússola Dimensional",
                            "§dRight-click§r§7 escaneia rifts da dimensão atual e aponta pro mais próximo.\n\n"
                                    + "§7Mostra coords + distância em metros no chat. NBT armazenado — brilha "
                                    + "quando tem alvo trancado. Update a cada uso.",
                            "liberthia:dimensional_compass"),
                    new Page("White Matter Finder",
                            "§8[REMOVIDO v0.1.7]§r §fDetector de §lmatéria clara§r§f no terreno.§r\n\n"
                                    + "§7Right-click escaneava 32 blocos em todas direções. Apontava o §ominério WM§r§7 "
                                    + "mais próximo. Útil em deserts e tundras.\n\n"
                                    + "§c§lAvisar:§r§7 item removido do mod. Substituto sugerido: §dGeiger Counter§r§7 "
                                    + "+ exploração manual de biomas frios."),
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
                                    + "sem amostra; rosto cheio com líquido roxo quando preenchido.\n\n"
                                    + "§a§lCraft (v0.1.7+):§r §73x Glass Pane em V + 1x Iron Nugget centro.\n"
                                    + "§7Detalhes completos em §dAnalyzer e Curas§r§7 > §oSample Vial workflow§r.",
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
                                    + "§7Right-click consome 1 purified_essence + cria §oaura sagrada§r§7 ao redor "
                                    + "(raio 6) por 60s. Repele mobs infected/dark.",
                            "liberthia:sanctify_orb"),
                    new Page("Purity Beacon",
                            "§fFarol de pureza colocável.§r\n\n"
                                    + "§7Bloco em formato cone. Em raio de 16 blocos: drena §dDM§r§7 do perfil "
                                    + "dos jogadores em -1/seg. Custo: 1 purified_essence por minuto ativo.",
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
                            "§8[REMOVIDO v0.1.7]§r §6Essência §lsagrada§r§6.§r\n\n"
                                    + "§7Era insumo de holy weapons + sanctify orb. Drop de Order Paladins.\n\n"
                                    + "§c§lAvisar:§r§7 item removido do mod. Migrado pra §dPurified Essence§r§7 "
                                    + "em todas as receitas das máquinas (DarkMatterForge, MatterInfuser, "
                                    + "MatterTransmuter, ResearchTable, ContainmentChamber)."),
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
            )),

            // ============================================================
            // MANUAL DE OPERAÇÃO — Bench/Forge/Infuser/Research/Chamber/Transmuter/Alchemizer
            // ============================================================
            new Chapter("§dManual de Operação", List.of(
                    new Page("Como ler este capítulo",
                            "§7Este capítulo documenta os §d7 blocos principais§r§7 da progressão Liberthia:\n\n"
                                    + "§a§l1.§r§7 §dPurification Bench§r§7 — limpa contaminação\n"
                                    + "§a§l2.§r§7 §dDark Matter Forge§r§7 — refina DM com fuel\n"
                                    + "§a§l3.§r§7 §dMatter Infuser§r§7 — infunde matéria em items\n"
                                    + "§a§l4.§r§7 §dResearch Table§r§7 — pesquisa e lore\n"
                                    + "§a§l5.§r§7 §dContainment Chamber§r§7 — contém criaturas\n"
                                    + "§a§l6.§r§7 §dMatter Transmuter§r§7 — converte tipos de matéria\n"
                                    + "§a§l7.§r§7 §dDark Matter Alchemizer§r§7 — alquimia top-tier\n\n"
                                    + "§7Cada página mostra: §ocomo craftar, ingredientes, output, receita visual§r§7."),

                    new Page("Purification Bench",
                            "§b§lPurificação básica§r§7 — converte items contaminados em essência limpa.\n\n"
                                    + "§a§lUso:§r§7 right-click no bloco abre GUI. Insira:\n"
                                    + "§7• §oslot 0§r: item contaminado (ex: dark_matter_shard)\n"
                                    + "§7• §oslot 1§r: catalisador (water_bottle)\n"
                                    + "§7• §oslot 2§r: saída (purified_essence)\n\n"
                                    + "§e§lCraft:§r §7Purified Essence (3x topo) + Quartz Block (2x lateral) + "
                                    + "Glass (centro) + Smooth Stone (3x base).\n\n"
                                    + "§c§lDrop:§r §7Quebra → dropa o bloco self.",
                            "liberthia:purification_bench"),

                    new Page("Dark Matter Forge",
                            "§5§lForja avançada§r§7 — combina dois insumos com fuel para criar novos items.\n\n"
                                    + "§a§lUso:§r§7 right-click abre GUI com 4 slots:\n"
                                    + "§7• §oslot 0§r: §dFuel§r (dark_matter_shard, dark_matter_bucket, dark_matter_block)\n"
                                    + "§7• §oslot 1§r: §dInput 1§r (insumo principal)\n"
                                    + "§7• §oslot 2§r: §dInput 2§r (insumo secundário)\n"
                                    + "§7• §oslot 3§r: §dOutput§r\n\n"
                                    + "§e§lReceitas conhecidas:§r\n"
                                    + "§7• DM Shard + Iron Ingot → Stabilized DM\n"
                                    + "§7• Stabilized DM + Void Crystal → Singularity Core\n"
                                    + "§7• DM Block + Purified Essence → 2x Purified Essence\n\n"
                                    + "§c§lDrop:§r §7Quebra → dropa o bloco self + items dentro caem no chão.",
                            "liberthia:dark_matter_forge"),

                    new Page("Matter Infuser",
                            "§3§lInfusão de matéria§r§7 — adiciona propriedades de matter a items existentes.\n\n"
                                    + "§a§lUso:§r§7 GUI com 5 slots (DM, CM, YM, Catalyst, Output).\n\n"
                                    + "§e§lExemplo de receita:§r\n"
                                    + "§7• Dark Matter Block + Clear Matter Block + Purified Essence → "
                                    + "3x Purified Essence\n"
                                    + "§7• ... + Yellow Matter Ingot → 4x Clear Matter Pill\n\n"
                                    + "§b§lCraft:§r §7Iron Ingot (4x cruz) + Glass (4x lateral) + "
                                    + "Quartz Block (centro).\n\n"
                                    + "§c§lDrop:§r §7Bloco self + items dentro.",
                            "liberthia:matter_infuser"),

                    new Page("Research Table",
                            "§e§lMesa de pesquisa§r§7 — produz §dwritten books§r§7 e desbloqueia receitas.\n\n"
                                    + "§a§lUso:§r§7 GUI com 3 slots (Material, Paper, Output).\n\n"
                                    + "§e§lReceitas:§r\n"
                                    + "§7• §dDark Matter Shard§r + §oPaper§r → §dWritten Book§r (research notes)\n"
                                    + "§7• §dPurified Essence§r + §oBook§r → §dWritten Book§r (lore pages)\n"
                                    + "§7• §dClear Matter Block§r + §oPaper§r → §dClear Matter Pill§r\n\n"
                                    + "§b§lCraft:§r §7Books (2x topo) + Iron Ingot (centro) + "
                                    + "Oak Planks (3x meio) + Sticks (laterais base).",
                            "liberthia:research_table"),

                    new Page("Containment Chamber",
                            "§b§lCâmara de contenção§r§7 — neutraliza energia hostil de blocos de matter.\n\n"
                                    + "§a§lUso:§r§7 GUI com 4 slots — 2 inputs + 1 catalyst + 1 output.\n\n"
                                    + "§e§lReceitas:§r\n"
                                    + "§7• §dDark Matter Block§r + §dDark Matter Shard§r → 4x §dDark Matter Shard§r\n"
                                    + "§7• §dYellow Matter Block§r + §dPurified Essence§r → §dSingularity Core§r\n\n"
                                    + "§b§lCraft:§r §7Obsidian (4x cantos) + Iron Block (2x topo/base meio) + "
                                    + "Glass (2x laterais meio) + Dark Matter Shard (centro).\n\n"
                                    + "§c§lDrop:§r §7Bloco self.",
                            "liberthia:containment_chamber"),

                    new Page("Matter Transmuter",
                            "§6§lTransmutação§r§7 — converte um tipo de matter em outro usando catalisador.\n\n"
                                    + "§a§lUso:§r§7 GUI com 3 slots (Input, Catalyst, Output).\n\n"
                                    + "§e§lReceitas:§r\n"
                                    + "§7• §dDark Matter Block§r + §dPurified Essence§r → §dClear Matter Block§r\n"
                                    + "§7• §dClear Matter Block§r + §dYellow Matter Ingot§r → §dYellow Matter Block§r\n\n"
                                    + "§b§lCraft:§r §7Gold ingots + Quartz Block + Glass — receita em "
                                    + "data/liberthia/recipes/matter_transmuter.json.\n\n"
                                    + "§c§lDrop:§r §7Bloco self + items dentro.",
                            "liberthia:matter_transmuter"),

                    new Page("Dark Matter Alchemizer",
                            "§5§lAlquimia top-tier§r§7 — combina os processos das outras máquinas em um "
                                    + "fluxo único. Alta resistência (1200F blast resist).\n\n"
                                    + "§a§lUso:§r§7 Tem GUI complexa — abre tela full-screen com tabs (não menu vanilla).\n\n"
                                    + "§e§lFunções:§r\n"
                                    + "§7• Combina §dForge§r + §dTransmuter§r§7 em um só\n"
                                    + "§7• Suporta §oupgrades§r§7 (Speed, Efficiency, Capacity)\n"
                                    + "§7• Output multi-slot\n\n"
                                    + "§c§lDrop:§r §7Bloco self. §lAtenção:§r§7 explosões fortes (TNT, creepers) "
                                    + "ainda podem destruir — 1200F é alto, não infinito.",
                            "liberthia:dark_matter_alchemizer")
            )),

            // ============================================================
            // PIPES E LOGÍSTICA
            // ============================================================
            new Chapter("§dPipes e Logística", List.of(
                    new Page("Visão geral",
                            "§7O sistema de §dpipes§r§7 do Liberthia transporta items entre inventários "
                                    + "(baús, máquinas, dispensers, etc) sem precisar de hopper.\n\n"
                                    + "§a§l3 blocos principais:§r\n"
                                    + "§7• §dItem Pipe§r§7 — só transporta (sem origem/destino)\n"
                                    + "§7• §dItem Extractor§r§7 — pega items de inventário adjacente\n"
                                    + "§7• §dItem Inserter§r§7 — coloca items em inventário adjacente\n\n"
                                    + "§a§l3 upgrades:§r §7Speed (4 ou 64 items/op), Efficiency (gasta menos), "
                                    + "Capacity (filtros maiores)."),

                    new Page("Como conectar",
                            "§a§l1.§r§7 Coloque §dItem Extractor§r§7 GRUDADO num baú/inventário fonte.\n\n"
                                    + "§a§l2.§r§7 Estenda a rede com §dItem Pipes§r§7 (qualquer caminho — eles "
                                    + "se conectam automaticamente).\n\n"
                                    + "§a§l3.§r§7 Coloque §dItem Inserter§r§7 GRUDADO no destino.\n\n"
                                    + "§a§l4.§r§7 §lShift+click§r§7 numa face do pipe pra ciclar modo:\n"
                                    + "§7•   §oDEFAULT§r — passa items mas não extrai/inseri direto\n"
                                    + "§7•   §oEXTRACT§r — puxa items dessa face\n"
                                    + "§7•   §oINSERT§r — manda items pra essa face\n"
                                    + "§7•   §oDISABLED§r — face fechada (item não passa)"),

                    new Page("Bug v0.1.7 corrigido",
                            "§c§lAntes:§r§7 ao puxar de um baú com vários tipos de item, o pipe gastava todo "
                                    + "o budget no slot 0 (1 tipo só). Tinha que esperar esse tipo acabar antes "
                                    + "de chegar nos outros.\n\n"
                                    + "§a§lAgora:§r§7 budget é POR SLOT. Todos os tipos de item se movem "
                                    + "§lsimultaneamente§r§7 no mesmo tick.\n\n"
                                    + "§7Resultado: pipe agora é §oN×§r§7 mais rápido em baús com muitos "
                                    + "tipos diferentes (onde N = nº de slots ocupados)."),

                    new Page("Speed Upgrade",
                            "§c§lSpeed Upgrade§r§7 — instala numa face do pipe (right-click com upgrade na mão).\n\n"
                                    + "§a§lEfeito:§r §71x = 4 items/op. §o2x = 16 items/op. 3x = 64 items/op§r§7.\n\n"
                                    + "§e§lCraft:§r §7Feather (centro) + Redstone (4x cruz) + Iron Ingot (4x cantos).",
                            "liberthia:speed_upgrade"),

                    new Page("Efficiency Upgrade",
                            "§b§lEfficiency Upgrade§r§7 — reduz consumo de energia (ou cooldown) da operação.\n\n"
                                    + "§a§lEfeito:§r §71x = -25% cooldown. 2x = -50%. 3x = -75%.\n\n"
                                    + "§e§lCraft:§r §7Copper Ingot (centro) + Redstone Torch (4x cruz) + "
                                    + "Iron Ingot (4x cantos).",
                            "liberthia:efficiency_upgrade"),

                    new Page("Capacity Upgrade",
                            "§6§lCapacity Upgrade§r§7 — aumenta tamanho da lista de filtros do pipe.\n\n"
                                    + "§a§lEfeito:§r §71x = 8 slots. 2x = 18 slots. 3x = 36 slots.\n\n"
                                    + "§e§lCraft:§r §7Shulker Shell (centro) + Leather (4x cruz) + "
                                    + "Iron Ingot (4x cantos).",
                            "liberthia:capacity_upgrade")
            )),

            // ============================================================
            // MATTER ANALYZER + CURAS
            // ============================================================
            new Chapter("§dAnalyzer e Curas", List.of(
                    new Page("Matter Analyzer — usage",
                            "§5§lAnalisador de matéria§r§7 — \"computador\" de leitura.\n\n"
                                    + "§a§lUso:§r§7 GUI com 1 slot input. Coloca um item e mostra:\n"
                                    + "§7• §dDM§r§7 / §fWM§r§7 / §eYM§r§7 atual\n"
                                    + "§7• Tipo de mutação composta (DARK, WHITE, YELLOW, mistas)\n"
                                    + "§7• Total de matéria (soma)\n\n"
                                    + "§7Aceita §oSample Vials§r§7 preenchidos (leitura do NBT) ou items do "
                                    + "§oMatterContentRegistry§r§7 (estático)."),

                    new Page("Sample Vial workflow",
                            "§a§l1.§r§7 Craft §dSample Vial§r§7 (3x Glass Pane em V + Iron Nugget centro).\n\n"
                                    + "§a§l2.§r§7 Right-click num bloco — coleta amostra (NBT do bloco + "
                                    + "varredura de aura DM em raio 5 ao redor).\n\n"
                                    + "§a§l3.§r§7 Coloca o vial preenchido no §dMatter Analyzer§r§7.\n\n"
                                    + "§a§l4.§r§7 Lê os valores. Vial é §oreusável§r§7 — vazia e reusa."),

                    new Page("Matter Cure",
                            "§b§lCura de emergência§r§7 — zera DM/WM/YM do player + dá Regen II + Resistance I "
                                    + "por 10s.\n\n"
                                    + "§a§lQuando usar:§r§7 quando seu HUD de matéria tá pra explodir ou você "
                                    + "absorveu demais de uma matéria que não queria.\n\n"
                                    + "§c§lLimitação:§r§7 se você não tem matéria pra purificar, NÃO consome "
                                    + "(evita desperdício).\n\n"
                                    + "§e§lCraft:§r §7Glowstone Dust (topo) + 2x Purified Essence (lateral) + "
                                    + "Clear Matter Pill (centro) + Glass Bottle (base).",
                            "liberthia:matter_cure"),

                    new Page("Daily Pill",
                            "§e§lPílula diária§r§7 — manutenção rotineira.\n\n"
                                    + "§a§lEfeito (1 dia in-game = 20min real):§r\n"
                                    + "§7• §dResistance I§r§7 — reduz dano em 20%\n"
                                    + "§7• §dRegeneration I§r§7 — cura passiva\n"
                                    + "§7• §dAbsorption I§r§7 (1min) — +2 corações de escudo\n\n"
                                    + "§a§lNão zera matter§r§7 — só amortece o dano de exposição.\n\n"
                                    + "§e§lCraft:§r §74x Glow Berries + 2x Sugar + 2x Nether Wart + Clear Matter Pill (centro) "
                                    + "→ §o4 pílulas§r§7.",
                            "liberthia:daily_pill")
            )),

            // ============================================================
            // CATÁLOGO EXAUSTIVO — INÍCIO (v0.1.7)
            // Documenta TODOS os 248 items + 102 blocos por categoria.
            // ============================================================

            new Chapter("§dCatálogo — Matter (Items)", List.of(
                    new Page("Visão geral — Sistema Matter",
                            "§7Liberthia gira em torno de §53 tipos de matéria§r§7:\n\n"
                                    + "§5• Dark Matter (DM)§r§7 — caótica, infectante, mais poderosa\n"
                                    + "§f• Clear Matter (CM)§r§7 — pura, neutralizadora\n"
                                    + "§e• Yellow Matter (YM)§r§7 — solar, divina, mais rara\n\n"
                                    + "§7Cada uma tem: §oingot/shard/bloco/balde + ferramentas + armadura§r§7. "
                                    + "Combina-se em máquinas (§dForge§r§7, §dTransmuter§r§7, §dInfuser§r§7, etc) "
                                    + "pra criar items top-tier."),

                    new Page("Dark Matter Shard",
                            "§5Fragmento básico de matéria escura.§r\n\n"
                                    + "§a§lObtenção:§r\n"
                                    + "§7• Drop na mineração (stone/deepslate/etc) — chance 0.5-3% por Y baixo\n"
                                    + "§7• Quebrar §dDark Matter Ore§r§7\n"
                                    + "§7• Sacrificar em Blood Altar com mob mob hostile\n\n"
                                    + "§a§lUso:§r §7Combustível da §dDark Matter Forge§r§7. Insumo de "
                                    + "§dStabilized DM§r§7, §dContainment Chamber§r§7. TNT em cima dele "
                                    + "= §dYellow Matter Ingot§r§7 (v0.1.7).",
                            "liberthia:dark_matter_shard"),

                    new Page("Inactive / Active / Stabilized DM",
                            "§5§lCadeia de refinação do Dark Matter:§r\n\n"
                                    + "§a§l1. Inactive Dark Matter§r§7 (40 DM/8 WM) — versão CRUA, drop em "
                                    + "mobs corrompidos ou refinada do shard.\n\n"
                                    + "§a§l2. Active Dark Matter§r§7 (80 DM) — ativada com §dPurified Essence§r§7 "
                                    + "no Matter Infuser. Estado volátil — instabilidade alta.\n\n"
                                    + "§a§l3. Stabilized Dark Matter§r§7 — pré-Singularity Core. Combinada com "
                                    + "§dVoid Crystal§r§7 no Dark Matter Forge vira o core."),

                    new Page("Dark Matter Catalyst",
                            "§5Catalisador denso.§r\n\n"
                                    + "§a§lFunção:§r§7 acelera reações no Matter Infuser e Alchemizer. "
                                    + "Reduz tempo de craft em ~40%.\n\n"
                                    + "§e§lCraft:§r §7DM Shard (5x) + Void Crystal + Glowstone — receita em "
                                    + "data/liberthia/recipes/dark_matter_catalyst.json.\n\n"
                                    + "§7Carga interna: §o30 DM / 5 WM / 5 YM§r§7 (composto).",
                            "liberthia:dark_matter_catalyst"),

                    new Page("Dark Matter Cell",
                            "§5Célula de armazenamento.§r\n\n"
                                    + "§7Bateria portátil — guarda até §o100 DM§r§7. Slot do §dDark Matter "
                                    + "Generator§r§7 e em armaduras avançadas.\n\n"
                                    + "§e§lCraft:§r §7Iron + Active DM + Glass (em formato bateria).",
                            "liberthia:dark_matter_cell"),

                    new Page("Dark Matter Bucket",
                            "§5Líquido escuro.§r\n\n"
                                    + "§a§lObtenção:§r §7Right-click com balde vazio em fonte de DM líquida "
                                    + "(profundo no overworld) OU em Blood Volcano.\n\n"
                                    + "§a§lUso:§r §7Combustível da §dDark Matter Forge§r§7 — 1 balde = "
                                    + "5 minutos de queima. Tomar por engano = §oWither IV§r§7.",
                            "liberthia:dark_matter_bucket"),

                    new Page("Clear Matter Block / Pill / Bucket",
                            "§f§lMatéria Clara — purificadora.§r\n\n"
                                    + "§a§lClear Matter Block§r§7 — encontrado em §obiomas frios§r§7 (snowy "
                                    + "biomes). Quase peso zero. Insumo da Pill e Bucket.\n\n"
                                    + "§a§lClear Matter Pill§r§7 (stack 16) — consumível. Reduz §dWhite "
                                    + "Matter§r§7 do perfil em 30. Cura infecção dark leve.\n\n"
                                    + "§a§lClear Matter Bucket§r§7 — líquido. Apaga blood_fire instantâneo.",
                            "liberthia:clear_matter_pill"),

                    new Page("Yellow Matter Ingot",
                            "§eIngot dourado raro.§r\n\n"
                                    + "§a§lObtenção:§r\n"
                                    + "§7• §dDark Matter Shard§r§7 + TNT explodindo → §oconversão 1:1§r§7\n"
                                    + "§7• Matter Transmuter (Clear Block + outro Ingot = Yellow Block)\n"
                                    + "§7• Drop ultra-raro de Order Paladin\n\n"
                                    + "§a§lUso:§r §7Insumo de armadura Yellow + Shield + Sword Yellow.",
                            "liberthia:yellow_matter_ingot"),

                    new Page("Yellow Matter Block / Bucket",
                            "§e§lForma condensada de YM.§r\n\n"
                                    + "§7Block: 9 ingots compactados. Brilha forte (light level 12).\n"
                                    + "§7Bucket: forma líquida — encontrado em raros geysers dourados."),

                    new Page("White Matter Ore / Bomb / TNT / Syringe",
                            "§f§lMatéria Branca — items legacy.§r\n\n"
                                    + "§a§lWhite Matter Ore§r§7: minério em biomas geados. Drop §oraw WM§r§7 "
                                    + "(removido do registry em v0.1.7 mas o block ainda existe pra worldgen).\n\n"
                                    + "§a§lWhite Matter Bomb / TNT§r§7: explosivo que cria zona de §oCM forte§r§7 "
                                    + "no raio — purifica blocos infected.\n\n"
                                    + "§a§lWhite Matter Syringe§r§7: injeta WM no jogador (+50). Risco se "
                                    + "perfil já tem muito YM (mutação cruzada)."),

                    new Page("Singularity Core",
                            "§5Núcleo de singularidade — top-tier.§r\n\n"
                                    + "§a§lObtenção:§r §7Stabilized DM + Void Crystal no Dark Matter Forge.\n\n"
                                    + "§a§lUso:§r §7Crafta §dDimensional Chest§r§7 + §dWormhole Block§r§7. "
                                    + "Drop ULTRA-raro de wormholes naturais (rifts).",
                            "liberthia:singularity_core"),

                    new Page("Void Crystal",
                            "§8Cristal do vazio.§r\n\n"
                                    + "§a§lObtenção:§r §7Drop em rifts dimensionais (raríssimo). Pode ser "
                                    + "criado em Containment Chamber (DM Block + DM Shard).\n\n"
                                    + "§a§lUso:§r §7Storage infinito conceitual. Insumo do Singularity Core "
                                    + "e Wormhole Block.",
                            "liberthia:void_crystal"),

                    new Page("Tainted Essence",
                            "§5Essência contaminada.§r\n\n"
                                    + "§a§lObtenção:§r §7Drop de mobs corrompidos / Infected Vein break.\n\n"
                                    + "§a§lUso:§r §7Pode ser purificada na §dPurification Bench§r§7 pra virar "
                                    + "§dPurified Essence§r§7. 3 Tainted → 1 Purified.",
                            "liberthia:tainted_essence"),

                    new Page("Purified Essence",
                            "§fEssência purificada.§r\n\n"
                                    + "§a§lObtenção:§r §7Saída da §dPurification Bench§r§7. Também drop "
                                    + "de Order Paladins.\n\n"
                                    + "§a§lUso:§r §7Catalisador em DarkMatterForge / MatterTransmuter / "
                                    + "MatterInfuser. Substituto da Holy Essence em todas as receitas.",
                            "liberthia:purified_essence"),

                    new Page("Equilibrium Crystal / Fragment",
                            "§dCristal do equilíbrio.§r\n\n"
                                    + "§7Equilibrium Crystal carrega §o50 WM + 50 YM§r§7 (sem DM) — único "
                                    + "item com balanço perfeito de matter.\n\n"
                                    + "§a§lFragment§r§7: drop comum. §a§lCrystal§r§7: 4 fragments + Singularity "
                                    + "Core na Containment Chamber.\n\n"
                                    + "§7Insumo do §dPriest Sigil§r§7."),

                    new Page("Matter Ampoule",
                            "§5Ampola de matter pura.§r\n\n"
                                    + "§7Container de vidro reforçado. Right-click em bloco/entidade → "
                                    + "armazena estado de matter pra análise depois.\n\n"
                                    + "§a§lCraft:§r §7Glass Bottle + DM Shard + Iron — em data/recipes/matter_ampoule.json"),

                    new Page("Matter Core",
                            "§dNúcleo de matter denso.§r\n\n"
                                    + "§7Bloco-básico pra construções avançadas que precisam canalizar matter. "
                                    + "Usado em estruturas como Phantom Portal frame.\n\n"
                                    + "§a§lCraft:§r §7DM Block + CM Block + YM Block + Singularity Core."),

                    new Page("Horus Eye Shard",
                            "§eFragmento do Olho de Horus.§r\n\n"
                                    + "§7Item lore raríssimo. Drop em expedições à Ilha de Horus. Carrega "
                                    + "§o90 DM§r§7 mas é §oneutralizado§r§7 visualmente — não causa infecção.\n\n"
                                    + "§7Crafta §dEye of Horus§r§7 (item lore final)."),

                    new Page("Unstable Matter Block",
                            "§5§lMatéria instável!§r\n\n"
                                    + "§c§lAtenção:§r §7explode quando minerado com ferramenta errada. Use "
                                    + "§dContainment Glove§r§7 ou §dYellow Matter Pickaxe§r§7.\n\n"
                                    + "§7Drop: 2-4 §dActive Dark Matter§r§7 quando minerado seguro.")
            )),

            new Chapter("§dCatálogo — Sangue & Blood", List.of(
                    new Page("Visão geral — Blood Economy",
                            "§4§lEconomia do Sangue§r§7 — paralela ao sistema de matter, focada em §oritual§r§7 "
                                    + "e §osacrifício§r§7.\n\n"
                                    + "§a§l3 níveis de progressão:§r\n"
                                    + "§7• §oVials§r — coleta de sangue básico (mob/player)\n"
                                    + "§7• §oRituais§r — Blood Altar + chalk_symbols ativam efeitos\n"
                                    + "§7• §oSanguine§r — wood set + ward armor (top-tier blood)\n\n"
                                    + "§4§lAtenção:§r §7Blood Pact Amulet pode §ocorromper§r§7 seu perfil "
                                    + "permanentemente."),

                    new Page("Blood Vial / Filled",
                            "§4Frasco de sangue.§r\n\n"
                                    + "§a§lObtenção:§r §7Glass Bottle + sacrifício no Blood Altar = vial "
                                    + "vazio. Right-click em mob ferido (HP <50%) = vial preenchido.\n\n"
                                    + "§a§lUso:§r §7Insumo de §dBlood Cure Pill§r§7, alimentação do "
                                    + "§dBlood Cauldron§r§7, ingrediente em sacrificial bowls.\n\n"
                                    + "§e§lSmelting:§r §7Vial Filled smelta em §dCongealed Blood§r§7 (item).",
                            "liberthia:blood_vial"),

                    new Page("Blood Bucket",
                            "§4Balde de sangue.§r\n\n"
                                    + "§7Forma líquida — só obtém em §dBlood Volcano§r§7 ou §dBlood Fountain§r§7. "
                                    + "Coloca como fluido (queima entities que entram).\n\n"
                                    + "§a§lUso:§r §7Combustível alternativo, alimenta Blood Altar pra ativar "
                                    + "rituais de alta tier."),

                    new Page("Blood Armor Set",
                            "§4§lArmadura sangue completa.§r\n\n"
                                    + "§a§lPeças:§r §7Helmet, Chestplate, Leggings, Boots.\n\n"
                                    + "§a§lEfeito:§r §7+15% dano em mobs sanguinários (vampire/cult mob). "
                                    + "Imune ao §dblood_fire§r§7. Drena §o1 HP/min§r§7 do jogador (custo).\n\n"
                                    + "§e§lCraft:§r §7Congealed Blood + Iron + Sanguine Leather."),

                    new Page("Blood Bow",
                            "§4Arco vampírico.§r\n\n"
                                    + "§7Dispara flechas que §olifesteal§r§7 — 25% do dano vira HP pro atirador. "
                                    + "Não funciona em players (PvP-safe).\n\n"
                                    + "§e§lCraft:§r §7Receita em data/recipes/blood_bow.json — Sanguine Wood + "
                                    + "String + Blood Vial Filled.",
                            "liberthia:blood_bow"),

                    new Page("Blood Scythe",
                            "§4Foice sangrenta.§r\n\n"
                                    + "§7Arma de AOE — golpea 3 entities em arco. Cada hit aplica "
                                    + "§oBleeding§r§7 (2 HP/s por 6s).\n\n"
                                    + "§a§lDano base:§r §o9§r§7. §a§lDurabilidade:§r §o620§r§7.",
                            "liberthia:blood_scythe"),

                    new Page("Blood Ritual Dagger",
                            "§4Adaga ritual.§r\n\n"
                                    + "§7Mata em 1 hit qualquer mob com HP ≤ 4 (incluindo bosses na fase "
                                    + "final). Drop chance §olife essence§r§7 quadruplicada.\n\n"
                                    + "§a§lUso especial:§r §7Right-click no próprio player no §dBlood Altar§r§7 "
                                    + "ativa §lblood pact§r§7 — sacrifica 50% HP e converte em §dActive DM§r§7.",
                            "liberthia:blood_ritual_dagger"),

                    new Page("Hemomancer Staff",
                            "§4Cajado hemomante.§r\n\n"
                                    + "§a§lModos (shift-right-click cicla):§r\n"
                                    + "§7• §oDispara§r — flecha de sangue (8 dano)\n"
                                    + "§7• §oAOE§r — explosão em raio 5 (10 dano)\n"
                                    + "§7• §oHeal-self§r — converte 20 sangue/inv em 4 HP\n"
                                    + "§7• §oSummon§r — invoca §obloodwarden§r§7 temporário (60s)\n\n"
                                    + "§a§lCusto:§r §7cada modo gasta §o1 Blood Vial Filled§r§7 do inv.",
                            "liberthia:hemomancer_staff"),

                    new Page("Blood Cure Pill",
                            "§aPílula de cura blood.§r\n\n"
                                    + "§a§lEfeito:§r §7zera §oBlood Infection§r§7 do perfil + Regen II por 10s. "
                                    + "Stack até 16.\n\n"
                                    + "§e§lCraft:§r §7Blood Vial Filled + Glass Bottle + Sugar.",
                            "liberthia:blood_cure_pill"),

                    new Page("Bloody Rag",
                            "§4Trapo ensanguentado.§r\n\n"
                                    + "§7Stop-gap medicinal — cobre ferida temporariamente (Regen I, 5s). "
                                    + "Reutilizável até 5x. Drop comum de mobs.",
                            "liberthia:bloody_rag"),

                    new Page("Cleansing Salt",
                            "§fSal purificador.§r\n\n"
                                    + "§7Joga no chão → cria área §ablood-free§r§7 de raio 3 por 30s. "
                                    + "Mobs sanguinários fogem da área.\n\n"
                                    + "§e§lCraft:§r §7Salt + Clear Matter Pill.",
                            "liberthia:cleansing_salt"),

                    new Page("Frost Flask",
                            "§bFrasco congelado.§r\n\n"
                                    + "§7Right-click em entity = §oFreezing IV§r§7 por 5s (Slowness V + impossível "
                                    + "atacar). Custo do §dFreeze Staff§r§7.\n\n"
                                    + "§e§lCraft:§r §7Glass Bottle + Powdered Snow + Blue Ice.",
                            "liberthia:frost_flask"),

                    new Page("Purifying Flask",
                            "§fFrasco purificador.§r\n\n"
                                    + "§7Joga no chão = AOE limpa §o5 DM + 5 WM + 5 YM§r§7 dos players no raio.\n\n"
                                    + "§e§lCraft:§r §7Glass + 3x Purified Essence.",
                            "liberthia:purifying_flask"),

                    new Page("Crystallized Blood Soul",
                            "§4Alma cristalizada de sangue.§r\n\n"
                                    + "§7Drop top-tier de boss blood (Blood Volcano boss).\n\n"
                                    + "§a§lUso:§r §7Insumo do §dSanguine Core§r§7. Carrega memória de "
                                    + "rituais antigos — pode ser \"lida\" no Research Table."),

                    new Page("Sanguine Core",
                            "§4Núcleo sanguíneo.§r\n\n"
                                    + "§7Top-tier blood — crafta §dSword Brum§r§7 e §dHemomancer Staff§r§7. "
                                    + "Drop de sacrifício no §dBlood Altar§r§7 (precisa de Ritual Dagger).",
                            "liberthia:sanguine_core"),

                    new Page("Sanguine Essence",
                            "§4Essência líquida.§r\n\n"
                                    + "§7Insumo de potions e ritual amplifiers. Drop comum de mobs blood.",
                            "liberthia:sanguine_essence"),

                    new Page("Congealed Blood",
                            "§4Sangue coagulado.§r\n\n"
                                    + "§7Versão sólida do vial. Obtido smelting de Blood Vial Filled.\n\n"
                                    + "§a§lUso:§r §7Insumo de §oBlood Armor§r§7. Stackável (até 64).",
                            "liberthia:congealed_blood"),

                    new Page("Blood Pact Amulet",
                            "§4§lAmuleto do pacto sangrento§r§4.§r\n\n"
                                    + "§a§lEfeito:§r §7equipado, quando você ia morrer: consome 1 amuleto + "
                                    + "50% HP cap por 60s. Você §lsobrevive§r§7.\n\n"
                                    + "§c§lRisco:§r §7§lcorrompe§r§7 o perfil pra blood-aligned — perfis "
                                    + "ficam com tendência negativa permanente. Use só em emergência real.",
                            "liberthia:blood_pact_amulet"),

                    new Page("Blood Ward Charm",
                            "§4Charm de proteção blood.§r\n\n"
                                    + "§7Equipado em qualquer slot, reduz §o20%§r§7 do dano de mobs blood. "
                                    + "200 durabilidade. Drop em Blood Altar success."),

                    new Page("Blood Warden Spawn Egg",
                            "§7Spawn egg do §dBlood Warden§r§7.\n\n"
                                    + "§a§lMob:§r §7boss-tier. 200 HP, dano 10. Drop §dSanguine Core§r§7 e "
                                    + "§dCrystallized Blood Soul§r§7. Spawna naturalmente em Blood Volcano."),

                    new Page("Chalk & Chalk Symbol",
                            "§7§lGiz de ritual§r§7.\n\n"
                                    + "§a§lChalk§r§7: 8 cargas. Right-click em chão liso desenha símbolo.\n"
                                    + "§a§lChalk Symbol§r§7: bloco passivo placed. Quando posicionado em "
                                    + "§opattern correto§r§7 + sangue/DM próximo, ativa rituais.\n\n"
                                    + "§7Veja §dResearcher Codex§r§7 pra patterns válidos.",
                            "liberthia:chalk"),

                    new Page("Blood Chalk",
                            "§4Giz de sangue.§r\n\n"
                                    + "§7Versão tier-2 do chalk normal. 16 cargas + símbolos especiais "
                                    + "(ritual circles maiores).",
                            "liberthia:blood_chalk")
            )),

            new Chapter("§dCatálogo — Sanguine Wood Set", List.of(
                    new Page("Visão geral — Sanguine Wood",
                            "§4§lSet completo de árvore Sanguine§r§7 — versão blood do oak set vanilla.\n\n"
                                    + "§7Cresce em biomas blood-infested. Galhos vermelhos, folhas escuras. "
                                    + "Madeira tem §ovetas pulsantes§r§7 ao ser cortada."),

                    new Page("Sanguine Sapling",
                            "§4Muda de Sanguine.§r\n\n"
                                    + "§7Plantar em §oblood_dirt§r§7 = árvore cresce em ~3 dias in-game. "
                                    + "Plantar em dirt normal = não cresce.",
                            "liberthia:sanguine_sapling"),

                    new Page("Sanguine Log / Wood / Stripped",
                            "§4§lTronco Sanguine.§r\n\n"
                                    + "§a§lLog§r§7: tronco com casca (drop natural).\n"
                                    + "§a§lWood§r§7: 4 logs em quadrado = wood (lados iguais).\n"
                                    + "§a§lStripped§r§7: tirar casca com axe = vermelho liso.\n\n"
                                    + "§7Smelting em fornace = §dCharcoal§r§7 com 1.5× burn time."),

                    new Page("Sanguine Planks / Slab / Stairs",
                            "§4Tábuas e variações.§r\n\n"
                                    + "§7Padrões vanilla: log → 4 planks. Planks → slabs, stairs, etc.\n\n"
                                    + "§7Visual: vermelho escuro com vetas pulsantes (animadas).",
                            "liberthia:sanguine_planks"),

                    new Page("Sanguine Door / Trapdoor",
                            "§4Porta e alçapão.§r\n\n"
                                    + "§7Door 6 planks (3x2) = 3 doors. Trapdoor 6 planks (3x2) = 2 trapdoors.\n\n"
                                    + "§7Quando fechada à noite, §oresiste§r§7 a quebra de zombies blood."),

                    new Page("Sanguine Fence / Fence Gate",
                            "§4Cerca e portão.§r\n\n"
                                    + "§7Receita vanilla — 4 planks + 2 sticks = fence. 2 planks + 4 sticks = gate."),

                    new Page("Sanguine Button / Pressure Plate",
                            "§4Botão e placa.§r\n\n"
                                    + "§7Redstone signals — mesmas regras do oak. Visual sangue."),

                    new Page("Sanguine Leaves",
                            "§4Folhas escuras.§r\n\n"
                                    + "§7Decoração. Pode ser quebrada pra dropar §oSanguine Sapling§r§7 "
                                    + "(chance ~5%) ou §oBlood Vial Filled§r§7 (chance ~1%).",
                            "liberthia:sanguine_leaves"),

                    new Page("Sanguine Snare",
                            "§4Armadilha sanguínea.§r\n\n"
                                    + "§7Bloco que aplica §oSlowness V§r§7 em qualquer entity que pisar — "
                                    + "exceto o owner. 30s cooldown.\n\n"
                                    + "§a§lCraft:§r §7Sanguine Wood + Iron Bars + Trip Wire.",
                            "liberthia:sanguine_snare"),

                    new Page("Sanguine Ward — Helmet/Chestplate/Leggings/Boots",
                            "§4§lWard Armor Set§r§7 — armadura blood top-tier.\n\n"
                                    + "§a§lEfeitos (set completo):§r\n"
                                    + "§7• §oWard III§r§7 — reduz dano físico em 30%\n"
                                    + "§7• §oBlood Resistance§r§7 — imune a blood_fire e thorn_briar\n"
                                    + "§7• §oRegen on kill§r§7 — +1 HP por mob morto\n\n"
                                    + "§e§lCraft:§r §7Crystallized Blood Soul + Sanguine Wood + Iron.",
                            "liberthia:sanguine_ward_chestplate"),

                    new Page("Sanguine Ward Pickaxe / Sword",
                            "§4§lFerramentas Ward.§r\n\n"
                                    + "§a§lPickaxe§r§7: 9 dano, mina blood_ores 2x mais rápido. Imune a "
                                    + "§dUnstable Matter§r§7 explosions.\n\n"
                                    + "§a§lSword§r§7: 11 dano, lifesteal 15%, aplica §oBleeding II§r§7 (5s).",
                            "liberthia:sanguine_ward_sword")
            )),

            new Chapter("§dCatálogo — Infecção & Corrupção", List.of(
                    new Page("Sistema de Infecção",
                            "§5§lA infecção§r§7 é o cancer da paisagem em Liberthia. Blocos infectados "
                                    + "§ospalham§r§7 sozinhos pra blocos vizinhos compatíveis. Players que "
                                    + "ficam expostos pegam §dDark Matter Infection§r§7 (potion effect).\n\n"
                                    + "§a§lEstágios visíveis:§r\n"
                                    + "§7• §oCorrupted§r§7 — leve (verde)\n"
                                    + "§7• §oScarred§r§7 — médio (roxo claro)\n"
                                    + "§7• §oCrystallizer§r§7 — denso (preto cristalino)"),

                    new Page("Infection Growth / Vein / Heart",
                            "§5§lCadeia de infecção§r§7 (em ordem de severidade):\n\n"
                                    + "§a§lGrowth§r§7 — vegetal corrompido. Spreads em grass adjacente.\n"
                                    + "§a§lVein§r§7 — veias na superfície dos blocos. Bloco intermediário.\n"
                                    + "§a§lHeart§r§7 — coração infeccioso. Ativo, late, gera DM passivo.\n\n"
                                    + "§7Quebrar um Heart drops §o3-5 Tainted Essence§r§7 + chance de "
                                    + "§dInactive Dark Matter§r§7.",
                            "liberthia:infection_heart"),

                    new Page("Corrupted Soil / Stone / Log",
                            "§5Blocos corrompidos básicos.§r\n\n"
                                    + "§7Spread automático em vizinhos compatíveis (dirt → corrupted_soil, "
                                    + "stone → corrupted_stone, log → corrupted_log).\n\n"
                                    + "§a§lLimpar:§r §7White Matter Bomb na área OU placeholder /clear region.",
                            "liberthia:corrupted_soil"),

                    new Page("Scarred Earth / Stone",
                            "§5Cicatrizes na terra.§r\n\n"
                                    + "§7Tier 2 de infecção. Carrega §o45 DM§r§7 (earth) ou §o55 DM§r§7 (stone). "
                                    + "Quebrar libera nuvem de DM no ar — use Containment Glove.",
                            "liberthia:scarred_earth"),

                    new Page("Crystallizer",
                            "§5Cristalizador denso.§r\n\n"
                                    + "§7Tier 3 — fonte máxima de DM. Bloco preto cristalino. Carrega "
                                    + "§o70 DM§r§7 por bloco. Quebrar é §lperigoso§r§7 — explode em 50% "
                                    + "de DM no raio se sem proteção.",
                            "liberthia:crystallizer"),

                    new Page("Attacking Flesh",
                            "§5Carne agressiva.§r\n\n"
                                    + "§7Bloco que §oataca§r§7 entities adjacentes a cada 2s (2 dano + "
                                    + "Withering I). Pode ser destruído com fire ou Holy Hammer.\n\n"
                                    + "§7Drop: §oLiving Flesh§r§7 + §oFlesh Thread§r§7.",
                            "liberthia:attacking_flesh"),

                    new Page("Flesh Mother",
                            "§5Mãe de Carne — boss block.§r\n\n"
                                    + "§7Bloco tier-boss. Tem §o500 HP§r§7 (não dropa em hit normal). Spawna "
                                    + "§oAttacking Flesh§r§7 ao redor. Drop final: §dHeart of the Mother§r§7.",
                            "liberthia:flesh_mother"),

                    new Page("Heart of Flesh / Living Flesh",
                            "§5Items de carne viva.§r\n\n"
                                    + "§a§lHeart of Flesh§r§7: drop de Flesh Mother. Pulsa visualmente.\n"
                                    + "§a§lLiving Flesh§r§7: drop comum. Insumo de Tome of the Mother.\n\n"
                                    + "§7Heart of Flesh block (placeable): late no chão, irradiia +1 §ofear§r§7 "
                                    + "pra players próximos.",
                            "liberthia:heart_of_flesh"),

                    new Page("Spore Bloom",
                            "§5Floração esporulada.§r\n\n"
                                    + "§7Decoração infectada. Quando entity passa perto (raio 2), explode em "
                                    + "esporos: §oPoison II§r§7 por 8s + nuvem de DM.",
                            "liberthia:spore_bloom"),

                    new Page("Thorn Briar / Venom Geyser",
                            "§5Espinheiro / Geiser de veneno.§r\n\n"
                                    + "§a§lThorn Briar§r§7: planta que causa 1 dano + §oBleeding I§r§7 ao toque.\n\n"
                                    + "§a§lVenom Geyser§r§7: bloco geyser. A cada 30s, expulsa nuvem de veneno "
                                    + "(raio 4, Poison III 6s).",
                            "liberthia:venom_geyser"),

                    new Page("Hemorrhage Spike",
                            "§4Espinho hemorrágico.§r\n\n"
                                    + "§7Bloco spike que causa §o6 dano + Bleeding III§r§7 ao tocar. Drops "
                                    + "§oCongealed Blood§r§7 ao quebrar.",
                            "liberthia:hemorrhage_spike"),

                    new Page("Blood Spike",
                            "§4Espeto de sangue.§r\n\n"
                                    + "§7Versão tier-1 do Hemorrhage. 3 dano. Decoração de Blood Altar.",
                            "liberthia:blood_spike")
            )),

            new Chapter("§dCatálogo — Order (Sacro)", List.of(
                    new Page("Visão geral — Order/Sacro",
                            "§6§lA Ordem§r§7 é o oposto do blood/dark. Items dourados, divinos. Cultos de "
                                    + "§oOrder Paladins§r§7 às vezes spawnam em Order Shrines.\n\n"
                                    + "§a§lEcosistema:§r §7armadura de Order + Sanctify Orb + Holy Hammer/Smite "
                                    + "Staff. Sinergiza com §dPurity Beacon§r§7."),

                    new Page("Holy Blade",
                            "§6Lâmina sagrada.§r\n\n"
                                    + "§a§lDano:§r §10 (12 em mobs infected).\n"
                                    + "§a§lEspecial:§r §7Right-click cria onda branca AOE (5 dano em cone).\n\n"
                                    + "§e§lCraft:§r §7Iron + Yellow Matter Ingot + Purified Essence.",
                            "liberthia:holy_blade"),

                    new Page("Holy Hammer",
                            "§6Martelo sagrado.§r\n\n"
                                    + "§7Arma + ferramenta. Right-click em §oAttacking Flesh§r§7 = quebra "
                                    + "instantâneo. Right-click em jogador = §oRegen III§r§7 5s (heal aliados).\n\n"
                                    + "§a§lDano:§r §11. §a§lDurabilidade:§r §800.",
                            "liberthia:holy_hammer"),

                    new Page("Holy Smite Staff",
                            "§6Cajado celestial.§r\n\n"
                                    + "§7Right-click invoca §olightning§r§7 no alvo onde o cursor está. "
                                    + "Custo: §o1 Purified Essence§r§7 por uso. Cooldown 5s.",
                            "liberthia:holy_smite_staff"),

                    new Page("Order Armor Set",
                            "§6§lArmadura de Paladim.§r\n\n"
                                    + "§a§lPeças:§r §7Helmet, Chestplate, Leggings, Boots — drop completa "
                                    + "de §oOrder Paladin§r§7.\n\n"
                                    + "§a§lEfeitos set:§r §7Resistance II + Regen I + §ohostility cap§r§7 "
                                    + "(mobs hostis não atacam você por 2s após hit).",
                            "liberthia:order_chestplate"),

                    new Page("Sanctify Orb",
                            "§6Orbe sagrado.§r\n\n"
                                    + "§7Right-click consome §o1 Purified Essence§r§7 + cria §oaura sagrada§r§7 "
                                    + "ao redor (raio 6) por 60s. Repele mobs infected/dark/blood.",
                            "liberthia:sanctify_orb"),

                    new Page("Order Shrine (Block)",
                            "§6Altar sagrado.§r\n\n"
                                    + "§a§lFunção:§r §7Right-click em alma + Purified Essence = §oPriest Sigil§r§7. "
                                    + "Permite invocação de Order Paladin como aliado por 5 min.\n\n"
                                    + "§e§lCraft:§r §7data/recipes/order_shrine.json — gold + quartz + purified.",
                            "liberthia:order_shrine"),

                    new Page("Priest Sigil",
                            "§6Selo de sacerdote.§r\n\n"
                                    + "§7Item lore. Right-click ativa benção em raio 8 — players ganham "
                                    + "§oResistance I§r§7 por 30s.\n\n"
                                    + "§a§lObtenção:§r §7Order Shrine ritual.",
                            "liberthia:priest_sigil"),

                    new Page("Order Paladin Spawn Egg",
                            "§6Spawn egg.§r\n\n"
                                    + "§7Spawn manual de Paladin (hostile com players com perfil dark/blood, "
                                    + "neutral com white/yellow). Drops Order Armor + Yellow Matter Ingot.",
                            "liberthia:order_paladin_spawn_egg"),

                    new Page("Purity Beacon",
                            "§fFarol de pureza.§r\n\n"
                                    + "§7Bloco em formato cone. Em raio de 16 blocos: drena §dDM§r§7 do perfil "
                                    + "dos jogadores em §o-1/seg§r§7. Custo: §o1 Purified Essence§r§7 por minuto "
                                    + "ativo.",
                            "liberthia:purity_beacon"),

                    new Page("Quarantine Ward",
                            "§fBarreira de quarentena.§r\n\n"
                                    + "§7Bloco-portal. Impede spread de infecção em raio 8. Apaga blood_fire. "
                                    + "Atrai Order Paladins espontâneos (rare).",
                            "liberthia:quarantine_ward"),

                    new Page("Withered Totem",
                            "§8Totem da §lmurchidão§r§8.§r\n\n"
                                    + "§7Item lore — apesar do nome, é um totem §odefensivo§r§7 (não cosmic). "
                                    + "No inventário: ativa-se quando você ia morrer, gasta 1 dura, te dá "
                                    + "§oRegen V§r§7 por 5s + 1 HP. Stack até 8.",
                            "liberthia:withered_totem")
            )),

            new Chapter("§dCatálogo — Cosmic Horror", List.of(
                    new Page("Visão geral — Horror Cósmico",
                            "§5§lO horror cósmico§r§7 vai além do blood/dark. Não é só matter — são "
                                    + "§oentidades§r§7, §odimensões alternativas§r§7, e §oitems que mexem "
                                    + "com sanidade§r§7.\n\n"
                                    + "§7Cuidado: alguns items §opermanentemente§r§7 alteram o perfil ou "
                                    + "abrem portais que não fecham."),

                    new Page("Cursed Idol",
                            "§8Ídolo amaldiçoado.§r\n\n"
                                    + "§7Drop em backrooms / nightmare sequences. Carregá-lo dá §oWither I§r§7 "
                                    + "passivo + atrai mobs hostis em raio 16. Insumo de §dTome of the Mother§r§7.",
                            "liberthia:cursed_idol"),

                    new Page("Veil of Madness",
                            "§5Véu da loucura.§r\n\n"
                                    + "§7Bloco passivo. Quando colocado, distorce a visão de qualquer player "
                                    + "em raio 12 por 30s ao olhar pra ele (§oNausea III§r§7). Drop em rifts.",
                            "liberthia:veil_of_madness"),

                    new Page("Withering Eye",
                            "§5Olho da murchidão.§r\n\n"
                                    + "§7Bloco que ataca players com §dWither III§r§7 a cada 5s se eles "
                                    + "olharem pra ele (line-of-sight). Quebrar com Holy Hammer.",
                            "liberthia:withering_eye"),

                    new Page("Eye of Decay / Horus",
                            "§5§lOlhos lore§r§7 — items raros end-game.\n\n"
                                    + "§a§lEye of Decay§r§7: drop de mob top-tier infected. Permite identificar "
                                    + "(via tooltip) o estado real de qualquer bloco/item — ignora ilusões.\n\n"
                                    + "§a§lEye of Horus§r§7: 5 Horus Eye Shards no Containment Chamber. Item "
                                    + "passivo — equipar dá §oRegen I + Speed I§r§7 permanente."),

                    new Page("Dimensional Compass",
                            "§5Bússola dimensional.§r\n\n"
                                    + "§a§lUso:§r §7Right-click escaneia §orifts§r§7 da dimensão atual e aponta "
                                    + "pro mais próximo. Mostra coords + distância no chat. NBT armazenado.",
                            "liberthia:dimensional_compass"),

                    new Page("Expedition Tracker",
                            "§eRastreador de expedições.§r\n\n"
                                    + "§7Marca §oposições importantes§r§7 (até 16). Right-click em bloco = save. "
                                    + "Right-click no ar = ciclar entre alvos. Mostra distância + direção em HUD.",
                            "liberthia:expedition_tracker"),

                    new Page("Revelation Lens",
                            "§dLente reveladora.§r\n\n"
                                    + "§7Segurando: blocos invisíveis (§dglitch_block§r§7, §dwormhole_block§r§7) "
                                    + "ficam §ovisíveis§r§7 com outline. Mobs disguised mostram identidade real.",
                            "liberthia:revelation_lens"),

                    new Page("Glitch Block",
                            "§5Bloco glitch.§r\n\n"
                                    + "§7Invisível em condições normais. Visível com §dRevelation Lens§r§7. "
                                    + "Atravessar = teleport aleatório dentro de raio 30 (rifts).",
                            "liberthia:glitch_block"),

                    new Page("Wormhole Block",
                            "§5Buraco de minhoca.§r\n\n"
                                    + "§7Right-click pra entrar = teleport pra um wormhole emparelhado da "
                                    + "mesma dimensão. Crafta com §dSingularity Core§r§7 + §dVoid Crystal§r§7 + "
                                    + "Obsidian.",
                            "liberthia:wormhole_block"),

                    new Page("Phantom Portal",
                            "§5Portal fantasma.§r\n\n"
                                    + "§7Frame block (16 unidades em moldura) → ativa quando rituais corretos "
                                    + "feitos perto. Abre passagem pra §oBackrooms§r§7 dimension.\n\n"
                                    + "§a§lCraft:§r §7Matter Core + Obsidian.",
                            "liberthia:phantom_portal"),

                    new Page("Screaming Soul",
                            "§5Alma penada.§r\n\n"
                                    + "§7Item lore. Right-click solta som triste + partícula soul_fire. Drop "
                                    + "de soul-bound entities.\n\n"
                                    + "§7Bloco (placeable): emite som ambient assustador a cada 10-30s.",
                            "liberthia:screaming_soul"),

                    new Page("Desecrated Holy Relic",
                            "§5Relíquia sacra profanada.§r\n\n"
                                    + "§7Item raro. Drop em altares profanados ou nightmares. Carregar = "
                                    + "§oCurse I§r§7 (1 HP a cada 30s) MAS players hostis em PvP recebem "
                                    + "§oNausea II§r§7 quando te olham.",
                            "liberthia:desecrated_holy_relic"),

                    new Page("Tome of the Mother / Pilgrim",
                            "§5§lTomos lore — livros de leitura long-form§r§7.\n\n"
                                    + "§a§lTome of the Mother§r§7: §oescrito§r§7 com Cursed Idol + Living Flesh + "
                                    + "Book. Conta a origem da Flesh Mother (chapter 1 de 7).\n\n"
                                    + "§a§lTome of the Pilgrim§r§7: relatos de quem voltou de Backrooms vivo. "
                                    + "Pista pra Pilgrimage system.",
                            "liberthia:tome_of_the_mother"),

                    new Page("Pulsing Heart",
                            "§4Coração pulsante.§r\n\n"
                                    + "§7Item passivo curioso — segurar na hotbar faz o HUD §opulsar§r§7 leve "
                                    + "no ritmo do batimento. Sem efeito mecânico, mas raro coletor.",
                            "liberthia:pulsing_heart"),

                    new Page("Heart of the Mother",
                            "§5Coração da Mãe — final boss item.§r\n\n"
                                    + "§7Drop da §dFlesh Mother§r§7 ao ser destruída completamente. Item lore "
                                    + "final — desbloqueia última página do Codex.",
                            "liberthia:heart_of_the_mother"),

                    new Page("Burning Gem",
                            "§6Gema flamejante.§r\n\n"
                                    + "§7Carrega: §oFire Resistance§r§7 sempre. Drop de Blood Volcano.",
                            "liberthia:burning_gem"),

                    new Page("Veiled Lantern",
                            "§8Lanterna velada.§r\n\n"
                                    + "§7Right-click coloca lanterna no chão. Em raio 8 reduz §ofear§r§7 dos "
                                    + "players + repele §oghost mobs§r§7.",
                            "liberthia:veiled_lantern"),

                    new Page("Containment Glove",
                            "§dLuva de contenção.§r\n\n"
                                    + "§7Equipado em qualquer slot: permite manipular §dDark Matter§r§7 sem "
                                    + "dano de radiação. Consome §o1 dura§r§7 a cada §o~6s§r§7 de exposição. "
                                    + "500 dura = ~50 min de uso. §cNÃO previne acúmulo de DM no perfil§r§7.",
                            "liberthia:containment_glove"),

                    new Page("Containment Suit Set",
                            "§dRoupa de proteção dark/blood.§r\n\n"
                                    + "§a§lPeças:§r §7Helmet, Chestplate, Leggings, Boots.\n\n"
                                    + "§a§lEfeito:§r §7Set completo = §oimune§r§7 a exposição passiva de DM "
                                    + "e blood. Penalidade: §oSlowness I§r§7 (peso da roupa).",
                            "liberthia:containment_suit_chestplate"),

                    new Page("Protection Ruby",
                            "§cRuby de proteção mágica.§r\n\n"
                                    + "§7Equipado em qualquer slot, reduz dano físico em 15%. Consome 1 dura "
                                    + "por hit absorvido. 200 dura.",
                            "liberthia:protection_ruby")
            )),

            new Chapter("§dCatálogo — Tools & Weapons", List.of(
                    new Page("Visão geral — Ferramentas e Armas",
                            "§7Tiers de tools/weapons em Liberthia:\n\n"
                                    + "§a§lTier 1 — Vanilla:§r §7iron, diamond, netherite (não documentado).\n"
                                    + "§a§lTier 2 — Matter:§r §7Dark/Clear/Yellow Matter sword/axe/pickaxe.\n"
                                    + "§a§lTier 3 — Sanguine Ward:§r §7sword, pickaxe.\n"
                                    + "§a§lTier 4 — Order:§r §7Holy Blade, Holy Hammer.\n"
                                    + "§a§lLegendary:§r §7Sword Brum, Soul Scream Sword."),

                    new Page("Dark Matter Pickaxe / Axe / Sword",
                            "§5§lTier matter escuro.§r\n\n"
                                    + "§a§lPickaxe§r §7(+8 dano, Fortune III implícita em DM ores). Mina quase "
                                    + "tão rápido quanto netherite.\n"
                                    + "§a§lAxe§r §7(+10 dano, atordoa em hit). Drop bonus em log corrupted.\n"
                                    + "§a§lSword§r §7(+9 dano, aplica §oDark Infection§r§7 em hit).\n\n"
                                    + "§c§lEfeito colateral:§r §7uso prolongado adiciona §o+10 DM§r§7 ao perfil "
                                    + "(equipar = exposição).",
                            "liberthia:dark_matter_sword"),

                    new Page("Clear Matter Pickaxe / Axe / Sword",
                            "§f§lTier matter clara.§r\n\n"
                                    + "§a§lPickaxe§r §7(+7 dano, dura 2x diamond). Não corrompe.\n"
                                    + "§a§lAxe§r §7(+9 dano).\n"
                                    + "§a§lSword§r §7(+8 dano, aplica §oSlowness II§r§7 em mob blood/infected).",
                            "liberthia:clear_matter_sword"),

                    new Page("Yellow Matter Pickaxe / Axe / Sword",
                            "§e§lTier matter amarela (top).§r\n\n"
                                    + "§a§lPickaxe§r §7(+9 dano, mineração mais rápida que netherite, drop bonus "
                                    + "em YM ore). Pode minerar §oUnstable Matter§r§7 sem explosão.\n"
                                    + "§a§lAxe§r §7(+11 dano).\n"
                                    + "§a§lSword§r §7(+10 dano, Smite-equivalent em undead +5).",
                            "liberthia:yellow_matter_sword"),

                    new Page("Yellow Matter Shield",
                            "§e§lEscudo dourado reflexivo.§r\n\n"
                                    + "§a§lEfeito:§r §7reflete 50% de projeteis no atacante. Cooldown menor "
                                    + "que vanilla. Consome 1 dura por reflexo.\n\n"
                                    + "§e§lCraft:§r §7Yellow Matter Ingot + Iron + Wood.",
                            "liberthia:yellow_matter_shield"),

                    new Page("Sword Brum",
                            "§dEspada lendária — referência meta.§r\n\n"
                                    + "§7Crafta com §lSanguine Core§r§7 + ferro + leather. §l+18 dano§r§7 + "
                                    + "custom swing animation. Drop ultra-raro de event-related.\n\n"
                                    + "§a§lReceita:§r §7data/recipes/sword_brum.json.",
                            "liberthia:sword_brum"),

                    new Page("Soul Scream Sword",
                            "§5Espada de almas.§r\n\n"
                                    + "§7Cada hit consome 1 §oScreaming Soul§r§7 do inv e dá §o+5 dano§r§7 (até "
                                    + "+15 com 3 kills consecutivos). Som assustador animado.",
                            "liberthia:soul_scream_sword"),

                    new Page("Rusted Dagger",
                            "§7Adaga enferrujada.§r\n\n"
                                    + "§7Tier baixo. 4 dano. Aplica §oPoison I§r§7 (3s). Drop comum de mobs "
                                    + "decaídos.",
                            "liberthia:rusted_dagger"),

                    new Page("Execution Stick",
                            "§cVara de execução — ferramenta admin.§r\n\n"
                                    + "§c§l⚠ Item ADMIN§r§7 — right-click em mob mata instantâneo. Em player "
                                    + "= kick. §lApenas OPs.§r",
                            "liberthia:execution_stick"),

                    new Page("Summon Staff",
                            "§dCajado de invocação.§r\n\n"
                                    + "§7Right-click invoca §oclone temporário§r§7 (worker_clone) que luta por "
                                    + "você por 30s. Cooldown 600t.",
                            "liberthia:summon_staff"),

                    new Page("Freeze Staff",
                            "§bCajado de gelo.§r\n\n"
                                    + "§7Right-click congela alvo onde olha por 5s (Slowness V + impossível "
                                    + "pular). Custo: 1 §dfrost_flask§r§7 por uso.",
                            "liberthia:freeze_staff"),

                    new Page("Thorn Staff",
                            "§2Cajado de espinhos.§r\n\n"
                                    + "§7Right-click spawna §dThorn Briar§r§7 (3 blocos numa linha) no chão. "
                                    + "Útil pra controle de mob ou criar área defensiva.",
                            "liberthia:thorn_staff"),

                    new Page("Lightning Staff",
                            "§eCajado de raio.§r\n\n"
                                    + "§7Right-click no alvo = §ostrike§r§7 (3 dano + Stun 1s). Custo: 1 carga "
                                    + "de §dLightning Coil§r§7 emparelhada.",
                            "liberthia:lightning_staff"),

                    new Page("Lightning Grenade",
                            "§eGranada elétrica.§r\n\n"
                                    + "§7Jogável. Explode em §ostrike multi-target§r§7 (3 raios random em "
                                    + "entities no raio 5). Stack até 8.",
                            "liberthia:lightning_grenade"),

                    new Page("Cleansing Grenade",
                            "§fGranada purificadora.§r\n\n"
                                    + "§7Jogável. Explode em AOE de §oWhite Matter§r§7 — limpa infection em "
                                    + "raio 4 + dá Resistance I aos players. Stack 16.",
                            "liberthia:cleansing_grenade"),

                    new Page("Marking Stick",
                            "§eBastão de marcação.§r\n\n"
                                    + "§7Right-click em 2 blocos pra definir área retangular. Útil pra demarcar "
                                    + "construção. Mostra outline visual quando segurado.",
                            "liberthia:marking_stick"),

                    new Page("Growth Rod / Shrink Rod",
                            "§dVaras de tamanho (Pehkui integration).§r\n\n"
                                    + "§a§lGrowth Rod§r§7: right-click em entity = +5% scale (cap 200%).\n"
                                    + "§a§lShrink Rod§r§7: -5% scale (cap 25%).\n\n"
                                    + "§7Reset com /pehkui ou sneak+right-click.",
                            "liberthia:growth_rod"),

                    new Page("Magnetic Wand",
                            "§7Varinha magnética.§r\n\n"
                                    + "§7Right-click em ItemEntity no chão = atrai todos items no raio 16 "
                                    + "pro player. Right-click no ar = repele.",
                            "liberthia:magnetic_wand")
            )),

            new Chapter("§dCatálogo — Detecção & Utilidade", List.of(
                    new Page("Sample Vial",
                            "§dFrasco de Amostra.§r\n\n"
                                    + "§7Right-click num bloco com matéria → coleta amostra no NBT. Insira "
                                    + "frasco preenchido no §dMatter Analyzer§r§7.\n\n"
                                    + "§a§lCraft (v0.1.7+):§r §73x Glass Pane em V + 1x Iron Nugget centro.\n"
                                    + "§7Detalhes em §dAnalyzer e Curas§r§7 > §oSample Vial workflow§r.",
                            "liberthia:sample_vial"),

                    new Page("Geiger Counter",
                            "§4Detector de radiação.§r\n\n"
                                    + "§7Right-click ou só segurar mostra: nível de radiação atual + previsão "
                                    + "de acúmulo de DM/min. Beep audível em hotspots.",
                            "liberthia:geiger_counter"),

                    new Page("Field Journal",
                            "§eDiário de campo.§r\n\n"
                                    + "§7Item read-only — entries automáticas de exposição/eventos do player. "
                                    + "Right-click abre GUI com log das últimas 50 \"observações\".",
                            "liberthia:field_journal"),

                    new Page("Researcher Codex",
                            "§5Codex do pesquisador.§r\n\n"
                                    + "§7Livro de lore + receitas conhecidas. Página por página é desbloqueada "
                                    + "conforme você descobre items/blocos.\n\n"
                                    + "§a§lObtenção:§r §7Drop inicial pra cada player no primeiro spawn.",
                            "liberthia:researcher_codex"),

                    new Page("Host Journal",
                            "§dDiário do Anfitrião.§r\n\n"
                                    + "§7Lore-heavy — escrito pelo §oAnfitrião-Chefe§r§7 da pesquisa. Páginas "
                                    + "narram a história das 3 ilhas.",
                            "liberthia:host_journal"),

                    new Page("Research Notes",
                            "§eAnotações de pesquisa.§r\n\n"
                                    + "§7Item utilitário. Right-click = pop-up com dicas sobre o último mob "
                                    + "que você matou ou item que você coletou.",
                            "liberthia:research_notes"),

                    new Page("Red Kiriko Book",
                            "§4Livro vermelho de Kiriko.§r\n\n"
                                    + "§7Livro lore mid-tier. Conta a história de §oKiriko§r§7 e seus "
                                    + "experimentos. Drop em backrooms.",
                            "liberthia:book_red_kiriko"),

                    new Page("Admin Tool",
                            "§dItem de admin.§r\n\n"
                                    + "§c§l⚠ Admin only§r§7 — abre tela cheia com controles de matter, "
                                    + "spawning, world commands.\n\n"
                                    + "§7Apenas OPs (level 2+) podem usar.",
                            "liberthia:admin_tool"),

                    new Page("Command Tablet / Script Tablet",
                            "§dTablets de controle.§r\n\n"
                                    + "§a§lCommand Tablet§r§7: execução de comando custom (admin).\n"
                                    + "§a§lScript Tablet§r§7: roda KubeJS script no contexto do player.",
                            "liberthia:command_tablet"),

                    new Page("Liberthia Manual",
                            "§5Manual Liberthia (este livro!).§r\n\n"
                                    + "§7Drop inicial. Right-click abre essa tela aqui. Vou parar a tooltip "
                                    + "recursiva — você já tá lendo isso.",
                            "liberthia:liberthia_manual"),

                    new Page("Liberthia Wrench",
                            "§7Chave inglesa Liberthia.§r\n\n"
                                    + "§a§lFunção:§r §7sneak+right-click em pipes/cabos = rotação 90°. "
                                    + "Right-click em máquina = abre GUI de filtros/upgrades.\n\n"
                                    + "§e§lCraft:§r §7Iron Ingot + Stick.",
                            "liberthia:liberthia_wrench"),

                    new Page("Pylon Remote",
                            "§dControle de pylons.§r\n\n"
                                    + "§7Right-click em §dCommand Pylon§r§7 ou §dMagnetic Pylon§r§7 = ativa/desativa. "
                                    + "Suporta múltiplos pylons emparelhados (sneak+right-click pra pair).",
                            "liberthia:pylon_remote"),

                    new Page("Energy Meter",
                            "§eMedidor de energia.§r\n\n"
                                    + "§7Right-click em §dEnergy Cable§r§7 / §dBattery§r§7 mostra: input, "
                                    + "output, current charge, capacity.",
                            "liberthia:energy_meter"),

                    new Page("Worker Inventory Viewer",
                            "§dVisor de inventário do clone.§r\n\n"
                                    + "§7Right-click em §dWorker Clone§r§7 = abre inv dele remotamente.",
                            "liberthia:worker_inventory_viewer"),

                    new Page("Player Lock",
                            "§dItem admin — tranca um player no spawn.§r\n\n"
                                    + "§c§l⚠ Apenas OPs.§r§7 Right-click no player = ele não pode quebrar/colocar "
                                    + "blocos por 5 min. Útil em events.",
                            "liberthia:player_lock")
            )),

            new Chapter("§dCatálogo — Workers/Clones", List.of(
                    new Page("Visão geral — Workers",
                            "§7§lWorkers§r§7 são §oclones§r§7 invocados pelo §dSummon Staff§r§7 ou Hemomancer. "
                                    + "Eles têm IA simples e podem ser equipados com badges/utensílios.\n\n"
                                    + "§a§lLimite:§r §72 workers ativos por player (cap automático)."),

                    new Page("Worker Clone (item)",
                            "§dClone trabalhador.§r\n\n"
                                    + "§7Right-click no chão = spawna worker. Vive 30s, defende o owner, "
                                    + "ataca mobs hostis. Tem inv pessoal (acessível via Worker Inventory Viewer).",
                            "liberthia:worker_clone"),

                    new Page("Worker Badge",
                            "§dCrachá de worker.§r\n\n"
                                    + "§7Equipável no slot de worker. Dá identidade visual (nameplate) + "
                                    + "salva preferências entre invocações.",
                            "liberthia:worker_badge"),

                    new Page("Worker Voice Box",
                            "§dCaixa de voz.§r\n\n"
                                    + "§7Worker fala frases automáticas (\"sim, chefe!\", \"cuidado!\", etc) "
                                    + "em chat quando dispara ações. Drop pequeno de fun.",
                            "liberthia:worker_voice_box"),

                    new Page("Worker Lightning / Teleporter",
                            "§dEquipamentos de worker.§r\n\n"
                                    + "§a§lLightning§r§7: worker dispara raio em mob hostile (cooldown 10s).\n"
                                    + "§a§lTeleporter§r§7: worker tp pro lado do player a cada 5s.",
                            "liberthia:worker_lightning")
            )),

            new Chapter("§dCatálogo — Mobs (Spawn Eggs)", List.of(
                    new Page("Lista de mobs do mod",
                            "§a§l5 mobs customizados:§r\n\n"
                                    + "§7• §dBlood Warden§r§7 — boss-tier blood (200 HP)\n"
                                    + "§7• §dOrder Paladin§r§7 — sacro neutral/hostile\n"
                                    + "§7• §dPossessed Skeleton§r§7 — skeleton corrompido (DM aura)\n"
                                    + "§7• §dPossessed Zombie§r§7 — zombie corrompido (blood aura)\n"
                                    + "§7• §dWeaving Shade§r§7 — sombra textil que ataca camuflada\n"
                                    + "§7• §dDisarmer§r§7 — mob que desarma players (drop weapon do hit)\n\n"
                                    + "§7Spawn naturalmente em estruturas (shrines, volcanos, backrooms). "
                                    + "Spawn manual via eggs do creative tab admin."),

                    new Page("Blood Warden (egg)",
                            "§4Spawn egg — Blood Warden.§r\n\n"
                                    + "§7Boss-tier. §o200 HP§r§7, §odano 10§r§7, summon adds. Drops: "
                                    + "§dSanguine Core§r§7, §dCrystallized Blood Soul§r§7, §dCongealed Blood§r§7 (×5).",
                            "liberthia:blood_warden_spawn_egg"),

                    new Page("Order Paladin (egg)",
                            "§6Spawn egg — Order Paladin.§r\n\n"
                                    + "§7Hostile com players blood/dark, neutral com outros. §o80 HP§r§7, "
                                    + "§odano 8§r§7. Drops: §dOrder Armor pieces§r§7, §dYellow Matter Ingot§r§7.",
                            "liberthia:order_paladin_spawn_egg"),

                    new Page("Possessed Skeleton / Zombie (eggs)",
                            "§5§lVersões corrompidas dos mobs vanilla.§r\n\n"
                                    + "§a§lSkeleton§r§7: tem §oDM aura§r§7 — players próximos ganham §o+1 DM§r§7 "
                                    + "por segundo. Drop: Inactive DM.\n\n"
                                    + "§a§lZombie§r§7: tem §oBlood aura§r§7 — players próximos ganham §oBleeding I§r§7. "
                                    + "Drop: Blood Vial Filled.",
                            "liberthia:possessed_zombie_spawn_egg"),

                    new Page("Weaving Shade (egg)",
                            "§8Spawn egg — Weaving Shade.§r\n\n"
                                    + "§7Mob §oinvisível§r§7 até atacar. 60 HP, dano 6 + §oBlindness 3s§r§7. "
                                    + "Drop: §dCorrupted Log§r§7 + §dScreaming Soul§r§7.",
                            "liberthia:weaving_shade_spawn_egg"),

                    new Page("Disarmer (egg)",
                            "§cSpawn egg — Disarmer.§r\n\n"
                                    + "§7Mob §oroubador§r§7 — em hit, pega weapon/tool da mão do player. 50 HP, "
                                    + "dano 4. Items roubados caem no chão (não consome).",
                            "liberthia:disarmer_spawn_egg")
            )),

            new Chapter("§dCatálogo — Energia & Geradores", List.of(
                    new Page("Sistema de Energia",
                            "§e§lLiberthia tem rede de energia própria§r§7 — não é Forge Energy puro (FE), "
                                    + "é §oDM Energy§r§7 (medido em DM units).\n\n"
                                    + "§a§lFluxo:§r\n"
                                    + "§7• §dGenerator§r§7 produz DM\n"
                                    + "§7• §dEnergy Cable§r§7 transporta\n"
                                    + "§7• §dBattery§r§7 armazena\n"
                                    + "§7• Máquinas (Forge/Infuser/etc) consomem\n\n"
                                    + "§7Mostre output/draw com §dEnergy Meter§r§7."),

                    new Page("Dark Matter Generator",
                            "§5Gerador básico de DM.§r\n\n"
                                    + "§a§lInput:§r §7DM Shard ou DM Bucket no slot fuel.\n"
                                    + "§a§lOutput:§r §720 DM/tick (até 400/s).\n"
                                    + "§a§lCapacidade interna:§r §o200 DM§r§7.\n\n"
                                    + "§e§lCraft:§r §7data/recipes/dark_matter_generator.json — Iron Block + DM Shard.",
                            "liberthia:dark_matter_generator"),

                    new Page("Fragmented Generator",
                            "§5Gerador fragmentado.§r\n\n"
                                    + "§7Versão tier-2 — usa §oactive_dark_matter§r§7 como fuel. 5× output do basic.\n\n"
                                    + "§a§lCraft:§r §7Dark Matter Generator + Active DM + Singularity Core.",
                            "liberthia:fragmented_generator"),

                    new Page("Energy Cable",
                            "§eCabo de energia.§r\n\n"
                                    + "§7Transporta DM entre máquinas. §oCapacidade:§r §720 DM/tick por cabo. "
                                    + "Liga automaticamente em faces de máquinas/baterias.\n\n"
                                    + "§a§lLiberthia Wrench§r§7 sneak+right-click = quebra/abre rede.",
                            "liberthia:energy_cable"),

                    new Page("DM Battery — Basic/Advanced/Quantum",
                            "§e§l3 tiers de bateria:§r\n\n"
                                    + "§a§lBasic§r§7 — §o1.000 DM§r§7. Iron + 4 DM Shards.\n"
                                    + "§a§lAdvanced§r§7 — §o10.000 DM§r§7. Basic + Active DM + Gold.\n"
                                    + "§a§lQuantum§r§7 — §o100.000 DM§r§7. Advanced + Singularity Core + Void Crystal.\n\n"
                                    + "§7Display visual na frente mostra carga atual.",
                            "liberthia:dm_battery_basic"),

                    new Page("Wireless Charger",
                            "§eCarregador wireless.§r\n\n"
                                    + "§7Bloco que §ocarrega itens§r§7 com energia (DM Cells, batteries de mão) "
                                    + "em raio 8. Consome §o5 DM/s§r§7 por item carregando.\n\n"
                                    + "§a§lCraft:§r §7Battery Basic + Lightning Coil + Iron Block.",
                            "liberthia:wireless_charger"),

                    new Page("Lightning Coil / Node",
                            "§e§lRede elétrica§r§7 — paralela à DM.\n\n"
                                    + "§a§lLightning Coil§r§7: gera 1 carga elétrica a cada strike de §olightning§r§7 "
                                    + "real (vanilla weather). Armazena até 16 cargas.\n\n"
                                    + "§a§lLightning Node§r§7: receptor — distribui cargas pra Coils emparelhadas. "
                                    + "Pode ser ativado por redstone.",
                            "liberthia:lightning_coil"),

                    new Page("Command Pylon",
                            "§dPylon de comando.§r\n\n"
                                    + "§7Bloco que executa comando vanilla configurado quando ativado por "
                                    + "redstone. Comando setado via §dCommand Tablet§r§7 (right-click).",
                            "liberthia:command_pylon"),

                    new Page("Magnetic Pylon",
                            "§dPylon magnético.§r\n\n"
                                    + "§7Atrai todos items no raio 16 pro centro do pylon. Útil pra coletor "
                                    + "automático em farms. Consome §o2 DM/s§r§7.",
                            "liberthia:magnetic_pylon")
            )),

            new Chapter("§dCatálogo — Ores e Worldgen", List.of(
                    new Page("Lista completa de ores",
                            "§a§lOres customizados:§r\n\n"
                                    + "§5• Dark Matter Ore§r§7 — overworld profundo (Y<32)\n"
                                    + "§5• Deepslate Dark Matter Ore§r§7 — bedrock-near (Y<-50)\n"
                                    + "§f• White Matter Ore§r§7 — snowy/icy biomes\n"
                                    + "§4• Blood Coal Ore§r§7 — blood biomes\n"
                                    + "§4• Blood Iron Ore§r§7\n"
                                    + "§4• Blood Gold Ore§r§7\n"
                                    + "§4• Blood Lapis Ore§r§7\n"
                                    + "§4• Blood Redstone Ore§r§7\n"
                                    + "§4• Blood Emerald Ore§r§7\n"
                                    + "§4• Blood Diamond Ore§r§7"),

                    new Page("Dark Matter Ore / Deepslate variant",
                            "§5Minério de Dark Matter.§r\n\n"
                                    + "§a§lDrop:§r §o1-2 DM Shards§r§7 (Fortune III dobra).\n"
                                    + "§a§lTier:§r §7Iron Pickaxe+.\n"
                                    + "§a§lSpawn:§r §7overworld Y<32 (Dark Matter Ore), Y<-50 (Deepslate variant).\n\n"
                                    + "§c§lAtenção:§r §7minerar sem proteção = §o+5 DM§r§7 no perfil por bloco.",
                            "liberthia:dark_matter_ore"),

                    new Page("White Matter Ore",
                            "§fMinério de White Matter.§r\n\n"
                                    + "§a§lDrop:§r §oRaw WM§r§7 (era item removido em v0.1.7, mas o block "
                                    + "ainda existe pra worldgen — quebrar agora dropa o bloco-self).\n\n"
                                    + "§a§lSpawn:§r §7snowy biomes (ice plains, frozen ocean).",
                            "liberthia:white_matter_ore"),

                    new Page("Blood Ores Family (×7)",
                            "§4§lMinérios sangrentos§r§7 — vanilla ores com tint sangue + buff/cost.\n\n"
                                    + "§a§lDrop:§r §7vanilla equivalent + chance de §oCongealed Blood§r§7 (15%).\n"
                                    + "§a§lEfeito ao minerar:§r §7+1 §oBlood Infection§r§7 (acumula no perfil).\n"
                                    + "§a§lSpawn:§r §7blood biomes (gerado em meta-camadas).\n\n"
                                    + "§7Lista: blood_coal/iron/gold/lapis/redstone/emerald/diamond_ore.",
                            "liberthia:blood_diamond_ore"),

                    new Page("Blood Dirt / Sand / Stone",
                            "§4Blocos de terreno blood.§r\n\n"
                                    + "§7Spawn em blood biomes. Cosméticos + condição pra plantar §dSanguine "
                                    + "Sapling§r§7 (só cresce em blood_dirt).",
                            "liberthia:blood_dirt"),

                    new Page("Blood Volcano",
                            "§4Vulcão de sangue.§r\n\n"
                                    + "§7Estrutura worldgen rara. Bloco central. Quando ativado por ritual, "
                                    + "erupção §obloodfire§r§7 + spawn de §dBlood Warden§r§7 boss.",
                            "liberthia:blood_volcano"),

                    new Page("Blood Fountain",
                            "§4Fonte de sangue.§r\n\n"
                                    + "§7Bloco gerador. Right-click com balde vazio = §dBlood Bucket§r§7. "
                                    + "Recarrega a cada 10 minutos. Drop natural em blood biomes.",
                            "liberthia:blood_fountain")
            )),

            new Chapter("§dReceitas Conhecidas (Resumo)", List.of(
                    new Page("Receitas — overview",
                            "§7§l65 receitas§r§7 documentadas em data/liberthia/recipes/.\n\n"
                                    + "§a§lCategorias:§r\n"
                                    + "§7• 7 receitas de §dpipes/upgrades§r\n"
                                    + "§7• 12 receitas de §dmáquinas§r §7(forge/infuser/transmuter/etc)\n"
                                    + "§7• 8 receitas de §dmatter items§r §7(catalyst/cell/core/ampoule)\n"
                                    + "§7• 6 receitas de §darmaduras yellow§r\n"
                                    + "§7• 4 receitas de §dconsumíveis§r §7(pills/cure)\n"
                                    + "§7• 2 receitas de §dsword/bow§r §7(brum/blood_bow)\n"
                                    + "§7• 4 receitas de §dblood items§r §7(altar/vial/pact/dagger)\n"
                                    + "§7• 11 receitas de §dgeradores e baterias§r\n"
                                    + "§7• ~11 outras (sample_vial, lens, etc)\n\n"
                                    + "§7Ver código fonte se quiser receita exata."),

                    new Page("Receitas dos blocos principais",
                            "§a§l7 blocos principais (já documentado em \"Manual de Operação\"):§r\n\n"
                                    + "§7• §dPurification Bench§r §7— PE×3 + Quartz×2 + Glass + SmoothStone×3\n"
                                    + "§7• §dDark Matter Forge§r §7— ver dark_matter_forge.json\n"
                                    + "§7• §dMatter Infuser§r §7— Iron×4 + Glass×4 + Quartz\n"
                                    + "§7• §dResearch Table§r §7— Books×2 + Iron + Planks×3 + Sticks×2\n"
                                    + "§7• §dContainment Chamber§r §7— Obsidian×4 + IronBlock×2 + Glass×2 + DM Shard\n"
                                    + "§7• §dMatter Transmuter§r §7— ver matter_transmuter.json\n"
                                    + "§7• §dDark Matter Alchemizer§r §7— ver dark_matter_alchemizer.json"),

                    new Page("Receitas: Pipes e Upgrades",
                            "§a§l7 receitas de logística:§r\n\n"
                                    + "§7• §dItem Pipe§r §7— Iron + Glass simples\n"
                                    + "§7• §dItem Extractor§r §7— Item Pipe + Iron Block + Funnel\n"
                                    + "§7• §dItem Inserter§r §7— Item Pipe + Iron Block + Comparator\n"
                                    + "§7• §dSpeed Upgrade§r §7— Feather centro + Redstone cruz + Iron cantos\n"
                                    + "§7• §dEfficiency Upgrade§r §7— Copper Ingot + Redstone Torch cruz + Iron cantos\n"
                                    + "§7• §dCapacity Upgrade§r §7— Shulker Shell + Leather cruz + Iron cantos"),

                    new Page("Receitas: Curas (v0.1.7)",
                            "§a§l4 receitas de cura:§r\n\n"
                                    + "§7• §dMatter Cure§r §7— Glowstone Dust topo + Purified Essence×2 lados + "
                                    + "Clear Matter Pill centro + Glass Bottle base\n"
                                    + "§7• §dDaily Pill§r §7— Glow Berries×4 + Sugar×2 + Nether Wart×2 + "
                                    + "Clear Matter Pill centro = §o4 pílulas§r§7\n"
                                    + "§7• §dBlood Cure Pill§r §7— Blood Vial Filled + Glass Bottle + Sugar\n"
                                    + "§7• §dClear Matter Pill§r §7— Clear Matter Block + Paper (Research Table)"),

                    new Page("Receitas: Energia",
                            "§a§l11 receitas de energia/geradores:§r\n\n"
                                    + "§7• §dDark Matter Generator§r §7— Iron Block + DM Shard\n"
                                    + "§7• §dFragmented Generator§r §7— DM Generator + Active DM + Singularity Core\n"
                                    + "§7• §dDM Battery Basic§r §7— Iron + DM Shards×4\n"
                                    + "§7• §dDM Battery Advanced§r §7— Basic + Active DM + Gold Block\n"
                                    + "§7• §dDM Battery Quantum§r §7— Advanced + Singularity Core + Void Crystal\n"
                                    + "§7• §dEnergy Cable§r §7— Iron + Redstone wraps\n"
                                    + "§7• §dWireless Charger§r §7— Battery Basic + Lightning Coil + Iron Block\n"
                                    + "§7• §dLightning Coil§r §7— Copper + Iron + Redstone\n"
                                    + "§7• §dCommand Pylon§r §7— Iron Block + Comparator + Stone Button\n"
                                    + "§7• §dMagnetic Pylon§r §7— Iron + Magnet Ore (vanilla copper bulk)\n"
                                    + "§7• §dDark Matter Cell§r §7— Iron + Active DM + Glass"),

                    new Page("Receitas: Blood",
                            "§a§l4 receitas de blood items:§r\n\n"
                                    + "§7• §dBlood Altar§r §7— Obsidian×4 + Blood Vial Filled + Sanguine Core\n"
                                    + "§7• §dBlood Cauldron§r §7— Iron Bars + Sanguine Wood + Blood Vial\n"
                                    + "§7• §dBlood Ritual Dagger§r §7— Iron Ingot + Sanguine Core + Blood Vial\n"
                                    + "§7• §dBlood Pact Amulet§r §7— Gold + Sanguine Core + Blood Vial Filled×3"),

                    new Page("Receitas: Lore",
                            "§7Items lore com receita explícita:\n\n"
                                    + "§7• §dHemomancer Staff§r §7— receita complexa em Hemomancer ritual\n"
                                    + "§7• §dSword Brum§r §7— Sanguine Core + Iron + Leather\n"
                                    + "§7• §dRevelation Lens§r §7— Glass + Eye of Decay\n"
                                    + "§7• §dDimensional Compass§r §7— Compass vanilla + Singularity Core\n"
                                    + "§7• §dMatter Core§r §7— DM Block + CM Block + YM Block + Singularity Core\n"
                                    + "§7• §dMatter Ampoule§r §7— Glass Bottle + DM Shard + Iron\n"
                                    + "§7• §dSingularity Core§r §7— Stabilized DM + Void Crystal (no Forge)\n"
                                    + "§7• §dOrder Shrine§r §7— Gold + Quartz Block + Purified Essence\n"
                                    + "§7• §dAuto Farmer§r §7— Hopper + Diamond + Iron Block + Stick"),

                    new Page("Como ler uma recipe JSON",
                            "§7Quer entender a receita exata? Os JSONs estão em:\n\n"
                                    + "§o<mod-jar>/data/liberthia/recipes/*.json§r\n\n"
                                    + "§a§lFormato:§r\n"
                                    + "§7• §opattern§r — 3 linhas de char (\"XYZ\")\n"
                                    + "§7• §okey§r — qual item cada char representa\n"
                                    + "§7• §oresult§r — item produzido + count\n\n"
                                    + "§7Pra ver as receitas in-game, usa JEI/REI (já testado, compatible)."),

                    new Page("Items SEM receita conhecida",
                            "§c§l⚠ Items que NÃO têm crafting recipe:§r\n\n"
                                    + "§7Esses são §oitems lore§r§7 ou §odrops§r§7 — não craftáveis:\n"
                                    + "§7• §dDark Matter Shard§r §7(drop em mineração / mob)\n"
                                    + "§7• §dPurified Essence§r §7(saída da Purification Bench)\n"
                                    + "§7• §dTainted Essence§r §7(drop de mob corrompido)\n"
                                    + "§7• §dVoid Crystal§r §7(drop ultra-raro em rifts)\n"
                                    + "§7• Todos os §dSpawn Eggs§r §7(admin tab)\n"
                                    + "§7• Items lore (cursed_idol, screaming_soul, etc)\n"
                                    + "§7• Items boss-drop (heart_of_the_mother, sanguine_core)\n\n"
                                    + "§7Lista completa: ~80 items são drops/lore, ~168 são craftáveis."),

                    new Page("Como descobrir receitas no jogo",
                            "§a§l3 jeitos:§r\n\n"
                                    + "§a§l1. JEI/REI:§r §7instala um mod de receipts (JEI, REI, EMI). Liberthia "
                                    + "é §ocompatible§r§7 — todas as receitas aparecem.\n\n"
                                    + "§a§l2. Research Table:§r §7coloca item desconhecido + paper = às vezes "
                                    + "gera §oresearch notes§r§7 com receita.\n\n"
                                    + "§a§l3. Codex do Pesquisador:§r §7algumas páginas mostram receita visual "
                                    + "quando o item correspondente é coletado."),

                    new Page("Modificar receitas em runtime",
                            "§a§lKubeJS Integration:§r\n\n"
                                    + "§7Liberthia suporta KubeJS scripts pra adicionar/remover/modificar "
                                    + "receitas em runtime, sem rebuild do mod.\n\n"
                                    + "§7No painel admin: §o/api/kubejs§r§7 endpoint pra upload de scripts. "
                                    + "Receitas em §o.kubejs/server_scripts/§r§7 → apply via /reload.")
            )),

            new Chapter("§dCatálogo — Decorativo & Naturais", List.of(
                    new Page("Visão geral — Decorativos",
                            "§7Items §oapenas decorativos§r§7 — sem mecânica forte mas com worldgen ou crafting.\n\n"
                                    + "§a§lLista resumida:§r\n"
                                    + "§7• §dBlood Torch§r§7 — versão sangue do torch\n"
                                    + "§7• §dCorrupted Log§r§7 — log infectado (worldgen)\n"
                                    + "§7• §dHeart of Flesh§r §7(item)\n"
                                    + "§7• §dFlesh Thread§r §7(material — crafta tomes)\n"
                                    + "§7• §dGolden Blood Bowl§r§7 — decoração de altar\n"
                                    + "§7• §dHorus Eye Shard§r §7(coletor lore)"),

                    new Page("Blood Torch",
                            "§4Tocha sangrenta.§r\n\n"
                                    + "§a§lDiferença do vanilla:§r §7emite §oluz vermelha§r§7 (light 11). "
                                    + "Não derrete ice/snow. Repele entities Order Paladin no raio 4.",
                            "liberthia:blood_torch"),

                    new Page("Auto Farmer",
                            "§aFazendeiro automático.§r\n\n"
                                    + "§a§lFunção:§r §7bloco que §oautomaticamente planta + colhe§r§7 crops "
                                    + "em raio 5. GUI tem slot de seed.\n\n"
                                    + "§a§lConsumo:§r §o2 DM/cycle§r§7 (~30s). Drops vão pro inventário interno "
                                    + "(connect com Item Pipe pra extrair).\n\n"
                                    + "§e§lCraft:§r §7Hopper + Diamond + Iron Block + Stick.",
                            "liberthia:auto_farmer"),

                    new Page("Laser Emitter",
                            "§cEmissor de laser.§r\n\n"
                                    + "§7Bloco que §oemite laser§r§7 em linha reta (até 32 blocos). Causa 3 "
                                    + "dano por tick em entities atravessadas.\n\n"
                                    + "§a§lAtivação:§r §7redstone signal.\n"
                                    + "§c§lAtenção:§r §7§lbomba ambulante§r§7 se quebrar com ele ativo — "
                                    + "explode em todas direções.",
                            "liberthia:laser_emitter"),

                    new Page("Gravity Anchor / Gravity Trap",
                            "§5Items de gravidade.§r\n\n"
                                    + "§a§lGravity Anchor§r§7: equipável. Imune a knockback e queda (custo: "
                                    + "§oSlowness I§r§7 passivo).\n\n"
                                    + "§a§lGravity Trap§r§7: bloco. Quando entity passa, §opuxa pra cima§r§7 "
                                    + "5 blocos + queda dano normal.",
                            "liberthia:gravity_anchor")
            )),

            new Chapter("§dQuick Reference — Como Encontrar X", List.of(
                    new Page("Encontrar Dark Matter",
                            "§5§lDark Matter — 5 fontes:§r\n\n"
                                    + "§a§l1. Mineração§r §7(stone/deepslate) — chance 0.5-3% por Y baixo "
                                    + "(v0.1.7+)\n"
                                    + "§a§l2. Dark Matter Ore§r §7em Y<32 (overworld)\n"
                                    + "§a§l3. Crystallizer§r §7em zonas infectadas (drop ao quebrar)\n"
                                    + "§a§l4. Mobs corrompidos§r §7(Possessed Skeleton/Zombie drops Inactive DM)\n"
                                    + "§a§l5. Blood Altar ritual§r §7(sacrificar mob hostile)"),

                    new Page("Encontrar Yellow Matter",
                            "§e§lYellow Matter — 3 fontes (raras):§r\n\n"
                                    + "§a§l1. TNT + DarkMatterShard§r §7(v0.1.7+) — joga shards no chão, "
                                    + "explode = §oconversão 1:1§r\n"
                                    + "§a§l2. Matter Transmuter§r §7(Clear Block + Yellow Ingot = Yellow Block)\n"
                                    + "§a§l3. Order Paladin drop§r §7(ultra-raro)"),

                    new Page("Encontrar Clear Matter",
                            "§f§lClear Matter — 2 fontes:§r\n\n"
                                    + "§a§l1. Biomas frios§r §7(snowy/ice/tundra) — block spawna naturalmente "
                                    + "em afloramentos\n"
                                    + "§a§l2. Matter Transmuter§r §7(Dark Block + Purified Essence = Clear Block)"),

                    new Page("Encontrar Purified Essence",
                            "§f§lPurified Essence — 2 fontes:§r\n\n"
                                    + "§a§l1. Purification Bench§r §7(processa Tainted Essence em Purified)\n"
                                    + "§a§l2. Drop de Order Paladin§r §7(rare)"),

                    new Page("Encontrar Sangue",
                            "§4§lSangue — 4 fontes:§r\n\n"
                                    + "§a§l1. Blood Vial§r §7em mob ferido (HP<50%)\n"
                                    + "§a§l2. Blood Fountain§r §7(estrutura worldgen) — recarrega a cada 10 min\n"
                                    + "§a§l3. Blood Volcano§r §7(spawn estrutura) — ritual produz blood\n"
                                    + "§a§l4. Blood Altar§r §7(sacrificar mob = blood)"),

                    new Page("Encontrar Singularity Core",
                            "§5§lSingularity Core — 2 fontes:§r\n\n"
                                    + "§a§l1. Dark Matter Forge§r §7(Stabilized DM + Void Crystal)\n"
                                    + "§a§l2. Drop ultra-raro de wormholes naturais§r §7(rifts dimensionais)"),

                    new Page("Encontrar Void Crystal",
                            "§8§lVoid Crystal — 2 fontes:§r\n\n"
                                    + "§a§l1. Rifts dimensionais§r §7(drop)\n"
                                    + "§a§l2. Containment Chamber§r §7(DM Block + DM Shard = 4× DM Shard "
                                    + "+ chance pequena de Void Crystal)"),

                    new Page("Encontrar items lore (rares)",
                            "§5§lItems lore raríssimos:§r\n\n"
                                    + "§a§l1. Horus Eye Shard§r §7— Expedições à Ilha de Horus\n"
                                    + "§a§l2. Heart of the Mother§r §7— Boss Flesh Mother\n"
                                    + "§a§l3. Sanguine Core§r §7— Blood Altar sacrifício com Ritual Dagger\n"
                                    + "§a§l4. Crystallized Blood Soul§r §7— Blood Warden boss drop\n"
                                    + "§a§l5. Eye of Horus§r §7— craft com 5 Horus Eye Shards"),

                    new Page("Encontrar mobs custom",
                            "§a§lMobs naturais (sem spawn egg):§r\n\n"
                                    + "§7• §dBlood Warden§r §7→ Blood Volcano ritual\n"
                                    + "§7• §dOrder Paladin§r §7→ Order Shrine (random) + spawn em areas com "
                                    + "muito DM no perfil de jogadores\n"
                                    + "§7• §dPossessed Zombie/Skeleton§r §7→ áreas com infection_growth\n"
                                    + "§7• §dWeaving Shade§r §7→ backrooms / nightmare sequences\n"
                                    + "§7• §dDisarmer§r §7→ estruturas Quarantine Ward")
            )),

            // ============================================================
            // MUDANÇAS RECENTES (CHANGELOG IN-GAME)
            // ============================================================
            new Chapter("§dMudanças v0.1.7", List.of(
                    new Page("O que mudou",
                            "§a§lFeatures novas:§r\n"
                                    + "§7• §dDarkMatterShard§r§7 dropa em mineração (pedra, deepslate, etc) "
                                    + "com chance até 3% no bedrock.\n"
                                    + "§7• §dTNT + DarkMatterShard§r§7 → §dYellowMatterIngot§r§7 via explosão.\n"
                                    + "§7• §dSampleVial§r§7 craftavel.\n"
                                    + "§7• Curas §dMatterCure§r§7 + §dDailyPill§r§7.\n"
                                    + "§7• Pipes agora transferem todos os tipos §osimultaneamente§r.\n\n"
                                    + "§c§lItems removidos:§r\n"
                                    + "§7• HolyEssence → migrado pra PurifiedEssence\n"
                                    + "§7• WhiteMatterFinder, SafeSiphon (cortados do design)"),

                    new Page("DarkMatterShard mineração",
                            "§7Ao §lminerar§r§7 stone/cobblestone/deepslate/tuff/netherrack/blackstone/basalt/"
                                    + "endstone/dripstone com §lpicareta de ferro+§r§7, há chance de dropar:\n\n"
                                    + "§a§lFórmula:§r §70.5%% + 0.005%% × (64 - Y)\n"
                                    + "§7• Em Y=64: §o0.5%%§r\n"
                                    + "§7• Em Y=0: §o0.82%%§r\n"
                                    + "§7• Em Y=-64: §o1.14%%§r\n"
                                    + "§7• Máximo cap: §o3%%§r\n\n"
                                    + "§7Picareta de §c§lmadeira/pedra§r§7 NÃO conta (evita farm fácil)."),

                    new Page("YellowMatterIngot — Receita",
                            "§a§lReceita §r§7(crafting table, 3×3):\n\n"
                                    + "§7  G  S  G\n"
                                    + "§7  D  D  D\n"
                                    + "§7  G  S  G\n\n"
                                    + "§dG§r§7 = §eGlowstone Dust§r§7 (4×)\n"
                                    + "§dS§r§7 = §eSunflower§r§7 (2×)\n"
                                    + "§dD§r§7 = §6DarkMatterShard§r§7 (3×)\n\n"
                                    + "§a§lResultado:§r §71× YellowMatterIngot.\n\n"
                                    + "§7§oA energia solar dos girassóis + brilho da glowstone "
                                    + "transmuta a matéria escura em amarela.§r"),

                    new Page("TNT → YellowMatterIngot §c(legado)",
                            "§c§lOBSOLETO§r§7 — recipe shaped no crafting é a forma oficial agora "
                                    + "(servidores que baniam TNT podem usar). O método antigo "
                                    + "ainda funciona pra compat:\n\n"
                                    + "§7Joga §dDarkMatterShards§r§7 no chão, coloca TNT, ativa.\n"
                                    + "§7Raio §o4 blocos§r§7. 1 shard = 1 ingot."),

                    // ============================================================
                    // MÁQUINAS DO MOD — Como usar cada uma
                    // ============================================================
                    new Page("§6§lMáquinas — Visão Geral",
                            "§7O mod tem várias máquinas processuais. Cada uma faz uma coisa:\n\n"
                                    + "§a• §dMatter Forge§r §7— funde shards/blocks em ingots básicos\n"
                                    + "§a• §dMatter Purifier§r §7— transforma ingots em §opurified ingots§r§7 (usados em armaduras)\n"
                                    + "§a• §dMatter Infuser§r §7— combina 3 matters + catalisador → item especial\n"
                                    + "§a• §dMatter Transmuter§r §7— converte matter A → matter B com catalisador\n"
                                    + "§a• §dContainment Chamber§r §7— neutraliza items infectados / contém radiação\n"
                                    + "§a• §dPurification Bench§r §7— purifica items contaminados sem FE\n"
                                    + "§a• §dMatter Pill Brewer§r §7— fabrica pílulas (Dark/Clear/Yellow)\n"
                                    + "§a• §dMatter Extractor§r §7— drena matter acumulada do player parado em cima"),

                    new Page("Dark Matter Forge §7(fundir)",
                            "§a§lO que faz:§r §7funde §dDarkMatterShard§r§7 em §dDarkMatterIngot§r§7.\n\n"
                                    + "§a§lComo usar:§r\n"
                                    + "§71. Coloque o bloco no mundo (right-click).\n"
                                    + "§72. Click pra abrir GUI.\n"
                                    + "§73. Slot esquerdo: §dDarkMatterShard§r§7 (input).\n"
                                    + "§74. Slot de combustível: §6carvão§r§7 / lava bucket / qualquer fuel vanilla.\n"
                                    + "§75. Slot direito: §dDarkMatterIngot§r§7 sai (output).\n\n"
                                    + "§a§lTempo:§r §710s por ingot (3× mais lento que furnace normal).\n"
                                    + "§a§lYield:§r §71 shard → 1 ingot (sem perda).\n\n"
                                    + "§7Necessário pra produção em massa de ingots brutos antes de purificar."),

                    new Page("Matter Purifier §7(purificar)",
                            "§a§lO que faz:§r §7transforma ingots brutos em §opurified ingots§r§7, "
                                    + "usados pra craftar armaduras e items finais.\n\n"
                                    + "§a§lAceita inputs:§r\n"
                                    + "§7• §dDarkMatterShard§r§7 / §dDarkMatterIngot§r§7 / §dDarkMatterBucket§r§7 → §6Purified Dark§r\n"
                                    + "§7• §dClearMatterIngot§r§7 / §dClearMatterBucket§r§7 → §6Purified Clear§r\n"
                                    + "§7• §dYellowMatterIngot§r§7 / §dYellowMatterBucket§r§7 → §6Purified Yellow§r\n\n"
                                    + "§a§lEnergia:§r §710k FE/operação §o(rápido, 5s)§r§7. SEM FE: 60s lento.\n"
                                    + "§a§lExtra:§r §7items §o☣ Infectados§r§7 também podem ser purificados (custa 100k FE)."),

                    new Page("Matter Infuser §7(combinar)",
                            "§a§lO que faz:§r §7combina §c3 ingots de matter diferente§r§7 + §dcatalisador§r§7 "
                                    + "em um item especial (definido pela receita).\n\n"
                                    + "§a§lLayout:§r\n"
                                    + "§7• 3 slots verticais à esquerda: §dDM§r§7 / §bCM§r§7 / §eYM§r§7\n"
                                    + "§7• Slot central: §dCatalisador§r§7 (magenta)\n"
                                    + "§7• Slot output direita: §6resultado§r§7\n\n"
                                    + "§a§lEnergia:§r §750k FE/operação.\n\n"
                                    + "§7Aceita §oingots, shards OU blocks§r§7 nos 3 slots de matter.\n"
                                    + "§7Usado pra craftar items raros que precisam dos 3 tipos de matter."),

                    new Page("Matter Transmuter §7(converter)",
                            "§a§lO que faz:§r §7converte UM tipo de matter em OUTRO, "
                                    + "consumindo um §dcatalisador§r§7.\n\n"
                                    + "§a§lRotas válidas:§r\n"
                                    + "§7• §dDM§r§7 → §bCM§r §7(catalyst: §bclear ingot§r§7)\n"
                                    + "§7• §bCM§r§7 → §eYM§r §7(catalyst: §eyellow ingot§r§7)\n"
                                    + "§7• §eYM§r§7 → §dDM§r §7(catalyst: §ddark ingot§r§7)\n"
                                    + "§7• §eYM§r§7 → §bCM§r §7(catalyst: §bclear ingot§r§7) §o[novo]§r\n\n"
                                    + "§a§lEntrada/saída:§r §7preserva forma (ingot in → ingot out, block in → block out).\n"
                                    + "§a§lEnergia:§r §730k FE/operação."),

                    new Page("Containment Chamber §7(neutralizar)",
                            "§a§lO que faz:§r §7§lNEUTRALIZA§r§7 items §c☣ Infectados pela Matéria Escura§r§7 "
                                    + "e contém radiação ambiente num raio de §o8 blocos§r§7.\n\n"
                                    + "§a§lComo usar:§r\n"
                                    + "§71. Posicione o bloco perto da área contaminada (ex: dentro de uma sala).\n"
                                    + "§72. Items infectados num raio de §o8 blocos§r §7não aplicam mais "
                                    + "dano/efeitos passivos ao player.\n"
                                    + "§73. Drena §o100 FE/tick§r§7 enquanto ativo. Buffer: 50k FE.\n\n"
                                    + "§a§lFunção secundária:§r §7coloque item infectado dentro pra purificar lentamente "
                                    + "(60s por item, sem custo de FE adicional).\n\n"
                                    + "§7Útil em bases com muito DarkMatter estocado."),

                    new Page("Purification Bench §7(banco)",
                            "§a§lO que faz:§r §7§lpurifica items contaminados§r§7 §oSEM CUSTO DE ENERGIA§r§7 — versão "
                                    + "manual e lenta do Matter Purifier.\n\n"
                                    + "§a§lComo usar:§r\n"
                                    + "§71. Right-click no bench pra abrir GUI.\n"
                                    + "§72. Coloque item §c☣ Infectado§r§7 no slot input.\n"
                                    + "§73. Aguarde §o90 segundos§r§7 (progresso visível na barra).\n"
                                    + "§74. Item sai limpo no output, sem a tag MatterInfected.\n\n"
                                    + "§a§lDiferença pro Matter Purifier:§r\n"
                                    + "§7• §oBench:§r §7sem FE, mais lento (90s), só items infectados.\n"
                                    + "§7• §oPurifier:§r §7com FE (10k), rápido (5s), também converte ingots.\n\n"
                                    + "§7Ideal pra early-game antes de ter geradores de energia.")
            )),

            // ============================================================
            // SISTEMA DE RITUAIS — chalks, candles, sigils, circle
            // ============================================================
            new Chapter("§5Sistema de Rituais", List.of(
                    new Page("§d§lVisão Geral",
                            "§7O sistema de rituais permite invocar §dentidades§r§7, §dpurificar§r§7 áreas, "
                                    + "§dconvocar tempestades§r§7 e §daprender feitiços§r§7 mexendo direto com "
                                    + "as três matérias.\n\n"
                                    + "§a§lComponentes:§r\n"
                                    + "§7• §dRitual Circle§r §7— bloco central onde tudo acontece\n"
                                    + "§7• §dChalks§r §7(5 cores) — desenham padrões no chão\n"
                                    + "§7• §dCandles§r §7(5 cores) — fontes de cera mágica\n"
                                    + "§7• §dSigils§r §7— selos rúnicos colocados no circle\n"
                                    + "§7• §dCatalisadores§r §7— items consumidos pelo ritual\n\n"
                                    + "§7Cada ritual exige uma §lcombinação específica§r§7 desses componentes. "
                                    + "Se a receita bater, ele executa. Se não bater, nada acontece (sem mensagem)."),

                    new Page("§d§lRitual Circle §7(bloco)",
                            "§a§lO bloco central.§r §7Right-click pra abrir a interface ritual.\n\n"
                                    + "§a§lLayout do menu:§r\n"
                                    + "§7• 4 slots pra §dsigils§r §7(centro, 1 por cada direção cardinal)\n"
                                    + "§7• 8 slots pra §dcatalisadores§r §7(items consumidos)\n"
                                    + "§7• Botão §a▶ INICIAR§r§7\n\n"
                                    + "§a§lQuando iniciar:§r\n"
                                    + "§71. Detecta §dchalks§r §7num raio de 4 blocos do circle.\n"
                                    + "§72. Detecta §dcandles ACESAS§r §7num raio de 4 blocos.\n"
                                    + "§73. Lê os sigils nos slots.\n"
                                    + "§74. Lê os catalisadores nos slots.\n"
                                    + "§75. Compara com receitas em §oRitualRegistry§r§7.\n"
                                    + "§76. Se bater: executa após §o5-15s§r §7(partículas + som épico)."),

                    new Page("§d§lChalks §7(giz mágico)",
                            "§7Right-click no chão pra desenhar um padrão. 5 cores:\n\n"
                                    + "§7• §5Chalk Roxo§r§7 — base ritualistica\n"
                                    + "§7• §dChalk Magenta§r§7 — caos / mutação\n"
                                    + "§7• §eChalk Dourado§r§7 — divinatório\n"
                                    + "§7• §fChalk Branco§r§7 — purificação\n"
                                    + "§7• §0Chalk Negro§r§7 — invocação\n\n"
                                    + "§a§lUso típico:§r §7desenhe um círculo de 8 chalks ao redor do "
                                    + "Ritual Circle. Algumas receitas pedem cores específicas (ex: ritual "
                                    + "de §dBael§r§7 quer 4 chalks negros nas diagonais).\n\n"
                                    + "§7§oChalks somem ao quebrar o bloco abaixo deles — não são "
                                    + "permanentes pra forçar o ritualista a redesenhar.§r"),

                    new Page("§d§lCandles §7(velas)",
                            "§7Velas de cera infundida com matter. 5 cores:\n\n"
                                    + "§7• §5Vela Roxa§r §7• §dVela Magenta§r §7• §eVela Dourada§r §7• "
                                    + "§fVela Branca§r §7• §0Vela Negra§r\n\n"
                                    + "§a§lComo acender:§r §7right-click com §dflint and steel§r§7 ou "
                                    + "§dfire charge§r§7. Velas ACESAS emitem partículas e contam pro ritual.\n\n"
                                    + "§a§lDuração:§r §7queimam por §o20 minutos§r§7 (24000 ticks). Após isso "
                                    + "apagam — re-acenda pra reutilizar.\n\n"
                                    + "§a§lUso típico:§r §7coloque §o4-8 velas§r §7em volta do Ritual Circle, "
                                    + "todas acesas. Cor da vela importa: ritual de invocação pede §0negras§r§7, "
                                    + "purificação pede §fbrancas§r§7."),

                    new Page("§d§lSigils §7(selos)",
                            "§7Tabletes rúnicos colocados nos 4 slots centrais do Ritual Circle. "
                                    + "Cada sigil representa uma §oentidade§r§7, §oconceito§r§7 ou §oescola mágica§r§7.\n\n"
                                    + "§a§lSigils principais:§r\n"
                                    + "§7• §dSigil de Bael§r§7 — invocação de invisibilidade/demonio\n"
                                    + "§7• §eSigil de Sandalphon§r§7 — bênção / cura\n"
                                    + "§7• §6Sigil de Metatron§r§7 — proteção / escudo\n"
                                    + "§7• §5Sigil de Astaroth§r§7 — conhecimento / abertura cósmica\n"
                                    + "§7• §0Sigil de Necromancia§r§7 — controle dos mortos\n"
                                    + "§7• §fSigil de Banimento§r§7 — LBRP, purificação total\n"
                                    + "§7• §bSigil do Vazio§r§7 — magia void cósmica\n\n"
                                    + "§a§lObtenção:§r §7craft com purified ingots + lapis, ou drop raro de "
                                    + "mobs específicos (Bael de Witches, Sandalphon de Villagers idosos)."),

                    new Page("§d§lReceitas — Invocação §0Bael",
                            "§5Bael, Demônio da Invisibilidade.§r §7Te dá invisibilidade prolongada.\n\n"
                                    + "§a§lReceita:§r\n"
                                    + "§7• §0Sigil de Bael§r§7 (qualquer slot)\n"
                                    + "§7• §02× Chalks Negros§r§7 + §52× Chalks Roxos§r §7em volta\n"
                                    + "§7• §04× Velas Negras§r §7acesas em volta\n"
                                    + "§7• §dCatalisador:§r §1Ink Sac§r§7 + §0Wither Skull§r\n\n"
                                    + "§a§lResultado:§r\n"
                                    + "§7• Aprende feitiço §d'Bael Invisibility'§r §7(grimoire)\n"
                                    + "§7• Bonus: 5 min de invisibility imediata\n\n"
                                    + "§c§lAviso:§r §7tentativas mal-feitas (faltando velas, sigil errado) "
                                    + "podem dar nausea e wither II por 30s."),

                    new Page("§d§lReceitas — Bênção §eSandalphon",
                            "§eSandalphon, Anjo Mensageiro.§r §7Te abençoa com cura+força+regen.\n\n"
                                    + "§a§lReceita:§r\n"
                                    + "§7• §eSigil de Sandalphon§r§7\n"
                                    + "§7• §e4× Chalks Dourados§r §7+ §f4× Chalks Brancos§r §7em cruz\n"
                                    + "§7• §f4× Velas Brancas§r §7+ §e2× Velas Douradas§r §7acesas\n"
                                    + "§7• §dCatalisador:§r §eGolden Apple§r §7+ §6Glistering Melon§r §7+ §fLapis§r\n\n"
                                    + "§a§lResultado:§r\n"
                                    + "§7• Aprende §e'Sandalphon Blessing'§r §7(buff coletivo de 5 min)\n"
                                    + "§7• Cura imediata 20 HP + Regen V por 60s\n"
                                    + "§7• Som angelical (privado pro ritualista)"),

                    new Page("§d§lReceitas — Escudo §6Metatron",
                            "§6Metatron, Voz de Deus.§r §7Cria escudo absorvendo dano cósmico.\n\n"
                                    + "§a§lReceita:§r\n"
                                    + "§7• §6Sigil de Metatron§r§7\n"
                                    + "§7• §f8× Chalks Brancos§r §7em círculo perfeito\n"
                                    + "§7• §f4× Velas Brancas§r §7nos pontos cardinais (NSEW)\n"
                                    + "§7• §dCatalisador:§r §6Netherite Ingot§r §7+ §dDiamond§r §7×3 + §bAmethyst Shard§r §7×4\n\n"
                                    + "§a§lResultado:§r\n"
                                    + "§7• Aprende §6'Metatron Shield'§r §7(escudo 100 HP por 3 min)\n"
                                    + "§7• Imune a dano divino + matter dano por 2 min\n"
                                    + "§7• Pode ser cast novamente sempre que aprendeu (mana 60)"),

                    new Page("§d§lReceitas — Necromancia",
                            "§0Levantar mortos§r§7 (zombies/skeletons que te obedecem).\n\n"
                                    + "§a§lReceita:§r\n"
                                    + "§7• §0Sigil de Necromancia§r§7\n"
                                    + "§7• §08× Chalks Negros§r §7em volta\n"
                                    + "§7• §06× Velas Negras§r §7acesas\n"
                                    + "§7• §dCatalisador:§r §0Bone§r §7×5 + §cRotten Flesh§r §7×3 + §0Wither Skull§r §7×1\n\n"
                                    + "§a§lResultado:§r\n"
                                    + "§7• Aprende §0'Raise Dead'§r §7(invoca 3 zombies aliados por 60s)\n"
                                    + "§7• Bonus: invoca 1 zumbi permanente imediato (até morrer)\n\n"
                                    + "§c§lAviso:§r §7§lse executar de dia§r§7, os zombies pegam fogo. "
                                    + "Faça à noite ou no Nether/dimensões escuras."),

                    new Page("§d§lReceitas — Banimento (LBRP)",
                            "§f§lLesser Banishing Ritual of the Pentagram.§r §7Limpa entidades hostis "
                                    + "num raio enorme.\n\n"
                                    + "§a§lReceita:§r\n"
                                    + "§7• §fSigil de Banimento§r§7\n"
                                    + "§7• §f5× Chalks Brancos§r §7em pentagrama\n"
                                    + "§7• §f5× Velas Brancas§r §7em pentagrama\n"
                                    + "§7• §dCatalisador:§r §fLapis§r §7×4 + §6Holy Water§r §7×1 + §dEnder Pearl§r §7×1\n\n"
                                    + "§a§lResultado:§r\n"
                                    + "§7• Aprende §f'LBRP'§r §7(banimento de 50 blocos de raio)\n"
                                    + "§7• Bonus imediato: bane todos hostis num raio de 80 blocos\n"
                                    + "§7• Watcher Stalkers e Peripheral Observers somem permanentemente\n\n"
                                    + "§a§l§oUSO PRINCIPAL: limpar bases infestadas pela Loom Dimension."),

                    new Page("§d§lDicas Gerais",
                            "§a§lPlanejamento:§r\n"
                                    + "§7• Construa uma §lcâmara ritualistica§r§7 fixa: Ritual Circle no centro, "
                                    + "espaço pra 8 chalks + 8 velas em raio 4.\n"
                                    + "§7• §lTenha estoque§r §7de cada cor de chalk e vela.\n\n"
                                    + "§a§lAcendimento:§r\n"
                                    + "§7• Use §dfire charge§r§7 pra acender múltiplas velas rápido (raio de "
                                    + "ignição do fire charge funciona).\n"
                                    + "§7• Velas se apagam em 20 min — automatize re-acendimento se for ritualizar "
                                    + "regularmente.\n\n"
                                    + "§a§lFalhas:§r\n"
                                    + "§7• Faltou chalk → ritual não inicia (sem mensagem)\n"
                                    + "§7• Velas apagadas → não contam (mesmo presentes)\n"
                                    + "§7• Sigil errado → aplica §coposto§r§7 (Sandalphon → Necromancia inverte)\n\n"
                                    + "§a§lProteção:§r §7sempre fique fora do círculo durante execução. Alguns "
                                    + "rituais (Bael, Necromancia) podem te afetar negativamente se estiver dentro.")
            )),

            // ============================================================
            // GRIMÓRIO & MAGIA — sistema de feitiços
            // ============================================================
            new Chapter("§5Grimório & Magia", List.of(
                    new Page("§d§lO Grimório",
                            "§7O §dGrimório§r§7 é seu §olivro de feitiços§r§7. Conforme você completa rituais "
                                    + "que ensinam magia, seus feitiços ficam gravados nele.\n\n"
                                    + "§a§lComo obter:§r\n"
                                    + "§7• Craft: §fBook§r §7+ §5Lapis§r §7×2 + §dPurified Dark Ingot§r §7×1 "
                                    + "+ §dEye of Ender§r §7×1\n"
                                    + "§7• Ou: drop raro de Witches (1%) e Evokers (5%)\n\n"
                                    + "§a§lInterface:§r\n"
                                    + "§7• §lShift + Right-Click§r §7— cicla pro próximo feitiço aprendido\n"
                                    + "§7• §lRight-Click§r §7— casta o feitiço atual (consome mana)\n"
                                    + "§7• Tooltip mostra: nome do feitiço, escola, custo de mana, descrição\n\n"
                                    + "§7Comece sem nenhum feitiço — você §lprecisa aprender§r §7via rituais."),

                    new Page("§d§lSistema de Mana",
                            "§7Cada feitiço consome §dmana§r§7. Sua barra de mana fica abaixo da xp bar.\n\n"
                                    + "§a§lValores base:§r\n"
                                    + "§7• Máximo: §d100 mana§r§7\n"
                                    + "§7• Regen: §d+2 mana/segundo§r §7(passivo, sempre)\n"
                                    + "§7• Tempo full vazio→cheio: §o50 segundos§r\n\n"
                                    + "§a§lCustos típicos:§r\n"
                                    + "§7• Feitiço self-buff: §o15-25 mana§r\n"
                                    + "§7• Projétil simples: §o20 mana§r\n"
                                    + "§7• Beam contínuo: §o2-5 mana/tick§r\n"
                                    + "§7• AOE/Summon: §o40-60 mana§r\n"
                                    + "§7• Boss spell (Metatron Shield, LBRP): §o60-80 mana§r\n\n"
                                    + "§a§lMana boost:§r §7armadura de §dpurified clear matter§r §7dá +25 mana "
                                    + "máximo por peça (até +100 com set completo)."),

                    new Page("§d§lEscolas Mágicas",
                            "§7Feitiços são organizados em §o6 escolas§r§7, cada uma com tema distinto:\n\n"
                                    + "§7• §0§lGoetia§r§7 — invocação demoníaca (Bael, Astaroth)\n"
                                    + "§7• §e§lKabbalah§r§7 — bênção angelical (Sandalphon, Metatron)\n"
                                    + "§7• §5§lNecromancia§r§7 — controle dos mortos\n"
                                    + "§7• §b§lHermetismo§r§7 — magia elemental clássica\n"
                                    + "§7• §dCerimonial§r§7 — rituais formais (LBRP, banimentos)\n"
                                    + "§7• §0§lCósmica§r§7 — magia void/insanidade (Forbidden Tome)\n\n"
                                    + "§7Cada escola pede §oartefatos diferentes§r §7pra aprender:\n"
                                    + "§7Goetia → sigils negros + sangue + ossos\n"
                                    + "§7Kabbalah → sigils dourados + apples + lapis\n"
                                    + "§7Cósmica → tomes proibidos + ender essence + soul shards"),

                    new Page("§d§lCastando — Mecânica",
                            "§a§lFluxo do cast:§r\n"
                                    + "§71. Segure o §dGrimório§r §7na mão principal.\n"
                                    + "§72. Verifique feitiço atual (tooltip ou shift-rclick pra cicl ar).\n"
                                    + "§73. §lRight-click§r §7pra castar.\n"
                                    + "§74. Mana é debitada §oANTES§r §7da execução. Se insuficiente: msg "
                                    + "§c'Mana insuficiente!'§r §7+ som de falha.\n"
                                    + "§75. Particula de buildup (1s) + execução.\n\n"
                                    + "§a§lTipos de execução:§r\n"
                                    + "§7• §oPROJECTILE§r§7 — lança projétil onde mira\n"
                                    + "§7• §oBEAM§r§7 — raio contínuo até soltar rclick\n"
                                    + "§7• §oAOE§r§7 — explosão em volta\n"
                                    + "§7• §oSELF_BUFF§r§7 — efeito em você\n"
                                    + "§7• §oTELEPORT§r§7 — TP pra onde mira (max 32 blocos)\n"
                                    + "§7• §oSUMMON§r§7 — invoca aliados temporários\n"
                                    + "§7• §oHEAL§r§7 — cura você ou aliado mirado\n"
                                    + "§7• §oCURSE§r§7 — debuff em alvo mirado"),

                    new Page("§d§lFeitiços Iniciais",
                            "§7§oAprendidos antes mesmo de qualquer ritual — funcionam só com o "
                                    + "Grimório nas mãos:§r\n\n"
                                    + "§a§l1. Magic Missile§r §7(15 mana, PROJECTILE)\n"
                                    + "§7Projétil mágico básico. 8 dmg.\n\n"
                                    + "§a§l2. Mana Surge§r §7(0 mana, SELF_BUFF)\n"
                                    + "§7Drena 20 HP pra ganhar 30 mana. Use em emergência.\n\n"
                                    + "§a§l3. Light§r §7(5 mana, SELF_BUFF)\n"
                                    + "§7Glow effect + night vision 60s. Útil em cavernas escuras.\n\n"
                                    + "§7§oOs outros feitiços você ganha completando rituais — "
                                    + "veja capítulo Sistema de Rituais.§r"),

                    new Page("§d§lFeitiços Avançados",
                            "§a§lCósmicos (Forbidden Tome required):§r\n"
                                    + "§7• §0Void Bolt§r §7(35 mana) — projétil que ignora armadura\n"
                                    + "§7• §0Rift Step§r §7(40 mana) — TP + 3s invisibilidade\n"
                                    + "§7• §0Soul Drain§r §7(30 mana) — beam que rouba HP do alvo\n\n"
                                    + "§a§lNecromancia:§r\n"
                                    + "§7• §5Raise Dead§r §7(50 mana) — 3 zombies aliados 60s\n"
                                    + "§7• §5Bone Spear§r §7(25 mana) — projétil bone, atravessa armor\n\n"
                                    + "§a§lAngelical:§r\n"
                                    + "§7• §eSandalphon Blessing§r §7(60 mana) — buff coletivo aliados\n"
                                    + "§7• §6Metatron Shield§r §7(70 mana) — escudo absorção 100 HP\n\n"
                                    + "§a§lCerimonial:§r\n"
                                    + "§7• §fLBRP§r §7(80 mana) — banimento 50 blocos raio\n"
                                    + "§7• §fAura of Protection§r §7(45 mana) — buff res IV self+aliados 60s"),

                    new Page("§d§lDicas Mágicas",
                            "§a§lEficiência:§r\n"
                                    + "§7• Pílulas §dClear Matter Pill§r §7dão +10 mana máx por 5 min\n"
                                    + "§7• Comer §dGlistering Melon§r §7regenera mana +5/s por 30s\n"
                                    + "§7• Beber §dPotion of Strength§r §7boost dmg dos feitiços (+25%)\n\n"
                                    + "§a§lEvitar:§r\n"
                                    + "§7• Castar SOBRE outro aliado feitiço hostil = friendly fire\n"
                                    + "§7• Castar com baixa luz lunar = -30% potência (matter clear instável)\n"
                                    + "§7• Castar em dimensão void (Loom Dimension) = mana drena 2× mais rápido\n\n"
                                    + "§a§lCombos:§r\n"
                                    + "§7• §dLight§r §7+ §0Void Bolt§r §7= alvos iluminados sofrem +50% dmg\n"
                                    + "§7• §fLBRP§r §7+ §6Metatron Shield§r §7= zona segura permanente curta\n"
                                    + "§7• §0Raise Dead§r §7+ §0Soul Drain§r §7= rouba HP dos zombies pra healing infinito")
            )),

            // ============================================================
            // LOOM DIMENSION — portal, monstros, ores
            // ============================================================
            new Chapter("§5Loom Dimension", List.of(
                    new Page("§d§lA Dimensão Tecedora",
                            "§7Uma dimensão §oentre dimensões§r§7 — onde a realidade é tecida por entidades "
                                    + "que §oobservam de volta§r§7.\n\n"
                                    + "§a§lAtmosfera:§r\n"
                                    + "§7• Céu §0negro vazio§r§7 com estrelas mortas\n"
                                    + "§7• Terreno §0voiditico§r§7, com cristais §5riftite§r§7 emergindo\n"
                                    + "§7• Som ambiente: respirações distantes, sussurros\n"
                                    + "§7• Sem dia/noite — sempre escuridão constante\n\n"
                                    + "§c§lAviso:§r §7entrar sem preparo é §lsuicídio§r§7. Mobs te atacam "
                                    + "constantemente, ores precisam de picareta especial, retornar exige "
                                    + "achar portal de volta."),

                    new Page("§d§lConstruindo o Portal",
                            "§a§lEstrutura:§r §7frame retangular 4×5 (igual portal Nether) feito de "
                                    + "§dLoom Stone§r§7.\n\n"
                                    + "§a§lLoom Stone:§r §7craft = §dObsidian§r §7+ §0Dark Matter Block§r §7+ "
                                    + "§fClear Matter Block§r §7+ §eYellow Matter Block§r §7(1 cada, "
                                    + "shapeless = 4 Loom Stone).\n\n"
                                    + "§a§lAtivação:§r\n"
                                    + "§71. Monte o frame 4×5 (10 Loom Stone total).\n"
                                    + "§72. §lShift + Right-Click§r §7na superfície interna com um §dEye of Ender§r§7.\n"
                                    + "§73. Portal abre — textura roxo-violeta tremida.\n"
                                    + "§74. Entre andando.\n\n"
                                    + "§a§lRetorno:§r §7há portais espontâneos na Loom Dimension. Procure "
                                    + "frames de Loom Stone com partículas. Atravesse pra voltar pro overworld "
                                    + "(coordenadas X/Z preservadas, Y=80)."),

                    new Page("§d§lMonstros — Watcher Stalker",
                            "§5Watcher Stalker§r §7— o §oclassico§r §7da dimensão.\n\n"
                                    + "§a§lComportamento:§r\n"
                                    + "§7• §lQuando você NÃO olha pra ele:§r §7corre rápido na sua direção, "
                                    + "ataque melee (6 dmg).\n"
                                    + "§7• §lQuando você OLHA pra ele:§r §7congela TOTAL (AI desativada via setNoAi).\n"
                                    + "§7• Detection cone: 53° em volta da sua mira (dot > 0.6).\n"
                                    + "§7• Distância max de observação: 48 blocos.\n\n"
                                    + "§a§lEstratégia:§r\n"
                                    + "§7• §lFique olhando§r §7e ele para. Mas você não pode olhar pra todos.\n"
                                    + "§7• Use §6Snowballs§r §7pra estuná-lo de longe.\n"
                                    + "§7• Drops: §dRiftite Shard§r §7(1-3), §0Soul Fragment§r §7(20%)\n"
                                    + "§7• HP: 40. Armor: 4. Knockback resist: 100%."),

                    new Page("§d§lMonstros — Peripheral Observer",
                            "§5Peripheral Observer§r §7— o mestre da §oilusão visual§r§7.\n\n"
                                    + "§a§lComportamento:§r\n"
                                    + "§7• Aparece na §operiferia da sua visão§r §7(dot 0.3-0.5).\n"
                                    + "§7• Se você §olha direto§r §7(dot > 0.97): §lteleporta imediatamente§r §7+ "
                                    + "aplica blindness 60s.\n"
                                    + "§7• 3 tentativas falhas de TP → despawn silencioso.\n\n"
                                    + "§a§lEstratégia:§r\n"
                                    + "§7• Não vale a pena perseguir — sempre escapa.\n"
                                    + "§7• Pra matar: lança item AOE (TNT, Cosmic Bomb) sem precisar olhar.\n"
                                    + "§7• Drops: §dRift Essence§r§7 (raro 5%), §0Cosmic Tear§r§7 (very rare 1%).\n"
                                    + "§7• Não conta pra advancement de kill — invisível mesmo morrendo."),

                    new Page("§d§lMonstros — Screaming Soul",
                            "§5Screaming Soul§r §7— bolas de plasma cinza flutuando.\n\n"
                                    + "§a§lComportamento:§r\n"
                                    + "§7• Vagueia aleatoriamente, sem agressão direta.\n"
                                    + "§7• Se te detecta: §oemite scream sonoro§r §7que causa nausea 100 + "
                                    + "weakness 100 + 4 dmg psíquico.\n"
                                    + "§7• Range do scream: 16 blocos.\n"
                                    + "§7• Pode aplicar §dInsanity§r §7em casos extremos.\n\n"
                                    + "§a§lEstratégia:§r\n"
                                    + "§7• Mate de longe com arco/projétil.\n"
                                    + "§7• HP baixo (15) mas levita = difícil acertar melee.\n"
                                    + "§7• Drops: §dSoul Essence§r §7(garantido), §0Whisper Shard§r §7(30%)\n"
                                    + "§7• §lEvite combate direto§r §7— scream pode te matar em loop."),

                    new Page("§d§lOres da Loom",
                            "§a§lLoom Voidite Ore§r §7— minério §0preto-violeta§r§7. Drop: §5Voidite§r §7×1-2.\n"
                                    + "§7Uso: cosméticos voiditicos, runas avançadas.\n\n"
                                    + "§a§lLoom Riftite Ore§r §7— minério §5roxo brilhante§r§7. Drop: §dRiftite "
                                    + "Shard§r §7×1-3.\n"
                                    + "§7Uso: craft de ferramentas/armaduras spirituais, sigils.\n\n"
                                    + "§a§lLoom Umbral Ore§r §7— minério §0escuro§r §7com partículas pretas. "
                                    + "Drop: §0Umbral Crystal§r §7×1.\n"
                                    + "§7Uso: forge Forbidden Tome, blocos cósmicos.\n\n"
                                    + "§c§lFerramenta exigida:§r §7picareta de §6Netherite+§r §7(diamond não "
                                    + "quebra). Sem picareta correta = ore some sem drop."),

                    new Page("§d§lLoot — Items Exclusivos",
                            "§a§lDa Loom Dimension:§r\n"
                                    + "§7• §dRiftite Sword§r §7— 8 dmg, +50% vs Loom mobs, fenders riftite\n"
                                    + "§7• §dVoidite Pickaxe§r §7— mineração rápida, drops dobrados ores Loom\n"
                                    + "§7• §dSpiritual Armor Set§r §7(craft com Spirit Essence + Riftite) — "
                                    + "set bonus: imune a scream, +25% magic resist, +30 mana máx\n"
                                    + "§7• §0Umbral Crystal§r §7— componente do Forbidden Tome\n"
                                    + "§7• §dEnder Lens§r §7— vê Peripheral Observers como sólidos\n\n"
                                    + "§a§lDrops de chefe (Loom Boss spawn aleatório):§r\n"
                                    + "§7• §0Cosmic Heart§r §7— consumível, +20 max HP permanente\n"
                                    + "§7• §dLoom Crown§r §7— equip cabeça, ver no escuro + +50% xp"),

                    new Page("§d§lEstratégia Geral",
                            "§a§lAntes de entrar:§r\n"
                                    + "§71. Equipe §darmadura Purified Clear§r §7(absorção + mana boost).\n"
                                    + "§72. Estoque §dEnder Pearls§r §7pra escape rápido.\n"
                                    + "§73. Tenha §dGrimório§r §7com §fLBRP§r §7aprendido (banir hostis em emergência).\n"
                                    + "§74. Pílulas §dDark Pill§r §7(+absorption) e §dClear Pill§r §7(+mana).\n\n"
                                    + "§a§lEm combate:§r\n"
                                    + "§7• Mantenha §ldistância§r §7sempre — mobs são fortes em melee.\n"
                                    + "§7• Use §darco encantado Power V§r §7+ flechas brancas (Crystal Arrow).\n"
                                    + "§7• Olhe pra §lWatcher Stalkers§r §7e quebre objetivos atrás dele.\n\n"
                                    + "§a§lRetorno:§r\n"
                                    + "§7• Marque o portal de entrada com §6tochas§r §7nos 4 cantos.\n"
                                    + "§7• Se perder o portal: ache outro frame Loom Stone ou cast §dRift Step§r§7.")
            )),

            // ============================================================
            // FORBIDDEN TOME & COSMIC HORROR
            // ============================================================
            new Chapter("§5Forbidden Tome & Cosmic Horror", List.of(
                    new Page("§d§lForbidden Tome",
                            "§0§lUm livro que NÃO deveria existir.§r §7Compilação de conhecimento extraído "
                                    + "de entidades além da matter.\n\n"
                                    + "§a§lObtenção:§r\n"
                                    + "§7• Ritual cósmico no Ritual Circle (Sigil Astaroth + 4 Velas Negras + "
                                    + "Umbral Crystal + Ender Eye + Wither Star).\n"
                                    + "§7• Drop raríssimo (0.1%) de Wither morto na Loom Dimension.\n"
                                    + "§7• Trade com Aldeão Caçador no nível Master (32 emeralds + 1 Soul Heart).\n\n"
                                    + "§a§lEfeitos passivos enquanto carrega:§r\n"
                                    + "§7• §dSanity drena 1/min§r §7(ver capítulo Sanity)\n"
                                    + "§7• §0Whispers§r §7aleatórios no chat (cosméticos)\n"
                                    + "§7• Mobs cosmicos te ignoram (camuflagem mental)"),

                    new Page("§d§lSanity",
                            "§7Stat invisível baseada no §dGrimório§r §7+ §0Forbidden Tome§r§7 + exposição a "
                                    + "§5horror cósmico§r§7.\n\n"
                                    + "§a§lValores:§r\n"
                                    + "§7• Max: §d100§r §7(início)\n"
                                    + "§7• Min: §00§r §7(insanidade total)\n\n"
                                    + "§a§lO que drena:§r\n"
                                    + "§7• Carregar Forbidden Tome: §c-1/min§r\n"
                                    + "§7• Ver Watcher Stalker: §c-5 instantâneo§r\n"
                                    + "§7• Ver Peripheral Observer (cara a cara): §c-10§r\n"
                                    + "§7• Tomar Scream de Screaming Soul: §c-15§r\n"
                                    + "§7• Castar magia Cósmica: §c-2 por cast§r\n\n"
                                    + "§a§lO que regenera:§r\n"
                                    + "§7• §dDormir§r §7em cama segura: §a+25§r\n"
                                    + "§7• §dMilk Bucket§r §7(beber): §a+15§r\n"
                                    + "§7• Sandalphon Blessing: §a+50 instantâneo§r"),

                    new Page("§d§lEfeitos de Baixa Sanity",
                            "§a§lEm 75 sanity:§r §7sussurros leves no chat.\n\n"
                                    + "§a§lEm 50 sanity:§r §7olhos vermelhos aparecem na periferia da tela "
                                    + "(visual only). HUD distorce levemente.\n\n"
                                    + "§a§lEm 25 sanity:§r §7§lalucinações§r §7— vê mobs falsos (Watchers, "
                                    + "Observers fantasmas que não dão dano). Sons aleatórios de monstros "
                                    + "inexistentes.\n\n"
                                    + "§a§lEm 10 sanity:§r §7tela vibra. Inputs §oinvertem aleatoriamente§r "
                                    + "§7(W vira S por 1s, etc). PostChain shaders cosmic_distort + "
                                    + "cosmic_chromatic ativam.\n\n"
                                    + "§c§lEm 0 sanity:§r §7§l⚠ INSANITY OUTBREAK§r§7 — invocação automática "
                                    + "de §5Inner Demon§r §7que te ataca por 60s. Sanity volta pra 30 após. "
                                    + "Se morrer durante: respawn com Sanity 50 (não permadeath, mas perde XP)."),

                    new Page("§d§lCósmico — Combate",
                            "§a§lMonstros §5cosmicos§r §7não respondem a armas convencionais bem:\n\n"
                                    + "§7• §dDano físico (espadas/arcos)§r §7→ §c-50% efetividade§r\n"
                                    + "§7• §dDano de magia (grimoire spells)§r §7→ §a+50% efetividade§r\n"
                                    + "§7• §dDano sagrado (Sandalphon/Metatron)§r §7→ §a+100% efetividade§r\n"
                                    + "§7• §dDano cósmico (Forbidden Tome spells)§r §7→ §6neutro (sem bonus)§r\n\n"
                                    + "§a§lArmas anti-cósmicas:§r\n"
                                    + "§7• §dSanctified Blade§r §7— craft com diamond + Sandalphon Sigil + "
                                    + "Holy Water. +200% vs cosmic.\n"
                                    + "§7• §dBanishing Arrow§r §7— craft 8 arrows + 1 Sigil Banishment. "
                                    + "Aplica weakness V e remove cosmic 50% chance.\n"
                                    + "§7• §dLBRP cast§r §7— bane num radius 50 bloks."),

                    new Page("§d§lShaders & Visuais",
                            "§a§lQuando você está exposto a horror cósmico, shaders ativam automaticamente:§r\n\n"
                                    + "§7• §dcosmic_chromatic§r §7— aberração cromática nas bordas\n"
                                    + "§7• §dcosmic_distort§r §7— distorção em onda na tela\n"
                                    + "§7• §dcosmic_vignette§r §7— vinheta escura nas bordas\n\n"
                                    + "§a§lTriggers:§r\n"
                                    + "§7• Sanity < 25: shaders ativam permanente\n"
                                    + "§7• Tomar scream: shader pulsa 2s\n"
                                    + "§7• Estar na Loom Dimension: cosmic_vignette ativo\n"
                                    + "§7• Cast Void Bolt: cosmic_distort pulsa 1s\n\n"
                                    + "§a§lDesativar:§r §7nas configs do mod (Options → Liberthia → Disable "
                                    + "Cosmic Shaders) pra jogo menos imersivo mas mais estável."),

                    new Page("§d§lCósmica — Resumo",
                            "§a§lFluxo recomendado:§r\n\n"
                                    + "§71. §lEarly§r §7— ignore horror cósmico. Foque em matter normal.\n\n"
                                    + "§72. §lMid§r §7— acumule Sanctified gear (Sandalphon ritual + Holy Water). "
                                    + "Aprenda LBRP.\n\n"
                                    + "§73. §lLate§r §7— entre na Loom Dimension preparado. Mate Watchers/Observers. "
                                    + "Colete Umbral Crystals.\n\n"
                                    + "§74. §lEndgame§r §7— craft Forbidden Tome. Aprenda Void Bolt, Rift Step, "
                                    + "Soul Drain. Mate Loom Boss (drop Cosmic Heart).\n\n"
                                    + "§75. §lPos-endgame§r §7— +20 HP permanente (Cosmic Heart), Loom Crown, "
                                    + "vence o mod.\n\n"
                                    + "§c§lAviso final:§r §7§lMantenha Sanity > 25§r§7. Insanity Outbreak pode "
                                    + "matar mesmo end-game players. Sempre tenha Milk Bucket no inventario.")
            )),

            // ============================================================
            // QUANTUM TERMINAL — controle remoto
            // ============================================================
            new Chapter("§5Quantum Terminal", List.of(
                    new Page("§d§lO Quantum Terminal",
                            "§7Bloco de §ointerface remota§r §7que permite §lcontrolar Dimensional Antennas§r "
                                    + "§7sem precisar ir até cada uma.\n\n"
                                    + "§a§lCraft:§r §72× §dDimensional Antenna§r §7+ §dPurified Clear Ingot§r §7×3 "
                                    + "+ §bAmethyst Cluster§r §7×4 + §6Comparator§r §7×2 = 1× Quantum Terminal.\n\n"
                                    + "§a§lFunção:§r\n"
                                    + "§7• Detecta TODAS as Dimensional Antennas que compartilham §dfrequência§r§7.\n"
                                    + "§7• Lista cada antena: posição, estado (ativa/inativa), energia, sintonia.\n"
                                    + "§7• Permite §lretunar§r §7frequência remotamente.\n"
                                    + "§7• Permite §lligar/desligar§r §7antenas individualmente."),

                    new Page("§d§lInterface Terminal",
                            "§a§lAo abrir o terminal (right-click):§r\n\n"
                                    + "§7• §dBarra superior:§r §7input de frequência atual (mesmo formato das antenas)\n"
                                    + "§7• §dLista de antenas:§r §7cada row mostra:\n"
                                    + "§7   - Coordenadas (X, Y, Z, dimensão)\n"
                                    + "§7   - Status (●ATIVA / ○INATIVA)\n"
                                    + "§7   - Energia (atual/max)\n"
                                    + "§7   - Sintonia (%)\n"
                                    + "§7   - Botão §a[●Toggle]§r §7e §6[⟳Retune]§r\n\n"
                                    + "§a§lScroll§r §7vertical pra navegar até 32 antenas simultaneamente."),

                    new Page("§d§lOperações Comuns",
                            "§a§lConectar nova antena:§r\n"
                                    + "§71. Coloque antena no mundo.\n"
                                    + "§72. Configure frequência (ex: \"base_alpha\").\n"
                                    + "§73. No Quantum Terminal, set mesma frequência.\n"
                                    + "§74. Antena aparece na lista automaticamente.\n\n"
                                    + "§a§lDesligar antena remota:§r\n"
                                    + "§7• Click [●Toggle] na row dela. Drena 100 FE do terminal.\n\n"
                                    + "§a§lRetunar:§r\n"
                                    + "§7• Click [⟳Retune], digite nova freq, confirma. Antena migra "
                                    + "pra nova rede (sai da lista atual).\n\n"
                                    + "§a§lMonitor de status:§r\n"
                                    + "§7• Refresh a cada 1s. Mostra alarms se sintonia < 40% (amber) ou "
                                    + "energia < 25% (red)."),

                    new Page("§d§lAplicações",
                            "§a§lFazenda de antenas:§r\n"
                                    + "§7Coloque 5-10 antenas em pontos chave do mundo (cada base, dimensão, "
                                    + "etc). Use Quantum Terminal central pra monitorar todas.\n\n"
                                    + "§a§lTeleporter network:§r\n"
                                    + "§7Antenas ativas + sintonia >75% formam pares teleportáveis. Use o "
                                    + "Terminal pra ativar/desativar destinos sob demanda.\n\n"
                                    + "§a§lDetectar invasão:§r\n"
                                    + "§7Antenas detectam mobs hostis num raio de 32 blocos. Status pisca "
                                    + "vermelho no Terminal se monstro entrar zona.\n\n"
                                    + "§a§lSinkhole de matter:§r\n"
                                    + "§7Antenas com sintonia alta drenam Dark Matter ambiente. Útil pra "
                                    + "limpar áreas após explosões/derramamentos."),

                    new Page("§d§lLimitações",
                            "§7• §lAlcance:§r §7sem limite TEÓRICO (cross-dimensional funciona).\n\n"
                                    + "§7• §lLag:§r §7cada antena pinga o Terminal a cada 1s. 30+ antenas "
                                    + "podem causar lag visível em servidores fracos.\n\n"
                                    + "§7• §lEnergia:§r §7Terminal precisa de §o100 FE§r §7por operação remota "
                                    + "(toggle/retune). Conecte um gerador FE ao Terminal.\n\n"
                                    + "§7• §lFrequência conflito:§r §7duas antenas na MESMA freq + posições "
                                    + "muito próximas (<16 blocos) interferem entre si → sintonia despenca.\n\n"
                                    + "§7• §lAuth:§r §7só o §oowner do Terminal§r §7(quem colocou) pode operar. "
                                    + "Use comando §o/liberthia terminal share <player>§r §7pra co-ownership.")
            )),

            // ============================================================
            // r55: PALE WATCH — Pendants + Clone Army + Spirit World controls
            // ============================================================
            new Chapter("§5Pale Watch — Artefatos do Outro Lado", List.of(
                    new Page("§d§lO que é Pale Watch",
                            "§7Família de §5artefatos espirituais§r§7 criados a partir de "
                                    + "minérios das dimensões do Outro Lado (§5Spirit World§r/§5Loom§r).\n\n"
                                    + "§a§lItens da família:§r\n"
                                    + "§7• §5Exército Pálido§r — invoca 5 cópias suas\n"
                                    + "§7• §5Pingente do Olhar Reverso§r — quem te observa, é observado\n"
                                    + "§7• §5Pingente da Respiração Pálida§r — TP atrás do alvo\n"
                                    + "§7• §5Pingente da Paralisia Branca§r — congela quem te ataca\n"
                                    + "§7• §eGuia Pálida§r — aponta saída do Outro Lado\n\n"
                                    + "§7Todos requerem §6Lingote de Ferro Pálido§r, §5Aço da Alma§r, ou §dCristal "
                                    + "do Rifte§r §7— minérios que só nascem no Outro Lado."),

                    new Page("§d§lExército Pálido (Clone Army)",
                            "§7Right-click pra invocar §l5 Cópias Pálidas§r§7 — entidades fantasmas com seus "
                                    + "atributos. Elas:\n\n"
                                    + "§a§l✓§r §7Atacam quem te bater (auto-target)\n"
                                    + "§a§l✓§r §7Têm Movement Speed II + Damage Boost II\n"
                                    + "§a§l✓§r §7Explodem em §6dano AoE§r §7quando morrem\n"
                                    + "§a§l✓§r §7Não atingem você (são leais)\n\n"
                                    + "§c§lCustos:§r\n"
                                    + "§7• Cooldown: 5 minutos\n"
                                    + "§7• §c-15 sanity§r §7por invocação\n"
                                    + "§7• Cópias persistem ~10 min ou até morrer"),

                    new Page("§d§lOlhar Reverso (Staredown)",
                            "§7Equipe no inventário/Curios.\n\n"
                                    + "§a§lEfeito passivo:§r §7se qualquer player ficar olhando direto pra você "
                                    + "(dot >0.95) por mais de §63 segundos§r§7, o §lmouse dele começa a tremer "
                                    + "e girar sozinho§r §7por §68 segundos§r§7.\n\n"
                                    + "§7Cooldown por observador: 20s.\n\n"
                                    + "§7Você recebe alerta in-game quando alguém te observa demais."),

                    new Page("§d§lRespiração Pálida (Blink)",
                            "§7Mire em qualquer entidade dentro de 30 blocos e use §dShift+Right-click§r§7.\n\n"
                                    + "§a§lEfeito:§r §7teleporta você §lpara trás do alvo§r§7, virado pro mesmo "
                                    + "lado que ele. Particles portal envolvem o teleport.\n\n"
                                    + "§c§lCusto:§r §7cooldown 15s.\n\n"
                                    + "§7Ideal pra backstab em PvP e pra evitar dano de mobs ranged."),

                    new Page("§d§lParalisia Branca",
                            "§7Equipe no inventário/Curios.\n\n"
                                    + "§a§lEfeito passivo:§r §7quando você toma dano de uma entidade, ela recebe "
                                    + "automaticamente:\n"
                                    + "§7• Slowness VI (3s)\n"
                                    + "§7• Jump-prevent (3s)\n"
                                    + "§7• Weakness III (3s)\n"
                                    + "§7• Nausea (3s)\n\n"
                                    + "§7Cooldown: 10s entre triggers.\n\n"
                                    + "§7Combina §lpoderosamente§r §7com §5Exército Pálido§r §7— atacante "
                                    + "fica imóvel enquanto seus clones o despedaçam."),

                    new Page("§d§lGuia Pálida",
                            "§7Right-click no §5Spirit World§r §7pra ver §6trail de particles§r §7apontando "
                                    + "para o portal de retorno (sua return position salva).\n\n"
                                    + "§7Indica também a distância em blocos restante.\n\n"
                                    + "§c§lLimitação:§r §7só funciona no Outro Lado. Em outras dims é silenciosa.\n\n"
                                    + "§7Se você foi exilado pelo §dForbidden Tome§r §7e tem Tome Curse, a Guia "
                                    + "Pálida não funciona — use §6Livro do Êxodo§r§7."),

                    new Page("§d§lCrafts (em construção)",
                            "§7As recipes da família Pale Watch usam:\n\n"
                                    + "§a§lLingote de Ferro Pálido§r — refinado de Riftite Ore (Spirit World)\n"
                                    + "§a§lAço da Alma (Soulsteel)§r — fusão de Iron + Ghast Tear + Soul Sand\n"
                                    + "§a§lCristal do Rifte§r — drop raro de mobs do Loom\n\n"
                                    + "§7§oOs recipes detalhados estão em construção. Por enquanto:\n"
                                    + "§7• Use §o/give @s liberthia:clone_army§r §7pra teste\n"
                                    + "§7• Drops naturais aparecerão em v0.1.119+")
            )),

            // ============================================================
            // r55: HUD CONFIGURABLE — sanity bar position
            // ============================================================
            new Chapter("§5Configuração de HUD", List.of(
                    new Page("§d§lAjustar posição da barra de Sanidade",
                            "§7Você pode customizar onde a barra de §5Sanidade§r §7aparece na tela.\n\n"
                                    + "§a§lComandos in-game:§r\n"
                                    + "§7• §o/liberthia hud sanity show§r — info atual\n"
                                    + "§7• §o/liberthia hud sanity anchor <0-3>§r — canto base\n"
                                    + "§7   §80§r = top-left, §81§r = top-right\n"
                                    + "§7   §82§r = bottom-left, §83§r = bottom-right (default)\n"
                                    + "§7• §o/liberthia hud sanity move <x> <y>§r — offset\n"
                                    + "§7• §o/liberthia hud sanity toggle§r — esconde/mostra\n"
                                    + "§7• §o/liberthia hud sanity reset§r — defaults\n\n"
                                    + "§7Config persistido em §oliberthia-client.toml§r §7(folder config/)."),

                    new Page("§d§lExemplos",
                            "§a§lTop esquerdo, 10px do canto:§r\n"
                                    + "§o/liberthia hud sanity anchor 0§r\n"
                                    + "§o/liberthia hud sanity move 10 10§r\n\n"
                                    + "§a§lBottom direito, 92 da direita, 50 do bottom (default):§r\n"
                                    + "§o/liberthia hud sanity anchor 3§r\n"
                                    + "§o/liberthia hud sanity move 92 50§r\n\n"
                                    + "§a§lEsconder completamente:§r\n"
                                    + "§o/liberthia hud sanity toggle§r")
            )),

            // ============================================================
            // r57: 3 LIMINAL DIMENSIONS — perception traps
            // ============================================================
            new Chapter("§5As Três Liminais", List.of(
                    new Page("§d§lO que são as Liminais",
                            "§7Não são dimensões §oonde§r §7você luta. São lugares §oerrados§r §7que "
                                    + "alguém esqueceu de fechar.\n\n"
                                    + "§a§lAs três:§r\n"
                                    + "§7• §bO Mar Invertido§r §7— oceano flutuando acima do céu\n"
                                    + "§7• §dA Cidade Dobrada§r §7— ruas que voltam pra si mesmas\n"
                                    + "§7• §2O Lugar de Madeira§r §7— floresta que respira\n\n"
                                    + "§7§oNão há monstros pra matar. Há §lespaço§r§7§o pra atravessar. "
                                    + "Atravesse devagar.\""),

                    new Page("§d§lEntrada",
                            "§a§lTrês chaves físicas — cada uma 3 usos:§r\n\n"
                                    + "§7• §bBússola Afogada§r §7→ Mar Invertido\n"
                                    + "§7• §dEndereço Dobrado§r §7→ Cidade Dobrada\n"
                                    + "§7• §2Sinal de Casca§r §7→ Lugar de Madeira\n\n"
                                    + "§7Right-click no item te leva. §7§oRight-click DENTRO te traz de volta.§r\n\n"
                                    + "§7Cada uso §lconsome 1 charge§r §7da chave. 3 charges = 3 viagens. "
                                    + "Aí ela vira pó."),

                    new Page("§d§lO Mar Invertido §8| §bUpside Sea",
                            "§7Acima do céu, há outro oceano. Você §lflutua§r §7lá. Slow Falling permanente.\n\n"
                                    + "§a§lO que acontece com você lá:§r\n"
                                    + "§7• §oCamera drifts§r §7sozinha periodicamente\n"
                                    + "§7• §oLevitation§r §7random — você sobe sem querer\n"
                                    + "§7• Sons de §obaleia§r §7vêm de longe (ghasts disfarçados)\n"
                                    + "§7• Partículas de água caem §lpara cima§r\n"
                                    + "§7• Cinza branca paira no ar\n\n"
                                    + "§8§o\"Os peixes nadam acima das nuvens.\""),

                    new Page("§d§lA Cidade Dobrada §8| §dFolded City",
                            "§7Uma cidade onde §oquadras voltam pra si mesmas§r§7. Construa um caminho — "
                                    + "logo descobrirá que ele §lse fecha§r§7.\n\n"
                                    + "§a§lO que acontece com você lá:§r\n"
                                    + "§7• Após §190s movendo + 50b de distância§r§7, snap §lvolta§r §7pro "
                                    + "anchor — sem aviso\n"
                                    + "§7• §oPedestres§r §7soul-particles aparecem ao longe — desaparecem\n"
                                    + "§7• Whispers + sky hum + false footsteps random\n"
                                    + "§7• Camera drifts sutilmente\n"
                                    + "§7• Cinza branca cai do teto invisível\n\n"
                                    + "§8§o\"Vire à esquerda. Sempre.\""),

                    new Page("§d§lO Lugar de Madeira §8| §2Wooden Place",
                            "§7Floresta §oviva§r. As §lFigurações§r §7não andam quando você as observa.\n\n"
                                    + "§a§lFreeze-on-look mechanic:§r\n"
                                    + "§7• Caminhantes de Casca spawn esporadicamente (max 3)\n"
                                    + "§7• Enquanto você OLHA pra eles → §lcongelados§r§7\n"
                                    + "§7• Quando você DESVIA o olhar → §lavanço rápido§r §7até você\n"
                                    + "§7• Heartbeat ambiente baixa frequência\n"
                                    + "§7• Folhas/galhos caem random\n"
                                    + "§7• Sons de madeira partindo ao longe\n\n"
                                    + "§4§lEstratégia:§r §7nunca tire os olhos da multidão.\n\n"
                                    + "§8§o\"Eles só andam quando ninguém vê. Faça vista.\""),

                    new Page("§d§lDeath protection nas Três",
                            "§a§l✓ Items NÃO são perdidos§r §7quando você morre em qualquer das 3 liminais.\n\n"
                                    + "§7Eles §lretornam§r §7no respawn (que ocorre na overworld). Sistema "
                                    + "idêntico ao do Spirit World — exploração arriscada sem perda de loot.\n\n"
                                    + "§8§o\"O lugar errado guarda você. Não fica com nada.\""),

                    new Page("§d§lComo sair",
                            "§a§lTrês formas:§r\n\n"
                                    + "§7§l1.§r §7§lUse a mesma chave novamente§r §7— right-click DENTRO da "
                                    + "liminal. Consome 1 charge mas volta.\n\n"
                                    + "§7§l2.§r §7§lMorra§r §7— acorda na overworld, mantém items (death "
                                    + "protection).\n\n"
                                    + "§7§l3.§r §7§l/liberthia spirit return§r §7(admin) — força return.\n\n"
                                    + "§c§lAVISO:§r §7sem chave + sem morrer = você fica. Os efeitos só "
                                    + "intensificam com o tempo.")
            )),

            // ============================================================
            // r56: COSMIC ARTIFACTS — 15 cryptic artifacts
            // ============================================================
            new Chapter("§5Os Quinze (Cosmic Artifacts)", List.of(
                    new Page("§d§lO que são os Quinze",
                            "§7Artefatos §oachados, não feitos§r§7. Sussurros materializados em metal, "
                                    + "pano e osso. Cada um tem um propósito que você não deveria saber até "
                                    + "usar.\n\n"
                                    + "§a§lFilosofia:§r §7tooltips são §ovagas§r§7 de propósito. Use, observe, "
                                    + "descubra. Aqui no manual há uma §opista§r §7sobre cada — mas a interação "
                                    + "real você só sente no jogo."),

                    new Page("§d§lLinha Puxada §8| §5Marca Silenciosa",
                            "§7§lLinha Puxada§r §7— passive. Quem te empurra, vem junto.\n"
                                    + "§7§lMarca Silenciosa§r §7— right-click numa entidade. Por 10s ela "
                                    + "está §lmarcada§r — dano contra ela é +50%."),

                    new Page("§d§lEco Solitário §8| §5Distância Dobrada",
                            "§7§lEco Solitário§r §7— passive. Quem te observa muito ouve §opassos§r §7que "
                                    + "não são seus.\n"
                                    + "§7§lDistância Dobrada§r §7— shift+rclick numa entidade. Você troca de "
                                    + "lugar com ela."),

                    new Page("§d§lSal da Garganta §8| §5Ferida Macia",
                            "§7§lSal da Garganta§r §7— right-click. Tudo em 8b silencia: weakness + confusion "
                                    + "por 6s.\n"
                                    + "§7§lFerida Macia§r §7— passive. Seu próximo ataque é §oadiado§r §73s "
                                    + "mas dá §c3x dano§r."),

                    new Page("§d§lEspelho que Olha §8| §5Meio Passo",
                            "§7§lEspelho que Olha§r §7— passive. Projéteis (arrows, snowball, ender pearl) "
                                    + "que vêm pra você §lvoltam§r §7pra quem atirou.\n"
                                    + "§7§lMeio Passo§r §7— passive. Você §lesquiva§r §7o primeiro ataque "
                                    + "que receber. Cooldown 10 minutos."),

                    new Page("§d§lFerro Torto §8| §5Moeda Pálida",
                            "§7§lFerro Torto§r §7— passive. 50% chance de degradar arma do atacante quando "
                                    + "você toma dano.\n"
                                    + "§7§lMoeda Pálida§r §7— passive. Quando você morre, ganha §a+50 XP§r "
                                    + "extra em vez de drops do mob."),

                    new Page("§d§lSino Encharcado §8| §5Assobio de Marfim",
                            "§7§lSino Encharcado§r §7— right-click. Slowness IV em todos em 12b por 5s.\n"
                                    + "§7§lAssobio de Marfim§r §7— right-click. Invoca esqueleto §6aliado§r "
                                    + "com damage boost por 60s. Cooldown 2 minutos."),

                    new Page("§d§lVidro que Escuta §8| §5Anel Submerso",
                            "§7§lVidro que Escuta§r §7— right-click. Mira em qualquer entidade até 50b e vê "
                                    + "HP atual / max.\n"
                                    + "§7§lAnel Submerso§r §7— passive. Water Breathing + Slow Falling "
                                    + "infinitos enquanto carrega."),

                    new Page("§d§lMão no Vidro",
                            "§7§lMão no Vidro§r §7— passive. Quando você morre, §l1 item§r §7(o melhor) é "
                                    + "preservado e devolvido no respawn.\n\n"
                                    + "§7Combina §lpoderosamente§r §7com Spirit World death protection — "
                                    + "lá nada se perde, em outras dims ao menos 1 item.")
            )),

            // ============================================================
            // r56: EXODUS BOOK + Wood Dimension Event
            // ============================================================
            new Chapter("§5Livro do Êxodo & Lugares Estranhos", List.of(
                    new Page("§d§lLivro do Êxodo",
                            "§e§o\"A primeira página é vazia.\n"
                                    + "§e§o A segunda também.\n"
                                    + "§e§o A décima diz uma palavra.\n"
                                    + "§e§o As outras você queima ao ler.\"§r\n\n"
                                    + "§a§lFunção (oculta — descubra):§r\n"
                                    + "§7Right-click no §5Spirit World§r §7→ §lquebra a Tome Curse§r §7e "
                                    + "te traz de volta. Item é consumido. Aplica regen II + saturation + "
                                    + "hero of the village (5min).\n\n"
                                    + "§a§lObtenção:§r §7§lAPENAS§r §7como drop de entidades sumonadas via "
                                    + "ritual cósmico — nunca crafted. Pergunte aos invocados."),

                    new Page("§d§lO Lugar de Madeira",
                            "§7Em sanidade ≤ 25, em qualquer dimensão, há uma chance pequena de você "
                                    + "ser §olevado a outro lugar§r§7.\n\n"
                                    + "§7Você sente o §oar ficar grosso§r§7. Particles de tronco e folhas. "
                                    + "Cinco figuras de madeira aparecem em volta.\n\n"
                                    + "§a§lComportamento das figuras:§r\n"
                                    + "§7• Quando você §lOLHA§r §7pra elas, ficam congeladas\n"
                                    + "§7• Quando você §lDESVIA§r §7o olhar, elas avançam\n"
                                    + "§7• Em <5 blocos = atacam (2 damage cada hit)\n\n"
                                    + "§7Após §6~20 segundos§r §7você §lvolta§r §7sozinho. Cooldown 30 min IRL.\n\n"
                                    + "§8§o\"Eles só andam quando ninguém vê. Faça vista.\"")
            )),

            // ============================================================
            // r55: SPIRIT WORLD — Survival Guide
            // ============================================================
            new Chapter("§5Guia do Outro Lado (Spirit World)", List.of(
                    new Page("§d§lO que é o Outro Lado",
                            "§7Dimensão paralela acessada via §dSoul Sever§r§7, §0Forbidden Tome§r§7 (forced "
                                    + "exile), ou §dRitual Espiritual§r§7. Lá:\n\n"
                                    + "§7• Geografia §oimpossível§r — flutuante, etérea\n"
                                    + "§7• Mobs §pálidos§r — invisíveis, sussurros\n"
                                    + "§7• Você §lnão pode digitar§r §7chat ou §o/tell§r §7lá (r54)\n"
                                    + "§7• §lAo morrer, items voltam§r §7(r55) — não há perda permanente\n"
                                    + "§7• Sanidade drena §c1/5s§r §7enquanto você está lá"),

                    new Page("§d§lComo sair",
                            "§a§l1. Via Return:§r §7clique no portal espiritual com qualquer item Soul Sever, "
                                    + "automaticamente teleporta de volta (se não está Tome-cursed).\n\n"
                                    + "§a§l2. Via Prayer Book / Livro do Êxodo:§r §7único método se você está "
                                    + "§4Tome-cursed§r §7(foi exilado pelo Forbidden Tome).\n\n"
                                    + "§a§l3. Via /liberthia spirit return:§r §7admin command.\n\n"
                                    + "§c§lO chat NÃO funciona lá.§r §7Não tente §o/tell§r §7ou §o/msg§r §7— "
                                    + "comandos de comunicação são bloqueados pra reforçar isolation. Mensagem "
                                    + "in-game: §o\"Sua voz não chega daqui.\"§r"),

                    new Page("§d§lParanóia de Sanity Baixa",
                            "§7Em baixa sanidade (≤50), você começa a §opercebter§r §7coisas que outros "
                                    + "players não vêem:\n\n"
                                    + "§7• §0Silhuetas§r §7de player apareçendo/sumindo a 10-20b\n"
                                    + "§7• §0Sombra Stalker§r §7— silhueta distante que se aproxima quando você "
                                    + "desvia o olhar\n"
                                    + "§7• §dMobs com olhos vermelhos§r §7encarando você (só você vê)\n"
                                    + "§7• §dÁrvores fantasmas§r §7flutuando perto de você\n"
                                    + "§7• Sons random: passos, sussurros, gritos distantes\n\n"
                                    + "§c§lQuanto menor a sanity, mais frequente.§r §7Em sanity ≤10 é "
                                    + "constante e perturbador."),

                    new Page("§d§lShadow Stalker — como sobreviver",
                            "§7Entidade que só aparece §6quando sua sanity ≤40§r §7ou você está no Spirit World.\n\n"
                                    + "§a§lComportamento:§r\n"
                                    + "§71. Spawn a 35 blocos. Você vê uma §0silhueta§r §7distante.\n"
                                    + "§72. Quando você OLHA pra ela, ela §lcongela§r§7.\n"
                                    + "§73. Quando você §lDESVIA o olhar§r§7, ela teleporta §c4 blocos mais perto§r§7.\n"
                                    + "§74. Em §c<9 blocos§r §7= scream + 12 damage + blindness + slowness 100t.\n\n"
                                    + "§a§lEstratégia:§r §7§lMantenha ela no campo de visão§r§7. Particles SOUL "
                                    + "ao redor indicam onde ela está. Recue para uma área iluminada com cama."),

                    new Page("§d§lIlusão de Chunk",
                            "§7Quando você está minerando §lprofundamente§r §7(Y≤30) com §lsanity baixa§r §7(≤50), "
                                    + "há uma §61% de chance§r §7por bloco quebrado de você ser §oteleportado pro "
                                    + "Spirit World§r §7nas mesmas coordenadas.\n\n"
                                    + "§a§lTrigger silencioso:§r §7sem mensagem. Você só percebe quando sobe e vê "
                                    + "o §dcéu pálido§r §7+ §dgeografia diferente§r§7.\n\n"
                                    + "§a§lSaída:§r §7use Guia Pálida pra encontrar o portal de retorno, ou "
                                    + "Soul Sever pra forçar return.\n\n"
                                    + "§c§lCooldown:§r §720 min IRL entre triggers."),

                    new Page("§d§lDeath protection",
                            "§7Spirit World é um lugar §olunático§r §7— Minecraft já é punitivo demais "
                                    + "lá. Por isso (r55):\n\n"
                                    + "§a§l✓ Items NÃO são dropados§r §7quando você morre no Outro Lado.\n"
                                    + "§a§l✓ Tudo é restaurado no respawn§r §7(que ocorre no overworld).\n"
                                    + "§a§l✓ XP §ré§r §7perdido (vanilla behavior)\n\n"
                                    + "§7Isso permite §lexploração arriscada§r §7sem medo de perda permanente. "
                                    + "Você ainda morre, ainda perde XP, ainda volta pro spawn — mas seu loot "
                                    + "duramente conquistado §lvolta junto§r§7.")
            ))
    );

    /**
     * Retorna o conjunto de capítulos baseado no locale do cliente.
     * Suporta PT-BR (default) e EN-US (fallback pra qualquer outro idioma).
     */
    /**
     * BUG histórico: cliente em locale EN carregava ManualContentEn (só 6
     * capítulos / 27 páginas) enquanto o PT tem 39 capítulos / 363 páginas.
     * Usuário com cliente em inglês via 86% menos conteúdo.
     *
     * Fix: SEMPRE retorna o PT-BR (que é o catálogo completo). EN existe só
     * pra capítulos básicos traduzidos — mas como é parcial, ignoramos.
     * Quando o EN estiver 100% completo, podemos voltar a usar locale-based.
     */
    public static List<Chapter> chaptersForLocale(String localeCode) {
        return CHAPTERS;
    }
}
