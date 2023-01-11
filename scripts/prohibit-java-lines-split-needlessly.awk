#!/usr/bin/env awk

function indent_width(str) {
  for (i = 1; i <= length(str); i++) {
    if (substr(str, i, 1) != " ") {
      return i - 1;
    }
  }
  return 0;
}

{
  this_indent_width = indent_width($0);
}

FNR != 1 && prev ~ /(,|->)$/ && $0 ~ /[;{]$/ && prev_indent_width < this_indent_width {
  # `+ 1` to account for an extra space added when joining the lines
  joined_length = length(prev) + length($0) - this_indent_width + 1
  MAX_LEN = 128
  if (joined_length <= MAX_LEN) {
    print FILENAME ":" FNR-1 " should be joined with the next line (will be " joined_length "<=" MAX_LEN " chars)"
    exit_code = 1
  }
}

{
  prev = $0;
  # Avoid re-computing the indent for `prev` in the following line
  # due to performance reasons (as this script is a part of the pre-commit hook)
  prev_indent_width = this_indent_width;
}

END { exit exit_code }
