package edu.illinois.cs.cogcomp.nlp.timeline;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.infer.ilp.GurobiHook;
import edu.illinois.cs.cogcomp.lbjava.classify.ScoreSet;
import edu.illinois.cs.cogcomp.lbjava.learn.Softmax;
import edu.illinois.cs.cogcomp.nlp.classifier.FeatureExtractor;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.*;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ee.ee_perceptron;
import edu.illinois.cs.cogcomp.nlp.classifier.my_ee_perceptron;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.EventStruct;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.ReaderConfigurator;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK.TlinkType;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.timeline.test.TwoClassifiers;
import edu.illinois.cs.cogcomp.nlp.util.*;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by qning2 on 12/20/16.
 */
public class GlobalEEClassifierExp {
    private ScoringFunc ee_scorer;
    private List<TemporalDocument> docs;
    public HashMap<String, HashMap<Integer, List<Integer>>> ignoreVarsMap;
    public HashMap<String, HashMap<Integer, HashMap<Integer, List<String>>>> knownVarsMap;
    public HashMap<String, HashMap<Integer, HashMap<Integer, double[]>>> knownScoresMap;
    public HashMap<String, Double> baseline_prob;
    public HashMap<String, HashMap<String, Double>> chain_prob;//not very useful
    private PrecisionRecallManager evaluator = new PrecisionRecallManager();
    public static boolean knownNONEs = false;
    public static boolean useBaselineProb = false;
    public boolean serialize = true;
    public String cacheDir = "serialized_data/McNemar-known/global/";
    public boolean force_update = true;
    public double kl_threshold = 0;
    public boolean sentDistFilter = false;

    public GlobalEEClassifierExp(List<TemporalDocument> docs, ScoringFunc ee_scorer){
        this.docs = docs;
        this.ee_scorer = ee_scorer;
        ignoreVarsMap = null;
        knownVarsMap = null;
        knownScoresMap = null;
        baseline_prob = null;
        chain_prob = null;
    }
    public GlobalEEClassifierExp(List<TemporalDocument> docs, String modelDirPath, String modelName) {
        String modelPath;
        String modelLexPath;
        modelPath = modelDirPath + modelName + ".lc";
        modelLexPath = modelDirPath + modelName + ".lex";
        ee_scorer = new my_ee_perceptron(new ee_perceptron(modelPath, modelLexPath));
        this.docs = docs;
        ignoreVarsMap = null;
        knownVarsMap = null;
        knownScoresMap = null;
        baseline_prob = null;
        chain_prob = null;
    }

