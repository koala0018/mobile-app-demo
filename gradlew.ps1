$ErrorActionPreference = 'Stop'

$AppHome = Split-Path -Parent $MyInvocation.MyCommand.Path
$PropsFile = Join-Path $AppHome 'gradle/wrapper/gradle-wrapper.properties'

if (-not (Test-Path $PropsFile)) {
    throw "Missing $PropsFile"
}

$DistUrl = (Get-Content $PropsFile | Where-Object { $_ -like 'distributionUrl=*' } | Select-Object -First 1).Split('=', 2)[1]
if (-not $DistUrl) {
    throw "Unable to read distributionUrl from $PropsFile"
}

$DistUrl = $DistUrl -replace '\\', ''

if ($DistUrl -notmatch 'gradle-([0-9.]+)-bin\.zip') {
    throw "Unable to parse Gradle version from $DistUrl"
}

$Version = $Matches[1]
$CacheDir = Join-Path $AppHome ".gradle-wrapper/gradle-$Version"
$ZipFile = Join-Path $CacheDir "gradle-$Version-bin.zip"
$UnpackDir = Join-Path $CacheDir 'unpacked'
$GradleBat = Join-Path $UnpackDir "gradle-$Version/bin/gradle.bat"

if (-not (Test-Path $GradleBat)) {
    New-Item -ItemType Directory -Force -Path $CacheDir | Out-Null

    if (-not (Test-Path $ZipFile)) {
        Invoke-WebRequest -Uri $DistUrl -OutFile $ZipFile
    }

    if (Test-Path $UnpackDir) {
        Remove-Item -Path $UnpackDir -Recurse -Force
    }

    New-Item -ItemType Directory -Force -Path $UnpackDir | Out-Null
    Expand-Archive -Path $ZipFile -DestinationPath $UnpackDir
}

& $GradleBat @args
exit $LASTEXITCODE
