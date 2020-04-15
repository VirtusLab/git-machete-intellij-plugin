newb() {
  git checkout -b $1
}

cmt() {
  b=$(git symbolic-ref --short HEAD)
  f=${b/\//-}-${1}-${2-}.txt
  touch $f
  git add $f
  git commit -m "$*"
}

newrepo() {
  path=$1
  dir=$2
  mkdir $path/$dir
  cd $path/$dir
  opt=${3-}
  git init $opt
}

gituserdata() {
  git config --local user.email "circleci@example.com"
  git config --local user.name "CircleCI"
}
