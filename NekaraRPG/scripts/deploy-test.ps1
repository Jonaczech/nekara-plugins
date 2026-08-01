[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$ServerPath
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceJar = Join-Path $projectRoot "dist\NekaraRPG.jar"
if (-not (Test-Path -LiteralPath $sourceJar -PathType Leaf)) {
    throw "Release JAR is missing. Run scripts\build-release.cmd first."
}

$resolvedServer = (Resolve-Path -LiteralPath $ServerPath).Path
$serverProperties = Join-Path $resolvedServer "server.properties"
if (-not (Test-Path -LiteralPath $serverProperties -PathType Leaf)) {
    throw "The selected directory does not look like a Minecraft server: $resolvedServer"
}

$pluginsDirectory = Join-Path $resolvedServer "plugins"
if (-not (Test-Path -LiteralPath $pluginsDirectory -PathType Container)) {
    New-Item -ItemType Directory -Path $pluginsDirectory | Out-Null
}

$destinationJar = Join-Path $pluginsDirectory "NekaraRPG.jar"
$conflictingJars = @(
    Get-ChildItem -LiteralPath $pluginsDirectory -File |
        Where-Object {
            ($_.Name -like "NekaraRPG*.jar" -and $_.FullName -ne $destinationJar) -or
            $_.Name -like "NekaraFishing*.jar"
        }
)
if ($conflictingJars.Count -gt 0) {
    $names = ($conflictingJars | ForEach-Object Name) -join ", "
    throw "Conflicting Nekara plugin JARs found in plugins: $names. Move them outside plugins before deployment."
}

Copy-Item -LiteralPath $sourceJar -Destination $destinationJar -Force
$sourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourceJar).Hash
$destinationHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $destinationJar).Hash
if ($sourceHash -ne $destinationHash) {
    throw "The deployed JAR hash does not match the release artifact."
}

Write-Host "Deployed NekaraRPG to $destinationJar"
Write-Host "SHA-256: $destinationHash"
Write-Host "Restart the test server; do not use Bukkit /reload for plugin replacement."
