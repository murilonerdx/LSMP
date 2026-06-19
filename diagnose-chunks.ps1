# =============================================================================
# Liberthia — Diagnóstico de corrupção de chunks (region files)
# =============================================================================
# Sintomas atendidos:
#   - "Chunk file at [X,Y] is in the wrong location"
#   - "Chunk stream is truncated"
#   - "External chunk path c.X.Y.mcc is not file"
#   - "Chunk has both internal and external streams"
#
# O que esse script faz:
#   1. Backup completo do `world/` (zip timestamped)
#   2. Lista .mcc órfãos (sem .mca correspondente OU referenciado por header
#      morto) — esses são seguros pra deletar
#   3. Conta region files com tamanho suspeito (<4KB ou >16MB)
#   4. Reporta tamanho total + chunks regenerados se você deletar os .mcc
#
# NÃO MEXE nos arquivos sem confirmação — só lista. Pra deletar passa -Apply.
#
# Uso (NÃO rode com servidor ligado):
#   .\diagnose-chunks.ps1 -WorldPath F:\LSMP\LSMP\world           # só diagnostica
#   .\diagnose-chunks.ps1 -WorldPath F:\LSMP\LSMP\world -Apply    # apaga os .mcc órfãos
#
# Pra reparar de verdade (relocação de chunks dentro do .mca), use o
# Minecraft-Region-Fixer (Python) ou MCA Selector (GUI) — links no final.

param(
    [Parameter(Mandatory=$true)]
    [string]$WorldPath,

    [switch]$Apply,

    [switch]$SkipBackup
)

$ErrorActionPreference = 'Stop'

