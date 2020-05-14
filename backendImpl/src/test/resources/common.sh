#!/usr/bin/env bash

newrepo() {
  if (( $# < 2 ))
  then
    echo "newrepo() needs at least 2 parameters, $# was given"
    exit 100
  fi
  path=$1
  dir=$2
  mkdir -p $path/$dir
  cd $path/$dir
  opt=${3-}
  git init $opt
}

newb() {
  if (( $# != 1 ))
  then
    echo "newb() needs 1 parameter, $# was given"
    exit 100
  fi
  git checkout -b $1
}

cmt() {
  if (( $# < 1 ))
  then
    echo "cmt() needs at least 1 parameters, $# was given"
    exit 100
  fi
  b=$(git symbolic-ref --short HEAD)
  f=${b/\//-}-${1}-${2-}.txt
  touch $f
  git add $f
  git commit -m "$*"
}

clone() {
  if (( $# != 3 ))
  then
    echo "clone() needs 3 parameters, $# was given"
    exit 100
  fi
  path=$1
  remote=$2
  destinationDir=$3
  cd $path
  git clone $remote $destinationDir
  cd $destinationDir
}

gituserdata() {
  git config --local user.email "circleci@example.com"
  git config --local user.name "CircleCI"
}
