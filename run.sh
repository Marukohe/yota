#!/bin/bash

set -o pipefail

DEFAULT='\033[0m'
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'

#jarfile="./build/libs/monkey-1.0-SNAPSHOT.jar"
jarfile="./monkeyinstance/Monkey-instance-$1.jar"
date_dir="./results/$1/$(date +%Y%m%d)_$(date +%H%M%S)"
mkdir -p "$date_dir"

#if [ ! -f "$jarfile" ]; then
#  echo -e "${RED}=============== generate monkey.jar first ===============${DEFAULT}"
#  ./gradlew shadowJar
#fi

for conf in src/main/resources/coverage/instance$1/*.conf ; do
  name=$(basename "$conf" ".conf")
  echo -e "${RED}=============== monkey test with $name.conf ===============${DEFAULT}"
  java -Xverify:none -Xmx6g -jar $jarfile "coverage/instance$1/$name.conf" 2>&1 | tee "$date_dir/$name"
  if [ $? -ne 0 ]; then
    echo "$name" >> "$date_dir/failed"
  fi
done
