#!/bin/bash
mvn install:install-file -Dfile=$GUROBI_HOME/lib/gurobi.jar -DgroupId=Gurobi -DartifactId=gurobi -Dversion=6.5.2 -Dpackaging=jar
mvn install:install-file -Dfile=lib/joda-time.jar -DgroupId=uwtime_dep -DartifactId=joda-time -Dversion=0.1 -Dpackaging=jar
mvn install:install-file -Dfile=lib/jyaml-1.3.jar -DgroupId=uwtime_dep -DartifactId=jyaml -Dversion=0.1 -Dpackaging=jar
mvn install:install-file -Dfile=lib/liblinear-1.94.jar -DgroupId=uwtime_dep -DartifactId=liblinear -Dversion=0.1 -Dpackaging=jar
mvn install:install-file -Dfile=lib/spf-1.5.5.jar -DgroupId=uwtime_dep -DartifactId=spf -Dversion=0.1 -Dpackaging=jar
mvn install:install-file -Dfile=lib/xom.jar -DgroupId=uwtime_dep -DartifactId=xom -Dversion=0.1 -Dpackaging=jar
