package com.virtuslab.gitmachete.test;

import com.virtuslab.gitmachete.backendroot.GitFactoryModule;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import java.nio.file.Paths;
import java.util.Optional;

public class Main {
  public static void main(String[] argv) throws Exception {
    IGitMacheteRepository repo = null;

    try {
      repo =
          GitFactoryModule.getInjector()
              .getInstance(GitMacheteRepositoryFactory.class)
              .create(
                  Paths.get(
                      "/tmp/machete-tests/machete-sandbox" /*System.getProperty("user.home"), "machete-sandbox"*/),
                  Optional.empty());
    } catch (GitMacheteException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }

    var branches = repo.getRootBranches();

    for (var b : branches) {
      if (b.getName().equals("develop")) {
        for (var bb : b.getBranches()) {
          if (bb.getName().equals("allow-ownership-link")) {
            var fp = bb.getCoreLocalBranch().getForkPoint();
            System.out.println(fp);
          }
        }
      }
    }

    // System.out.println(repo.getSubmoduleRepositories());
  }
}
