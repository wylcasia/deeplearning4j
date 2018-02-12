package org.deeplearning4j.clustering.randomprojection;

import lombok.Builder;
import lombok.Data;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.memory.enums.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.omg.SendingContext.RunTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Data
public class RPTree {
    private RPNode root;
    private RPHyperPlanes rpHyperPlanes;
    private int dim;
    //also knows as leave size
    private int maxSize;
    private INDArray X;
    private String similarityFunction = "euclidean";
    private WorkspaceConfiguration workspaceConfiguration;
    private ExecutorService searchExecutor;
    private int searchWorkers;

    /**
     *
     * @param dim the dimension of the vectors
     * @param maxSize the max size of the leaves
     *
     */
    @Builder
    public RPTree(int dim, int maxSize,String similarityFunction) {
        this.dim = dim;
        this.maxSize = maxSize;
        rpHyperPlanes = new RPHyperPlanes(dim);
        root = new RPNode(this,0);
        this.similarityFunction = similarityFunction;
        workspaceConfiguration = WorkspaceConfiguration.builder().cyclesBeforeInitialization(1)
                .policyAllocation(AllocationPolicy.STRICT).policyLearning(LearningPolicy.FIRST_LOOP)
                .policyMirroring(MirroringPolicy.FULL).policyReset(ResetPolicy.BLOCK_LEFT)
                .policySpill(SpillPolicy.REALLOCATE).build();

    }

    /**
     *
     * @param dim the dimension of the vectors
     * @param maxSize the max size of the leaves
     *
     */
    public RPTree(int dim, int maxSize) {
       this(dim,maxSize,"euclidean");
    }


    public void buildTree(INDArray x) {
        this.X = x;
        for(int i = 0; i < x.rows(); i++) {
            root.getIndices().add(i);
        }


        if (searchExecutor == null) {

            searchExecutor = Executors.newFixedThreadPool(searchWorkers, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);

                    t.setDaemon(true);
                    t.setName("VPTree thread");

                    // we don't want threads to be working on different devices
                    Nd4j.getAffinityManager().attachThreadToDevice(t,
                            Nd4j.getAffinityManager().getDeviceForCurrentThread());

                    return t;
                }
            });

            }

        // opening workspace, and creating it if that's the first call
        MemoryWorkspace workspace =
                Nd4j.getWorkspaceManager().getAndActivateWorkspace(workspaceConfiguration, "RPTREE_WORSKPACE");


        RPUtils.buildTree(this,root,rpHyperPlanes,
                x,maxSize,0,similarityFunction);

        // closing workspace
        workspace.notifyScopeLeft();
    }



    public void addNodeAtIndex(int idx,INDArray toAdd) {
        RPNode query = RPUtils.query(root,rpHyperPlanes,toAdd,similarityFunction);
        query.getIndices().add(idx);
    }


    public List<RPNode> getLeaves() {
        List<RPNode> nodes = new ArrayList<>();
        RPUtils.scanForLeaves(nodes,getRoot());
        return nodes;
    }


    public INDArray query(INDArray query,int numResults) {
        return RPUtils.queryAll(query,X,Arrays.asList(this),numResults,similarityFunction);
    }

    public List<Integer> getCandidates(INDArray target) {
        return RPUtils.getCandidates(target,Arrays.asList(this),similarityFunction);
    }


}
