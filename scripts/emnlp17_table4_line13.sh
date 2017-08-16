PKG=edu.illinois.cs.cogcomp.nlp
MAIN=CompareCAVEO.CoDL
mvn exec:java -Dexec.mainClass=$PKG.$MAIN -Dexec.args="0.3 1.4 1 -1 false"