
REM clean old jar
cd "C:\Users\user\MyFiles\workspace\build dir\jar"
del *.jar
cd "C:\Users\user\MyFiles\workspace\build dir"
rd /s output
REM build fat jar
cd "C:\Users\user\MyFiles\workspace\IdeaProjects\jerryfx"
call "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2020.1.1\plugins\maven\lib\maven3\bin\mvn.cmd" clean compile assembly:single
REM copy output
copy target\jerryfx-4.2-jar-with-dependencies.jar "C:\Users\user\MyFiles\workspace\build dir\jar"
cd "C:\Users\user\MyFiles\workspace\build dir"
jpackage --java-options '-splash:$APPDIR/splash.png' --type app-image -n jerryfx -i jar --main-jar jerryfx-4.2-jar-with-dependencies.jar -d output --icon jerry.ico
xcopy engine /s /i "C:\Users\user\MyFiles\workspace\build dir\output\jerryfx\app\engine"
xcopy book /s /i "C:\Users\user\MyFiles\workspace\build dir\output\jerryfx\app\book"
copy "C:\Users\user\MyFiles\workspace\IdeaProjects\jerryfx\src\main\resources\icons\splash.png" "C:\Users\user\MyFiles\workspace\build dir\output\jerryfx\app"
cd "C:\Users\user\MyFiles\workspace\IdeaProjects\jerryfx"