    /*solveDoc() hard copies tlinks*/
    public TemporalDocument solveDoc(TemporalDocument doc) throws Exception {
        TemporalDocument predDoc;
        if(!force_update) {
            predDoc = TemporalDocument.deserialize(cacheDir, doc.getDocID(), true);
            if (predDoc != null)
                return predDoc;
        }
        GurobiHook solver = new GurobiHook();
        FeatureExtractor featureExtractor = new FeatureExtractor(doc);
        HashMap<Integer, HashMap<Integer, HashMap<String, Integer>>> eeVar = new HashMap<>();
        HashMap<Integer, HashMap<String, HashMap<String, Integer>>> chainVar = new HashMap<>();
        HashMap<Integer, List<Integer>> ignoreVar = (ignoreVarsMap != null && ignoreVarsMap.containsKey(doc.getDocID())) ?
                ignoreVarsMap.get(doc.getDocID()) : null;
        HashMap<Integer, HashMap<Integer, List<String>>> knownVar = (knownVarsMap != null && knownVarsMap.containsKey(doc.getDocID())) ?
                knownVarsMap.get(doc.getDocID()) : null;
        HashMap<Integer, HashMap<Integer, double[]>> knownScore = (knownScoresMap != null && knownScoresMap.containsKey(doc.getDocID())) ?
                knownScoresMap.get(doc.getDocID()) : null;
        HashMap<Integer, List<Integer>> small_kl = new HashMap<>();
        /*Add variable*/
        Softmax sm = new Softmax();
        for (EventChunk ec1 : doc.getBodyEventMentions()) {
            int id1 = doc.getBodyEventMentions().indexOf(ec1);
            if (!eeVar.containsKey(id1)) {
                eeVar.put(id1, new HashMap<>());
            }
            for (EventChunk ec2 : doc.getBodyEventMentions()) {
                if (ec1.equals(ec2))
                    continue;
                int id2 = doc.getBodyEventMentions().indexOf(ec2);
                if (ignoreVar != null
                        && ignoreVar.containsKey(id1)
                        && ignoreVar.get(id1).contains(id2)) {
                    continue;
                }
                if (!eeVar.get(id1).containsKey(id2))
                    eeVar.get(id1).put(id2, new HashMap<>());

                ScoreSet normScores;
                if(knownScore!=null
                        &&knownScore.containsKey(ec1.getEiid())
                        &&knownScore.get(ec1.getEiid()).containsKey(ec2.getEiid())){
                    double[] prior_scores = knownScore.get(ec1.getEiid()).get(ec2.getEiid());
                    if(prior_scores.length!=TlinkType.values().length) {
                        System.out.println("knownScoresMap provided to GlobalEEClassifierExp is not valid. (length of double[] is not consistent with TlinkType)");
                        System.exit(-1);
                    }
                    String[] prior_values = new String[TlinkType.values().length];
                    for(int i=0;i<TlinkType.values().length;i++){
                        prior_values[i] = TlinkType.values()[i].toStringfull();
                    }
                    normScores = new ScoreSet(prior_values,prior_scores);
                }
                else {
                    String feat = featureExtractor.getFeatureString(featureExtractor.extractEEfeats(ec1, ec2), "test");//@@fix later
                    int pos = feat.lastIndexOf(ParamLBJ.FEAT_DELIMITER);
                    String goldLabel = feat.substring(pos + 1).trim();
                    LearningObj obj = new LearningObj(feat.substring(0, pos).trim(), goldLabel);
                    ScoreSet scores = ee_scorer.scores(obj);
                    normScores = sm.normalize(scores);
                }

                Set vals = normScores.values();
                double[] p = new double[vals.size()];
                int cnt = 0;
                for (Object val : vals) {
                    double offset = (baseline_prob != null && baseline_prob.containsKey((String) val)) ? baseline_prob.get(val) : 0;
                    int var = solver.addBooleanVariable(normScores.get((String) val) + offset);
                    eeVar.get(id1).get(id2).put((String) val, var);

                    p[cnt++] = normScores.get((String) val);
                }
                //System.out.println(KLDiv.kldivergence(p));
                if(KLDiv.kldivergence(p)<kl_threshold){// kl_div too small
                    if(!small_kl.containsKey(id1))
                        small_kl.put(id1,new ArrayList<Integer>());
                    small_kl.get(id1).add(id2);
                }
                if (!vals.contains(TLINK.TlinkType.UNDEF.toStringfull())) {
                    int var = solver.addBooleanVariable(0);
                    eeVar.get(id1).get(id2).put(TLINK.TlinkType.UNDEF.toStringfull(), var);
                }
            }
        }
        if (chain_prob != null) {
            int nType = TlinkType.values().length;
            for (int ne = 1; ne < doc.getBodyEventMentions().size() - 1; ne++) {
                int eiid1 = doc.getBodyEventMentions().get(ne - 1).getEiid();
                int eiid2 = doc.getBodyEventMentions().get(ne).getEiid();
                int eiid3 = doc.getBodyEventMentions().get(ne + 1).getEiid();
                if (ignoreVar != null
                        && ignoreVar.containsKey(ne-1)
                        && ignoreVar.get(ne-1).contains(ne)) {
                    continue;
                }
                if (ignoreVar != null
                        && ignoreVar.containsKey(ne)
                        && ignoreVar.get(ne).contains(ne+1)) {
                    continue;
                }
                chainVar.put(ne, new HashMap<>());
                for (int k1 = 0; k1 < nType; k1++) {
                    String tt1 = TlinkType.values()[k1].toStringfull();
                    chainVar.get(ne).put(tt1, new HashMap<>());
                    for (int k2 = 0; k2 < nType; k2++) {
                        /*Add variable*/
                        String tt2 = TlinkType.values()[k2].toStringfull();
                        int var = solver.addBooleanVariable(chain_prob.get(tt1).get(tt2));
                        chainVar.get(ne).get(tt1).put(tt2, var);

                        /*Add constraints*/
                        int[] vars = new int[3];
                        double[] coefs = new double[]{1, 1, -1};
                        vars[0] = eeVar.get(ne-1).get(ne).get(tt1);
                        vars[1] = eeVar.get(ne).get(ne+1).get(tt2);
                        vars[2] = var;
                        solver.addLessThanConstraint(vars, coefs, 1);
                    }
                }
            }

        }
        /*Use knownVar information*/
        if (knownVar != null) {
            for (int id1 : knownVar.keySet()) {
                for (int id2 : knownVar.get(id1).keySet()) {
                    if (id1 == id2)
                        continue;
                    if (ignoreVar != null
                            && ignoreVar.containsKey(id1)
                            && ignoreVar.get(id1).contains(id2)) {
                        continue;
                    }
                    int n = knownVar.get(id1).get(id2).size();
                    int[] vars = new int[n];
                    double[] coefs = new double[n];
                    int i = 0;
                    for (String str : knownVar.get(id1).get(id2)) {
                        vars[i] = eeVar.get(id1).get(id2).get(str);
                        coefs[i] = 1;
                        i++;
                    }
                    solver.addEqualityConstraint(vars, coefs, 1);
                }
            }
        }
        /*Add uniqueness constraints*/
        for (EventChunk ec1 : doc.getBodyEventMentions()) {
            int id1 = doc.getBodyEventMentions().indexOf(ec1);
            for (EventChunk ec2 : doc.getBodyEventMentions()) {
                int id2 = doc.getBodyEventMentions().indexOf(ec2);
                if (ec1.equals(ec2))
                    continue;
                if (ignoreVar != null
                        && ignoreVar.containsKey(id1)
                        && ignoreVar.get(id1).contains(id2)) {
                    continue;
                }
                Set<String> rels = eeVar.get(id1).get(id2).keySet();
                int n = rels.size();
                int[] vars = new int[n];
                double[] coefs = new double[n];
                int i = 0;
                for (String rel : rels) {
                    vars[i] = eeVar.get(id1).get(id2).get(rel);
                    coefs[i] = 1;
                    i++;
                }
                solver.addEqualityConstraint(vars, coefs, 1);
            }
        }
        /*Add symmetry constraints*/
        for (EventChunk ec1 : doc.getBodyEventMentions()) {
            int id1 = doc.getBodyEventMentions().indexOf(ec1);
            for (EventChunk ec2 : doc.getBodyEventMentions()) {
                int id2 = doc.getBodyEventMentions().indexOf(ec2);
                if (ec1.equals(ec2))
                    continue;
                if (ignoreVar != null
                        && ignoreVar.containsKey(id1)
                        && ignoreVar.get(id1).contains(id2)) {
                    continue;
                }
                Set<String> rels = eeVar.get(id1).get(id2).keySet();
                double[] coefs = new double[]{1, -1};
                int[] vars = new int[2];
                for (String rel : rels) {
                    vars[0] = eeVar.get(id1).get(id2).get(rel);
                    vars[1] = eeVar.get(id2).get(id1).get(TLINK.TlinkType.reverse(rel).toStringfull());
                    solver.addEqualityConstraint(vars, coefs, 0);
                }
            }
        }
        /*Add transitivity constraints*/
        for (EventChunk ec1 : doc.getBodyEventMentions()) {
            int id1 = doc.getBodyEventMentions().indexOf(ec1);
            for (EventChunk ec2 : doc.getBodyEventMentions()) {
                int id2 = doc.getBodyEventMentions().indexOf(ec2);
                if (ec1.equals(ec2))
                    continue;
                if (ignoreVar != null
                        && ignoreVar.containsKey(id1)
                        && ignoreVar.get(id1).contains(id2)) {
                    continue;
                }
                for (EventChunk ec3 : doc.getBodyEventMentions()) {
                    int id3 = doc.getBodyEventMentions().indexOf(ec3);
                    if (ec1.equals(ec3))
                        continue;
                    if (ec2.equals(ec3))
                        continue;
                    if (ignoreVar != null
                            && ignoreVar.containsKey(id1)
                            && ignoreVar.get(id1).contains(id3)) {
                        continue;
                    }
                    if (ignoreVar != null
                            && ignoreVar.containsKey(id2)
                            && ignoreVar.get(id2).contains(id3)) {
                        continue;
                    }
                    List<TransitivityTriplets> transTriplets = TransitivityTriplets.transTriplets();
                    for (TransitivityTriplets triplet : transTriplets) {
                        int n = triplet.getThird().length;
                        double[] coefs = new double[n + 2];
                        int[] vars = new int[n + 2];
                        coefs[0] = 1;
                        coefs[1] = 1;
                        vars[0] = eeVar.get(id1).get(id2).get(triplet.getFirst().toStringfull());
                        vars[1] = eeVar.get(id2).get(id3).get(triplet.getSecond().toStringfull());
                        for (int i = 0; i < n; i++) {
                            coefs[i + 2] = -1;
                            vars[i + 2] = eeVar.get(id1).get(id3).get(triplet.getThird()[i].toStringfull());
                        }
                        solver.addLessThanConstraint(vars, coefs, 1);
                    }
                }
            }
        }

        solver.setMaximize(true);
        solver.solve();

        predDoc = new TemporalDocument(doc);
        predDoc.setBodyTlinks(null);//reset TLINKs
        List<TLINK> predTlinks = new ArrayList<>();
        int lid = 0;
        for (EventChunk ec1 : doc.getBodyEventMentions()) {
            int id1 = doc.getBodyEventMentions().indexOf(ec1);
            for (EventChunk ec2 : doc.getBodyEventMentions()) {
                int id2 = doc.getBodyEventMentions().indexOf(ec2);
                if (ec1.equals(ec2))
                    continue;
                if (ignoreVar != null
                        && ignoreVar.containsKey(id1)
                        && ignoreVar.get(id1).contains(id2)) {
                    continue;
                }
                if(small_kl.containsKey(id1)&&small_kl.get(id1).contains(id2))
                    continue;
                Set<String> rels = eeVar.get(id1).get(id2).keySet();
                int var;
                String result = "";
                int cnt = 0;
                for (String rel : rels) {
                    var = eeVar.get(id1).get(id2).get(rel);
                    if (solver.getBooleanValue(var)) {
                        result = rel;
                        cnt++;
                    }
                }
                if (cnt > 1)
                    System.exit(-1);
                if (result.toLowerCase().equals("undef"))//don't store undefined tlinks
                    continue;
                TLINK tmp = new TLINK(lid, "", TempEval3Reader.Type_Event, TempEval3Reader.Type_Event, ec1.getEiid(), ec2.getEiid(), TLINK.TlinkType.str2TlinkType(result));
                lid++;
                predTlinks.add(tmp);
            }
        }
        //predTlinks.addAll(doc.getETlinks());//gold ET
        predDoc.setBodyTlinks(predTlinks);
        LocalETClassifierExp.addPredETlinks(doc,predDoc,knownNONEs);

        /*Filter out distant event pairs*/
        if(sentDistFilter) {
            List<TLINK> filtered = new ArrayList<>();
            for (TLINK tlink : predDoc.getBodyTlinks()) {
                if (!tlink.getSourceType().equals(TempEval3Reader.Type_Event)
                        || !tlink.getTargetType().equals(TempEval3Reader.Type_Event)) {
                    filtered.add(tlink);
                    continue;
                }
                EventChunk ec1 = predDoc.getEventMentionFromEIID(tlink.getSourceId());
                EventChunk ec2 = predDoc.getEventMentionFromEIID(tlink.getTargetId());
                int sentId1 = predDoc.getSentId(ec1);
                int sentId2 = predDoc.getSentId(ec2);
                if(Math.abs(sentId1-sentId2)>=1)
                    continue;
                filtered.add(tlink);
            }
            predDoc.setBodyTlinks(filtered);
        }

        /*If ignoreVar is active, saturate before returning the predicted doc.*/
        /*if(ignoreVar!=null)
            predDoc.saturateTlinks();*/
        if(serialize)
            predDoc.serialize(cacheDir,doc.getDocID(),true);
        return predDoc;
    }
    public List<TemporalDocument> solve() throws Exception {
        int n = docs.size();
        List<TemporalDocument> res_docs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            TemporalDocument doc = docs.get(i);
            if(doc.getTextAnnotation()==null){
                res_docs.add(null);
            }
            //doc.createTextAnnotation(ta_pipeline);
            /*int none_edge = Integer.MAX_VALUE;
            int ignore_edge = Integer.MAX_VALUE;
            int include_edge = Integer.MAX_VALUE;*/
            int none_edge = 20;
            int ignore_edge = 9;
            int include_edge = 2;
            boolean sentId_or_not = false;
            if(knownNONEs)
                getKnownVarsMap();
            else{
                /*pred knownVarsMap*/
                HashMap<TlinkType,Pair<Integer,Integer>> reliableRange = new HashMap<>();
                reliableRange.put(TlinkType.BEFORE,new Pair<Integer,Integer>(0,none_edge));
                reliableRange.put(TlinkType.AFTER,new Pair<Integer,Integer>(0,none_edge));
                reliableRange.put(TlinkType.INCLUDES,new Pair<Integer,Integer>(0,include_edge));
                reliableRange.put(TlinkType.IS_INCLUDED,new Pair<Integer,Integer>(0,include_edge));
                reliableRange.put(TlinkType.EQUAL,new Pair<Integer,Integer>(0,none_edge));
                reliableRange.put(TlinkType.UNDEF,new Pair<Integer,Integer>(0,Integer.MAX_VALUE));
                setReliableRange(reliableRange,sentId_or_not);
                /*Ignore maps*/
                genIgnoreMapByDist(ignore_edge,none_edge, sentId_or_not);
            }
            if(useBaselineProb)
                getBaselineProb();

            System.out.printf("Solving: [" + (i + 1) + "/" + n + "]:" + doc.getDocID() + "...");
            ExecutionTimeUtil timer = new ExecutionTimeUtil();
            timer.start();
            TemporalDocument predDoc;
            try{
                predDoc = solveDoc(doc);
            }
            catch (Exception e){
                e.printStackTrace();
                predDoc = null;
            }
            res_docs.add(predDoc);
            timer.end();
            timer.print();
            if(predDoc != null) {
                List<Pair<String, String>> results = TempDocEval.evalEEBetweenDocs(doc, predDoc, new String[]{TLINK.TlinkType.UNDEF.toStringfull()});
                for (Pair<String, String> result : results) {
                    evaluator.addPredGoldLabels(result.getSecond(), result.getFirst());
                }
            }
        }
        evaluator.printPrecisionRecall(new String[]{TLINK.TlinkType.UNDEF.toStringfull()});
        evaluator.printConfusionMatrix();
        return res_docs;
    }

    public void getBaselineProb() {
        if(knownNONEs) {
            HashMap<String, Double> baseline_prob = new HashMap<>();
            baseline_prob.put("before", 0.407);
            baseline_prob.put("after", 0.407);
            baseline_prob.put("includes", 0.049);
            baseline_prob.put("included", 0.049);
            baseline_prob.put("equal", 0.087);
            this.baseline_prob = baseline_prob;
        }
        else{
            System.out.println("Has not counted yet.");
            System.exit(-1);
        }
    }

    public void getChainProb() {
        HashMap<String, HashMap<String, Double>> chain_prob = new HashMap<>();
        double lambda = 1500;
        chain_prob.put("equal",
                new HashMap<String, Double>() {{
                    put("equal", lambda * 0.1);
                    put("before", lambda * 0.19);
                    put("after", lambda * 0.1);
                    put("includes", lambda * 0.04);
                    put("included", lambda * 0.06);
                    put("undef", lambda * 0.51);
                }});
        chain_prob.put("before",
                new HashMap<String, Double>() {{
                    put("equal", lambda * 0.05);
                    put("before", lambda * 0.17);
                    put("after", lambda * 0.23);
                    put("includes", lambda * 0.04);
                    put("included", lambda * 0.07);
                    put("undef", lambda * 0.44);
                }});
        chain_prob.put("after",
                new HashMap<String, Double>() {{
                    put("equal", lambda * 0.06);
                    put("before", lambda * 0.31);
                    put("after", lambda * 0.13);
                    put("includes", lambda * 0.04);
                    put("included", lambda * 0.09);
                    put("undef", lambda * 0.39);
                }});
        chain_prob.put("includes",
                new HashMap<String, Double>() {{
                    put("equal", lambda * 0.08);
                    put("before", lambda * 0.14);
                    put("after", lambda * 0.13);
                    put("includes", lambda * 0.04);
                    put("included", lambda * 0.08);
                    put("undef", lambda * 0.53);
                }});
        chain_prob.put("included",
                new HashMap<String, Double>() {{
                    put("equal", lambda * 0.06);
                    put("before", lambda * 0.15);
                    put("after", lambda * 0.07);
                    put("includes", lambda * 0.24);
                    put("included", lambda * 0.03);
                    put("undef", lambda * 0.46);
                }});
        chain_prob.put("undef",
                new HashMap<String, Double>() {{
                    put("equal", lambda * 0.07);
                    put("before", lambda * 0.12);
                    put("after", lambda * 0.1);
                    put("includes", lambda * 0.04);
                    put("included", lambda * 0.03);
                    put("undef", lambda * 0.64);
                }});
        this.chain_prob = chain_prob;
    }

    /*Ignore event pairs with distance between min_dist and max_dist (inclusive).*/
    public void genIgnoreMapByDist(int min_dist, int max_dist, boolean sentId_or_not) {
        if(ignoreVarsMap==null)
            ignoreVarsMap = new HashMap<>();
        for (TemporalDocument doc : docs) {
            ignoreVarsMap.put(doc.getDocID(), new HashMap<>());
            List<EventChunk> bodyEvents = doc.getBodyEventMentions();
            for (EventChunk ec1 : bodyEvents) {
                int id1 = bodyEvents.indexOf(ec1);
                int dist1 = sentId_or_not? doc.getSentId(ec1) : id1;
                ignoreVarsMap.get(doc.getDocID()).put(id1, new ArrayList<>());
                for (EventChunk ec2 : bodyEvents) {
                    if (ec1 == ec2)
                        continue;
                    int id2 = bodyEvents.indexOf(ec2);
                    int dist2 = sentId_or_not? doc.getSentId(ec2) : id2;
                    if (Math.abs(dist1-dist2) <= max_dist && Math.abs(dist1-dist2) >= min_dist)
                        ignoreVarsMap.get(doc.getDocID()).get(id1).add(id2);
                }
            }
        }
    }
    /*Get the true NONE maps and force others to be not NONE*/
    public void getKnownVarsMap() {
        if(knownVarsMap==null)
            knownVarsMap = new HashMap<>();
        for(TemporalDocument doc:docs) {
            HashMap<Integer, HashMap<Integer, List<String>>> knownVar = getKnownVarsMap(doc,true);
            knownVarsMap.put(doc.getDocID(),knownVar);
        }
    }
    public static HashMap<Integer,HashMap<Integer,List<String>>> getKnownVarsMap(TemporalDocument doc){
        return getKnownVarsMap(doc, false);
    }
    public static HashMap<Integer,HashMap<Integer,List<String>>> getKnownVarsMap(TemporalDocument doc, boolean index_or_eiid){
        //index_or_eiid:
        //true: index
        //false: eiid
        HashMap<Integer,HashMap<Integer,List<String>>> knownVarsMap = new HashMap<>();
        HashMap<Integer,List<Integer>> eeNONEmap = doc.getEENONEmap(index_or_eiid);
        /*Get NONE links as priori*/
        for(int id1:eeNONEmap.keySet()){
            knownVarsMap.put(id1,new HashMap<Integer,List<String>>());
            for(int id2:eeNONEmap.get(id1)){
                List<String> tmp = new ArrayList<>();
                tmp.add(TLINK.TlinkType.UNDEF.toStringfull());
                knownVarsMap.get(id1).put(id2, tmp);
            }
        }
        /*Other links are not NONE*/
        for(EventChunk ec1:doc.getBodyEventMentions()){
            int id1 = index_or_eiid?doc.getBodyEventMentions().indexOf(ec1):ec1.getEiid();
            for(EventChunk ec2:doc.getBodyEventMentions()){
                if(ec1==ec2)
                    continue;
                int id2 = index_or_eiid?doc.getBodyEventMentions().indexOf(ec2):ec2.getEiid();
                if (eeNONEmap.containsKey(id1)
                        &&eeNONEmap.get(id1).contains(id2))
                    continue;
                List<String> tmp = new ArrayList<>();
                tmp.add(TLINK.TlinkType.BEFORE.toStringfull());
                tmp.add(TLINK.TlinkType.AFTER.toStringfull());
                tmp.add(TLINK.TlinkType.INCLUDES.toStringfull());
                tmp.add(TLINK.TlinkType.IS_INCLUDED.toStringfull());
                tmp.add(TLINK.TlinkType.EQUAL.toStringfull());
                if(eeNONEmap.containsKey(id1))
                    knownVarsMap.get(id1).put(id2,tmp);
                else
                    knownVarsMap.put(id1,new HashMap<Integer,List<String>>(){{put(id2,tmp);}});
            }
        }
        return knownVarsMap;
    }

    /*Setup the reliable range for every label. For example, we can force events with distance>2 to be not "includes"*/
    public void setReliableRange(HashMap<TlinkType,Pair<Integer,Integer>> reliableRange, boolean sentId_or_not) {
        if(knownVarsMap==null)
            knownVarsMap = new HashMap<>();
        for(TemporalDocument doc:docs) {
            HashMap<Integer, HashMap<Integer, List<String>>> knownVar = new HashMap<>();
            List<EventChunk> bodyEvents = doc.getBodyEventMentions();
            for (EventChunk ec1 : bodyEvents) {
                int id1 = bodyEvents.indexOf(ec1);
                int dist1 = sentId_or_not? doc.getSentId(ec1) : id1;
                knownVar.put(id1, new HashMap<>());
                for (EventChunk ec2 : bodyEvents) {
                    if (ec1 == ec2)
                        continue;
                    int id2 = bodyEvents.indexOf(ec2);
                    int dist2 = sentId_or_not? doc.getSentId(ec2) : id2;
                    int diff = Math.abs(dist1-dist2);
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
                        knownVar.get(id1).put(id2,tmp);
                }
            }
            knownVarsMap.put(doc.getDocID(), knownVar);
        }
    }

    public static void sanityCheck(TemporalDocument doc, HashMap<Integer, HashMap<Integer, List<String>>> knownVar) {
        for (EventChunk ec1 : doc.getBodyEventMentions()) {
            int eiid1 = ec1.getEiid();
            for (EventChunk ec2 : doc.getBodyEventMentions()) {
                if (ec1 == ec2)
                    continue;
                int eiid2 = ec2.getEiid();
                TLINK tlink = doc.getTlink(ec1, ec2);
                if (tlink == null || tlink.getReducedRelType() == TLINK.TlinkType.UNDEF) {
                    if (!knownVar.get(eiid1).get(eiid2).contains(TLINK.TlinkType.UNDEF.toStringfull())) {
                        System.out.println("Doc " + doc.getDocID() + ":eiid1=" + eiid1 + ", eiid2=" + eiid2);
                    }
                } else {
                    if (!knownVar.get(eiid1).get(eiid2).contains(tlink.getReducedRelType().toStringfull())) {
                        System.out.println("Doc " + doc.getDocID() + ":eiid1=" + eiid1 + ", eiid2=" + eiid2);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ResourceManager rm = new ReaderConfigurator().getDefaultConfig();
        ResourceManager rm2 = new ClassifierConfigurator().getDefaultConfig();
        List<TemporalDocument> testdocs = TempEval3Reader.deserialize(
                rm.getString("ser_dir")+ File.separator+rm.getString("platinum_label"),
                Integer.MAX_VALUE,
                true);
        /*test set loading*/
        /*if(GlobalEEClassifierExp.knownNONEs) {
            testReader.orderTimexes();
            testReader.saturateTlinks();//only used for unofficial evaluation?
        }*/
        //testReader.createTextAnnotation();
        GlobalEEClassifierExp exp;
        if(GlobalEEClassifierExp.knownNONEs){
            exp = new GlobalEEClassifierExp(testdocs,
                    rm2.getString("eeModelDirPath"),
                    rm2.getString("eeModelName"));
        }
        else {
            exp = new GlobalEEClassifierExp(testdocs,
                    rm2.getString("eeModelDirPath"),
                    rm2.getString("eeModelName_none") + "_0.05");
            /*exp = new GlobalEEClassifierExp(testdocs,
                    rm2.getString("eeModelDirPath"),
                    rm2.getString("eeModelName"));*/
        }
        exp.force_update = true;
        List<TemporalDocument> predDocs = exp.solve();
        List<TemporalDocument> cleartk = TempEval3Reader.deserialize("./serialized_data/ClearTK_Output");
        for(TemporalDocument doc:predDocs) {
            doc.removeTTlinks();
            doc.removeETlinks();
            for(TemporalDocument tkdoc:cleartk){
                if(doc.getDocID().equals(tkdoc.getDocID())) {
                    doc.getBodyTlinks().addAll(tkdoc.getETlinks());
                    break;
                }
            }
            doc.temporalDocumentToText("./output/pred_global_unknown/" + doc.getDocID() + ".tml");
        }
        /*EVALUATION*/
        Runtime rt = Runtime.getRuntime();
        String cmd = "sh scripts/evaluate_test.sh global_unknown";
        Process pr = rt.exec(cmd);
        /*for(TemporalDocument doc:testdocs)
            doc.temporalDocumentToText("./output/gold/"+doc.getDocID()+".tml");*/

        /*Copy gold E-T links from testdocs*/
        /*for(TemporalDocument doc:predDocs){
            List<TLINK> tlinks = doc.getBodyTlinks();
            List<TLINK> TTlinks = testdocs.get(predDocs.indexOf(doc)).getTTlinks();
            tlinks.addAll(TTlinks);
        }
        HeidelTimeExp.evalDocs_BestRef(testdocs,2);
        HeidelTimeExp.evalDocs_BestRef(predDocs,2);*/
    }
}

