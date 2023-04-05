@file:Suppress("FINAL_SUPERTYPE", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE", "EXPOSED_SUPER_CLASS", "CANNOT_OVERRIDE_INVISIBLE_MEMBER", "EXPOSED_FUNCTION_RETURN_TYPE")

package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle
import git4idea.rebase.GitRebaseEntryWithDetails
import git4idea.rebase.interactive.dialog.GitInteractiveRebaseDialog

class Dialog(
  project: Project,
  root: VirtualFile,
  entriesWithDetails: List<GitRebaseEntryWithDetails>,
) :
  GitInteractiveRebaseDialog<GitRebaseEntryWithDetails>(project, root, entriesWithDetails) {

  override fun createNorthPanel() = pushInfo(
    GitMacheteBundle.getString("action.GitMachete.BaseTraverseAction.dialog.push-approval.ahead.text.HTML"),
  )

  fun getModel() = super.getModel()
}
