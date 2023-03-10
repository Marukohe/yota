#!/bin/bash

OS=`uname -s`
PROPERTY_DIR='src/main/resources'

if [ ! -d "monkeyinstance" ]; then
  mkdir monkeyinstance
fi

for ((i=1; i<=$1; ++i)) do
  if [ ${OS} == "Darwin" ]; then
    gsed -i "s/instance.*/instance=${i}/g" $PROPERTY_DIR/mac_runtime.properties
    cat $PROPERTY_DIR/mac_runtime.properties
    ./gradlew clean build
    mv build/libs/Monkey-1.0-SNAPSHOT.jar monkeyinstance/Monkey-instance-${i}.jar
  else
    sed -i "s/instance.*/instance=${i}/g" $PROPERTY_DIR/linux_runtime.properties
    cat $PROPERTY_DIR/linux_runtime.properties
    ./gradlew clean build
    mv build/libs/monkey-1.0-SNAPSHOT.jar monkeyinstance/Monkey-instance-${i}.jar
  fi
done
