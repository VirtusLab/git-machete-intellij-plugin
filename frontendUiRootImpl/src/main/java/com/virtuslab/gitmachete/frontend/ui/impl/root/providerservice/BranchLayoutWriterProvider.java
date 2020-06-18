package com.virtuslab.gitmachete.frontend.ui.impl.root.providerservice;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.Getter;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutWriter;

@Service
public final class BranchLayoutWriterProvider {

  @Getter
  private final IBranchLayoutWriter branchLayoutWriter;

  public BranchLayoutWriterProvider(Project project) {
    this.branchLayoutWriter = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutWriter.class);
  }
}
