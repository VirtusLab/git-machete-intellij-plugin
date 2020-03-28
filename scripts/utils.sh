
function die() {
  if [[ $# -ge 1 ]]; then
    echo -e "$@"
  fi
  exit 1
}

function extract_version_from_build_gradle_stdin() {
  tty --silent && die "${FUNCNAME[0]}: expecting non-terminal stdin, aborting" || true

  grep -Po "(?<=PLUGIN_VERSION = ')[0-9]+\.[0-9]+\.[0-9]+"
}

function extract_version_from_git_revision() {
  [[ $# -eq 1 ]] || die "${FUNCNAME[0]}: expecting 1 argument (<revision>), aborting"
  local revision=$1

  git show "$revision":version.gradle | extract_version_from_build_gradle_stdin
}

function parse_version_from_git_revision() {
  [[ $# -eq 2 ]] || die "${FUNCNAME[0]}: expecting 2 arguments (<revision> <var_prefix>), aborting"
  local revision=$1
  local var_prefix=$2

  local version=$(extract_version_from_git_revision "$revision")
  declare -g "$var_prefix"_version="$version"
  IFS=. read -r ${var_prefix}_major ${var_prefix}_minor ${var_prefix}_patch <<< "$version"
}
