set JDK=d:\java\jdk1.3
set NUSUTH_HOME=..

set SERVLET_LIB=%NUSUTH_HOME%\lib\servlet.jar
set XERCES_LIB=%NUSUTH_HOME%\lib\xerces.jar
set NUSUTH_LIB=%NUSUTH_HOME%\lib\nusuth_c.jar
set JNDI_LIB=%NUSUTH_HOME%\lib\jndi.jar
set LOG_LIB=%NUSUTH_HOME%\lib\log4j.jar
set TOOLS_LIB=%JDK%\lib\tools.jar
set JSSE_LIB=%NUSUTH_HOME%\lib\jsse.jar;%NUSUTH_HOME%\lib\jcert.jar;%NUSUTH_HOME%\lib\jnet.jar
set TYREX_LIB=%NUSUTH_HOME%\lib\tyrex.jar;%NUSUTH_HOME%\lib\tyrex_env.jar;%NUSUTH_HOME%\lib\jdbc2_0-stdext.jar;%NUSUTH_HOME%\lib\jta1.0.1.jar;%NUSUTH_HOME%\lib\lightOTS.jar
set XT_LIB=%NUSUTH_HOME%\lib\xt.jar

set CLASSPATH=%CLASSPATH%;%TYREX_LIB%;%SERVLET_LIB%;%XERCES_LIB%;%NUSUTH_LIB%;%JNDI_LIB%;%LOG_LIB%;%TOOLS_LIB%;%JSSE_LIB%;%XT_LIB%

if "%1"=="start" goto start

if "%1"=="stop" goto stop

:start
start "Nusuth 1.0b" %JDK%\bin\java -Xmx128m -Xms8m -classpath %CLASSPATH% -Dnusuth.home=%NUSUTH_HOME% com.azoft.nusuth.container.NusuthContainerStarter
goto end

:stop
%JDK%\bin\java -classpath %CLASSPATH% -Dnusuth.home=%NUSUTH_HOME% com.azoft.nusuth.container.NusuthContainerStopper
goto end

:end
