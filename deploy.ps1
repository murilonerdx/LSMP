# Build + deploy do mod Liberthia para as instances do MC.
#
# Workflow:
#   1. Le versao atual de gradle.properties
#   2. Incrementa o patch (0.1.7 -> 0.1.8 ...)
#   3. Atualiza gradle.properties
#   4. Roda gradlew build -x test
#   5. Localiza o jar gerado
#   6. Remove versoes antigas das pastas de deploy
#   7. Copia o jar novo
#
# Uso:
#   .\deploy.ps1                       -> incrementa patch + deploy
#   .\deploy.ps1 -NoIncrement          -> rebuild sem virar versao
#   .\deploy.ps1 -VersionPart minor    -> 0.1.7 -> 0.2.0
#   .\deploy.ps1 -VersionPart major    -> 0.1.7 -> 1.0.0

param(
    [switch]$NoIncrement,
    [ValidateSet('patch', 'minor', 'major')]
    [string]$VersionPart = 'patch'
)

$ErrorActionPreference = 'Stop'

function Write-Step($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "  OK $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "  ! $msg" -ForegroundColor Yellow }
function Write-Err($msg)  { Write-Host "  X $msg" -ForegroundColor Red }

$ModRoot = $PSScriptRoot
if (-not $ModRoot) { $ModRoot = (Get-Location).Path }

$DeployTargets = @(
    'C:\Users\T-GAMER\curseforge\minecraft\Instances\liberthia\mods',
    'C:\Users\T-GAMER\Desktop\a\mods'
)

# 1. Le versao atual
Write-Step "Lendo versao atual"
$GradleProps = Join-Path $ModRoot 'gradle.properties'
if (-not (Test-Path $GradleProps)) {
    Write-Err "gradle.properties nao encontrado em $ModRoot"
    exit 1
}
$Content = Get-Content $GradleProps -Raw
if ($Content -notmatch 'mod_version=(\d+)\.(\d+)\.(\d+)') {
    Write-Err 'mod_version nao encontrado no formato X.Y.Z em gradle.properties'
    exit 1
}
$Major = [int]$Matches[1]
$Minor = [int]$Matches[2]
$Patch = [int]$Matches[3]
$CurrentVersion = "$Major.$Minor.$Patch"
Write-Ok "Versao atual: $CurrentVersion"

# 2. Incrementa
if ($NoIncrement) {
    $NewVersion = $CurrentVersion
    Write-Warn "Sem incremento (mantendo $CurrentVersion)"
} else {
    switch ($VersionPart) {
        'major' { $Major++; $Minor = 0; $Patch = 0 }
        'minor' { $Minor++; $Patch = 0 }
        'patch' { $Patch++ }
    }
    $NewVersion = "$Major.$Minor.$Patch"
    Write-Step "Incrementando ${VersionPart}: $CurrentVersion -> $NewVersion"

    $NewContent = $Content -replace 'mod_version=\d+\.\d+\.\d+', "mod_version=$NewVersion"
    [System.IO.File]::WriteAllText($GradleProps, $NewContent, [System.Text.UTF8Encoding]::new($false))
    Write-Ok "gradle.properties atualizado: mod_version=$NewVersion"
}

# 3. Build
Write-Step "Rodando gradlew build -x test"
Push-Location $ModRoot
try {
    & cmd /c '.\gradlew.bat build -x test'
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Build FALHOU (exit code $LASTEXITCODE)"
        if (-not $NoIncrement) {
            Write-Warn "Revertendo gradle.properties para $CurrentVersion"
            [System.IO.File]::WriteAllText($GradleProps, $Content, [System.Text.UTF8Encoding]::new($false))
        }
        exit 1
    }
    Write-Ok "Build SUCCESSFUL"
} finally {
    Pop-Location
}

# 4. Localiza o jar
$JarName = "liberthia-$NewVersion.jar"
$JarSource = Join-Path $ModRoot "build\libs\$JarName"
if (-not (Test-Path $JarSource)) {
    Write-Err "Jar nao encontrado em $JarSource"
    Write-Host "Conteudo de build/libs:" -ForegroundColor Yellow
    Get-ChildItem (Join-Path $ModRoot 'build\libs') -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "  - $($_.Name)" }
    exit 1
}
$JarSize = [math]::Round((Get-Item $JarSource).Length / 1MB, 2)
Write-Ok "Jar gerado: $JarName ($JarSize MB)"

# 5. Deploy nas pastas
foreach ($Target in $DeployTargets) {
    Write-Step "Deploy em: $Target"

    if (-not (Test-Path $Target)) {
        Write-Warn "Pasta nao existe -- pulando"
        continue
    }

    $OldJars = Get-ChildItem -Path $Target -Filter 'liberthia-*.jar' -File -ErrorAction SilentlyContinue
    foreach ($Old in $OldJars) {
        if ($Old.Name -eq $JarName) { continue }
        Remove-Item -Path $Old.FullName -Force
        Write-Ok "Removido antigo: $($Old.Name)"
    }

    $DestJar = Join-Path $Target $JarName
    Copy-Item -Path $JarSource -Destination $DestJar -Force
    Write-Ok "Copiado: $DestJar"
}

# 6. Status final
Write-Host ""
Write-Host "Deploy completo" -ForegroundColor Green
Write-Host "  Versao:  $NewVersion" -ForegroundColor Green
Write-Host "  Arquivo: $JarName ($JarSize MB)" -ForegroundColor Green
Write-Host "  Pastas:  $($DeployTargets.Count) destinos" -ForegroundColor Green
