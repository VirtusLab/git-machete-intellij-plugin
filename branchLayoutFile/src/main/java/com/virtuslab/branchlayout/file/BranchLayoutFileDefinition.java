package com.virtuslab.branchlayout.file;

import java.nio.file.Path;

import lombok.Data;

@Data
public class BranchLayoutFileDefinition {
  final Path path;
  Character indentCharacter = null;
  int levelWidth = 0;
}
