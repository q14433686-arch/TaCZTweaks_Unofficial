@echo off
@rem ===========================================================================
@rem TaCZ Tweaks (Unofficial NeoForge 1.21.11 port) — Windows build helper.
@rem
@rem Requires: JDK 21 on PATH or JAVA_HOME pointing at a JDK 21 installation.
@rem Usage (in cmd.exe / PowerShell):
@rem     build.bat            -> same as `gradlew.bat build`
@rem     build.bat test       -> run unit tests only
@rem     build.bat idea       -> regenerate IntelliJ IDEA project
@rem     build.bat clean build --no-daemon
@rem
@rem The Gradle daemon/Kotlin daemon heap/CodeCache/Metaspace defaults live in
@rem gradle.properties (org.gradle.jvmargs). The values there are tuned for a
@rem normal dev machine (Xmx2g / ReservedCodeCacheSize=256m / Metaspace=512m),
@rem which is required to compile Kotlin 2.4.10 (K2) without hitting
@rem "Out of space in CodeCache for adapters". If you really need to build on
@rem a <2GB machine, set JAVA_TOOL_OPTIONS / GRADLE_OPTS before running, e.g.:
@rem
@rem     set JAVA_TOOL_OPTIONS=-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1
@rem     build.bat --no-daemon
@rem ===========================================================================

setlocal EnableExtensions

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

echo [INFO] JAVA_HOME=%JAVA_HOME%
where java >nul 2>&1 && java -version
echo [INFO] Running: gradlew.bat %TASKS%
echo.

call "%~dp0gradlew.bat" %TASKS%
set RC=%ERRORLEVEL%

echo.
if %RC% neq 0 (
    echo [ERROR] Gradle failed with exit code %RC%. 1>&2
    echo         If you saw "Out of space in CodeCache for adapters" or      1>&2
    echo         "MethodHandle.linkToStatic ... InternalError" that means    1>&2
    echo         the JVM did not have enough CodeCache/Metaspace/heap to     1>&2
    echo         compile Kotlin 2.4.10 (K2). Either:                         1>&2
    echo           - close other Java apps / IDEs and retry, or              1>&2
    echo           - raise org.gradle.jvmargs in gradle.properties, or       1>&2
    echo           - rerun on a 64-bit JDK 21 with at least 2GB free RAM.    1>&2
    exit /b %RC%
)

echo [OK]    Gradle finished successfully. Artifacts are under build\libs\.
exit /b 0
