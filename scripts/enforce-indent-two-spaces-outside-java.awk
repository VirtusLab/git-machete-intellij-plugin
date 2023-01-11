#!/usr/bin/env awk

FNR == 1 { prev_non_empty = "" }

( /^ {3}/ && prev_non_empty !~ /^ {2}/ ) || ( /^ {5}/ && prev_non_empty !~ /^ {4}/ ) {
  print FILENAME ":" FNR ": likely three or four spaces used for indent instead of two"
  exit_code = 1
}

/^.+$/ { prev_non_empty = $0 }

END { exit exit_code }
