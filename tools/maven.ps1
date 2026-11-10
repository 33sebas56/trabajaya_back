$ErrorActionPreference = 'Stop'
$version = '3.9.11'
$root = Split-Path $PSScriptRoot -Parent
$tools = Join-Path $root '.tools'
$maven = Join-Path $tools "apache-maven-$version\bin\mvn.cmd"
try {
  if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Instala Java JDK 21 y configura JAVA_HOME antes de continuar.' }
  if (-not (Test-Path $maven)) {
    New-Item -ItemType Directory -Force -Path $tools | Out-Null
    $zip = Join-Path $tools "apache-maven-$version-bin.zip"
    $base = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version/apache-maven-$version-bin.zip"
    Write-Host 'Descargando Maven. La primera ejecucion requiere Internet.'
    Invoke-WebRequest -UseBasicParsing -Uri $base -OutFile $zip
    $checksum = (Invoke-WebRequest -UseBasicParsing -Uri ($base + '.sha512')).Content
    if ($checksum -is [byte[]]) { $checksum = [System.Text.Encoding]::UTF8.GetString($checksum) }
    $expected = ([string]$checksum).Trim().Split()[0].ToUpperInvariant()
    $actual = (Get-FileHash -Path $zip -Algorithm SHA512).Hash
    if ($actual -ne $expected) { Remove-Item $zip -Force; throw 'La verificacion SHA-512 de Maven fallo. Vuelve a intentar con una red confiable.' }
    Expand-Archive -Path $zip -DestinationPath $tools -Force
    Remove-Item $zip -Force
  }
  Push-Location $root
  try { & $maven @args; $result = $LASTEXITCODE } finally { Pop-Location }
  exit $result
} catch { Write-Error $_; exit 1 }
