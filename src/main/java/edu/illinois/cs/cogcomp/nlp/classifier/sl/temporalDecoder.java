package edu.illinois.cs.cogcomp.nlp.classifier.sl;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.infer.ilp.GurobiHook;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK.TlinkType;
import edu.illinois.cs.cogcomp.nlp.timeline.GlobalEEClassifierExp;
import edu.illinois.cs.cogcomp.nlp.util.ExecutionTimeUtil;
import edu.illinois.cs.cogcomp.nlp.util.TransitivityTriplets;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;

import java.util.*;

/**
 * Created by qning2 on 12/27/16.
 */
public class temporalDecoder extends AbstractInferenceSolver{
    private static final long serialVersionUID = -3871757972011621103L;
    private tempFeatureGenerator feat;
    public boolean usePrior;
    public boolean verbose = true;
    public int none_edge = Integer.MAX_VALUE;
    public int ignore_edge = 5;
    public int include_edge = 2;
    public boolean sentId_or_not = false;
    public boolean null_none_or_not = true;
    public double[][][] localScores;

    public temporalDecoder (AbstractFeatureGenerator featureGenerator){
        feat = (tempFeatureGenerator) featureGenerator;
        usePrior = true;
    }
    public temporalDecoder (AbstractFeatureGenerator featureGenerator, boolean usePrior){
        feat = (tempFeatureGenerator) featureGenerator;
        this.usePrior = usePrior;
    }

    public IStructure getBestStructure(WeightVector weight,
                                       IInstance ins) throws Exception{
        try {
            return getLossAugmentedBestStructure(weight, ins, null);
        }
        catch (Exception e){
            e.printStackTrace();
            TemporalLog.println("SL_FAILURES",((temporalInstance)ins).doc.getDocID()+" failed.");
            TemporalLog.println("SL_FAILURES",System.err);
            return new temporalStructure((temporalInstance)ins);
        }
    }

    public float getLoss(IInstance ins, IStructure gold, IStructure pred){
        float loss = 0.0f;
        temporalStructure predStruct = (temporalStructure) pred;
        temporalStructure goldStruct = (temporalStructure) gold;
        int nE = predStruct.nE;
        for(int i=0;i<nE;i++){
            for(int j=i+1;j<nE;j++){
                String tmp = predStruct.getRelStr()[i][j];
                String tmp2=  goldStruct.getRelStr()[i][j];
                if(!tmp.equals(tmp2))
                    loss += 1.0f;
            }
        }
        return loss;
    }

