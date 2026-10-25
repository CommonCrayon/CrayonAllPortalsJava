@echo off
setlocal

rem ============================================================
rem  Builds CrayonAllPortalsJava with Gradle.
rem    compile.bat          build and run the tests
rem    compile.bat notest   build without running the tests
rem  The runnable jar ends up in build\libs.
rem ============================================================

cd /d "%~dp0"
set "RESULT=0"
set "VERSION_FILE=%TEMP%\crayon-java-version.txt"

rem Gradle 8.14 (see gradle\wrapper\gradle-wrapper.properties) can only run on Java 17 to 24.
rem Raise MAX_JAVA if you upgrade Gradle.
set "MIN_JAVA=17"
set "MAX_JAVA=24"

set "GRADLE_TASKS=build"
if /i "%~1"=="notest" set "GRADLE_TASKS=build -x test"

rem ---- Find a Java that Gradle can run on ------------------------

rem 1. JAVA_HOME
if not defined JAVA_HOME goto tryPath
set "JAVA_HOME=%JAVA_HOME:"=%"
call :isUsable "%JAVA_HOME%\bin\java.exe" && goto javaFound
echo JAVA_HOME isn't Java %MIN_JAVA%-%MAX_JAVA%, looking for another JDK...

rem 2. java on the PATH
:tryPath
call :isUsable java && goto usePath
if defined JMAJOR echo Skipping Java %JMAJOR% on PATH - this Gradle version needs Java %MIN_JAVA%-%MAX_JAVA%.
goto tryInstalled

:usePath
set "JAVA_HOME="
goto javaFound

rem 3. The newest suitable JDK downloaded by IntelliJ or installed in the usual places
:tryInstalled
set "FOUND="
set "FOUND_MAJOR=0"
for /d %%D in ("%USERPROFILE%\.jdks\*" "%ProgramFiles%\Java\*" "%ProgramFiles%\Eclipse Adoptium\*" "%ProgramFiles%\Microsoft\jdk-*" "%ProgramFiles%\Zulu\*" "%ProgramFiles%\Amazon Corretto\*") do call :consider "%%~fD"
if not defined FOUND goto noJava
set "JAVA_HOME=%FOUND%"
set "JMAJOR=%FOUND_MAJOR%"

:javaFound
if defined JAVA_HOME echo Using Java %JMAJOR%: %JAVA_HOME%
if not defined JAVA_HOME echo Using Java %JMAJOR% from PATH

rem ---- Build ----------------------------------------------------
echo.
call gradlew.bat %GRADLE_TASKS%
set "RESULT=%ERRORLEVEL%"
if not "%RESULT%"=="0" goto failed

rem The newest jar in build\libs is the one just built
set "JAR="
for /f "delims=" %%F in ('dir /b /o-d "build\libs\*.jar" 2^>nul') do if not defined JAR set "JAR=%%F"
echo.
echo Done! Jar: "%CD%\build\libs\%JAR%"
goto end

:failed
echo.
echo BUILD FAILED - scroll up to see the error.
goto end

:noJava
echo.
echo ERROR: Couldn't find Java %MIN_JAVA%-%MAX_JAVA%.
echo Install a JDK 21 (e.g. from https://adoptium.net) or set JAVA_HOME to one.
set "RESULT=1"
goto end

:end
if exist "%VERSION_FILE%" del "%VERSION_FILE%"
rem Keep the window open when the script was double-clicked
echo %cmdcmdline% | "%SystemRoot%\System32\find.exe" /i "/c" >nul && pause
endlocal & exit /b %RESULT%


rem ---- Remembers the JDK folder in FOUND if it's usable and newer than the best so far.
:consider
call :isUsable "%~1\bin\java.exe" || exit /b 0
if %JMAJOR% LEQ %FOUND_MAJOR% exit /b 0
set "FOUND=%~1"
set "FOUND_MAJOR=%JMAJOR%"
exit /b 0

rem ---- Succeeds if the given java is between MIN_JAVA and MAX_JAVA.
:isUsable
call :javaVersion "%~1" || exit /b 1
if %JMAJOR% LSS %MIN_JAVA% exit /b 1
if %JMAJOR% GTR %MAX_JAVA% exit /b 1
exit /b 0

rem ---- Sets JMAJOR to the major version of the given java. Fails if it can't run it.
:javaVersion
set "JMAJOR="
set "JV="
"%~1" -version 2> "%VERSION_FILE%" >nul || exit /b 1
rem e.g.  openjdk version "21.0.5" 2024-10-15   or   java version "1.8.0_401"
for /f "usebackq tokens=1-3" %%A in ("%VERSION_FILE%") do (
    if /i "%%B"=="version" if not defined JV set "JV=%%~C"
)
if not defined JV exit /b 1
for /f "delims=.-_+ " %%M in ("%JV%") do set "JMAJOR=%%M"
if not defined JMAJOR exit /b 1
exit /b 0
