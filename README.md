# Git Machete IntelliJ Plugin

[![CircleCI](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master.svg?style=shield)](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master)
[![JetBrains Plugins](https://img.shields.io/jetbrains/plugin/v/14221-git-machete.svg)](https://plugins.jetbrains.com/plugin/14221-git-machete)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/14221-git-machete.svg)](https://plugins.jetbrains.com/plugin/14221-git-machete)

<img src="docs/logo.svg" style="width: 100%; display: block; margin-bottom: 10pt;"/>

Git Machete plugin is a robust tool that **simplifies your git related workflow**.
It's a great complement to the JetBrains products' built-in version control system.<br/>

The _bird's eye view_ provided by Git Machete makes **merges/rebases/push/pulls hassle-free**
even when **multiple branches** are present in the repository
(master/develop, your topic branches, teammate's branches checked out for review, etc.).<br/>

A look at a Git Machete tab gives an instant answer to the questions:
* What branches are in this repository?
* What is going to be merged (rebased/pushed/pulled) and to what?

With this plugin, you can maintain **small, focused, easy-to-review pull requests** with little effort.

**It is compatible with all JetBrains products except Android Studio.
The minimum required version is 2020.1**.

Git Machete IntelliJ Plugin is a port of a handy console tool &mdash; [git-machete](https://github.com/VirtusLab/git-machete#git-machete), into an IntelliJ plugin.


# Table of Contents

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
<!-- To install doctoc run `npm install -g doctoc`, to use it run `doctoc <this-file-path> -->

- [Installing from JetBrains Marketplace](#installing-from-jetbrains-marketplace)
- [Where to find the plugin tab](#where-to-find-the-plugin-tab)
- [Getting started with Git Machete](#getting-started-with-git-machete)
  - [Scenario 1: Review](#scenario-1-review)
  - [Scenario 2: Branch update](#scenario-2-branch-update)
  - [Scenario 3: Stacked PRs (sequential branch setup)](#scenario-3-stacked-prs-sequential-branch-setup)
  - [Scenario 4: Merge (maintaining linear history)](#scenario-4-merge-maintaining-linear-history)
- [Feature List](#feature-list)
- [Build](#build)
- [Issue reporting](#issue-reporting)
- [References](#references)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->


## Installing from JetBrains Marketplace

This plugin is available on [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/14221-git-machete). <br/>
To install this plugin go to `File > Settings > Plugins` in your IDE,
then make sure you are on `Marketplace` tab (not `Installed`), in search box type `Git Machete` and click `Install`.
After installation, depending on the IDE version the restart may be required.
In that case, just click `Restart IDE` and confirm that action in a message box.


## Where to find the plugin tab

Git Machete IntelliJ Plugin is available under the `Git` tool window in the `Git Machete` tab.
You can also use `Ctrl + Alt + Shift + M` shortcut to open it.

## Getting started with Git Machete


The examples below show a few common situations where Git Machete finds an exceptional application.

**If you are a Git Master or have used the Git Machete CLI version already, you may want to jump directly to the [feature list](FEATURE-LIST.md).**


### Scenario 1: Review
- master
  - sticky-header
    - fancy-footer

---
- mention review state of PRs (branches)
- note checkout (by d-click option)
- note slide out and delete option
---

0. all in sync (to remote and parent)
1. slide in (and checkout) `common-scripts` (someone's PR)
2. checkout `master`
3. slide out `common-scripts`

|gif|

|desc|


### Scenario 2: Branch update
- master
  - sticky-header
    - fancy-footer

---
- mention review state of PRs (branches)
- explain why pull is needed (in terms of syncs)
- note checkout (by d-click option)
---

0. all in sync (to remote and parent)
1. fetch (`master` gets out of sync to remote)
2. pull `master`
4. rebase `sticky-header`
5. rebase `fancy-footer`
6. push'em all

|gif|

|desc|


### Scenario 3: Stacked PRs (sequential branch setup)
- master
  - sticky-header
    - fancy-footer

---
- mention review state of PRs (branches)
- mention push/force push difference and why is it needed
- note checkout (by d-click option)
---

0. all in sync (to remote and parent)
1. commit (to `sticky-header`)
2. rebase `fancy-footer`
3. push'em all

|gif|

|desc|


### Scenario 4: Merge (maintaining linear history)
- master
  - sticky-header
    - fancy-footer

---
- mention review state of PRs (branches)
- mention push/force push difference and why is it needed
- add note that ff merge is only an option (link [git scm](https://git-scm.com/docs/git-merge#_fast_forward_merge))
- note checkout (by d-click option)
---

0. all in sync (to remote and parent, be on `fancy-footer`)
1. ff `master` to match `sticky-header`
3. push `master`
4. slide out `sticky-header`

|gif|

|desc|


## Feature List

Please see the [feature list](FEATURE-LIST.md) for more specific features description.


## Build

Please see the [development documentation](DEVELOPMENT.md) for instruction on how to build this plugin on your own.


## Issue reporting

If you see any bug or just would like to propose any new feature, feel free to create an issue.
When you report a bug please include logs from IntelliJ.<br/>
It can be very helpful for us to enable logging on a debug level and then reproduce a bug.
To do this, go to `Help > Diagnostic Tools > Debug Log Settings` and then paste the following lines:

```
binding
branchlayout
gitcore
gitmachete.backend
gitmachete.frontend.actions
gitmachete.frontend.externalsystem
gitmachete.frontend.graph
gitmachete.frontend.ui
```

Then reproduce the bug and go to `Help > Show Log in Files` to open the log file.


## References

See also [git-machete](https://github.com/VirtusLab/git-machete#git-machete) &mdash; a CLI version of this plugin.

For more information about the `git machete`, look at the [reference blog post](https://medium.com/virtuslab/make-your-way-through-the-git-rebase-jungle-with-git-machete-e2ed4dbacd02).
