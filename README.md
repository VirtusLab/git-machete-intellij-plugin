# Git Machete IntelliJ Plugin

[![CircleCI](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master.svg?style=shield)](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master)
[![JetBrains Plugins](https://img.shields.io/jetbrains/plugin/v/14221-git-machete.svg)](https://plugins.jetbrains.com/plugin/14221-git-machete)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/14221-git-machete.svg)](https://plugins.jetbrains.com/plugin/14221-git-machete)

![](src/main/resources/META-INF/pluginIcon.svg)

Git Machete plugin is a robust tool that simplifies your git related workflow.
It's a great complement of IntelliJ version control system.
The "bird’s eye view" provided by Git Machete makes merges/rebases/push/pulls hassle-free
even when multiple branches are present in the repository
(master/develop, your topic branches, teammate’s branches checked out for review etc.).
A look at Git Machete tab gives an instant answer to the question
"what are the branches in this repository, what is going to be merged (rebased/pushed/pulled) and to what?".

With this plugin you can simply maintain small pull request that are easy to review.

**It is compatible with all JetBrains products except Android Studio.
Minimum required version is 2020.1**.

![](docs/sample_workflow.gif)

See also [git-machete](https://github.com/VirtusLab/git-machete#git-machete) - CLI version of this plugin.


## Installing

### Install from JetBrains Marketplace

This plugin is available on [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/14221-git-machete). <br/>
To install this plugin go to `File` -> `Settings` -> `Plugins`, then make sure you are on `Marketplace` tab (not `Installed`), in search box type `Git Machete` and click `Install`.
After installation depending on IDE version restart may be required. In that case just click `Restart IDE` and confirm that action in a messagebox.


## How it works

Git Machete IntelliJ Plugin is a port of a great console tool - [git-machete](https://github.com/VirtusLab/git-machete#git-machete), into an IntelliJ plugin.
Git Machete can help you manage your repository branches and keep it in sync with each other and its counterparts on a remote repository.
Let's see how this plugin can help you:

### Where to find the plugin tab

Git Machete IntelliJ Plugin is available under `Git` (`VCS` in older IntelliJ versions) tool window in the `Git Machete` tab.
You can also use `Ctrl` + `Alt` + `Shift` + `M` shortcut to open it.

![](docs/open_git_machete.gif)

### Machete file edition

`machete` file describes relations between branches in your repository (these relations are probably determined by order of branch creation - which branch form which - but this is not a rule).
It's located inside `.git` directory in your repo.

You can define this file manually or use [discover](#discover) action.

This is the example of `machete` file:
```
develop
    allow-ownership-link PR #123
        build-chain PR #124
    call-ws
master
    hotfix/add-trigger
```
`develop`, `allow-ownership-link`, `build-chain`, `call-ws`, `master` and `hotfix/add-trigger` are branch names.
Two of them (`allow-ownership-link` and `build-chain`) have custom annotation - it's some kind of short description for given branch and it can be any text that will be displayed next to this branch - in this case they are pull request numbers.
Relation between these branches is determined by indentations - here single indent is 4 spaces, but a tab can be used as well.
In the example above branches `allow-ownership-link` and `call-ws` are children of `develop` and `build-chain` is a child of `allow-ownership-link`.
The same for `hotfix/add-trigger` and `master` - the former is a child of the latter.
`develop` and `master` are root branches.

When branch layout is created, Git Machete indicates relation between parent branch and each of its child branches.
If edge between them is green that means child branch is in sync with its parent branch - in other words there are no commits in parent branch that don't belong to the child.
But if there are some commits in the parent branch that are **not** reachable from the child, then edge is red - we need to [rebase](#rebase) child branch onto the parent.

Machete file editor will help you to manage `machete` file: it underlines any errors (bad indentation or nonexistent branches) and proposes branch names based on local repository branches.
When file editing is done, you can click button in top right corner of file editor to refresh machete branch layout.

![](docs/machete_file_editor.gif)

For more information about `machete` file look at the [reference blog post](https://medium.com/virtuslab/make-your-way-through-the-git-rebase-jungle-with-git-machete-e2ed4dbacd02).

### Discover

Branch layout can be automatically discovered based on your git repository by `Discover Branch Layout` action.
It detects a layout of around 10 most recently used branches.
This action is proposed in Git Machete tab in case of empty or nonexistent `machete` file,
but you can also run it any time you want from IntelliJ's `Search Everywhere` (double Shift) by typing `Discover Branch Layout`.

![](docs/discover.gif)

### Checking out branches

With Git Machete you can easily check out branches that have been defined in `machete` file.
Just right-click on the branch you wont to check out and select `Checkout Branch`.
You can also double-click on the selected branch to check it out.
Current branch is underlined in branch layout.

![](docs/checkout.gif)

### Listing commits

Git Machete can display commits belonging to the branches.
It displays only the commits that are unique to the selected branch (i.e. have never been a tip of a different branch).
When you click on the eye button on the left side of plugin, you can toggle between showing and hiding commits.

![](docs/toggle_listing_commits.gif)


### Rebase

Rebase with Git Machete plugin is easy!
Just right-click on the child branch and from context menu select `(Checkout and) Rebase Branch onto Parent`.
Standard IntelliJ dialog for interactive rebase will appear.

![](docs/rebase.gif)

### Push

After a rebase, you might want to push the rebased branch to the remote.
Right-click on the given branch and select `Push (Current) Branch` from context menu.
Push dialog will appear with proper push button (regular push or force push) depending on which of them is needed.
Basically, we need force push in case when we rebased our branch, because local and remote branch diverged from each other (they have different commits), so git won't let us do regular push.

![](docs/push.gif)

### Pull

If branch is behind (or in sync with) its remote, it can be pulled.
To do this, right-click on given branch and select `Pull Branch`.
This action does `--ff-only` pull only which is enough in this case.

![](docs/pull.gif)

### Reset to remote

If given branch is not in sync with its remote, we can reset it to point to the exact same commit as the remote.
From right-click context menu select `Checkout and Reset Branch to Remote`.
This action performs `git reset --keep` under the hood. `--keep`, as opposed to `--hard`, keeps your uncommitted changes safe from getting overwritten.

![](docs/reset.gif)

### Fast forward parent

When you are done with the changes on the selected branch (e.g. PR was approved and all required fixes are applied), you probably want to merge this branch to its parent branch.
When these branches are in sync (green edge) you can do a fast-forward merge.
Fast forward merge is nothing else like move tip of branch to which we merge to the same commit as tip of merged branch.
This is what `Fast Forward Parent Branch To Match Current Branch` context menu action do.
As you can see, edge between given branch and its parent was changed to grey - it means that the child branch has been merged.

![](docs/fast_forward.gif)

### Slide out branch

When branch was merged (and sometimes in other cases as well), you probably want to remove this branch from branch layout.
To do this with ease (and without manual editing of `machete` file) you can use `Slide Out Selected Branch` action.
This action will modify branch layout for you - delete the selected branch and reattach its children as children of deleted branch parent.
When the `machete.slideOut.deleteLocalBranch` git configuration key is set to `true`, then this action delete also sided out branch from your git repository.
The easiest way to set this key is using `git config --add machete.slideOut.deleteLocalBranch true` command inside given repository.
If you want to set this key globally (to by used for all repositories that don't have this key set explicitly), add `--global` option to the previous command.

![](docs/slide_out_leaf_node.gif)

![](docs/slide_out_non_leaf_node.gif)

### Slide in branch

You can also create new branch bellow selected one (also without manual editing `machete` file). To do this, choose `Slide In Branch Bellow Selected Branch` action from context menu.
Dialog window will appear.
In text box type new branch name and click `Slide In` button.
Another dialog will appear - it is a standard new branch IntelliJ dialog.

![](docs/slide_in_new_branch.gif)

Slide in action can also add a branch that already exists in git repository to the branch layout.
Again, select `Slide In Branch Bellow Selected Branch` from context menu and type already existent branch name.

![](docs/slide_in_existent_branch.gif)

This action can also be used to reattach an existing branch below a selected one.
In this case you just must type name of existing branch that you want to reattach and then click `Slide In`.
You can also select `Reattach children` checkbox to move along all children of the reattached branch.

![](docs/slide_in_reattach_without_children.gif)

![](docs/slide_in_reattach_with_children.gif)

### Other actions

On the left side bar you can find other actions (from top to bottom):
- ![](docs/left_bar_actions/refresh.png) **Refresh Status** - refreshing (reload) branch layout (the graph displayed in main plugin window)
- ![](docs/left_bar_actions/toggle_listing_commits.png) **Toggle Listing Commits** - show or hide commits belonging to branches (for more details see section [Listing commits](#listing-commits))
- ![](docs/left_bar_actions/open_machete_file.png) **Open Machete File** - open `machete` file in IntelliJ editor (to see what this editor can do see [Machete file editor](#machete-file-editor) section)
- ![](docs/left_bar_actions/fetch_all_remotes.png) **Fetch All Remotes** - equivalent of `git fetch --all` command
- The most suitable action (each is equivalent of context menu actions) for a current branch that can be one of:
    - ![](docs/left_bar_actions/push.png) **Push Current Branch**
    - ![](docs/left_bar_actions/pull.png) **Pull Current Branch**
    - ![](docs/left_bar_actions/reset.png) **Reset Current Branch to Remote**
    - ![](docs/left_bar_actions/slide_out.png) **Slide Out Current Branch**

  Available action is selected based on a relation between current branch and its parent and remote branch.
- ![](docs/left_bar_actions/slide_in.png) **Slide In Branch Below Current Branch** - shortcut of [slide in](#slide-in-branch) action for current branch
- ![](docs/left_bar_actions/rebase.png) **Rebase Current Branch Onto Parent** - shortcut of [rebase](#rebase) action for the current branch
- ![](docs/left_bar_actions/help.png) **Show Help Window** - show window with example branch layout and explanation what parts of this graph mean

### Multi-repository support

Git Machete support many git repositories in one project. It can be many regular repositories or submodules.
If more than one repository is detected, then selection list will appear in top of main plugin window.
In that case each repository will have its own `machete` file.

![](docs/multi_repo.gif)


## Build

### Prerequisites

* git
* latest 2020.1 Community Edition/Ultimate

  * Install [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok/)
  * Enable annotation processing (for Lombok):
    `File` -> `Settings` -> `Build`, `Execution`, `Deployment` -> `Compiler` -> `Annotation Processors` -> `Enable Annotation Processing`
  * Set Project SDK to JDK 11: `Project Structure` -> `Project`

Consider increasing maximum heap size for the IDE (the default value is 2048 MB) under `Help` -> `Change Memory Settings`.

For running `./gradlew` from command line, make sure that `java` and `javac` are in `PATH` and point to Java 11.

### Compilation

To compile the project, run `./gradlew build`.

Currently, very generous maximum heap size options are applied for Gradle's Java compilation tasks (search for `-Xmx` in [build.gradle](build.gradle)). <br/>
To overwrite them, use `compileJavaJvmArgs` Gradle project property
(e.g. `./gradlew -PcompileJavaJvmArgs='-Xmx2g -XX:+HeapDumpOnOutOfMemoryError' build`,
or equivalently with an env var: `ORG_GRADLE_PROJECT_compileJavaJvmArgs='-Xmx2g -XX:+HeapDumpOnOutOfMemoryError' ./gradlew compileJava`).

By default, Lombok's annotation processor runs on the fly and Delomboked sources are not saved to {subproject}/build/delombok/...<br/>
To enable Delombok, set `useDelombok` Gradle project property (e.g. `./gradlew -PuseDelombok build`).

In case of spurious cache-related issues with Gradle build, try one of the following:
* `./gradlew --stop` to shut down gradle daemon
* `./gradlew clean` and re-run the failing `./gradlew` command with `--no-build-cache`
* remove .gradle/ directory in the project directory
* remove ~/.gradle/caches/ (or even the entire ~/.gradle/) directory


### Generate plugin zip

To generate a plugin archive run `:buildPlugin` Gradle task (`Gradle panel` -> `Tasks` -> `intellij` -> `buildPlugin` or `./gradlew buildPlugin`).<br/>
The resulting file will be available under `build/distributions/`.


## Issue reporting

If you see any bug or just would like to propose any new feature, feel free to create an issue.
When you report a bug please include logs from IntelliJ.<br/>
It can be very helpful for us to enable logging on debug level and then reproduce bug.
To do this, go to `Help` -> `Diagnostic Tools` -> `Debug Log Settings` and then paste the following lines:

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

Then reproduce the bug and go to `Help` -> `Show Log in Files` to open the log file.

## Development

For more details on development of the project, see the [guideline](DEVELOPMENT.md).
