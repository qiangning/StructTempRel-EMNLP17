package edu.illinois.cs.cogcomp.nlp.timeline;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.infer.ilp.GurobiHook;
import edu.illinois.cs.cogcomp.lbjava.classify.ScoreSet;
import edu.illinois.cs.cogcomp.lbjava.learn.Softmax;
import edu.illinois.cs.cogcomp.nlp.classifier.FeatureExtractor;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.LearningObj;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ParamLBJ;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ee.ee_perceptron;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.et.et_perceptron;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.EventStruct;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK.TlinkType;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TimexStruct;
import edu.illinois.cs.cogcomp.nlp.util.ExecutionTimeUtil;
import edu.illinois.cs.cogcomp.nlp.util.TransitivityTriplets;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by qning2 on 1/15/17.
 */
public class GlobalClassifier {
    private static ee_perceptron ee_classifier;
    private static et_perceptron et_classifier;
    public HashMap<String, List<String>> ignoreVarsMap = null;
    public HashMap<String, HashMap<String, List<String>>> knownVarsMap = null;
    public TemporalDocument doc;
    public static double lambda_et = 0.3;
    public static Pair<Integer,Integer> ee_ignore = new Pair<>(9,19);
    public static Pair<Integer,Integer> ee_none = new Pair<>(20,Integer.MAX_VALUE);;
    public static Pair<Integer,Integer> ee_include = new Pair<>(0,2);
    public static Pair<Integer,Integer> et_ignore = new Pair<>(1,Integer.MAX_VALUE);
    public static Pair<Integer,Integer> et_none = new Pair<>(Integer.MAX_VALUE,Integer.MAX_VALUE);;
    public static Pair<Integer,Integer> et_include = new Pair<>(0,1);

    public GlobalClassifier(TemporalDocument doc){
        this.doc = doc;
    }

    public static void setEe_classifier(ee_perceptron ee_classifier) {
        GlobalClassifier.ee_classifier = ee_classifier;
    }

    public static void setEt_classifier(et_perceptron et_classifier) {
        GlobalClassifier.et_classifier = et_classifier;
    }

    public void setIgnoreVarsMap(HashMap<String, List<String>> ignoreVarsMap) {
        this.ignoreVarsMap = ignoreVarsMap;
    }

    public void setKnownVarsMap(HashMap<String, HashMap<String, List<String>>> knownVarsMap) {
        this.knownVarsMap = knownVarsMap;
    }

    public HashMap<String, List<String>> defaultIgnoreVarsMap(){
        HashMap<String, List<String>> ignoremap = new HashMap<>();
        //----EE ignore
        List<EventChunk> bodyEvents = doc.getBodyEventMentions();
        for (EventChunk ec1 : bodyEvents) {
            String ec1_str = doc.getObjectTypeIdStr(ec1);
            int id1 = bodyEvents.indexOf(ec1);
            for (EventChunk ec2 : bodyEvents) {
                String ec2_str = doc.getObjectTypeIdStr(ec2);
                if (ec1_str.equals(ec2_str))
                    continue;
                int id2 = bodyEvents.indexOf(ec2);
                if (Math.abs(id1-id2) <= ee_ignore.getSecond() && Math.abs(id1-id2) >= ee_ignore.getFirst()) {
                    if(!ignoremap.containsKey(ec1_str))
                        ignoremap.put(ec1_str, new ArrayList<>());
                    if(!ignoremap.get(ec1_str).contains(ec2_str))
                        ignoremap.get(ec1_str).add(ec2_str);

                    if(!ignoremap.containsKey(ec2_str))
                        ignoremap.put(ec2_str, new ArrayList<>());
                    if(!ignoremap.get(ec2_str).contains(ec1_str))
                        ignoremap.get(ec2_str).add(ec1_str);
                }
            }
        }
        //----TT ignore
        /*Ignore all of them*/
        List<TemporalJointChunk> bodyTimexes = doc.getBodyTimexMentions();
        /*List<TemporalJointChunk> allTimexes = doc.deepCopyTimex();
        allTimexes.add(doc.getDocumentCreationTime().deepCopy());
        for(TemporalJointChunk tjc1:allTimexes){
            String tjc1_str = doc.getObjectTypeIdStr(tjc1);
            for(TemporalJointChunk tjc2:allTimexes){
                String tjc2_str = doc.getObjectTypeIdStr(tjc2);
                if(tjc1_str.equals(tjc2_str))
                    continue;
                if(!ignoremap.containsKey(tjc1_str))
                    ignoremap.put(tjc1_str, new ArrayList<>());
                if(!ignoremap.get(tjc1_str).contains(tjc2_str))
                    ignoremap.get(tjc1_str).add(tjc2_str);

                if(!ignoremap.containsKey(tjc2_str))
                    ignoremap.put(tjc2_str, new ArrayList<>());
                if(!ignoremap.get(tjc2_str).contains(tjc1_str))
                    ignoremap.get(tjc2_str).add(tjc1_str);
            }
        }*/

        //----ET/TE ignore
        TextAnnotation ta = doc.getTextAnnotation();
        for(EventChunk ec:bodyEvents){
            String ec_str = doc.getObjectTypeIdStr(ec);
            EventStruct targetEvent = new EventStruct(ec,ta);
            IntPair targetEventOff = targetEvent.getPrimaryTriggerWordOffset();
            int eSentId = ta.getSentenceId(targetEventOff.getFirst());
            for(TemporalJointChunk tjc:bodyTimexes){
                String tjc_str = doc.getObjectTypeIdStr(tjc);
                TimexStruct targetTimex = new TimexStruct(tjc,ta);
                IntPair targetTimexOff = targetTimex.getWordOffset();
                int tSentId = ta.getSentenceId(targetTimexOff.getFirst());
                if (Math.abs(eSentId-tSentId) <= et_ignore.getSecond() && Math.abs(eSentId-tSentId) >= et_ignore.getFirst()) {
                    if(!ignoremap.containsKey(ec_str))
                        ignoremap.put(ec_str,new ArrayList<>());
                    if(!ignoremap.get(ec_str).contains(tjc_str))
                        ignoremap.get(ec_str).add(tjc_str);

                    if(!ignoremap.containsKey(tjc_str))
                        ignoremap.put(tjc_str,new ArrayList<>());
                    if(!ignoremap.get(tjc_str).contains(ec_str))
                        ignoremap.get(tjc_str).add(ec_str);
                }
            }
        }

        return ignoremap;
    }

