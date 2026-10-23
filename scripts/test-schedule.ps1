$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$testOutput = Join-Path $project 'build/schedule-tests'
New-Item -ItemType Directory -Path $testOutput -Force | Out-Null
$javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { 'javac' }
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$source = Join-Path $project 'app/src/main/java/cn/edu/njfu/timetable'
$files = @('Course.java','Weeks.java','CalendarEngine.java','PeriodTimes.java','CalendarDays.java','CalendarLayout.java') | ForEach-Object { Join-Path $source $_ }
& $javac -encoding UTF-8 -d $testOutput $files (Join-Path $project 'tests/ScheduleTests.java')
if ($LASTEXITCODE -ne 0) { throw 'Schedule tests compilation failed' }
& $java -cp $testOutput cn.edu.njfu.timetable.ScheduleTests
if ($LASTEXITCODE -ne 0) { throw 'Schedule tests failed' }
