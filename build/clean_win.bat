REM clean build remainings
cd ..
set SRC_DIR=%CD%
set BUILD_TOOL_DIR=%SRC_DIR%\build
set BUILD_DIR=%CD%\build\build_win
set JAR_DIR=%BUILD_DIR%\jar
REM clean old jar
cd %JAR_DIR%
del *.jar
cd %BUILD_DIR%
rd /s output
cd %BUILD_TOOL_DIR%