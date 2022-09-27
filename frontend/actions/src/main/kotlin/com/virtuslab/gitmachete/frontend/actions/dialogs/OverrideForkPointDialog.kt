package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch
import com.virtuslab.gitmachete.backend.api.IForkPointCommitOfManagedBranch
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import java.awt.Color
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.UIManager

enum class OverrideOption {
    PARENT,
    INFERRED
}

class OverrideForkPointDialog(
    project: Project,
    private val parentBranch: IManagedBranchSnapshot,
    private val branch: INonRootManagedBranchSnapshot
) : DialogWrapper(project, /* canBeParent */ true) {

    private var myOverrideOption = OverrideOption.PARENT

    private var customCommit = branch.forkPoint

    init {
        title =
            getString("action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.title")
        setOKButtonMnemonic('O'.code)
        super.init()
    }

    fun showAndGetSelectedCommit() =
        if (showAndGet()) {
            when (myOverrideOption) {
                OverrideOption.PARENT -> parentBranch.pointedCommit
                OverrideOption.INFERRED -> branch.forkPoint
            }
        } else {
            customCommit
        }

    override fun createCenterPanel() = panel {
        row {
            label(
                format(
                    getString(
                        "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.label.HTML"
                    ),
                    branch.name
                )
            )
        }
        row {
            label(
                format(
                    getString(
                        "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.radio-button.parent"
                    ),
                    parentBranch.name
                )
            )
        }

        row {
            label(
                format(
                    getString(
                        "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.radio-button.inferred"
                    )
                )
            )
        }

        row("The fork point commit:") {
            comboBox(
                branch.commits.toMutableList(),
                object : DefaultListCellRenderer() {
                    private val defaultBackground = UIManager.get("List.background") as Color
                    override fun getListCellRendererComponent(
                        list: JList<*>?,
                        value: Any,
                        index: Int,
                        isSelected: Boolean,
                        cellHasFocus: Boolean
                    ): Component {
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                        val commit: ICommitOfManagedBranch = value as ICommitOfManagedBranch

                        var prefix = ""

                        if (parentBranch.pointedCommit.shortHash.equals(commit.shortHash)) {
                            prefix = "parent pointed "
                        } else if (branch.forkPoint != null && branch.forkPoint?.shortHash.equals(commit.shortHash)) {
                            prefix = "branch fork point "
                        }
                        text = "$prefix[${commit.shortHash}] [${commit.shortMessage}]"

                        if (!isSelected) {
                            setBackground(if (index % 2 == 0) background else defaultBackground)
                        }
                        return this
                    }
                }

            ).bindItem(
                MutableProperty(::customCommit) {
                    customCommit = it as IForkPointCommitOfManagedBranch?
                }
            )
        }
    }
}
