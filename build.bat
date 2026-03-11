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

javac -cp "%LIB%" -d "%OUT%" ^
  %SRC%\common\Message.java ^
  %SRC%\database\DatabaseManager.java ^
  %SRC%\client\ChatClient.java ^
  %SRC%\server\ClientHandler.java ^
  %SRC%\server\ChatServer.java ^
  %SRC%\gui\Main.java ^
  %SRC%\gui\LoginWindow.java ^
  %SRC%\gui\ChatWindow.java

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
echo Build complete!
echo   Run server:  java -jar ChatServer.jar
echo   Run client:  java -jar ChatClient.jar
echo.
GOTO END

:RUN_SERVER
echo [Run] Starting server...
java -jar %JAR_SERVER%
GOTO END

:RUN_CLIENT
echo [Run] Starting client...
java -jar %JAR_CLIENT%
GOTO END

:END
