rd /s /q "logs"
rd /s /q "world"
xcopy map world /D /E /C /R /H /I /K /Y
"C:\Program Files\Java\jdk1.8.0_251\bin\java.exe" -jar spigot-1.16.2.jar
