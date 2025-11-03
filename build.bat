REM clean old jar
cd "C:\MyFiles\workspace\build dir\jar"
del *.jar
cd "C:\MyFiles\workspace\build dir"
rd /s output
cd "C:\MyFiles\workspace\build dir\javafx-mods"
del *.jar
REM build jar
cd "C:\MyFiles\workspace\IdeaProjects\jfxchess"
call "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.1\plugins\maven\lib\maven3\bin\mvn.cmd" clean package
REM copy output
copy target\jfxchess-4.6-jar-with-dependencies.jar "C:\MyFiles\workspace\build dir\jar"
copy target\javafx-mods\*.* "C:\MyFiles\workspace\build dir\javafx-mods"
cd "C:\MyFiles\workspace\build dir"
jpackage --module-path javafx-mods  --add-modules javafx.controls,javafx.web,javafx.swing --java-options '-splash:$APPDIR/splash.png' --type app-image -n jfxchess -i jar --main-jar jfxchess-4.6-jar-with-dependencies.jar -d output --icon jfxchess.ico
copy jfxchess.ico "C:\MyFiles\workspace\build dir\output\jfxchess"
xcopy engine /s /i "C:\MyFiles\workspace\build dir\output\jfxchess\app\engine"
xcopy book /s /i "C:\MyFiles\workspace\build dir\output\jfxchess\app\book"
copy "C:\MyFiles\workspace\IdeaProjects\jfxchess\src\main\resources\icons\splash.png" "C:\MyFiles\workspace\build dir\output\jfxchess\app"
cd "C:\MyFiles\workspace\IdeaProjects\jfxchess"
