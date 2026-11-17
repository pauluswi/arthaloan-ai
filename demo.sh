#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
MAVEN_VERSION="3.9.9"
MAVEN_SHA512="a555254d6b53d267965a3404ecb14e53c3827c09c3b94b5678835887ab404556bfaf78dcfe03ba76fa2508649dca8531c74bca4d5846513522404d48e8c4ac8b"
MAVEN_CACHE="${HOME}/.cache/arthaloan-ai/apache-maven-${MAVEN_VERSION}"

fail() {
  printf 'Error: %s\n' "$1" >&2
  exit 1
}

command -v java >/dev/null 2>&1 || fail "Java 25 is required. Install a JDK and try again."
command -v node >/dev/null 2>&1 || fail "Node.js 22.22.3+, 24.15.0+, or 26+ is required."
command -v npm >/dev/null 2>&1 || fail "npm is required with Node.js."
command -v curl >/dev/null 2>&1 || fail "curl is required to download Maven when it is not installed."

JAVA_VERSION="$(java -version 2>&1 | sed -n '1s/.*version "\([^"]*\)".*/\1/p')"
JAVA_MAJOR="${JAVA_VERSION%%.*}"
if [[ ! "$JAVA_MAJOR" =~ ^[0-9]+$ ]] || (( JAVA_MAJOR < 25 )); then
  fail "Java 25 or newer is required (found ${JAVA_VERSION:-unknown})."
fi

NODE_MAJOR="$(node -p 'process.versions.node.split(".")[0]')"
NODE_MINOR="$(node -p 'process.versions.node.split(".")[1]')"
NODE_PATCH="$(node -p 'process.versions.node.split(".")[2]')"
if ! {
  { (( NODE_MAJOR == 22 )) && { (( NODE_MINOR > 22 )) || { (( NODE_MINOR == 22 )) && (( NODE_PATCH >= 3 )); }; }; } ||
  { (( NODE_MAJOR == 24 )) && { (( NODE_MINOR > 15 )) || (( NODE_MINOR == 15 )); }; } ||
  (( NODE_MAJOR >= 26 ))
}; then
  fail "Node.js 22.22.3+, 24.15.0+, or 26+ is required (found $(node --version))."
fi

if command -v mvn >/dev/null 2>&1; then
  MVN="$(command -v mvn)"
else
  if [[ ! -x "${MAVEN_CACHE}/bin/mvn" ]]; then
    ARCHIVE="${HOME}/.cache/arthaloan-ai/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
    mkdir -p "${HOME}/.cache/arthaloan-ai"
    printf 'Downloading Maven %s...\n' "${MAVEN_VERSION}"
    curl --fail --location --silent --show-error \
      "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
      --output "${ARCHIVE}" || fail "Could not download Maven ${MAVEN_VERSION}."
    ACTUAL_MAVEN_SHA512="$(openssl dgst -sha512 "${ARCHIVE}" | awk '{print $NF}')"
    if [[ "${ACTUAL_MAVEN_SHA512}" != "${MAVEN_SHA512}" ]]; then
      rm -f "${ARCHIVE}"
      fail "The downloaded Maven archive failed its SHA-512 checksum."
    fi
    tar -xzf "${ARCHIVE}" -C "${HOME}/.cache/arthaloan-ai"
    rm -f "${ARCHIVE}"
  fi
  MVN="${MAVEN_CACHE}/bin/mvn"
fi

cd "${PROJECT_DIR}/frontend"
printf 'Installing frontend dependencies...\n'
npm ci
printf 'Building dashboard...\n'
npm run build

cd "${PROJECT_DIR}"
printf 'Packaging application...\n'
"${MVN}" -B package
printf 'Starting ArthaLoan AI at http://localhost:%s\n' "${SERVER_PORT:-8080}"
exec java -jar target/arthaloan-ai-1.0-SNAPSHOT.jar
