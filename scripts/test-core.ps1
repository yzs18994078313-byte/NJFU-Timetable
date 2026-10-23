$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$testOutput = Join-Path $project 'build/core-tests'
New-Item -ItemType Directory -Path $testOutput -Force | Out-Null
$javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { 'javac' }
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$source = Join-Path $project 'app/src/main/java/cn/edu/njfu/timetable'
& $javac -encoding UTF-8 -d $testOutput (Join-Path $source 'Course.java') (Join-Path $source 'Weeks.java') (Join-Path $source 'CalendarEngine.java') (Join-Path $project 'tests/CoreTests.java')
if ($LASTEXITCODE -ne 0) { throw 'Core compilation failed' }
& $java -cp $testOutput cn.edu.njfu.timetable.CoreTests
if ($LASTEXITCODE -ne 0) { throw 'Core tests failed' }
