#!/bin/bash
set -euo pipefail
IFS=$'\n\t'
DIR="$(dirname $(realpath "$0"))"
echo $DIR

export STORK_VERSION=$(cat "$DIR/project.yml" | yq eval '.release.current-version' -)
cd docs
pipenv run mike deploy --push --update-aliases $STORK_VERSION latest --branch gh-pages
pipenv run mike set-default --push latest