# Changelog

## Unreleased

## v3.0.1
- Add a warning dialog window that pops up when `Sync To Parent By Merge` is about to be used.

## v3.0.0
- Drop support for IntelliJ 2020.3, 2021.1, 2021.2 and 2021.3.
  Note that the versions of this plugin published so far will remain available for download in IntelliJ 2020.3-2021.3 indefinitely.
  The change in the range of supported IntelliJ versions will only affect the new plugin releases, starting from this one.
- Add autocomplete functionality to <i>Slide In</i> dialog as suggested by @KotlinIsland.

## v2.2.0
- Add an action to automatically squash the commits belonging to the given branch.

## v2.1.0
- Add support for IntelliJ 2022.2.
- Show a path to repository (from project root) instead of a repository name alone in the repository selection combobox.

## v2.0.1
- The Pull action displays custom notification message when it is executed on branch that no longer exists remotely.

## v2.0.0
- Fix docs on the shortcut that opens Git Machete Tab (command option shift M) on Mac.
- Add Merge Parent Into Current action. The action merges a parent branch into the current branch, as suggested by @mkondratek and @pedroadame.
- Drop support for IntelliJ 2020.1 and 2020.2.
- The Pull action on specific branch displays additional information whether fetch has been performed before fast-forward merge, as suggested by @micpiotrowski.

## v1.3.0
- Squash-merges are now detected (and marked with gray edge) as in git-machete CLI - first suggested by @asford.
- The reoccurring problem with AlreadyDisposedException (reported by @yeahbutstill) is now hopefully solved.
- The Pull action fetches all branches from the remote. Pulling a branch now behaves consistently with git pull by fetching all branches.
- Add Show in Git log action to per-branch context menu options. The action points to a branch (its latest local commit) within IntelliJ's built-in Git log, as suggested by @KotlinIsland.

## v1.2.1
- The slide-out dialog displays name of the branch to be slid-out and potentially deleted (contributed by @DcortezMeleth).
- Change machete file location for worktrees to the top-level .git/machete (rather than .git/worktrees/.../machete), as reported by @jeffjensen.This location is now compatible with git-machete CLI.
- Provide separate code style settings for machete files. Opening machete file in editor no longer changes global code style settings for Other File Types, as reported by @radeusgd.

## v1.2.0
- Add support for IntelliJ 2022.1.
- Improve reporting of errors as GitHub issues (contributed by @DcortezMeleth).

## v1.1.0
- Add support for IntelliJ 2021.3.

## v1.0.1
- Disable Fetch button when there are no remotes in the current repository.

## v0.9.2
- For one minute after a fetch, refrain from re-fetching the repo when doing a pull
- Fix newly created branches sometimes incorrectly recognized as merged to parent

## v0.9.1
- Add support for IntelliJ 2021.1
- Add dialog that suggests deleting local branches when sliding out
- Add support for git config core.hooksPath property
- Allow slide out of currently checked out branch
- Slide Out action slides out all occurrences of branch entry
- Add slide out suggestion action link for the skipped branches to the skipped branches warning
- Drop external system - hackish approach to provide machete-file-to-graph-table synchronization
- Add machete file changes listener - reasonable approach to provide machete-file-to-graph-table synchronization
- Add discover action to the toolbar
- Do not show double-listed branches in graph table
- Add tooltip for root branch as well as non-root branches
- Enhance messages related to the ill-formed branch layout
- Correctly check if Git Machete Tab is opened the project resolver (fixes premature discover)
- Improve branch layout discover/write/backup reliability

## v0.9.0
- Add support for IntelliJ 2021.1
- Add dialog that suggests deleting local branches when sliding out
- Add support for git config core.hooksPath property
- Allow slide out of currently checked out branch
- Slide Out action slides out all occurrences of branch entry
- Add slide out suggestion action link for the skipped branches to the skipped branches warning
- Add discover action to the toolbar
- Do not show double-listed branches in graph table
- Add tooltip for root branch as well as non-root branches
- Correctly check if Git Machete Tab is opened the project resolver (fixes premature discover)
- Improve branch layout discover/write/backup reliability

