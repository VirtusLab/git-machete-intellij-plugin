package com.virtuslab.gitmachete.buildsrc

fun getFlagsForAddExports(vararg packages: String, module: String): List<String> = packages.map { "--add-exports=$module/$it=ALL-UNNAMED" }

fun getFlagsForAddOpens(vararg packages: String, module: String): List<String> = packages.map { "--add-opens=$module/$it=ALL-UNNAMED" }
