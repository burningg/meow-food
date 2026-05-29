#!/usr/bin/env bash
set -euo pipefail

if ! command -v /usr/libexec/java_home >/dev/null 2>&1; then
  echo "未找到 /usr/libexec/java_home，无法自动切换到 JDK 17。"
  exit 1
fi

export JAVA_HOME="$("/usr/libexec/java_home" -v 17)"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Using JAVA_HOME=$JAVA_HOME"
java -version

mvn spring-boot:run
