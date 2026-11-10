#!/usr/bin/env sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
if command -v mvn >/dev/null 2>&1; then exec mvn "$@"; fi
version=3.9.11
tools="$PWD/.tools"
binary="$tools/apache-maven-$version/bin/mvn"
if [ ! -x "$binary" ]; then
  command -v java >/dev/null 2>&1 || { echo 'Instala Java JDK 21.' >&2; exit 1; }
  command -v curl >/dev/null 2>&1 || { echo 'Instala curl o Maven.' >&2; exit 1; }
  mkdir -p "$tools"
  archive="$tools/apache-maven-$version-bin.tar.gz"
  base="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version/apache-maven-$version-bin.tar.gz"
  echo 'Descargando Maven. La primera ejecucion requiere Internet.'
  curl --fail --location --retry 2 "$base" -o "$archive"
  curl --fail --location "$base.sha512" -o "$archive.sha512"
  expected=$(awk '{print $1}' "$archive.sha512")
  if command -v sha512sum >/dev/null 2>&1; then actual=$(sha512sum "$archive" | awk '{print $1}'); else actual=$(shasum -a 512 "$archive" | awk '{print $1}'); fi
  [ "$actual" = "$expected" ] || { rm -f "$archive"; echo 'Fallo de integridad SHA-512.' >&2; exit 1; }
  tar -xzf "$archive" -C "$tools"
  rm -f "$archive" "$archive.sha512"
fi
exec "$binary" "$@"
