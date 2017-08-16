PKG=edu.illinois.cs.cogcomp.nlp
MAIN=timeline.CoDL_Exp
mvn exec:java -Dexec.mainClass=$PKG.$MAIN -Dexec.args="0.6 1.4 1 false"