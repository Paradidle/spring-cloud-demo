@echo off
cd /d D:\gitproject\spring-cloud-demo\crawler-api
call mvn exec:java "-Dexec.mainClass=com.paradidle.crawler.api.PostDetailParseTest" "-Dexec.classpathScope=test"
pause