    public HashMap<String, HashMap<String, List<String>>> defaultKnownVarsMap(){
        HashMap<String, HashMap<String, List<String>>> knownmap = new HashMap<>();
        //----EE known
        List<EventChunk> bodyEvents = doc.getBodyEventMentions();
        for (EventChunk ec1 : bodyEvents) {
            int id1 = bodyEvents.indexOf(ec1);
            knownmap.put("e"+ec1.getEiid(), new HashMap<>());
            for (EventChunk ec2 : bodyEvents) {
                if (ec1 == ec2)
                    continue;
                int id2 = bodyEvents.indexOf(ec2);
                int diff = Math.abs(id1-id2);
                List<String> tmp = new ArrayList<>();
                tmp.add(TlinkType.UNDEF.toStringfull());
                if(diff>=ee_include.getFirst()&&diff<=ee_include.getSecond()){
                    tmp.add(TlinkType.INCLUDES.toStringfull());
                    tmp.add(TlinkType.IS_INCLUDED.toStringfull());
                }
                if(diff<ee_none.getFirst()){
                    tmp.add(TlinkType.BEFORE.toStringfull());
                    tmp.add(TlinkType.AFTER.toStringfull());
                    tmp.add(TlinkType.EQUAL.toStringfull());
                }
                knownmap.get("e"+ec1.getEiid()).put("e"+ec2.getEiid(),tmp);
            }
        }
        //----TT known
        List<TemporalJointChunk> bodyTimexes = doc.getBodyTimexMentions();
        doc.removeTTlinks();
        doc.orderTimexes();
        List<TLINK> ttlinks = doc.getTTlinks();
        for(TLINK tlink:ttlinks){
            List<String> tmp = new ArrayList<>();
            if(!knownmap.containsKey("t"+tlink.getSourceId()))
                knownmap.put("t"+tlink.getSourceId(),new HashMap<>());
            tmp.add(tlink.getReducedRelType().toStringfull());
            knownmap.get("t"+tlink.getSourceId()).put("t"+tlink.getTargetId(),tmp);
        }

        //----ET known
        TextAnnotation ta = doc.getTextAnnotation();
        for(EventChunk ec:bodyEvents){
            EventStruct targetEvent = new EventStruct(ec,ta);
            IntPair targetEventOff = targetEvent.getPrimaryTriggerWordOffset();
            int eSentId = ta.getSentenceId(targetEventOff.getFirst());
            for(TemporalJointChunk tjc:bodyTimexes){
                TimexStruct targetTimex = new TimexStruct(tjc,ta);
                IntPair targetTimexOff = targetTimex.getWordOffset();
                int tSentId = ta.getSentenceId(targetTimexOff.getFirst());
                int diff = Math.abs(eSentId-tSentId);
                List<String> tmp = new ArrayList<>();
                tmp.add(TlinkType.UNDEF.toStringfull());
                if(diff>=et_include.getFirst()&&diff<=et_include.getSecond()){
                    tmp.add(TlinkType.INCLUDES.toStringfull());
                    tmp.add(TlinkType.IS_INCLUDED.toStringfull());
                }
                if(diff<et_none.getFirst()){
                    tmp.add(TlinkType.BEFORE.toStringfull());
                    tmp.add(TlinkType.AFTER.toStringfull());
                    tmp.add(TlinkType.EQUAL.toStringfull());
                }
                knownmap.get("e"+ec.getEiid()).put("t"+tjc.getTID(),tmp);
            }
        }
        return knownmap;
    }

