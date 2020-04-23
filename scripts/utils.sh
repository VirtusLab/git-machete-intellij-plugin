
function die() {
  red='\033[91m'
  endc='\033[0m'

  if [[ $# -ge 1 ]]; then
    echo -e "\n${red}>>> $@ <<<${endc}\n"
  fi
  exit 1
}

function extract_version_from_gradle_file_stdin() {
  tty --silent && die "${FUNCNAME[0]}: expecting non-terminal stdin, aborting" || true

  grep -Po "(?<=PLUGIN_VERSION = ').*(?=')"
}

function extract_version_from_current_wd() {
  extract_version_from_gradle_file_stdin < version.gradle
}

function extract_version_from_git_revision() {
  [[ $# -eq 1 ]] || die "${FUNCNAME[0]}: expecting 1 argument (<revision>), aborting"
  local revision=$1

  git show "$revision":version.gradle | extract_version_from_gradle_file_stdin
}

function parse_version() {
  [[ $# -eq 2 ]] || die "${FUNCNAME[0]}: expecting 2 arguments (<var_prefix> <version>), aborting"
  local var_prefix=$1
  local version=${2//[-+]*/} # remove pre-release indicator/build metadata; note that this is glob, not regex - so `*` here means the same as `.*` in regex

  declare -g "$var_prefix"_version="$version"
  IFS=. read -r ${var_prefix}_major ${var_prefix}_minor ${var_prefix}_patch <<< "$version"
}

function parse_version_from_current_wd() {
  [[ $# -eq 1 ]] || die "${FUNCNAME[0]}: expecting 1 argument (<var_prefix>), aborting"
  local var_prefix=$1

  parse_version "$var_prefix" "$(extract_version_from_current_wd)"
}

function parse_version_from_git_revision() {
  [[ $# -eq 2 ]] || die "${FUNCNAME[0]}: expecting 2 arguments (<var_prefix> <revision>), aborting"
  local var_prefix=$1
  local revision=$2

  parse_version "$var_prefix" "$(extract_version_from_git_revision "$revision")"
}
