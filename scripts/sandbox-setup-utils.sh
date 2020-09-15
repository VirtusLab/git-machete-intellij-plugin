#!/usr/bin/env bash

set -e

function create_branch() {
  git checkout -b $1
}

function commit() {
  b=$(git symbolic-ref --short HEAD)
  f=${b/\//-}-${1}-${2}.txt
  touch $f
  git add $f
  git commit -m "$*"
}

function create_repo() {
  dir=$sandboxDir/$1
  mkdir ~/$dir
  cd ~/$dir
  opt=$2
  git init $opt
}

function newremote() {
  dir=$sandboxDir/$1-remote
  mkdir ~/$dir
  cd ~/$dir
  git init --bare
}

function push() {
  b=$(git symbolic-ref --short HEAD)
  git push -u origin $b
}

function init() {
  rm -fr /tmp/_$sandboxDir
  mv -f ~/$sandboxDir /tmp/_$sandboxDir || true
  mkdir ~/$sandboxDir
}