## v0.8.2
- Add link to Open Machete File action from discovery success notification
- Improve stability of fast-forward pull action
- Prohibit re-checking out the currently checked out with double click
- Rename "fast-forward parent to match branch" action to "fast-forward merge branch to parent"
- Treat develop branch as root in discovery, even if master branch is present

## v0.8.1
- Add support for IntelliJ 2020.3.
- Hide "Slide In Branch Below Current Branch" action for unmanaged branches
- Fix faulty .git/machete file parsing when the last line has only whitespace
- Improve UX of Override Fork Point dialog
- Allow fast forwarding parent when a branch is connected to the parent with a yellow edge
- Fix font size dependent graph scaling
- Fix incorrect indication of newly created branch after pulling its parent as merged
- Allow checkout and rebase on detached state
- Improve fork point inference
- Add rediscover suggestion after long Git Machete non-use time

## v0.8.0
- Add support for IntelliJ 2020.3.
- Hide "Slide In Branch Below Current Branch" action for unmanaged branches
- Fix faulty .git/machete file parsing when the last line has only whitespace
- Improve UX of Override Fork Point dialog
- Allow fast forwarding parent when a branch is connected to the parent with a yellow edge

## v0.7.2
- Disallow sliding out root branches
- Success notifications for reset to remote, fast-forward, rebase and slide out actions have been made clearer
- Automatically refresh machete file in the editor once the status is refreshed in the VCS tab
- Both 'develop' and 'main' can also be considered root branches in layout discovery if 'master' branch is absent
- Automatically refresh status after resetting current branch to remote

## v0.7.1
- Add 'Edit' option to the discovered dialog that opens machete file
- Enable branch name completion and syntax annotation while indexing
- Prohibit force push of protected branches
- Ensure each project-readiness-dependent action is disabled & hidden as long as project is not ready
- Remove unreliable links; autodiscover creates non-existing/empty machete file

## v0.7.0
- Add discover functionality
- Add fork point override functionality
- Branch reset is possible without checking it out
- Revamp toolbar menu to suggest most suitable actions for the current branch
- Pull action uses inferred remote branch in case tracking config is missing
- Add tooltips with information about synchronization to parent branch status for given branch
- Sliding in a branch that does not exist in the local repository but has a remote counterpart was fixed
- Fast forwarding given branch to the currently checked out branch is possible
- Branch names of submodule repository are checked and proposed correctly in machete file editor
- Disable toggle listing commits in case there are no commits to display

## v0.6.0
- Add support for pulling current branch
- Add support for sliding in a new branch (or reattaching a existing one) without the need to edit machete file manually
- Add Open Machete Tab action under VCS/Git submenu with key shortcut Ctrl + Alt + Shift + M
- Current branch is marked even if there is an ongoing operation in repo (rebasing, bisecting, etc.) and name of this operation is displayed
- Add a notification when skipping branches declared in machete file but nonexistent in the repository
- Reset action is preceded by information dialog
- Reset is prohibited when uncommitted changes are present in repository
- Detecting existing indentation style in machete file editor
- Refreshing of branch tree after slide-out action
- Reset always gets up-to-date commit from remote tracking branch
- Fast-forward is possible without remote tracking branch information

## v0.5.2
- Add editor for .git/machete file with syntax highlighting
- Add action for fast-forwarding parent to the given branch
- Add action for resetting the given branch to its remote tracking branch
- Add fetch/pull actions
- Add help action
- Improve displaying of status
- Improve push action to automatically suggest force-push when needed
- Improve stability of push dialog (v0.5.1)
- Improve stability of slide out (v0.5.2)

## v0.5.1
- Improve stability of push dialog

## v0.5.0
- Add editor for .git/machete file with syntax highlighting
- Add action for fast-forwarding parent to the given branch
- Add action for resetting the given branch to its remote tracking branch
- Add fetch/pull actions
- Add help action
- Improve displaying of status
- Improve push action to automatically suggest force-push when needed