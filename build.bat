
REM clean old jar
cd "D:\MyFiles\workspace\build dir\jar"
del *.jar
cd "D:\MyFiles\workspace\build dir"
rd /s output
REM build fat jar
cd "D:\MyFiles\workspace\IdeaProjects\jfxchess"
call "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2023.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" clean compile assembly:single
REM copy output
copy target\jfxchess-4.3-jar-with-dependencies.jar "D:\MyFiles\workspace\build dir\jar"
cd "D:\MyFiles\workspace\build dir"
jpackage --java-options '-splash:$APPDIR/splash.png' --type app-image -n jfxchess -i jar --main-jar jfxchess-4.3-jar-with-dependencies.jar -d output --icon jfxchess.ico
xcopy engine /s /i "D:\MyFiles\workspace\build dir\output\jfxchess\app\engine"
xcopy book /s /i "D:\MyFiles\workspace\build dir\output\jfxchess\app\book"
copy "D:\MyFiles\workspace\IdeaProjects\jfxchess\src\main\resources\icons\splash.png" "D:\MyFiles\workspace\build dir\output\jfxchess\app"
cd "D:\MyFiles\workspace\IdeaProjects\jfxchess"