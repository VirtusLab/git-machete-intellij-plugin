package com.virtuslab.gitmachete.frontend.actions.common

data class FastForwardMergeProps(
    val branchName: String,
    val branchFullName: String,
    val targetBranchName: String,
    val targetBranchFullName: String
)
