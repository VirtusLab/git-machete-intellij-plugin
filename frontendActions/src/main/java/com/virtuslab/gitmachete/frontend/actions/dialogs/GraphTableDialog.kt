package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.virtuslab.binding.RuntimeBinding
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot
import com.virtuslab.gitmachete.frontend.ui.api.table.ISimpleGraphTableProvider
import java.awt.event.ActionEvent
import java.util.function.Consumer
import javax.swing.AbstractAction

class GraphTableDialog
    private constructor(
        private val table: JBTable,
        private val repositorySnapshot: IGitMacheteRepositorySnapshot?,
        private val dimension: JBDimension,
        private val okAction: Consumer<IGitMacheteRepositorySnapshot>?,
        private val editAction: Runnable?,
        private val cancelButtonVisible: Boolean,
        private val windowTitle: String,
        private val okButtonText: String
    ) : DialogWrapper(/* canBeParent */ false) {

  init {
    super.init()
    title = windowTitle
    setOKButtonText(okButtonText)
  }

  companion object {
    // @JvmOverloads annotation instructs the Kotlin compiler to generate overloads
    // for this function that substitute default parameter values (dimension in our case).
    @JvmOverloads
    fun of(
        gitMacheteRepositorySnapshot: IGitMacheteRepositorySnapshot,
        windowTitle: String,
        dimension: JBDimension = JBDimension(/* width */ 800, /* height */ 500),
        emptyTableText: String?,
        okAction: Consumer<IGitMacheteRepositorySnapshot>?,
        editAction: Runnable?,
        okButtonText: String,
        cancelButtonVisible: Boolean,
        hasBranchActionToolTips: Boolean
    ) =
        RuntimeBinding.instantiateSoleImplementingClass(ISimpleGraphTableProvider::class.java)
            .deriveInstance(
                gitMacheteRepositorySnapshot,
                /* isListingCommitsEnabled */ false,
                hasBranchActionToolTips)
            .apply { setTextForEmptyTable(emptyTableText ?: "") }
            .let {
              GraphTableDialog(
                  /* table */ it,
                  gitMacheteRepositorySnapshot,
                  dimension,
                  okAction,
                  editAction,
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
              dimension = JBDimension(/* width */ 800, /* height */ 250),
              okAction = null,
              editAction = null,
              cancelButtonVisible = false,
              windowTitle = "Git Machete Help",
              okButtonText = "Close")
        }
  }

  override fun createActions() =
      if (cancelButtonVisible)
          arrayOf(getSaveAndEditAction(), getOKAction(), cancelAction)
              .filterNotNull()
              .toTypedArray()
      else arrayOf(getOKAction())

  private fun getSaveAndEditAction() =
      if (editAction == null) null
      else
          object : AbstractAction("Save && Edit") {
            override fun actionPerformed(e: ActionEvent?) {
              doOKAction()
              editAction.run()
            }
          }

  override fun createCenterPanel() =
      JBUI.Panels.simplePanel(/* hgap */ 0, /* vgap */ 2).apply {
        addToCenter(ScrollPaneFactory.createScrollPane(this@GraphTableDialog.table))
        preferredSize = dimension
      }

  @Override
  override fun doOKAction() {
    repositorySnapshot?.let { okAction?.accept(it) }
    close(OK_EXIT_CODE)
  }
}