    /*solveDoc() hard copies tlinks*/
    public TemporalDocument solveDoc() throws Exception {
        GurobiHook solver = new GurobiHook();
        FeatureExtractor featureExtractor = new FeatureExtractor(doc);
        HashMap<String, HashMap<String, HashMap<String, Integer>>> allVar = new HashMap<>();
        /*Add variable*/
        Softmax sm = new Softmax();
        List<Object> mentions = new ArrayList<Object>();//place holder for both event and timex
        mentions.addAll(doc.getBodyEventMentions());
        mentions.addAll(doc.getBodyTimexMentions());
        mentions.add(doc.getDocumentCreationTime());
        for (Object o1 : mentions) {
            String o1_str = doc.getObjectTypeIdStr(o1);
            allVar.put(o1_str, new HashMap<>());
            for (Object o2 : mentions) {
                if (o1.equals(o2))
                    continue;
                String o2_str = doc.getObjectTypeIdStr(o2);
                if (ignoreVarsMap != null
                        && ignoreVarsMap.containsKey(o1_str)
                        && ignoreVarsMap.get(o1_str).contains(o2_str)) {
                    continue;
                }
                allVar.get(o1_str).put(o2_str,new HashMap<>());
                if(o1 instanceof TemporalJointChunk){//if o1 is timex, add the var with weight=0
                    for(TlinkType tt:TlinkType.values()) {
                        int var = solver.addBooleanVariable(0);
                        allVar.get(o1_str).get(o2_str).put(tt.toStringfull(),var);
                    }
                }
                else{
                    ScoreSet normScores;
                    ScoreSet scores;
                    String feat;
                    if(o2 instanceof EventChunk){
                        feat = featureExtractor
                                .getFeatureString(featureExtractor
                                        .extractEEfeats((EventChunk) o1, (EventChunk) o2), "test");//@@fix later
                    }
                    else{
                        feat = featureExtractor
                                .getFeatureString(featureExtractor
                                        .extractETfeats((EventChunk) o1, (TemporalJointChunk) o2), "test");//@@fix later
                    }
                    int pos = feat.lastIndexOf(ParamLBJ.FEAT_DELIMITER);
                    String goldLabel = feat.substring(pos + 1).trim();
                    LearningObj obj = new LearningObj(feat.substring(0, pos).trim(), goldLabel);
                    if(o2 instanceof EventChunk){
                        scores = ee_classifier.scores(obj);
                    }
                    else{
                        scores = et_classifier.scores(obj);
                    }
                    normScores = sm.normalize(scores);
                    Set vals = normScores.values();
                    for (Object val : vals) {
                        double offset = 0;
                        int var = solver.addBooleanVariable(
                                o2 instanceof TemporalJointChunk?(normScores.get((String) val)*lambda_et):normScores.get((String) val)
                                        + offset);
                        allVar.get(o1_str).get(o2_str).put((String) val, var);
                    }
                    if (!vals.contains(TLINK.TlinkType.UNDEF.toStringfull())) {
                        int var = solver.addBooleanVariable(0);
                        allVar.get(o1_str).get(o2_str).put(TLINK.TlinkType.UNDEF.toStringfull(), var);
                    }
                }
            }
        }
        /*Use knownVar information*/
        if (knownVarsMap != null) {
            for (String o1_str : knownVarsMap.keySet()) {
                for (String o2_str : knownVarsMap.get(o1_str).keySet()) {
                    if (o1_str.equals(o2_str))
                        continue;
                    if (ignoreVarsMap != null
                            && ignoreVarsMap.containsKey(o1_str)
                            && ignoreVarsMap.get(o1_str).contains(o2_str)) {
                        continue;
                    }
                    int n = knownVarsMap.get(o1_str).get(o2_str).size();
                    int[] vars = new int[n];
                    double[] coefs = new double[n];
                    int i = 0;
                    for (String str : knownVarsMap.get(o1_str).get(o2_str)) {
                        try {
                            vars[i] = allVar.get(o1_str).get(o2_str).get(str);
                            coefs[i] = 1;
                            i++;
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    solver.addEqualityConstraint(vars, coefs, 1);
                }
            }
        }
        /*Add uniqueness constraints*/
        for (Object o1 : mentions) {
            String o1_str = doc.getObjectTypeIdStr(o1);
            for (Object o2 : mentions) {
                String o2_str = doc.getObjectTypeIdStr(o2);
                if (o1_str.equals(o2_str))
                    continue;
                if (ignoreVarsMap != null
                        && ignoreVarsMap.containsKey(o1_str)
                        && ignoreVarsMap.get(o1_str).contains(o2_str)) {
                    continue;
                }
                Set<String> rels = allVar.get(o1_str).get(o2_str).keySet();
                int n = rels.size();
                int[] vars = new int[n];
                double[] coefs = new double[n];
                int i = 0;
                for (String rel : rels) {
                    vars[i] = allVar.get(o1_str).get(o2_str).get(rel);
                    coefs[i] = 1;
                    i++;
                }
                solver.addEqualityConstraint(vars, coefs, 1);
            }
        }
        /*Add symmetry constraints*/
        for (Object o1 : mentions) {
            String o1_str = doc.getObjectTypeIdStr(o1);
            for (Object o2 : mentions) {
                String o2_str = doc.getObjectTypeIdStr(o2);
                if (o1_str.equals(o2_str))
                    continue;
                if (ignoreVarsMap != null
                        && ignoreVarsMap.containsKey(o1_str)
                        && ignoreVarsMap.get(o1_str).contains(o2_str)) {
                    continue;
                }
                Set<String> rels = allVar.get(o1_str).get(o2_str).keySet();
                double[] coefs = new double[]{1, -1};
                int[] vars = new int[2];
                for (String rel : rels) {
                    vars[0] = allVar.get(o1_str).get(o2_str).get(rel);
                    vars[1] = allVar.get(o2_str).get(o1_str).get(TLINK.TlinkType.reverse(rel).toStringfull());
                    solver.addEqualityConstraint(vars, coefs, 0);
                }
            }
        }
        /*Add transitivity constraints*/
        for (Object o1 : mentions) {
            String o1_str = doc.getObjectTypeIdStr(o1);
            for (Object o2 : mentions) {
                String o2_str = doc.getObjectTypeIdStr(o2);
                if (o1_str.equals(o2_str))
                    continue;
                if (ignoreVarsMap != null
                        && ignoreVarsMap.containsKey(o1_str)
                        && ignoreVarsMap.get(o1_str).contains(o2_str)) {
                    continue;
                }
                for (Object o3 : mentions) {
                    String o3_str = doc.getObjectTypeIdStr(o3);
                    if (o1_str.equals(o3_str))
                        continue;
                    if (o2_str.equals(o3_str))
                        continue;
                    if (ignoreVarsMap != null
                            && ignoreVarsMap.containsKey(o1_str)
                            && ignoreVarsMap.get(o1_str).contains(o3_str)) {
                        continue;
                    }
                    if (ignoreVarsMap != null
                            && ignoreVarsMap.containsKey(o2_str)
                            && ignoreVarsMap.get(o2_str).contains(o3_str)) {
                        continue;
                    }
                    List<TransitivityTriplets> transTriplets = TransitivityTriplets.transTriplets();
                    for (TransitivityTriplets triplet : transTriplets) {
                        int n = triplet.getThird().length;
                        double[] coefs = new double[n + 2];
                        int[] vars = new int[n + 2];
                        coefs[0] = 1;
                        coefs[1] = 1;
                        vars[0] = allVar.get(o1_str).get(o2_str).get(triplet.getFirst().toStringfull());
                        vars[1] = allVar.get(o2_str).get(o3_str).get(triplet.getSecond().toStringfull());
                        for (int i = 0; i < n; i++) {
                            coefs[i + 2] = -1;
                            vars[i + 2] = allVar.get(o1_str).get(o3_str).get(triplet.getThird()[i].toStringfull());
                        }
                        solver.addLessThanConstraint(vars, coefs, 1);
                    }
                }
            }
        }

        solver.setMaximize(true);
        solver.solve();

        TemporalDocument predDoc = new TemporalDocument(doc);
        predDoc.setBodyTlinks(null);//reset TLINKs
        List<TLINK> predTlinks = new ArrayList<>();
        int lid = 0;
        for (Object o1:mentions) {
            String o1_str = doc.getObjectTypeIdStr(o1);
            for (Object o2:mentions) {
                String o2_str = doc.getObjectTypeIdStr(o2);
                if (o1_str.equals(o2_str))
                    continue;
                if (ignoreVarsMap != null
                        && ignoreVarsMap.containsKey(o1_str)
                        && ignoreVarsMap.get(o1_str).contains(o2_str)) {
                    continue;
                }
                Set<String> rels = allVar.get(o1_str).get(o2_str).keySet();
                int var;
                String result = "";
                int cnt = 0;
                for (String rel : rels) {
                    var = allVar.get(o1_str).get(o2_str).get(rel);
                    if (solver.getBooleanValue(var)) {
                        result = rel;
                        cnt++;
                    }
                }
                if (cnt > 1)
                    System.exit(-1);
                if (result.toLowerCase().equals("undef"))//don't store undefined tlinks
                    continue;
                if(o1 instanceof TemporalJointChunk && o2 instanceof EventChunk)//don't add T-E
                    continue;
                TLINK tmp = new TLINK(lid, "",
                        o1 instanceof EventChunk?TempEval3Reader.Type_Event:TempEval3Reader.Type_Timex,
                        o2 instanceof EventChunk?TempEval3Reader.Type_Event:TempEval3Reader.Type_Timex,
                        o1 instanceof EventChunk?((EventChunk) o1).getEiid():((TemporalJointChunk)o1).getTID(),
                        o2 instanceof EventChunk?((EventChunk) o2).getEiid():((TemporalJointChunk)o2).getTID(),
                        TLINK.TlinkType.str2TlinkType(result));
                lid++;
                predTlinks.add(tmp);
            }
        }
        predDoc.setBodyTlinks(predTlinks);

        /*If ignoreVar is active, saturate before returning the predicted doc.*/
        /*if(ignoreVar!=null)
            predDoc.saturateTlinks();*/
        return predDoc;
    }
    public static void main(String[] args) throws Exception{
        if(args.length>0) {
            GlobalClassifier.lambda_et = Double.parseDouble(args[0]);
            System.out.println("lambda_et="+args[0]);
        }
        ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
        /*test set loading*/
        TempEval3Reader testReader;
        testReader = new TempEval3Reader("TIMEML", "te3-platinum", "data/TempEval3/Evaluation/");
        testReader.ReadData();
        testReader.createTextAnnotation();

        String ee_modelPath = rm.getString("eeModelDirPath");
        String et_modelPath = rm.getString("etModelDirPath");
        String ee_modelName = rm.getString("eeModelName_none")+"_0.05";
        String et_modelName = rm.getString("etModelName");
        GlobalClassifier.setEe_classifier(new ee_perceptron(ee_modelPath+ee_modelName+".lc",ee_modelPath+ee_modelName+".lex"));
        GlobalClassifier.setEt_classifier(new et_perceptron(et_modelPath+et_modelName+".lc",et_modelPath+et_modelName+".lex"));
        List<TemporalDocument> testdocs = testReader.getDataset().getDocuments();
        for(TemporalDocument docGold:testdocs) {
            System.out.printf("Solving: [" + (testdocs.indexOf(docGold) + 1) + "/" + testdocs.size() + "]:" + docGold.getDocID() + "...");
            ExecutionTimeUtil timer = new ExecutionTimeUtil();
            timer.start();
            GlobalClassifier globalClassifier = new GlobalClassifier(docGold);
            globalClassifier.setIgnoreVarsMap(globalClassifier.defaultIgnoreVarsMap());
            globalClassifier.setKnownVarsMap(globalClassifier.defaultKnownVarsMap());
            TemporalDocument docPred = globalClassifier.solveDoc();
            docPred.saturateTlinks();
            docPred.removeTTlinks();
            docGold.saturateTlinks();
            docGold.removeTTlinks();
            docPred.temporalDocumentToText("./output/pred/"+docPred.getDocID()+".tml");
            docGold.temporalDocumentToText("./output/gold/"+docGold.getDocID()+".tml");
            timer.end();
            timer.print();
        }
    }
}
