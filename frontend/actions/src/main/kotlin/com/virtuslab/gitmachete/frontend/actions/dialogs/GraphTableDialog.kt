package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.virtuslab.binding.RuntimeBinding
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import com.virtuslab.gitmachete.frontend.ui.api.table.ISimpleGraphTableProvider
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.util.function.Consumer
import javax.swing.AbstractAction
import javax.swing.Action

class GraphTableDialog
private constructor(
    val table: JBTable,
    val repositorySnapshot: IGitMacheteRepositorySnapshot?,
    val dimension: JBDimension,
    val saveAction: Consumer<IGitMacheteRepositorySnapshot>?,
    val saveAndEditAction: Consumer<IGitMacheteRepositorySnapshot>?,
    val cancelButtonVisible: Boolean,
    val windowTitle: String,
    val okButtonText: String
) : DialogWrapper(/* canBeParent */ false) {

  init {
    super.init()
    title = windowTitle
    setOKButtonText(okButtonText)
    setOKButtonMnemonic(KeyEvent.VK_S)
    cancelAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C)
  }

  companion object {
    // @JvmOverloads annotation instructs the Kotlin compiler to generate overloads
    // for this function that substitute default parameter values (dimension in our case).
    @JvmOverloads
    fun of(
        gitMacheteRepositorySnapshot: IGitMacheteRepositorySnapshot,
        windowTitle: String,
        dimension: JBDimension = JBDimension(/* width */ 400, /* height */ 250),
        emptyTableText: String?,
        saveAction: Consumer<IGitMacheteRepositorySnapshot>?,
        saveAndEditAction: Consumer<IGitMacheteRepositorySnapshot>?,
        okButtonText: String,
        cancelButtonVisible: Boolean,
        shouldDisplayActionToolTips: Boolean
    ) =
        RuntimeBinding.instantiateSoleImplementingClass(ISimpleGraphTableProvider::class.java)
            .deriveInstance(
                gitMacheteRepositorySnapshot,
                /* isListingCommitsEnabled */ false,
                shouldDisplayActionToolTips)
            .apply { setTextForEmptyTable(emptyTableText ?: "") }
            .let {
              GraphTableDialog(
                  /* table */ it,
                  gitMacheteRepositorySnapshot,
                  dimension,
                  saveAction,
                  saveAndEditAction,
                  cancelButtonVisible,
                  windowTitle,
                  okButtonText)
            }

    fun ofDemoRepository() =
        RuntimeBinding.instantiateSoleImplementingClass(ISimpleGraphTableProvider::class.java)
            .deriveDemoInstance()
            .let {
              GraphTableDialog(
                  table = it,
                  repositorySnapshot = null,
                  dimension = JBDimension(/* width */ 850, /* height */ 250),
                  saveAction = null,
                  saveAndEditAction = null,
                  cancelButtonVisible = false,
                  windowTitle = "Git Machete Help",
                  okButtonText = "Close")
            }
  }

  override fun createActions() =
      if (cancelButtonVisible)
          arrayOf(getSaveAndEditAction(), okAction, cancelAction).filterNotNull().toTypedArray()
      else arrayOf(okAction)

  private fun getSaveAndEditAction() =
      if (saveAndEditAction == null) null
      else
          object :
              AbstractAction(
                  getString(
                      "action.GitMachete.DiscoverAction.discovered-branch-tree-dialog.save-and-edit-button-text")) {
            override fun actionPerformed(e: ActionEvent?) {
              repositorySnapshot?.let { saveAndEditAction.accept(it) }
              close(OK_EXIT_CODE)
            }
          }

  override fun createCenterPanel() =
      JBUI.Panels.simplePanel(/* hgap */ 0, /* vgap */ 2).apply {
        addToCenter(ScrollPaneFactory.createScrollPane(this@GraphTableDialog.table))
        preferredSize = dimension
      }

  @Override
  override fun doOKAction() {
    repositorySnapshot?.let { saveAction?.accept(it) }
    close(OK_EXIT_CODE)
  }
}
