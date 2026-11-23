#!/bin/sh
# Builds Tolk for the five supported systems into target/dist/, and its
# manual page with them:
#   sh build.sh <version>
# Static binaries (no cgo), without local paths and debug tables, with the
# version built in. Needs Go and nothing else: the modules Tolk uses are in
# vendor/, so no module cache or module proxy is needed.
set -eu

version="${1:?usage: sh build.sh <version>}"
cd "$(dirname "$0")"

if ! command -v go >/dev/null 2>&1; then
    echo "build.sh: Go is not installed; see https://go.dev/dl/" >&2
    exit 1
fi

dist=target/dist
rm -rf "$dist"
mkdir -p "$dist"

for target in linux/amd64 linux/arm64 darwin/amd64 darwin/arm64 windows/amd64; do
    os="${target%/*}"
    arch="${target#*/}"
    name="tolk-$version-$os-$arch"
    if [ "$os" = windows ]; then
        name="$name.exe"
    fi
    echo "building $name"
    CGO_ENABLED=0 GOOS="$os" GOARCH="$arch" go build -trimpath \
        -ldflags "-s -w -X main.version=$version" \
        -o "$dist/$name" ./cmd/tolk
done

# The manual page, in English and German, written by Tolk itself from the
# texts of its help: 'man ./tolk.1' shows it, and man1/ of a man directory
# is where it is installed.
echo "writing tolk.1 and tolk.de.1"
go run -trimpath -ldflags "-X main.version=$version" ./cmd/tolk man --lang en > "$dist/tolk.1"
go run -trimpath -ldflags "-X main.version=$version" ./cmd/tolk man --lang de > "$dist/tolk.de.1"

# Checksums, for whoever receives a binary.
(
    cd "$dist"
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum tolk-* tolk.1 tolk.de.1 > SHA256SUMS
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 256 tolk-* tolk.1 tolk.de.1 > SHA256SUMS
    fi
)
echo "built in $dist"
