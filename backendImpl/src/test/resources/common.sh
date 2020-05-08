
# The repo setup scripts sometimes spuriously crash on CI, let's investigate that.
set -x

newb() {
  if (( $# != 1 ))
  then
    echo "newb() need 1 parameter, $# was given"
    exit 100
  fi
  git checkout -b $1
}

cmt() {
  if (( $# < 1 ))
  then
    echo "cmt() need at least 1 parameters, $# was given"
    exit 100
  fi
  b=$(git symbolic-ref --short HEAD)
  f=${b/\//-}-${1}-${2-}.txt
  touch $f
  git add $f
  git commit -m "$*"
}

newrepo() {
  if (( $# < 2 ))
  then
    echo "newrepo() need at least 2 parameters, $# was given"
    exit 100
  fi
  path=$1
  dir=$2
  mkdir $path/$dir
  cd $path/$dir
  opt=${3-}
  git init $opt
}

clone() {
  if (( $# != 3 ))
  then
    echo "clone() need 3 parameters, $# was given"
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
