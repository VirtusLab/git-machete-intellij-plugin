@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE", "EXPOSED_FUNCTION_RETURN_TYPE")

package com.virtuslab.gitmachete.frontend.actions.handlers

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.virtuslab.gitmachete.frontend.actions.dialogs.Dialog
import git4idea.DialogManager
import git4idea.rebase.GitInteractiveRebaseEditorHandler
import git4idea.rebase.GitRebaseEntry
import git4idea.rebase.GitRebaseEntryWithDetails
import git4idea.rebase.interactive.GitRebaseTodoModel

class MyGitInteractiveRebaseEditorHandler(
  val project: Project,
  val root: VirtualFile,
) : GitInteractiveRebaseEditorHandler(project, root) {

  override fun collectNewEntries(entries: List<GitRebaseEntry>): List<GitRebaseEntry>? {
    val newText = Ref.create<List<GitRebaseEntry>>()
    val entriesWithDetails = loadDetailsForEntries(entries)

    ApplicationManager.getApplication().invokeAndWait {
      newText.set(showInteractiveRebaseDialog(entriesWithDetails))
    }
    return newText.get()
  }

  private fun showInteractiveRebaseDialog(entriesWithDetails: List<GitRebaseEntryWithDetails>): List<GitRebaseEntry>? {
    val editor = Dialog(project, root, entriesWithDetails)
    DialogManager.show(editor)
    if (editor.isOK()) {
      val rebaseTodoModel = editor.getModel()
      processModel(rebaseTodoModel)
      return convertToEntries(rebaseTodoModel)
    }
    return null
  }
}

internal fun <T : GitRebaseEntry> convertToModel(entries: List<T>): GitRebaseTodoModel<T> {
  val result = mutableListOf<GitRebaseTodoModel.Element<T>>()
  // consider auto-squash
  entries.forEach { entry ->
    val index = result.size
    when (entry.action) {
      GitRebaseEntry.Action.PICK, GitRebaseEntry.Action.REWORD -> {
        val type = GitRebaseTodoModel.Type.NonUnite.KeepCommit.Pick
        result.add(GitRebaseTodoModel.Element.Simple(index, type, entry))
      }
      GitRebaseEntry.Action.EDIT -> {
        val type = GitRebaseTodoModel.Type.NonUnite.KeepCommit.Edit
        result.add(GitRebaseTodoModel.Element.Simple(index, type, entry))
      }
      GitRebaseEntry.Action.DROP -> {
        // move them to the end
      }
      GitRebaseEntry.Action.FIXUP, GitRebaseEntry.Action.SQUASH -> {
        val lastElement = result.lastOrNull() ?: throw IllegalArgumentException("Couldn't unite with non-existed commit")
        val root = when (lastElement) {
          is GitRebaseTodoModel.Element.UniteChild<T> -> lastElement.root
          is GitRebaseTodoModel.Element.UniteRoot<T> -> lastElement
          is GitRebaseTodoModel.Element.Simple<T> -> {
            when (val rootType = lastElement.type) {
              is GitRebaseTodoModel.Type.NonUnite.KeepCommit -> {
                val newRoot = GitRebaseTodoModel.Element.UniteRoot(lastElement.index, rootType, lastElement.entry)
                result[newRoot.index] = newRoot
                newRoot
              }
              is GitRebaseTodoModel.Type.NonUnite.Drop -> {
                throw IllegalStateException()
              }
            }
          }
        }
        val element = GitRebaseTodoModel.Element.UniteChild(index, entry, root)
        root.addChild(element)
        result.add(element)
      }
      is GitRebaseEntry.Action.Other -> throw IllegalArgumentException("Couldn't convert unknown action to the model")
    }
  }
  entries.filter { it.action is GitRebaseEntry.Action.DROP }.forEach { entry ->
    val index = result.size
    result.add(GitRebaseTodoModel.Element.Simple(index, GitRebaseTodoModel.Type.NonUnite.Drop, entry))
  }
  return GitRebaseTodoModel(result)
}

internal fun <T : GitRebaseEntry> convertToEntries(x: GitRebaseTodoModel<out T>): List<GitRebaseEntry> = x.elements.map { element ->
  val entry = element.entry
  GitRebaseEntry(element.type.command, entry.commit, entry.subject)
}
