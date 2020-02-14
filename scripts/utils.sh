
function derive_version() {
  local revision=$1

  # `git show ... | grep -Po ...` is necessary (instead of `git grep -Po ...`)
  # since the old version of git (2.7.x) installed on the build image doesn't support `-o` flag of `git grep`.
  git show "$revision":build.gradle | grep -Po "(?<=version ')[0-9]+\.[0-9]+\.[0-9]+" || \
    git show "$revision":intellijPlugin/build.gradle | grep -Po "(?<=^version ')[0-9]+\.[0-9]+\.[0-9]+" # TODO remove once chore/git-hooks is merged
}

function parse_version() {
  local revision=$1
  local var_prefix=$2
  local version=$(derive_version "$revision")
  declare -g "$var_prefix"_version="$version"
  IFS=. read -r ${var_prefix}_major ${var_prefix}_minor ${var_prefix}_patch <<< "$version"
}

function die() {
  echo "$1" 1>&2
  exit 1
}
