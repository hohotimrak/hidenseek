param([string[]]$GradleArgs = @('build'))
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$buildRoot = $projectRoot
# JDK 21's Windows argument-file decoding can break non-ASCII classpath entries.
# A junction preserves the exact same checkout and avoids copying source files.
if ($projectRoot -match '[^\x00-\x7F]') {
    $hash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($projectRoot))).Substring(0,12)
    $links = Join-Path $env:LOCALAPPDATA 'StillHuntBuildLinks'
    New-Item -ItemType Directory -Force -Path $links | Out-Null
    $buildRoot = Join-Path $links $hash
    if (Test-Path -LiteralPath $buildRoot) {
        $existing = Get-Item -LiteralPath $buildRoot
        if ($existing.LinkType -ne 'Junction' -or $existing.Target -ne $projectRoot) { throw "Build junction already points somewhere else: $buildRoot" }
    } else { New-Item -ItemType Junction -Path $buildRoot -Target $projectRoot | Out-Null }
}
Push-Location -LiteralPath $buildRoot
try { & .\gradlew.bat @GradleArgs; if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" } }
finally { Pop-Location }
