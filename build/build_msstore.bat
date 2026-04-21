REM path variables
REM prepare prior:
REM build_dir\
REM  jfxchess.ico
REM  book\extbook.bin
REM  engine\stockfish.exe
REM  engine\stockfish5.exe
REM  jar\ (empty)
REM  output\(empty)
REM source directory should be the parent directory 
REM to this script file
cd ..
set SRC_DIR=%CD%
set BUILD_TOOL_DIR=%SRC_DIR%\build
set SPLASH_ICON_PATH=%SRC_DIR%\src\main\resources\icons\splash.png
set MAVEN_BIN="C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.1\plugins\maven\lib\maven3\bin\mvn.cmd"
set BUILD_DIR=%CD%\build\build_win
set JAR_DIR=%BUILD_DIR%\jar
set OUTPUT_DIR=%BUILD_DIR%\output\jfxchess
set OUTPUT_APP_DIR=%BUILD_DIR%\output\jfxchess\app
set OUTPUT_ENGINE_DIR=%BUILD_DIR%\output\jfxchess\app\engine
set OUTPUT_BOOK_DIR=%BUILD_DIR%\output\jfxchess\app\book
REM clean old jar
cd %JAR_DIR%
del *.jar
cd %BUILD_DIR%
rd /s output
REM build jar
cd %SRC_DIR%\src\main\java\org\asdfjkl\jfxchess\gui
powershell -Command "(Get-Content DialogEngines.java) -replace '//btnAdd.setEnabled\(false\)', 'btnAdd.setEnabled(false)' | Set-Content DialogEngines.java"
cd %SRC_DIR%
call %MAVEN_BIN% clean package
REM copy output
copy target\jfxchess-5.0-jar-with-dependencies.jar %JAR_DIR%
cd %BUILD_DIR%
jpackage --java-options '-splash:$APPDIR/splash.png' --type app-image -n jfxchess -i jar --main-jar jfxchess-5.0-jar-with-dependencies.jar -d output --icon jfxchess.ico
copy jfxchess.ico %OUTPUT_DIR%
xcopy engine /s /i %OUTPUT_ENGINE_DIR%
xcopy book /s /i %OUTPUT_BOOK_DIR%
copy %SPLASH_ICON_PATH% %OUTPUT_APP_DIR%
cd %BUILD_TOOL_DIR%