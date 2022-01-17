#!/bin/bash
set -euo pipefail
IFS=$'\n\t'
DIR="$(dirname $(realpath "$0"))"
echo $DIR
STORK_VERSION=$(cat "$DIR/project.yml" | yq eval '.release.current-version' -)  && mike deploy --push --update-aliases $STORK_VERSION latest --branch gh-pages
STORK_VERSION=$(cat "$DIR/project.yml" | yq eval '.release.current-version' -)  && mike set-default --push latest