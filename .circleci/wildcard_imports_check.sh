#!/bin/bash

RESULT=$(git grep -n 'import .*\.\*' '*.java')
RESULT_COUNT=$(echo "$RESULT" | wc -l)
if [[ ${#RESULT} -gt 0 ]]
then
  echo "$RESULT_COUNT wildcard import(s) has been detected:"
  echo "$RESULT"
  exit 1
fi