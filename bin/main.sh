NUSUTH_HOME=..
PATH=/home/syntone/jdk1.3.1_02/bin:$PATH
JDK=/home/syntone/jdk1.3.1_02/jre/lib/rt.jar
XERCES=../lib/xerces.jar
JSDK=../lib/servlet.jar
JNDI=../lib/jndi.jar
NUSUTH=../lib/nusuth_c.jar
LOG=../lib/log4j.jar
TOOLS=/home/syntone/jdk1.3.1_02/lib/tools.jar
JSSE=../lib/jsse.jar:../lib/jcert.jar:../lib/jnet.jar
TYREX=../lib/tyrex.jar:../lib/tyrex_env.jar:../lib/lightOTS.jar:../lib/jta1.0.1.jar
JH=../lib/jh.jar:../help
JDBC=../lib/jdbc2_0-stdext.jar
XT=../lib/xt.jar

CLASSPATH=$JDBC:$JH:$TYREX:$JDK:$XERCES:$JSDK:$JNDI:$NUSUTH:$TOOLS:$LOG:$JSSE:$XT

if [ "$1" = "start" ] ; then
java -classpath $CLASSPATH -Dnusuth.home=$NUSUTH_HOME com.azoft.nusuth.container.NusuthContainerStarter 1>../logs/out.log 2>../logs/err.log &
fi

if [ "$1" = "stop" ] ; then
java -classpath $CLASSPATH -nusuth.home=$NUSUTH_HOME com.azoft.nusuth.container.NusuthContainerStopper
fi