    public IStructure getBestLocalStructure(WeightVector weight, IInstance ins)
            throws Exception{
        temporalInstance docInst = (temporalInstance) ins;
        int nE = docInst.nE;
        int nType = TlinkType.values().length;
        /*Step 0: obtain bias scores*/
        double[] bias = new double[nType];
        for(int i=0;i<nType;i++){
            List<String> featureMap = new ArrayList<>();
            featureMap.add("EventIJ:"+TlinkType.values()[i].toStringfull());
            bias[i] = weight.dotProduct(feat.getFeatureFromMap(featureMap));
        }
        /*Step 1: obtain local classifier scores*/
        double[][][] localScores = new double[nE][nE][nType];
        for(int i=0;i<nE;i++){
            for(int j=i+1;j<nE;j++){
                for(int k=0;k<nType;k++){
                    List<String> featureMap = new ArrayList<>();
                    String tt = TlinkType.values()[k].toStringfull();
                    feat.addEventPairFeats(i,j,docInst,tt,featureMap);

                    localScores[i][j][k] = weight.dotProduct(feat.getFeatureFromMap(featureMap))
                            -bias[k];
                }
            }
        }
        /*Step 2: prior knowledge*/
        HashMap<Integer, HashMap<Integer, List<String>>> knownVarsMap = new HashMap<>();
        if(usePrior) {
            knownVarsMap = GlobalEEClassifierExp.getKnownVarsMap(docInst.doc);
        }
        /*Step 3: obtain best local classifier*/
        String[][] rels = new String[nE][nE];
        for(int i=0;i<nE;i++){
            rels[i][i] = TlinkType.EQUAL.toStringfull();
            int eiid1 = docInst.events[i].getEiid();
            for(int j=i+1;j<nE;j++){
                int eiid2 = docInst.events[j].getEiid();
                List<String> possible_rels = new ArrayList<>();
                for(int k=0;k<nType;k++)
                    possible_rels.add(TlinkType.values()[k].toStringfull());
                if(usePrior) {
                    if (knownVarsMap.containsKey(eiid1)
                            && knownVarsMap.get(eiid1).containsKey(eiid2))
                        possible_rels = knownVarsMap.get(eiid1).get(eiid2);
                    if (possible_rels.size() == 1) {
                        rels[i][j] = possible_rels.get(0);
                        rels[j][i] = TlinkType.reverse(rels[i][j]).toStringfull();
                    }
                }
                double max = Double.NEGATIVE_INFINITY;
                int k_max = -1;
                for(int k=0;k<nType;k++){
                    if(!possible_rels.contains(TlinkType.values()[k].toStringfull()))
                        continue;
                    if(localScores[i][j][k]>max){
                        max = localScores[i][j][k];
                        k_max = k;
                    }
                }
                if(k_max==-1) {
                    System.out.println("k_max==-1.");
                    System.exit(-1);
                }
                rels[i][j] = TlinkType.values()[k_max].toStringfull();
                rels[j][i] = TlinkType.reverse(rels[i][j]).toStringfull();
            }
        }
        return new temporalStructure(nE,rels);
    }
    public IStructure getLossAugmentedBestStructure(
            WeightVector weight, IInstance ins, IStructure goldStructure)
            throws Exception{
        temporalInstance docInst = (temporalInstance) ins;
        temporalStructure gold = goldStructure!=null?(temporalStructure) goldStructure
                : null;
        if(verbose)
            System.out.printf("Solving %s...",docInst.doc.getDocID());
        ExecutionTimeUtil timer = new ExecutionTimeUtil();
        timer.start();
        /*----------------Obtain parameters for ILP----------------*/
        int nE = docInst.nE;
        int nType = TlinkType.values().length;
        /*Step 0: obtain bias scores*/
        double[] bias = new double[nType];
        for(int i=0;i<nType;i++){
            List<String> featureMap = new ArrayList<>();
            featureMap.add("EventIJ:"+TlinkType.values()[i].toStringfull());
            bias[i] = weight.dotProduct(feat.getFeatureFromMap(featureMap));
        }
        /*Step 1: obtain local classifier scores*/
        double[][][] localScores = new double[nE][nE][nType];
        for(int i=0;i<nE;i++){
            for(int j=i+1;j<nE;j++){
                double[] normScores = new double[nType];
                double sum = 0;
                for(int k=0;k<nType;k++){
                    List<String> featureMap = new ArrayList<>();
                    String tt = TlinkType.values()[k].toStringfull();
                    feat.addEventPairFeats(i,j,docInst,tt,featureMap);

                    double score = weight.dotProduct(feat.getFeatureFromMap(featureMap))
                            -bias[k];
                    localScores[i][j][k] = score;
                    /*localScores[i][j][k] = weight.dotProduct(feat.getFeatureFromMap(featureMap))
                                            -bias[k];*/
                }
                /*Normalize localScores using soft-max*/
                if(null_none_or_not){
                    double offset = Double.NEGATIVE_INFINITY;
                    for(int k=0;k<nType-1;k++) {
                        if(localScores[i][j][k]>offset){
                            offset = localScores[i][j][k];
                        }
                    }
                    for (int k = 0; k < nType-1; k++) {
                        double tmp = Math.exp(localScores[i][j][k] - offset);
                        normScores[k] = tmp;
                        sum += tmp;
                    }
                    for (int k = 0; k < nType-1; k++) {
                        localScores[i][j][k] = normScores[k] / sum;
                        /*Sanity check*/
                        if (Double.isInfinite(localScores[i][j][k])
                                || Double.isNaN(localScores[i][j][k]))
                            System.out.printf("localScores[%d][%d][%d] is invalid.\n", i, j, k);
                    }
                    localScores[i][j][nType-1] = 0;
                }
                else {
                    double offset = Double.NEGATIVE_INFINITY;
                    for(int k=0;k<nType;k++) {
                        if(localScores[i][j][k]>offset){
                            offset = localScores[i][j][k];
                        }
                    }
                    for (int k = 0; k < nType; k++) {
                        double tmp = Math.exp(localScores[i][j][k] - offset);
                        normScores[k] = tmp;
                        sum += tmp;
                    }
                    for (int k = 0; k < nType; k++) {
                        localScores[i][j][k] = normScores[k] / sum;
                        /*Sanity check*/
                        if (Double.isInfinite(localScores[i][j][k])
                                || Double.isNaN(localScores[i][j][k]))
                            System.out.printf("localScores[%d][%d][%d] is invalid.\n", i, j, k);
                    }
                }
            }
        }
        this.localScores = localScores;

        /*Step 2: obtain relation chain scores*/
        double[][][][][] chainScores = new double[nType][nType][nType][nType][nType];
        for(int i_prev=0;i_prev<nType;i_prev++){
            for(int i_next=0;i_next<nType;i_next++){
                for(int j_prev=0;j_prev<nType;j_prev++){
                    for(int j_next=0;j_next<nType;j_next++){
                        double[] normScores = new double[nType];
                        double sum = 0;
                        for(int ij=0;ij<nType;ij++){
                            List<String> featureMap = new ArrayList<>();
                            String tmp1 = "EventI_prev:"+TlinkType.values()[i_prev].toStringfull();
                            String tmp2 = "EventI_next:"+TlinkType.values()[i_next].toStringfull();
                            String tmp3 = "EventJ_prev:"+TlinkType.values()[j_prev].toStringfull();
                            String tmp4 = "EventJ_next:"+TlinkType.values()[j_next].toStringfull();
                            String tmp5 = "EventIJ:"+TlinkType.values()[ij].toStringfull();
                            featureMap.add(tmp1+"&"+tmp2+"&"+tmp3+"&"+tmp4+"&"+tmp5);
                            chainScores[i_prev][i_next][j_prev][j_next][ij] = weight.dotProduct(feat.getFeatureFromMap(featureMap));
                            normScores[ij] = Math.exp(chainScores[i_prev][i_next][j_prev][j_next][ij]);
                            sum += normScores[ij];
                        }
                        /*Normalize localScores using soft-max*/
                        for(int ij=0;ij<nType;ij++) {
                            normScores[ij] /= sum;
                            chainScores[i_prev][i_next][j_prev][j_next][ij] = normScores[ij];
                        }
                    }
                }
            }
        }

        /*Step 3: obtain baseline scores*/
        double[] normScores = new double[nType];
        double sum = 0;
        double[] baseScores = new double[nType];
        for(int i=0;i<nType;i++){
            List<String> featureMap = new ArrayList<>();
            featureMap.add("EventIJ:"+TlinkType.values()[i].toStringfull());
            baseScores[i] = weight.dotProduct(feat.getFeatureFromMap(featureMap));
            normScores[i] = Math.exp(baseScores[i]);
            sum += normScores[i];
        }
        for(int i=0;i<nType;i++){
            normScores[i] /= sum;
            baseScores[i] = normScores[i];
        }
        /*Step 4: use priori info*/
        HashMap<Integer, HashMap<Integer, List<String>>> knownVarsMap = new HashMap<>();
        HashMap<Integer, List<Integer>> ignoreVarsMap = new HashMap<>();
        if(usePrior) {
            knownVarsMap = GlobalEEClassifierExp.getKnownVarsMap(docInst.doc,true);
        }
        else{
            /*pred knownVarsMap*/
            HashMap<TlinkType,Pair<Integer,Integer>> reliableRange = new HashMap<>();
            reliableRange.put(TlinkType.BEFORE,new Pair<Integer,Integer>(0,none_edge));
            reliableRange.put(TlinkType.AFTER,new Pair<Integer,Integer>(0,none_edge));
            reliableRange.put(TlinkType.INCLUDES,new Pair<Integer,Integer>(0,include_edge));
            reliableRange.put(TlinkType.IS_INCLUDED,new Pair<Integer,Integer>(0,include_edge));
            reliableRange.put(TlinkType.EQUAL,new Pair<Integer,Integer>(0,none_edge));
            reliableRange.put(TlinkType.UNDEF,new Pair<Integer,Integer>(0,Integer.MAX_VALUE));
            List<EventChunk> bodyEvents = docInst.doc.getBodyEventMentions();
            for (EventChunk ec1 : bodyEvents) {
                int id1 = bodyEvents.indexOf(ec1);
                int sentId1 = docInst.doc.getSentId(ec1);
                if(!knownVarsMap.containsKey(id1))
                    knownVarsMap.put(id1, new HashMap<>());
                for (EventChunk ec2 : bodyEvents) {
                    if (ec1 == ec2)
                        continue;
                    int id2 = bodyEvents.indexOf(ec2);
                    int sentId2 = docInst.doc.getSentId(ec2);
                    int diff = sentId_or_not? Math.abs(sentId1-sentId2) : Math.abs(id1-id2);
                    Set<TlinkType> types = reliableRange.keySet();
                    if(types.size()==0) {
                        System.out.println("reliableRange.keySet.size()==0!");
                        System.exit(-1);
                    }
                    List<String> tmp = new ArrayList<>();
                    for(TlinkType type:types){
                        int min_dist = reliableRange.get(type).getFirst();
                        int max_dist = reliableRange.get(type).getSecond();
                        if(diff>=min_dist&&diff<=max_dist){
                            tmp.add(type.toStringfull());
                        }
                    }
                    if(tmp.size()>0)
                        knownVarsMap.get(id1).put(id2,tmp);
                }
            }
            /*Ignore maps*/
            for (EventChunk ec1 : bodyEvents) {
                int id1 = bodyEvents.indexOf(ec1);
                int sentId1 = docInst.doc.getSentId(ec1);
                ignoreVarsMap.put(id1, new ArrayList<>());
                for (EventChunk ec2 : bodyEvents) {
                    if (ec1 == ec2)
                        continue;
                    int id2 = bodyEvents.indexOf(ec2);
                    int sentId2 = docInst.doc.getSentId(ec2);
                    int diff = sentId_or_not? Math.abs(sentId1-sentId2) : Math.abs(id1-id2);
                    if (diff <= none_edge && diff >= ignore_edge)
                        ignoreVarsMap.get(id1).add(id2);
                }
            }
        }

        /*----------------Use Gurobi to solve ILP----------------*/
        GurobiHook solver = new GurobiHook();
        HashMap<Integer,HashMap<Integer,HashMap<String,Integer>>> eeVar = new HashMap<>();

        /*Step 1: Add variable*/
        /*Now we haven't impose chain relations*/
        for(int i=0;i<nE;i++){
            eeVar.put(i,new HashMap<>());
            for(int j=i+1;j<nE;j++){
                if (ignoreVarsMap != null
                        && ignoreVarsMap.containsKey(i)
                        && ignoreVarsMap.get(i).contains(j)) {
                    continue;
                }
                eeVar.get(i).put(j,new HashMap<>());
                for(int k=0;k<nType;k++){
                    int var = solver.addBooleanVariable(localScores[i][j][k]);
                    eeVar.get(i).get(j).put(TlinkType.values()[k].toStringfull(),var);
                }
            }
            for(int j=0;j<i;j++)//j=i is skipped on purpose
            {
                if (ignoreVarsMap != null
                        && ignoreVarsMap.containsKey(i)
                        && ignoreVarsMap.get(i).contains(j)) {
                    continue;
                }
                eeVar.get(i).put(j,new HashMap<>());
                for(int k=0;k<nType;k++){
                    int var = solver.addBooleanVariable(0);//if j<i, don't care the weight
                    eeVar.get(i).get(j).put(TlinkType.values()[k].toStringfull(),var);
                }
            }
        }
        /*Step 2: Add uniqueness constraints*/
        for(int i=0;i<nE;i++){
            for(int j=0;j<nE;j++){
                if(i==j)
                    continue;
                if (ignoreVarsMap != null
                        && ignoreVarsMap.containsKey(i)
                        && ignoreVarsMap.get(i).contains(j)) {
                    continue;
                }
                int[] vars = new int[nType];
                double[] coefs = new double[nType];
                for(int k=0;k<nType;k++){
                    vars[k] = eeVar.get(i).get(j).get(TlinkType.values()[k].toStringfull());
                    coefs[k] = 1;
                }
                solver.addEqualityConstraint(vars,coefs,1);
            }
        }
        /*Step 3: Add symmetry constraints*/
        for(int i=0;i<nE;i++){
            for(int j=0;j<nE;j++){
                if(i==j)
                    continue;
                if (ignoreVarsMap != null
                        && ignoreVarsMap.containsKey(i)
                        && ignoreVarsMap.get(i).contains(j)) {
                    continue;
                }
                int[] vars = new int[2];
                double[] coefs = new double[]{1,-1};
                for(int k=0;k<nType;k++){
                    TlinkType tt = TlinkType.values()[k];
                    vars[0] = eeVar.get(i).get(j).get(tt.toStringfull());
                    vars[1] = eeVar.get(j).get(i).get(tt.reverse().toStringfull());
                    solver.addEqualityConstraint(vars,coefs,0);
                }
            }
        }
        /*Step 4:Add transitivity constraints*/
        for(int i=0;i<nE;i++){
            for(int j=0;j<nE;j++){
                if(i==j)
                    continue;
                if (ignoreVarsMap != null
                        && ignoreVarsMap.containsKey(i)
                        && ignoreVarsMap.get(i).contains(j)) {
                    continue;
                }
                for(int m=0;m<nE;m++){
                    if(i==m||j==m)
                        continue;
                    int eiid3 = docInst.events[m].getEiid();
                    if (ignoreVarsMap != null
                            && ignoreVarsMap.containsKey(i)
                            && ignoreVarsMap.get(i).contains(m)) {
                        continue;
                    }
                    if (ignoreVarsMap != null
                            && ignoreVarsMap.containsKey(j)
                            && ignoreVarsMap.get(j).contains(m)) {
                        continue;
                    }
                    List<TransitivityTriplets> transTriplets = TransitivityTriplets.transTriplets();
                    for(TransitivityTriplets triplet:transTriplets){
                        int n = triplet.getThird().length;
                        double[] coefs = new double[n+2];
                        int[] vars = new int[n+2];
                        coefs[0] = 1;
                        coefs[1] = 1;
                        vars[0] = eeVar.get(i).get(j).get(triplet.getFirst().toStringfull());
                        vars[1] = eeVar.get(j).get(m).get(triplet.getSecond().toStringfull());
                        for(int k=0;k<n;k++) {
                            coefs[k+2] = -1;
                            vars[k+2] = eeVar.get(i).get(m).get(triplet.getThird()[k].toStringfull());
                        }
                        solver.addLessThanConstraint(vars,coefs,1);
                    }
                }
            }
        }
        /*Step 5: Use knownVarsMap information*/
        if(knownVarsMap!=null) {
            for (int id1 : knownVarsMap.keySet()) {
                for (int id2 : knownVarsMap.get(id1).keySet()) {
                    if (id1 == id2)
                        continue;
                    if (ignoreVarsMap != null
                            && ignoreVarsMap.containsKey(id1)
                            && ignoreVarsMap.get(id1).contains(id2)) {
                        continue;
                    }
                    int n = knownVarsMap.get(id1).get(id2).size();
                    /*int i = docInst.doc.getIndexFromEIID(id1);//id1 is already index, not eiid anymore.
                    int j = docInst.doc.getIndexFromEIID(id2);*/
                    int i = id1;
                    int j = id2;
                    int[] vars = new int[n];
                    double[] coefs = new double[n];
                    int cnt = 0;
                    for (String str : knownVarsMap.get(id1).get(id2)) {
                        vars[cnt] = eeVar.get(i).get(j).get(str);
                        coefs[cnt] = 1;
                        cnt++;
                    }
                    solver.addEqualityConstraint(vars, coefs, 1);
                }
            }
        }

        solver.setMaximize(true);
        boolean solved = false;
        try{
            solved = solver.solve();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        /*----------------Obtain temporalStructure----------------*/
        String[][] rels = new String[nE][nE];
        for(int i=0;i<nE;i++){
            for(int j=0;j<nE;j++){
                if(i==j)
                    continue;
                if (ignoreVarsMap != null
                        && ignoreVarsMap.containsKey(i)
                        && ignoreVarsMap.get(i).contains(j)) {
                    rels[i][j] = TlinkType.UNDEF.toStringfull();
                    continue;
                }
                if(solved) {
                    for (int k = 0; k < nType; k++) {
                        int var = eeVar.get(i).get(j).get(TlinkType.values()[k].toStringfull());
                        if (solver.getBooleanValue(var)) {
                            rels[i][j] = TlinkType.values()[k].toStringfull();
                        }
                    }
                }
                else{
                    rels[i][j] = TlinkType.UNDEF.toStringfull();
                }
            }
        }
        if(!solved)
            System.out.println("Gurobi failed. ");
        timer.end();
        if(verbose)
            timer.print();
        return new temporalStructure(nE,rels);
    }

    public void setEdges(int none_edge,int ignore_edge, int include_edge){
        this.none_edge = none_edge;
        this.ignore_edge = ignore_edge;
        this.include_edge = include_edge;
    }

    @Override
    public Object clone(){
        return new temporalDecoder(feat);
    }
}
