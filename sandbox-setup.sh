#!/usr/bin/env bash

newb() {
	git checkout -b $1
}

cmt() {
	b=$(git symbolic-ref --short HEAD)
	f=${b/\//-}-${1}-${2}.txt
	touch $f
	git add $f
	git commit -m "$*"
}

newrepo() {
	dir=machete-sandbox/$1
	rm -fr /tmp/_$dir
	mv ~/$dir /tmp/_$dir
	mkdir -p ~/$dir
	cd ~/$dir
	opt=$2
	git init $opt
}

push() {
	b=$(git symbolic-ref --short HEAD)
	git push -u origin $b
}

#todo remove machete sandbox dir

newrepo filter-service-remote --bare
newrepo filter-service
git remote add origin ~/machete-sandbox/filter-service-remote

newb root
	cmt Root
newb develop
	cmt Develop commit
newb allow-ownership-link
	cmt Allow ownership links
	push
newb build-chain
	cmt Build arbitrarily long chains
git checkout allow-ownership-link
	cmt 1st round of fixes
git checkout develop
	cmt Other develop commit
	push
newb call-ws
	cmt Call web service
	cmt 1st round of fixes
	push
newb drop-constraint # not added to definition file
	cmt Drop unneeded SQL constraints
git checkout call-ws
	cmt 2nd round of fixes

git checkout root
newb master
	cmt Master commit
	push
newb hotfix/add-trigger
	cmt HOTFIX Add the trigger
	push
	git commit --amend -m 'HOTFIX Add the trigger (amended)'

cat >.git/machete <<EOF
develop
    allow-ownership-link PR #123
        build-chain PR #124
    call-ws
master
    hotfix/add-trigger
EOF

git branch -d root

echo
echo
git machete status $1
echo
echo

newrepo generator-service-remote --bare
newrepo generator-service
git remote add origin ~/machete-sandbox/generator-service-remote

newb root
	cmt Root
newb develop
    cmt 'Develop commit (probalby empty)'
newb forbid-ownership-link
	cmt Forbid ownership links
	push
newb destroy-chain
	cmt Destroy arbitrarily long chains
git checkout forbid-ownership-link
	cmt n-th round of fixes
git checkout develop
	cmt Other develop commit - this one too
	push
newb send-a-pigeon-for-ws
	cmt Send a pigeon for web service
	cmt 1st round of bugs
	push
git checkout forbid-ownership-link
newb toggle-rain
	cmt toggle
	cmt toggle 2
	push
    cmt toggle 3
newb make-coffee
	cmt Get water
	cmt Search for help
	push
git checkout forbid-ownership-link
newb go-home
	cmt run
	cmt ide
	push
git checkout forbid-ownership-link
newb add-constraint # not added to definition file
	cmt Add unneeded SQL constraints
git checkout send-a-pigeon-ws
	cmt 2nd round of bugs

git checkout root
newb master
    cmt 'Master of puppets!!111111oneoneone commit'
	push
newb coldfix/knor
	cmt pudliszki
	push
newb hotfix/remove-trigger
	cmt HOTFIX Remove the trigger
	push
	git commit --amend -m 'HOTFIX Remove the trigger (amended)'

cat >.git/machete <<EOF
develop
    forbid-ownership-link PR #123
        send-a-pigeon-for-ws
    destroy-chain PR #124
        toggle-rain PR #21
            make-coffee PR #37
        go-home
master
    coldfix/knor
        hotfix/remove-trigger
EOF


git branch -d root

echo
echo
git machete status $1
echo
echo

