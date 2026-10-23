param([Parameter(Mandatory=$true)][string]$JsoupJar)
$ErrorActionPreference='Stop'
$project=Split-Path -Parent $PSScriptRoot
$testOutput=Join-Path $project 'build/parser-tests'
New-Item -ItemType Directory -Path $testOutput -Force | Out-Null
$javac=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/javac.exe'}else{'javac'}
$java=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}else{'java'}
$source=Join-Path $project 'app/src/main/java/cn/edu/njfu/timetable'
& $javac -encoding UTF-8 -cp $JsoupJar -d $testOutput (Join-Path $source 'Course.java') (Join-Path $source 'Weeks.java') (Join-Path $source 'NjfuParser.java') (Join-Path $project 'tests/ParserTests.java')
if($LASTEXITCODE -ne 0){throw 'Parser compilation failed'}
& $java '-Dfile.encoding=UTF-8' -cp "$JsoupJar;$testOutput" cn.edu.njfu.timetable.ParserTests (Join-Path $project 'tests/fixtures/njfu-response.html')
if($LASTEXITCODE -ne 0){throw 'Parser tests failed'}
