# Changelog

## v3.2.0
- Added a new `alt + enter` intention action to create a non-existing branch if user adds it to the machete file.
- Added an option to manually pick a commit for the fork point override.
- Fixed project-dependent settings persistence.
- Added a notification after checking out an unmanaged branch with an option to add the branch to the branch layout.

## v3.1.1
- Removed automatic rediscovery in case of empty machete file.
- Fixed indication of a repeated entry in machete file.

## v3.1.0
- Added support for IntelliJ 2022.3.
- Fixed spurious `NullPointerException` thrown when opening machete file via toolbar, as reported by @oksana-cherniavskaia.
- Added branch navigation allowing to check out:
  * previous branch with `alt + up-arrow`,
  * next branch with `alt + down-arrow`,
  * parent branch with `alt + left-arrow`,
  * first child branch with `alt + right-arrow`
  when Git Machete tab is opened, and has focus.

  Note that `option` key is the equivalent of `alt` in macOS.

## v3.0.3
- Enabled Ctrl + left-click to work as a right-click for macOS users, for displaying the machete actions on a branch.

## v3.0.2
- Enabled context-menu <i>Override Fork Point...</i> action for branches that are out of sync (red-edge).
- Fixed missing <i>Open Git Machete Tab</i> in Git menu.

## v3.0.1
- Added a warning dialog window that pops up when `Sync To Parent By Merge` is about to be used.

## v3.0.0
- Dropped support for IntelliJ 2020.3, 2021.1, 2021.2 and 2021.3.
  Note that the versions of this plugin published so far will remain available for download in IntelliJ 2020.3-2021.3 indefinitely.
  The change in the range of supported IntelliJ versions will only affect the new plugin releases, starting from this one.
- Added autocomplete functionality to <i>Slide In</i> dialog as suggested by @KotlinIsland.

## v2.2.0
- Add an action to automatically squash the commits belonging to the given branch.

## v2.1.0
- Add support for IntelliJ 2022.2.
- Show a path to repository (from project root) instead of a repository name alone in the repository selection combobox.

## v2.0.1
- The Pull action displays custom notification message when it is executed on branch that no longer exists remotely.

## v2.0.0
- Fix docs on the shortcut that opens Git Machete Tab (command option shift M) on Mac.
- Merge Parent Into Current action has been added.The action merges a parent branch into the current branch, as suggested by @mkondratek and @pedroadame.
- Dropped support for IntelliJ 2020.1 and 2020.2.
- The Pull action on specific branch displays additional information whether fetch has been performed before fast-forward merge, as suggested by @micpiotrowski.

## v1.3.0
- Squash-merges are now detected (and marked with gray edge) as in git-machete CLI - first suggested by @asford.
- The reoccurring problem with AlreadyDisposedException (reported by @yeahbutstill) is now hopefully solved.
- The Pull action fetches all branches from the remote.Pulling a branch now behaves consistently with git pull by fetching all branches.
- Show in Git log action has been added to per-branch context menu options.The action points to a branch (its latest local commit) within IntelliJ's built-in Git log, as suggested by @KotlinIsland.

## v1.2.1
- The slide-out dialog displays name of the branch to be slid-out and potentially deleted (contributed by @DcortezMeleth).
- Change machete file location for worktrees to the top-level .git/machete (rather than .git/worktrees/.../machete), as reported by @jeffjensen.This location is now compatible with git-machete CLI.
- Provide separate code style settings for machete files.Opening machete file in editor no longer changes global code style settings for Other File Types, as reported by @radeusgd.

## v1.2.0
- Added support for IntelliJ 2022.1.
- Improved reporting of errors as GitHub issues (contributed by @DcortezMeleth).

## v1.1.0
- Added support for IntelliJ 2021.3.

## v1.0.1
- Disabled Fetch button when there are no remotes in the current repository.

## v0.9.2
- For one minute after a fetch, refrain from re-fetching the repo when doing a pull
- Fix newly created branches sometimes incorrectly recognized as merged to parent

## v0.9.1
- Added support for IntelliJ 2021.1
- Added dialog that suggests deleting local branches when sliding out
- Added support for git config core.hooksPath property
- Allowed slide out of currently checked out branch
- Slide Out action slides out all occurrences of branch entry
- Added slide out suggestion action link for the skipped branches to the skipped branches warning
- Dropped external system - hackish approach to provide machete-file-to-graph-table synchronization
- Added machete file changes listener - reasonable approach to provide machete-file-to-graph-table synchronization
- Add discover action to the toolbar
- Do not show double-listed branches in graph table
- Added tooltip for root branch as well as non-root branches
- Enhance messages related to the ill-formed branch layout
- Correctly check if Git Machete Tab is opened the project resolver (fixes premature discover)
- Improve branch layout discover/write/backup reliability

