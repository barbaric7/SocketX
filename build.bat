@echo off
REM =============================================================
REM  ChatApp Build Script (Windows)
REM  Usage:
REM    build.bat           -- compile and package
REM    build.bat server    -- run the server
REM    build.bat client    -- run the client
REM =============================================================

SET LIB=lib\sqlite-jdbc-3.36.0.3.jar
SET SRC=src
SET OUT=classes
SET JAR_SERVER=ChatServer.jar
SET JAR_CLIENT=ChatClient.jar

IF "%1"=="server" GOTO RUN_SERVER
IF "%1"=="client" GOTO RUN_CLIENT

REM ── Check for SQLite JDBC ────────────────────────────────────
IF NOT EXIST "%LIB%" (
    echo [Build] sqlite-jdbc jar not found.
    echo [Build] Please download it from:
    echo         https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar
    echo [Build] and place it in the lib\ folder.
    pause
    EXIT /B 1
)

REM ── Compile ───────────────────────────────────────────────────
echo [Build] Compiling sources...
IF EXIST "%OUT%" RMDIR /S /Q "%OUT%"
MKDIR "%OUT%"

REM Using wildcards to compile all java files in these directories
javac -cp "%LIB%" -d "%OUT%" ^
  %SRC%\common\*.java ^
  %SRC%\database\*.java ^
  %SRC%\client\*.java ^
  %SRC%\server\*.java ^
  %SRC%\gui\*.java

IF ERRORLEVEL 1 (
    echo [Build] Compilation FAILED.
    pause
    EXIT /B 1
)
echo [Build] Compilation successful.

REM ── Extract sqlite into classes ──────────────────────────────
cd "%OUT%"
jar xf ..\%LIB%
cd ..

REM ── Package Server JAR ───────────────────────────────────────
echo Main-Class: server.ChatServer > manifest_server.txt
jar cfm %JAR_SERVER% manifest_server.txt -C %OUT% .
DEL manifest_server.txt
echo [Build] %JAR_SERVER% ready.

REM ── Package Client JAR ───────────────────────────────────────
echo Main-Class: gui.Main > manifest_client.txt
jar cfm %JAR_CLIENT% manifest_client.txt -C %OUT% .
DEL manifest_client.txt
echo [Build] %JAR_CLIENT% ready.
echo.
echo [Build] Done! You can now run:
echo         build.bat server
echo         build.bat client
pause
EXIT /B 0

REM ── Run Server ───────────────────────────────────────────────
:RUN_SERVER
IF NOT EXIST "%JAR_SERVER%" (
    echo [Run] %JAR_SERVER% not found. Run build.bat first.
    pause
    EXIT /B 1
)
java -cp "classes;%LIB%" server.ChatServer
EXIT /B 0

REM ── Run Client ───────────────────────────────────────────────
:RUN_CLIENT
IF NOT EXIST "%JAR_CLIENT%" (
    echo [Run] %JAR_CLIENT% not found. Run build.bat first.
    pause
    EXIT /B 1
)
java -cp "classes;%LIB%" gui.Main
EXIT /B 0