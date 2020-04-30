package com.virtuslab.branchlayout.api.manager;

import java.nio.file.Path;

public interface IBranchLayoutManagerFactory {
  IBranchLayoutManager create(Path path);
}
