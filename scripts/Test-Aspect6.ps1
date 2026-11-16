param(
    [string]$JdkBin = 'C:\Program Files\Java\jdk-25.0.4\bin',
    [string]$JavaFxLib = (Join-Path $env:USERPROFILE 'javafx-sdk-25.0.4\lib'),
    [string]$H2Jar = (Join-Path $env:USERPROFILE 'h2\bin\h2-2.4.240.jar'),
    [string]$JUnitJar = '',
    [ValidateSet('Aspect6', 'Regression', 'All')][string]$Scope = 'Regression',
    [switch]$RunDemo
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$buildRoot = Join-Path $projectRoot 'build\aspect6'
if (!$JUnitJar) {
    $JUnitJar = Join-Path $buildRoot 'tools\junit-platform-console-standalone-1.14.4.jar'
    if (!(Test-Path -LiteralPath $JUnitJar)) {
        New-Item -ItemType Directory -Path (Split-Path -Parent $JUnitJar) -Force | Out-Null
        Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.14.4/junit-platform-console-standalone-1.14.4.jar' -OutFile $JUnitJar
    }
}
foreach ($required in @((Join-Path $JdkBin 'javac.exe'), (Join-Path $JdkBin 'java.exe'), $JavaFxLib, $H2Jar, $JUnitJar)) {
    if (!(Test-Path -LiteralPath $required)) { throw "Missing dependency: $required. Supply its path using the script parameters." }
}

$runRoot = Join-Path $buildRoot ('run-' + [guid]::NewGuid().ToString('N'))
$classes = Join-Path $runRoot 'classes'
$testHome = Join-Path $runRoot 'test-home'
$reports = Join-Path $runRoot 'reports'
New-Item -ItemType Directory -Path $classes,$testHome,$reports -Force | Out-Null
$sourceList = Join-Path $runRoot 'sources.txt'
# Use the class path for a portable console run; Eclipse still uses module-info.java.
$sources = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Recurse -Filter '*.java' |
    Where-Object Name -ne 'module-info.java' | ForEach-Object { '"' + $_.FullName.Replace('\','/') + '"' }
$sources | Set-Content -LiteralPath $sourceList -Encoding utf8
& (Join-Path $JdkBin 'javac.exe') -g -encoding UTF-8 -cp "$JavaFxLib/*;$JUnitJar;$H2Jar" -d $classes "@$sourceList" *> (Join-Path $runRoot 'compile.txt')
if ($LASTEXITCODE -ne 0) { Get-Content -LiteralPath (Join-Path $runRoot 'compile.txt'); throw 'Compilation failed.' }
$classPath = "$JUnitJar;$classes;$H2Jar;$JavaFxLib/*"
$selection = switch ($Scope) {
    'Aspect6' { @('--select-package','curatorRefinementTests') }
    'Regression' { @('--select-package','curatorRefinementTests','--select-package','curatorHomeTests',
        '--select-package','curatorLockTests','--select-class','lessonTests.SharedAiServiceTests') }
    'All' { @('--scan-class-path',$classes) }
}
& (Join-Path $JdkBin 'java.exe') "-Duser.home=$testHome" -cp $classPath org.junit.platform.console.ConsoleLauncher execute @selection --disable-banner --details summary --reports-dir $reports *> (Join-Path $runRoot 'junit.txt')
$testExit = $LASTEXITCODE
Get-Content -LiteralPath (Join-Path $runRoot 'junit.txt') -Tail 25
Write-Output "Evidence: $runRoot"
if ($testExit -ne 0) { throw 'JUnit reported failures. Read junit.txt; docs/aspect6-prototype.md lists the existing main-branch failures.' }

# These 66 existing Contributor tests predate JUnit; keep their regression check too.
& (Join-Path $JdkBin 'java.exe') "-Duser.home=$testHome" -cp $classPath lessonTests.AllContributorTests *> (Join-Path $runRoot 'contributor.txt')
$legacyExit = $LASTEXITCODE
$legacyText = Get-Content -LiteralPath (Join-Path $runRoot 'contributor.txt') -Raw
Get-Content -LiteralPath (Join-Path $runRoot 'contributor.txt') -Tail 5
if ($legacyExit -ne 0 -or $legacyText -notmatch 'Number of tests passed: 66\b' -or $legacyText -notmatch 'Number of tests failed: 0\b') {
    throw 'Contributor regression failed. See contributor.txt.'
}
if ($RunDemo) {
    $demoHome = Join-Path $runRoot 'demo-home'
    New-Item -ItemType Directory -Path $demoHome -Force | Out-Null
    & (Join-Path $JdkBin 'java.exe') "-Duser.home=$demoHome" -cp $classPath guiCuratorRefine.CuratorRefinementDemo *> (Join-Path $runRoot 'demo.txt')
    $demoExit = $LASTEXITCODE
    Get-Content -LiteralPath (Join-Path $runRoot 'demo.txt')
    if ($demoExit -ne 0) { throw 'Prototype demo failed.' }
}
Write-Output 'Aspect 6 verification passed.'
