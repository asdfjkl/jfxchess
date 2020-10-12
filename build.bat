
REM clean old jar
cd "C:\Users\user\MyFiles\workspace\build_dir"
del *.jar
REM build fat jar
cd "C:\Users\user\MyFiles\workspace\IdeaProjects\jerryfx"
call "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2020.1.1\plugins\maven\lib\maven3\bin\mvn.cmd" clean compile assembly:single
REM copy output
copy target\jerryfx-4.0-jar-with-dependencies.jar "C:\Users\user\MyFiles\workspace\build_dir"