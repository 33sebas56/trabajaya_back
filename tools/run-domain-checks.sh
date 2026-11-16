#!/usr/bin/env sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
mkdir -p target/domain-checks
javac --release 21 -d target/domain-checks \
 src/main/java/com/trabajaya/jobs/JobRules.java \
 src/main/java/com/trabajaya/events/DomainEvent.java \
 src/main/java/com/trabajaya/shared/PageWindow.java \
 src/main/java/com/trabajaya/storage/ObjectKey.java \
 src/main/java/com/trabajaya/users/PasswordPolicy.java \
 src/main/java/com/trabajaya/users/FixedWindowRateLimiter.java \
 tools/DomainSmoke.java
java -cp target/domain-checks DomainSmoke
