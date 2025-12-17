@echo off
REM Build and run PB-Tree project on Windows

set CLASSPATH=bin;lib\gson-2.10.1.jar

echo === Compiling PB-Tree Project ===
if not exist bin mkdir bin
if not exist lib mkdir lib

REM Check for gson library
if not exist lib\gson-2.10.1.jar (
    echo Downloading Gson library...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar' -OutFile 'lib\gson-2.10.1.jar'"
)

echo Compiling Java files...
javac -cp "%CLASSPATH%" -d bin src\main\java\pbtree\model\*.java src\main\java\pbtree\algorithm\*.java src\main\java\pbtree\parser\*.java src\main\java\pbtree\visualization\*.java src\main\java\pbtree\*.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    exit /b 1
)

echo === Running PB-Tree ===
java -cp "%CLASSPATH%" pbtree.Main %*