## v0.9.0
- Added support for IntelliJ 2021.1
- Added dialog that suggests deleting local branches when sliding out
- Added support for git config core.hooksPath property
- Allowed slide out of currently checked out branch
- Slide Out action slides out all occurrences of branch entry
- Added slide out suggestion action link for the skipped branches to the skipped branches warning
- Add discover action to the toolbar
- Do not show double-listed branches in graph table
- Added tooltip for root branch as well as non-root branches
- Correctly check if Git Machete Tab is opened the project resolver (fixes premature discover)
- Improve branch layout discover/write/backup reliability

## v0.8.2
- Add link to Open Machete File action from discovery success notification
- Improve stability of fast-forward pull action
- Prohibit re-checking out the currently checked out with double click
- Rename "fast-forward parent to match branch" action to "fast-forward merge branch to parent"
- Treat develop branch as root in discovery, even if master branch is present

## v0.8.1
- Added support for IntelliJ 2020.3.
- "Slide In Branch Below Current Branch" action is now hidden for unmanaged branches
- Fixed faulty .git/machete file parsing when the last line has only whitespace
- Improved UX of Override Fork Point dialog
- Allowed fast forwarding parent when a branch is connected to the parent with a yellow edge
- Fixed font size dependent graph scaling
- Fixed incorrect indication of newly created branch after pulling its parent as merged
- Allowed checkout and rebase on detached state
- Improved fork point inference
- Added rediscover suggestion after long Git Machete non-use time

## v0.8.0
- Added support for IntelliJ 2020.3.
- "Slide In Branch Below Current Branch" action is now hidden for unmanaged branches
- Fixed faulty .git/machete file parsing when the last line has only whitespace
- Improved UX of Override Fork Point dialog
- Allowed fast forwarding parent when a branch is connected to the parent with a yellow edge

## v0.7.2
- Sliding out root branches is now allowed
- Success notifications for reset to remote, fast-forward, rebase and slide out actions have been made clearer
- Machete file is automatically refreshed in the editor once the status is refreshed in the VCS tab
- Both 'develop' and 'main' can also be considered root branches in layout discovery if 'master' branch is absent
- Status is now always automatically refreshed after resetting current branch to remote

## v0.7.1
- Added 'Edit' option to the discovered dialog that opens machete file
- Enable branch name completion and syntax annotation while indexing
- Prohibit force push of protected branches
- Ensure each project-readiness-dependent action is disabled & hidden as long as project is not ready
- Unreliable links has been removed; non-existing/empty machete file is being created by autodiscover

## v0.7.0
- Added discover functionality
- Added fork point override functionality
- Branch reset is possible without checking it out
- Toolbar menu was revamped to suggest most suitable actions for the current branch
- Pull action uses inferred remote branch in case tracking config is missing
- Added tooltips with information about synchronization to parent branch status for given branch
- Sliding in a branch that does not exist in the local repository but has a remote counterpart was fixed
- Fast forwarding given branch to the currently checked out branch is possible
- Branch names of submodule repository are checked and proposed correctly in machete file editor
- Toggle listing commits is disabled in case there are no commits to display

## v0.6.0
- Added support for pulling current branch
- Added support for sliding in a new branch (or reattaching a existing one) without the need to edit machete file manually
- Open Machete Tab action was added under VCS/Git submenu with key shortcut Ctrl + Alt + Shift + M
- Current branch is marked even if there is an ongoing operation in repo (rebasing, bisecting, etc.) and name of this operation is displayed
- Added a notification when skipping branches declared in machete file but nonexistent in the repository
- Reset action is preceded by information dialog
- Reset is prohibited when uncommitted changes are present in repository
- Detecting existing indentation style in machete file editor
- Refreshing of branch tree after slide-out action
- Reset always gets up-to-date commit from remote tracking branch
- Fast-forward is possible without remote tracking branch information

## v0.5.2
- Added editor for .git/machete file with syntax highlighting
- Added action for fast-forwarding parent to the given branch
- Added action for resetting the given branch to its remote tracking branch
- Added fetch/pull actions
- Added help action
- Improved displaying of status
- Improved push action to automatically suggest force-push when needed
- Improved stability of push dialog (v0.5.1)
- Improved stability of slide out (v0.5.2)

## v0.5.1
- Improved stability of push dialog

## v0.5.0
- Added editor for .git/machete file with syntax highlighting
- Added action for fast-forwarding parent to the given branch
- Added action for resetting the given branch to its remote tracking branch
- Added fetch/pull actions
- Added help action
- Improved displaying of status
- Improved push action to automatically suggest force-push when needed