#!/bin/bash
mkdir prepared_repo
cd prepared_repo || exit

cp -r /original_repo/{build.gradle,settings.gradle,gradle,gradlew} ./

while read -r dir
do
	mkdir "$dir"
	if [[ -f "/original_repo/$dir/build.gradle" ]]
	then
	  cp "/original_repo/$dir/build.gradle" "./$dir/"
  fi
done < <(cat /original_repo/settings.gradle | grep -Po "include\s*('|\")\K.*(?=('|\"))")

./gradlew --info
