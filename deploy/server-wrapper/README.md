# Liberthia — Manutenção de servidor & backup de chunks

Sistema embutido no mod pra lidar com corrupção de chunks: backup datado dos
arquivos `.mca`, desligamento gracioso (salva → backup → kicka → para) e
restauração segura de regions corrompidas no próximo boot.

Todos os comandos são `/liberthia server ...` (console ou OP).

## Comandos

| Comando | OP | O que faz |
|---|----|-----------|
| `/liberthia server backup` | 4 | Salva o mundo e copia TODOS os `.mca` (region/entities/poi de todas as dimensões) pra `liberthia_chunk_backups/<data>/`. |
| `/liberthia server shutdown [motivo]` | 4 | Sequência completa: salva → backup → aviso com contagem (proporcional ao nº de players, 10–60s) → kicka todos → espera 10s → **para** o servidor. |
| `/liberthia server backups` | 2 | Lista as pastas de backup. |
| `/liberthia server regions <backup>` | 2 | Lista os `.mca` de um backup (com tab-complete). |
| `/liberthia server restore <backup> <region>` | 4 | **Agenda** restaurar uma region (ex. `region/r.0.0.mca`). Aplica no próximo boot. |
| `/liberthia server restores` | 2 | Lista as restaurações agendadas. |
| `/liberthia server restore-cancel` | 4 | Cancela as restaurações agendadas. |

## Como consertar uma chunk corrompida

1. Identifique a region corrompida (ex. olhando o crash/log: `r.X.Z.mca`).
2. `/liberthia server backups` → veja os backups (de quando o mapa estava ok).
3. `/liberthia server regions <backup>` → ache o caminho da region.
4. `/liberthia server restore <backup> region/r.X.Z.mca` → agenda a troca.
5. `/liberthia server shutdown` → o servidor salva, faz backup e **para**.
6. Suba o servidor de novo (manual, ou pelo wrapper abaixo). No boot, ANTES do
   mundo carregar, o mod copia o `.mca` do backup por cima do corrompido. Uma
   cópia do arquivo antigo (mesmo corrompido) vai pra `liberthia_chunk_backups/_pre_restore_<data>/`
   caso precise desfazer.

> A restauração é feita no `ServerAboutToStartEvent` (arquivo não está aberto/locked).
> Por isso ela só vale **no próximo boot** — não troca `.mca` com o mundo carregado.

## Onde ficam os arquivos

Relativos à **pasta do servidor** (pai da pasta `world`):

- `liberthia_chunk_backups/<data>/...` — backups (espelham os caminhos do mundo).
- `liberthia_chunk_backups/_pre_restore_<data>/...` — cópia dos arquivos trocados.
- `liberthia_pending_restores.txt` — fila de restaurações (vira `.applied-<data>` após o boot).

## Auto-restart (opcional)

O mod **só para** o servidor — o relançamento é manual. Se quiser que ele volte
sozinho, lance o servidor pelos wrappers deste diretório em vez do `java` direto:

- **Windows:** `start_server.bat`
- **Linux/macOS:** `start_server.sh` (`chmod +x` primeiro)

Ajuste `JAR` e `MEM` no topo do script. Eles re-executam o servidor toda vez que
o processo sai. (Em Linux, uma unit `systemd` com `Restart=always` faz o mesmo.)
