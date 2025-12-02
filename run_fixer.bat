@echo off
setlocal

if "%~1"=="" (
    echo Usage: run_fixer.bat ^<path_to_mcpack^> [threshold]
    echo Example: run_fixer.bat "Actions and Stuff 1.8.mcpack" 60
    exit /b 1
)

set JAR_PATH=build\libs\BetterPack-1.0.0.jar
set CLASS_NAME=com.kremecn.geyser.extension.betterpack.util.PathFixer

if not exist "%JAR_PATH%" (
    echo Error: BetterPack jar not found at %JAR_PATH%
    echo Please run 'gradlew shadowJar' first.
    exit /b 1
)

echo Running PathFixer on "%~1"...
java -cp "%JAR_PATH%" %CLASS_NAME% "%~1" %2

echo.
echo Done.
pause
