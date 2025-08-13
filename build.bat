
REM clean old jar
cd "C:\MyFiles\workspace\build dir\jar"
del *.jar
cd "C:\MyFiles\workspace\build dir"
rd /s output
REM build fat jar
cd "C:\MyFiles\workspace\IdeaProjects\jfxchess"
call "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.3\plugins\maven\lib\maven3\bin\mvn.cmd" clean compile assembly:single
REM copy output
copy target\jfxchess-4.5-jar-with-dependencies.jar "C:\MyFiles\workspace\build dir\jar"
cd "C:\MyFiles\workspace\build dir"
jpackage --java-options '-splash:$APPDIR/splash.png' --type app-image -n jfxchess -i jar --main-jar jfxchess-4.5-jar-with-dependencies.jar -d output --icon jfxchess.ico
copy jfxchess.ico "C:\MyFiles\workspace\build dir\output\jfxchess"
xcopy engine /s /i "C:\MyFiles\workspace\build dir\output\jfxchess\app\engine"
xcopy book /s /i "C:\MyFiles\workspace\build dir\output\jfxchess\app\book"
copy "C:\MyFiles\workspace\IdeaProjects\jfxchess\src\main\resources\icons\splash.png" "C:\MyFiles\workspace\build dir\output\jfxchess\app"
cd "C:\MyFiles\workspace\IdeaProjects\jfxchess"