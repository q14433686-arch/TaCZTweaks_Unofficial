@echo off
@rem ===========================================================================
@rem TaCZ Tweaks (Unofficial NeoForge 1.21.11 port) — Windows build helper.
@rem
@rem Requires: JDK 21 on PATH or JAVA_HOME pointing at a JDK 21 installation.
@rem Usage (in cmd.exe / PowerShell):
@rem     build.bat            -> same as `./gradlew build`
@rem     build.bat test       -> run unit tests only
@rem     build.bat idea       -> regenerate IntelliJ IDEA project
@rem ===========================================================================

setlocal

if "%~1"=="" (
    set TASKS=build
) else (
    set TASKS=%*
)

where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    if not defined JAVA_HOME (
        echo [ERROR] Java 21 was not found on PATH and JAVA_HOME is not set. 1>&2
        echo         Install a JDK 21 and either add `java.exe` to PATH or set  1>&2
        echo         the JAVA_HOME environment variable, e.g.:                  1>&2
        echo             set "JAVA_HOME=C:\Program Files\Java\jdk-21"            1>&2
        exit /b 1
    )
)

echo [INFO] Running: gradlew.bat %TASKS%
call "%~dp0gradlew.bat" %TASKS%
set RC=%ERRORLEVEL%
if %RC% neq 0 (
    echo [ERROR] Gradle failed with exit code %RC%. 1>&2
) else (
    echo [OK]    Gradle finished successfully. Artifacts are under build\libs\.
)
exit /b %RC%
