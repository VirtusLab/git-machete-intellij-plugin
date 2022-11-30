
function info() {
  green='\033[32m'
  endc='\033[0m'
  if [[ -t 1 ]]; then
    echo -e "\n${green}>>> $* <<<${endc}\n"
  else
    echo -e "\n>>> $* <<<\n"
  fi
}

function error() {
  red='\033[91m'
  endc='\033[0m'

  if [[ $# -ge 1 ]]; then
    if [[ -t 1 ]]; then
      echo -e "${red}>>> $* <<<${endc}"
    else
      echo -e ">>> $* <<<"
    fi
  fi
}

function die() {
  echo
  error "$@"
  echo
  exit 1
}

function extract_version_from_gradle_file_stdin() {
  [[ -t 0 ]] && die "${FUNCNAME[0]}: expecting non-terminal stdin, aborting" || true

  # Let's avoid any external process calls (cat/grep) to speed things up.

  # Slurp stdin into a var. All lines will be squashed into just one.
  output=$(</dev/stdin)
  # Get rid of everything after&including the final double quote.
  output=${output%\"*}
  # Get rid of everything before&including the first double quote.
  output=${output#*\"}
  echo -n "$output"
}

function extract_version_from_current_wd() {
  extract_version_from_gradle_file_stdin < version.gradle.kts
}

function extract_version_from_git_revision() {
  [[ $# -eq 1 ]] || die "${FUNCNAME[0]}: expecting 1 argument (<revision>), aborting"
  local revision=$1

  git show "$revision":version.gradle.kts | extract_version_from_gradle_file_stdin
}

if ! grep --perl-regexp --quiet 'foo(?=bar)' <<< foobar >/dev/null 2>/dev/null; then
  error 'Your `grep` does not support `--perl-regexp` (`-P`) mode.'
  die 'Check CONTRIBUTING.md -> Ctrl+F `grep`.'
fi

if ! git grep --perl-regexp --quiet 'foo(?=bar)' >/dev/null 2>/dev/null; then
  error 'Your `git grep` does not support `--perl-regexp` (`-P`) mode.'
  die 'Check CONTRIBUTING.md -> Ctrl+F `USE_LIBPCRE`.'
fi
