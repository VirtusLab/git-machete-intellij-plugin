
function extract_version_from_build_gradle_stdin() {
  grep -Po "(?<=PLUGIN_VERSION = ')[0-9]+\.[0-9]+\.[0-9]+"
}

function extract_version_from_git_revision() {
  local revision=$1

  # `git show ... | grep -Po ...` is necessary (instead of `git grep -Po ...`)
  # since the old version of git (2.7.x) installed on the build image doesn't support `-o` flag of `git grep`.
  { git show "$revision":version.gradle 2>/dev/null || git show "$revision":build.gradle; } | extract_version_from_build_gradle_stdin
}

function parse_version_from_git_revision() {
  local revision=$1
  local var_prefix=$2
  local version=$(extract_version_from_git_revision "$revision")
  declare -g "$var_prefix"_version="$version"
  IFS=. read -r ${var_prefix}_major ${var_prefix}_minor ${var_prefix}_patch <<< "$version"
}

function die() {
  echo "$1" 1>&2
  exit 1
}
