param(
    [Parameter(Mandatory)][string]$ModsDirectory,
    [Parameter(Mandatory)][string]$GravityJar
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$libsPath = Join-Path $projectRoot 'libs'
New-Item -ItemType Directory -Force -Path $libsPath | Out-Null
$required = @{
    'alexsmobs-2.1.9-fabric+26.2.jar' = 'alexsmobs.jar'
    'codxlib-1.5.1-fabric+26.2.jar' = 'codxlib-1.5.1-fabric+26.2.jar'
    'cloth-config-26.2.155.jar' = 'cloth-config-26.2.155.jar'
}
$optional = @{
    'scalebrews-0.1.0-beta.4.jar' = 'scalebrews-0.1.0-beta.4.jar'
}
$alexHash = 'ac5128a25af23b0894044f35e7a906ec4790b152834e0d1710e25d79ac43e51d56c4b2bc31234c51e83e84f36b5a0a7175d934f0213470ef1d0369c09abc565b'
$gravityHash = '1bf67c47f09ab516165f7f9d35b0211272e81f6c09a9f09599c2c9c349721245279e5e876f9809078b51d9f82b8b261683314cd9d7688b24ca798c0eaf04b1f8'
foreach ($name in $required.Keys) {
    $sourcePath = Join-Path $ModsDirectory $name
    if (!(Test-Path -LiteralPath $sourcePath -PathType Leaf)) { throw "Missing dependency: $name" }
}
if ((Get-FileHash -LiteralPath $GravityJar -Algorithm SHA512).Hash -ne $gravityHash) { throw 'Gravity Changer does not match the audited version.' }
$alexPath = Join-Path $ModsDirectory 'alexsmobs-2.1.9-fabric+26.2.jar'
if ((Get-FileHash -LiteralPath $alexPath -Algorithm SHA512).Hash -ne $alexHash) { throw "Alex's Mobs does not match the audited version." }
foreach ($name in $required.Keys) {
    Copy-Item -LiteralPath (Join-Path $ModsDirectory $name) -Destination (Join-Path $libsPath $required[$name])
}
foreach ($name in $optional.Keys) {
    $sourcePath = Join-Path $ModsDirectory $name
    if (Test-Path -LiteralPath $sourcePath -PathType Leaf) {
        Copy-Item -LiteralPath $sourcePath -Destination (Join-Path $libsPath $optional[$name])
    }
}
Copy-Item -LiteralPath $GravityJar -Destination (Join-Path $libsPath 'gravity.jar')
Write-Output 'Audited required dependencies prepared. Optional Scale Brews was copied only when present. Nothing was installed into the modpack.'
