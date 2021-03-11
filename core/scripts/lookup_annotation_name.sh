#!/bin/bash

if [ -e pom.xml ]
then
  mvn -q -e exec:java@getannotations -Dexec.args="$1" 2>&1 | grep -v "^SLF4J:"
else
  if [ -e ../pom.xml ]
  then
    cd ..
    mvn -q -e exec:java@getannotations -Dexec.args="$1" 2>&1 | grep -v "^SLF4J:"
    cd scripts
  else
    echo "This script must be run from the HBCP home directory (with pom.xml) or 'scripts' directory."
  fi
fi