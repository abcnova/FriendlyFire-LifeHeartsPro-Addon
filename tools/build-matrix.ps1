param(
    [string]$MavenCommand = "mvn"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$apiVersions = @(
    @{ Minecraft = "1.21"; Api = "1.21-R0.1-SNAPSHOT" },
    @{ Minecraft = "1.21.1"; Api = "1.21.1-R0.1-SNAPSHOT" },
    @{ Minecraft = "1.21.3"; Api = "1.21.3-R0.1-SNAPSHOT" },
    @{ Minecraft = "1.21.4"; Api = "1.21.4-R0.1-SNAPSHOT" },
    @{ Minecraft = "1.21.5"; Api = "1.21.5-R0.1-SNAPSHOT" },
    @{ Minecraft = "1.21.6"; Api = "1.21.6-R0.1-SNAPSHOT" },
    @{ Minecraft = "1.21.7"; Api = "1.21.7-R0.1-SNAPSHOT" },
    @{ Minecraft = "1.21.8"; Api = "1.21.8-R0.1-SNAPSHOT" },
    @{ Minecraft = "1.21.9"; Api = "1.21.9-R0.1-SNAPSHOT" },
    @{ Minecraft = "1.21.10"; Api = "1.21.10-R0.1-SNAPSHOT" },
    @{ Minecraft = "1.21.11"; Api = "1.21.11-R0.1-SNAPSHOT" },
    @{ Minecraft = "26.1–26.1.2 (latest stable API)"; Api = "26.1.2.build.74-stable" },
    @{ Minecraft = "26.2 (latest stable API)"; Api = "26.2.build.134-stable" },
    @{ Minecraft = "26.3 (latest stable API)"; Api = "26.3.build.207-stable" }
)

Push-Location $projectRoot
try {
    foreach ($entry in $apiVersions) {
        Write-Host "Compile-checking Paper API for Minecraft $($entry.Minecraft)..."
        & $MavenCommand -B "-Dapi.group=io.papermc.paper" "-Dapi.artifact=paper-api" "-Dapi.version=$($entry.Api)" clean verify
        if ($LASTEXITCODE -ne 0) { throw "Paper API build failed for Minecraft $($entry.Minecraft)." }
    }

    foreach ($entry in @(
        @{ Minecraft = "1.21.2"; Api = "1.21.2-R0.1-SNAPSHOT" },
        @{ Minecraft = "26.1"; Api = "26.1-R0.1-SNAPSHOT" },
        @{ Minecraft = "26.1.1"; Api = "26.1.1-R0.1-SNAPSHOT" },
        @{ Minecraft = "26.1.2"; Api = "26.1.2-R0.1-SNAPSHOT" },
        @{ Minecraft = "26.2"; Api = "26.2-R0.1-SNAPSHOT" },
        @{ Minecraft = "26.3"; Api = "26.3-R0.1-SNAPSHOT" }
    )) {
        Write-Host "Compile-checking Spigot API for Minecraft $($entry.Minecraft)..."
        & $MavenCommand -B "-Dapi.group=org.spigotmc" "-Dapi.artifact=spigot-api" "-Dapi.version=$($entry.Api)" clean verify
        if ($LASTEXITCODE -ne 0) { throw "Spigot API build failed for Minecraft $($entry.Minecraft)." }
    }

    Write-Host "Building the release against the Spigot 1.21 baseline API..."
    & $MavenCommand -B "-Dapi.group=org.spigotmc" "-Dapi.artifact=spigot-api" "-Dapi.version=1.21-R0.1-SNAPSHOT" clean verify
    if ($LASTEXITCODE -ne 0) { throw "Release build failed against the 1.21 API." }

    $releaseDir = Join-Path $projectRoot "release"
    New-Item -ItemType Directory -Force -Path $releaseDir | Out-Null
    Copy-Item (Join-Path $projectRoot "target\friendlyfire-lhp-addon-1.1.3.jar") $releaseDir -Force
    Write-Host "Built release: $(Join-Path $releaseDir 'friendlyfire-lhp-addon-1.1.3.jar')"
} finally {
    Pop-Location
}
