import java.util.List;

public interface Branch {
    String getName();

    List<Commit> getCommits();

    List<Branch> getBranches();

    SyncToParentStatus getSyncToParentStatus();

    SyncToOriginStatus getSyncToOriginStatus();
}