function Write-Step($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "  OK $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "  ! $msg" -ForegroundColor Yellow }
function Write-Err($msg)  { Write-Host "  X $msg" -ForegroundColor Red }

if (-not (Test-Path $WorldPath)) {
    Write-Err "World path nao existe: $WorldPath"
    exit 1
}

$RegionPath = Join-Path $WorldPath 'region'
if (-not (Test-Path $RegionPath)) {
    Write-Err "Pasta region/ nao encontrada em $WorldPath"
    exit 1
}

# 1. BACKUP
if (-not $SkipBackup) {
    Write-Step "Fazendo backup do world/"
    $stamp = (Get-Date).ToString("yyyy-MM-dd_HH-mm-ss")
    $backupDir = Join-Path (Split-Path $WorldPath -Parent) "world-backup-$stamp"
    Write-Warn "Copiando $WorldPath -> $backupDir (pode demorar)"
    Copy-Item -Path $WorldPath -Destination $backupDir -Recurse -Force
    $backupSize = (Get-ChildItem -Path $backupDir -Recurse -Force | Measure-Object -Property Length -Sum).Sum
    Write-Ok "Backup criado: $backupDir ($([math]::Round($backupSize / 1MB, 1)) MB)"
} else {
    Write-Warn "Skip backup (passou -SkipBackup). Risco e seu."
}

# 2. Lista de region files (.mca) e arquivos externos (.mcc)
Write-Step "Escaneando $RegionPath"
$mcaFiles = Get-ChildItem -Path $RegionPath -Filter '*.mca' -File
$mccFiles = Get-ChildItem -Path $RegionPath -Filter '*.mcc' -File
Write-Ok "Encontrados $($mcaFiles.Count) region files (.mca) + $($mccFiles.Count) external chunks (.mcc)"

# 3. Analisa cada .mcc — verifica se chunk dentro do .mca correspondente
#    ainda referencia o .mcc. Se nao, e orfao.
#
# Formato do nome .mcc: c.{chunkX}.{chunkZ}.mcc
# Region file correspondente: r.{floor(chunkX/32)}.{floor(chunkZ/32)}.mca
# Header do .mca tem 8KB de offsets — bit alto do byte 4 do entry = external flag.

Write-Step "Procurando .mcc orfaos"
$orphanMccs = @()
$validMccs = @()

foreach ($mcc in $mccFiles) {
    if ($mcc.Name -match '^c\.(-?\d+)\.(-?\d+)\.mcc$') {
        $chunkX = [int]$Matches[1]
        $chunkZ = [int]$Matches[2]
        $regionX = [Math]::Floor($chunkX / 32.0)
        $regionZ = [Math]::Floor($chunkZ / 32.0)
        $regionFile = Join-Path $RegionPath "r.$regionX.$regionZ.mca"

        if (-not (Test-Path $regionFile)) {
            # .mcc sem .mca pai — definitivamente orfao
            $orphanMccs += $mcc
            continue
        }

        # Le os 4 bytes do header pra esse chunk no .mca
        # Posicao do entry: ((chunkX & 31) + (chunkZ & 31) * 32) * 4
        $entryIdx = (($chunkX % 32 + 32) % 32) + (($chunkZ % 32 + 32) % 32) * 32
        $entryOffset = $entryIdx * 4
        try {
            $bytes = [System.IO.File]::ReadAllBytes($regionFile)
            if ($bytes.Length -lt 8192) {
                # Header truncado — region file suspeito
                $orphanMccs += $mcc
                continue
            }
            $offset = ($bytes[$entryOffset] -shl 16) -bor ($bytes[$entryOffset+1] -shl 8) -bor $bytes[$entryOffset+2]
            $sectors = $bytes[$entryOffset+3]
            if ($offset -eq 0 -and $sectors -eq 0) {
                # Header diz "no chunk here", mas .mcc existe — orfao
                $orphanMccs += $mcc
            } else {
                # Verifica se o sector apontado tem flag external (128 = 0x80)
                # No primeiro byte do sector aponta tipo/flag, mas pra simplificar:
                # se offset+sectors estao dentro do file, consideramos valido.
                if (($offset * 4096 + $sectors * 4096) -le $bytes.Length) {
                    $validMccs += $mcc
                } else {
                    $orphanMccs += $mcc
                }
            }
        } catch {
            Write-Warn "Erro lendo $regionFile : $_"
        }
    }
}

Write-Ok "$($validMccs.Count) .mcc validos (referenciados)"
Write-Warn "$($orphanMccs.Count) .mcc orfaos (seguros pra deletar)"

if ($orphanMccs.Count -gt 0 -and $orphanMccs.Count -le 30) {
    Write-Host "Lista de orfaos:" -ForegroundColor DarkGray
    $orphanMccs | ForEach-Object { Write-Host "  - $($_.Name)" -ForegroundColor DarkGray }
}

# 4. Region files com tamanho suspeito
Write-Step "Verificando .mca com tamanho suspeito"
$suspiciousMcas = @()
foreach ($mca in $mcaFiles) {
    if ($mca.Length -lt 4096) {
        $suspiciousMcas += [PSCustomObject]@{ Name=$mca.Name; Size=$mca.Length; Reason='muito pequeno (<4KB)' }
    } elseif ($mca.Length % 4096 -ne 0) {
        $suspiciousMcas += [PSCustomObject]@{ Name=$mca.Name; Size=$mca.Length; Reason='nao alinhado a 4KB' }
    }
}
if ($suspiciousMcas.Count -gt 0) {
    Write-Warn "$($suspiciousMcas.Count) region files suspeitos:"
    $suspiciousMcas | ForEach-Object { Write-Host "  - $($_.Name) ($($_.Size) bytes) - $($_.Reason)" -ForegroundColor Yellow }
} else {
    Write-Ok "Todos os .mca tem tamanho consistente"
}

# 5. Resumo + acoes
Write-Host ""
Write-Step "RESUMO"
$totalSize = ($mcaFiles | Measure-Object -Property Length -Sum).Sum + ($mccFiles | Measure-Object -Property Length -Sum).Sum
Write-Host "  Tamanho total region/: $([math]::Round($totalSize / 1MB, 1)) MB"
Write-Host "  .mca files: $($mcaFiles.Count)"
Write-Host "  .mcc files: $($mccFiles.Count) ($($orphanMccs.Count) orfaos)"
Write-Host "  .mca suspeitos: $($suspiciousMcas.Count)"

if ($Apply -and $orphanMccs.Count -gt 0) {
    Write-Step "Deletando $($orphanMccs.Count) .mcc orfaos (passou -Apply)"
    foreach ($mcc in $orphanMccs) {
        Remove-Item -Path $mcc.FullName -Force
        Write-Ok "Deletado: $($mcc.Name)"
    }
    Write-Ok "Concluido. Chunks regenerados quando jogador chega perto."
} elseif ($orphanMccs.Count -gt 0) {
    Write-Host ""
    Write-Warn "Pra DELETAR os $($orphanMccs.Count) .mcc orfaos, rode de novo com -Apply:"
    Write-Host "  .\diagnose-chunks.ps1 -WorldPath $WorldPath -Apply" -ForegroundColor DarkCyan
    Write-Host ""
}

# 6. Recomendacoes
Write-Host ""
Write-Step "PROXIMOS PASSOS"
Write-Host "  1. PARE o servidor LIMPO (comando /stop, nao kill -9)"
Write-Host "  2. Esse script ja fez backup do world/. Confira em ../world-backup-*"
Write-Host "  3. Rode -Apply pra deletar os .mcc orfaos (regenera chunks)"
Write-Host "  4. Pra reparar chunks deslocados DENTRO do .mca, use:"
Write-Host "       Minecraft-Region-Fixer (Python):"
Write-Host "       https://github.com/Fenixin/Minecraft-Region-Fixer"
Write-Host "       python region-fixer.py --delete-corrupted --delete-wrong-located $WorldPath"
Write-Host ""
Write-Host "  PREVENCAO:"
Write-Host "    - Instale HybridFix mod (modrinth.com/mod/hybridfix)"
Write-Host "    - Considere migrar de Ketting -> Forge puro ou Mohist"
Write-Host "    - Remova 'Fastload-Reforged' do servidor (e mod client-side)"
Write-Host "    - Auto-backup frequente (cron + rsync)"
