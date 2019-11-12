package com.virtuslab;

/*import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.util.mxCellRenderer;*/
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.ReflogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.revwalk.filter.RevFilter;
/* import org.jgraph.graph.DefaultEdge;
import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.slf4j.impl.StaticLoggerBinder; */

import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] argv) throws IOException, GitAPIException {
        org.eclipse.jgit.lib.Repository repo = new FileRepository(System.getProperty("user.home")+"/simple-test/.git");
        Git git = new Git(repo);
        RevWalk walk = new RevWalk(repo);

        /*List<Ref> branches = git.branchList().call();

        System.out.println(String.valueOf(branches.size()));

        ReflogCommand rf = new ReflogCommand(repo);
        rf.setRef("Test-2");*/

        //BranchTrackingStatus bts = BranchTrackingStatus.of(git.getRepository(), "master");

        //int aheadCount = bts.getAheadCount();
        //int behindCount = bts.getBehindCount();

        //System.out.println("Ahead: "+String.valueOf(aheadCount)+", Behind: "+String.valueOf(behindCount));

        /*if (aheadCount == 0 && behindCount == 0) {
            System.out.println("SAME");
        } else if (aheadCount > 0 && behindCount == 0) {
            System.out.println("AHEAD");
        } else if (aheadCount == 0 && behindCount > 0) {
            System.out.println("BEHIND");
        } else {
            System.out.println("DIVERGED");
        }*/

        /*for(var p : walk.parseCommit(ObjectId.fromString("b604a834e1714ef0345caf1e2b53bcfc32a1ad3a")).getParents()) {
            System.out.println(p);
        }*/


        /*DefaultDirectedGraph<Commit, DefaultEdge> directedGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Commit commit = null;

        for(var com : git.log().all().call()) {
            commit = new Commit(com.getId(), com.getShortMessage());
            directedGraph.addVertex(commit);
        }


        for(var com : git.log().all().call()) {
            commit = new Commit(com.getId(), com.getShortMessage());
            for(var p : com.getParents()) {
                var c = new Commit(p.getId(), p.getShortMessage());
                directedGraph.addEdge(commit, c);
            }
        }


        File iFile = new File("src/main/resources/graph.png");
        iFile.createNewFile();

        JGraphXAdapter<Commit, DefaultEdge> graphAdapter =
                new JGraphXAdapter<>(directedGraph);
        mxIGraphLayout layout = new mxCircleLayout(graphAdapter);
        layout.execute(graphAdapter.getDefaultParent());

        BufferedImage image =
                mxCellRenderer.createBufferedImage(graphAdapter, null, 2, Color.WHITE, true, null);
        File imgFile = new File("src/main/resources/graph.png");
        ImageIO.write(image, "PNG", imgFile);*/




        /*
        //System.out.println(repo.resolve("refs/heads/test1"));

        var rf = new ReflogCommand(repo);


        var list = git.branchList().call();

        var it = list.iterator();

        //var el = it.next();

        //System.out.println(el.getName()+": "+el.getObjectId().getName());


        DefaultDirectedGraph<Commit, DefaultEdge> directedGraph = new DefaultDirectedGraph<>(DefaultEdge.class);


        /*var e = it.next();
        rf.setRef(e.getName());

        var rfrev = new LinkedList<>(rf.call());
        Collections.reverse(rfrev);

        var ref = rfrev.get(0);

        var c = new Commit(ref.getNewId(), walk.parseCommit(ref.getNewId()).getShortMessage());
        directedGraph.addVertex(c);

        ref = rfrev.get(1);

        var d = new Commit(ref.getNewId(), walk.parseCommit(ref.getNewId()).getShortMessage());
        directedGraph.addVertex(d);

        System.out.println(c.equals(d));
        System.out.println(c.hashCode() == d.hashCode());*/


        /*Commit commitNext, commitPrev = null;

        while(it.hasNext()) {
            var e = it.next();
            System.out.println(e.getName()+": "+e.getObjectId().getName());

            rf.setRef(e.getName());

            var rfrev = new LinkedList<>(rf.call());
            Collections.reverse(rfrev);

            for(var rfe : rfrev) {
                //System.out.println(rfe.getOldId()+" "+rfe.getNewId());
                if(!rfe.getNewId().equals(rfe.getOldId())) {
                    commitNext = new Commit(rfe.getNewId(), walk.parseCommit(rfe.getNewId()).getShortMessage());
                    directedGraph.addVertex(commitNext);
                    //if(!rfe.getOldId().equals(ObjectId.zeroId()))
                        //directedGraph.addEdge(commitNext, commitPrev);
                    commitPrev = commitNext;
                }
            }

            //System.out.println("******");

            /*walk = new RevWalk(repo);
            walk.setRevFilter(RevFilter.MERGE_BASE);
            walk.markStart(walk.parseCommit(el.getObjectId()));
            walk.markStart(walk.parseCommit(e.getObjectId()));

            RevCommit c = walk.iterator().next();

            System.out.println("Common commit: "+c.getName()+": "+c.getShortMessage());

            el = e;

            walk.reset();*/
        //}

        /*it = list.iterator();

        while(it.hasNext()) {
            var e = it.next();
            System.out.println(e.getName()+": "+e.getObjectId().getName());

            rf.setRef(e.getName());

            var rfrev = new LinkedList<>(rf.call());
            Collections.reverse(rfrev);

            for(var rfe : rfrev) {
                //System.out.println(rfe.getOldId()+" "+rfe.getNewId());
                if(!rfe.getNewId().equals(rfe.getOldId())) {
                    commitNext = new Commit(rfe.getNewId(), walk.parseCommit(rfe.getNewId()).getShortMessage());
                    if(!rfe.getOldId().equals(ObjectId.zeroId()))
                        directedGraph.addEdge(commitNext, commitPrev);
                    commitPrev = commitNext;
                }
            }
        }


        File iFile = new File("src/main/resources/graph.png");
        iFile.createNewFile();

        JGraphXAdapter<Commit, DefaultEdge> graphAdapter =
                new JGraphXAdapter<>(directedGraph);
        mxIGraphLayout layout = new mxCircleLayout(graphAdapter);
        layout.execute(graphAdapter.getDefaultParent());

        BufferedImage image =
                mxCellRenderer.createBufferedImage(graphAdapter, null, 2, Color.WHITE, true, null);
        File imgFile = new File("src/main/resources/graph.png");
        ImageIO.write(image, "PNG", imgFile);


        /*var rfc = rf.call();

        for(var e : rfc) {
            System.out.println(e.getComment());
            var co = e.parseCheckout();
            if(co != null)
                System.out.println(co.getFromBranch()+" "+co.getToBranch());
        }*/


        System.out.println("---------------------------");

        //walk = new RevWalk(repo);
        walk.setRevFilter(RevFilter.MERGE_BASE);
        //walk.markStart(git.log().add(repo.resolve("Other-1")).call().iterator().next());
        walk.markStart(walk.parseCommit(repo.resolve("Test-3")));
        walk.markStart(walk.parseCommit(repo.resolve("Test-2")));
        //walk.parseCommit(git.log().add(repo.resolve("master")).call().iterator().next());

        /*for(var c : walk) {
            System.out.println(c.toString());
            System.out.println(c.getFullMessage());
        }*/


        /*RevCommit c = walk.iterator().next();
        System.out.println(c.toString());
        System.out.println(c.getFullMessage());

        System.out.println(repo.resolve("Test-2").toObjectId());

        if(c.getName().equals(repo.resolve("Test-2").toObjectId().getName()))
            System.out.println("TRUE");
        else
            System.out.println("FALSE");*/

        /*RevWalk walk2 = new RevWalk(repo);
        walk2.sort(RevSort.REVERSE);
        walk2.markStart(walk2.parseCommit(repo.resolve("HEAD")));

        for(var c2 : walk2) {
            System.out.println(c2.toString());
            System.out.println(c2.getFullMessage());
        }*/

        /*for (ReflogEntry e : rf.call())
        {
            var cho = e.parseCheckout();
            if(cho != null)
                System.out.println("FROM: "+cho.getFromBranch()+", TO: "+cho.getToBranch());
        }*/
    }
}
