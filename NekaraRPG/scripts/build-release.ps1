[CmdletBinding()]
param(
    [string]$JavaTrustStore,
    [string]$JavaTrustStorePassword = "changeit"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$propertiesPath = Join-Path $projectRoot "gradle.properties"
$changelogPath = Join-Path $projectRoot "CHANGELOG.md"

$versionLine = Select-String -Path $propertiesPath -Pattern '^plugin_version=(.+)$' | Select-Object -First 1
if ($null -eq $versionLine) {
    throw "gradle.properties does not contain plugin_version."
}
$version = $versionLine.Matches[0].Groups[1].Value.Trim()
$escapedVersion = [Regex]::Escape($version)
if (-not (Select-String -Path $changelogPath -Pattern "^## $escapedVersion$" -Quiet)) {
    throw "CHANGELOG.md does not contain a release heading for $version."
}

$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
Push-Location $projectRoot
try {
    if ($JavaTrustStore) {
        $trustStorePath = (Resolve-Path -LiteralPath $JavaTrustStore).Path
        $trustStoreOptions = "-Djavax.net.ssl.trustStore=`"$trustStorePath`" " +
            "-Djavax.net.ssl.trustStorePassword=$JavaTrustStorePassword " +
            "-Djavax.net.ssl.trustStoreType=PKCS12"
        $env:JAVA_TOOL_OPTIONS = if ($previousJavaToolOptions) {
            "$previousJavaToolOptions $trustStoreOptions"
        }
        else {
            $trustStoreOptions
        }
    }

    & ".\gradlew.bat" clean release --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle release failed with exit code $LASTEXITCODE."
    }
}
finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
    Pop-Location
}

$stableJar = Join-Path $projectRoot "dist\NekaraRPG.jar"
$rootDist = [IO.Path]::GetFullPath((Join-Path $projectRoot "..\..\dist"))
$rootStableJar = Join-Path $rootDist "NekaraRPG.jar"
foreach ($jarPath in @($stableJar, $rootStableJar)) {
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        throw "Expected release artifact was not created: $jarPath"
    }
}
$unexpectedJars = @(
    foreach ($distDirectory in @((Join-Path $projectRoot "dist"), $rootDist)) {
        Get-ChildItem -LiteralPath $distDirectory -Filter "NekaraRPG-*.jar" `
            -File -ErrorAction SilentlyContinue
    }
)
if ($unexpectedJars.Count -gt 0) {
    throw "Versioned release artifacts must not be created: $($unexpectedJars.Name -join ', ')"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($stableJar)
try {
    $pluginEntry = $archive.GetEntry("plugin.yml")
    if ($null -eq $pluginEntry) {
        throw "plugin.yml is missing from $stableJar."
    }
    $reader = [System.IO.StreamReader]::new($pluginEntry.Open())
    try {
        $pluginYaml = $reader.ReadToEnd()
    }
    finally {
        $reader.Dispose()
    }

    $manifestEntry = $archive.GetEntry("META-INF/MANIFEST.MF")
    if ($null -eq $manifestEntry) {
        throw "META-INF/MANIFEST.MF is missing from $stableJar."
    }
    if ($null -eq $archive.GetEntry("org/sqlite/JDBC.class")) {
        throw "Bundled SQLite JDBC driver is missing from $stableJar."
    }
    $manifestReader = [System.IO.StreamReader]::new($manifestEntry.Open())
    try {
        $manifestText = $manifestReader.ReadToEnd()
    }
    finally {
        $manifestReader.Dispose()
    }
}
finally {
    $archive.Dispose()
}

if ($pluginYaml -notmatch "(?m)^version: $escapedVersion$") {
    throw "plugin.yml inside the JAR does not report version $version."
}
if ($manifestText -notmatch "(?m)^Implementation-Title: NekaraRPG\r?$") {
    throw "JAR manifest does not identify NekaraRPG."
}
if ($manifestText -notmatch "(?m)^Implementation-Version: $escapedVersion\r?$") {
    throw "JAR manifest does not report version $version."
}

$hash = Get-FileHash -Algorithm SHA256 -LiteralPath $stableJar
Write-Host "Release NekaraRPG $version completed."
Write-Host "Artifact: $stableJar"
Write-Host "SHA-256:  $($hash.Hash)"
