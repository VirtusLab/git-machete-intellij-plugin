package com.virtuslab.gitmachete.gitmachetejgit;

import java.nio.file.Path;

public interface GitMacheteLoaderFactory {
    GitMacheteLoader create(Path pathToRoot);
}
