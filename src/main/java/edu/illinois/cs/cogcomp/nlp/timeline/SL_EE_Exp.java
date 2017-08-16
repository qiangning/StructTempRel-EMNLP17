package edu.illinois.cs.cogcomp.nlp.timeline;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.classifier.sl.tempFeatureGenerator;
import edu.illinois.cs.cogcomp.nlp.classifier.sl.temporalDecoder;
import edu.illinois.cs.cogcomp.nlp.classifier.sl.temporalInstance;
import edu.illinois.cs.cogcomp.nlp.classifier.sl.temporalStructure;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.util.ExecutionTimeUtil;
import edu.illinois.cs.cogcomp.nlp.util.IOManager;
import edu.illinois.cs.cogcomp.nlp.util.KLDiv;
import edu.illinois.cs.cogcomp.nlp.util.PrecisionRecallManager;
import edu.illinois.cs.cogcomp.sl.core.*;
import edu.illinois.cs.cogcomp.sl.learner.Learner;
import edu.illinois.cs.cogcomp.sl.learner.LearnerFactory;
import edu.illinois.cs.cogcomp.sl.learner.structured_perceptron.StructuredPerceptron;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by qning2 on 12/28/16.
 */
public class SL_EE_Exp {
    private static int MAX_EVENTS_Train = 60;//60 will ignore 2/20 in platinum
    private static int MAX_EVENTS_Test = 80;
    private static final boolean usePriorInTrain = false;
    private static final boolean usePriorInTest = false;
    private static int none_edge = Integer.MAX_VALUE;
    private static int ignore_edge = 2;
    private static int include_edge = 1;
    private static boolean sentId_or_not = true;
    public static void main(String[] args) throws Exception{
        int[] iters = new int[]{3,6,9};
        float[] rates = new float[]{0.005f,0.05f,0.5f};
        double[] thresholds = new double[] {0.6,0.8,1,1.2,1.4};
        int bestiter;
        float bestrate;
        double bestth;
        if(usePriorInTrain){
            bestiter=20;
            bestrate=0.01f;
            bestth=0;
        }
        else{
            bestiter=3;
            bestrate=0.05f;
            bestth=0.8;
        }

        boolean retrain = false;
        boolean development = false;
        StructuredPerceptron.random = new Random(0);
        String configFilePath = "config/StructuredPerceptron.config";
        /*-----SPLITTING-----*/
        double devratio = 0.1;
        SLProblem trainset_all = getStructuredTrainData();
        int numberOfTrainInstance = (int)Math.floor((1-devratio)*trainset_all.size());
        if(development) {
            edu.illinois.cs.cogcomp.sl.util.Pair<SLProblem, SLProblem> splitTrainDev = trainset_all.splitTrainTest(numberOfTrainInstance);
            SLProblem trainset = splitTrainDev.getFirst();
            SLProblem devset = splitTrainDev.getSecond();
            String golddir = "./output/dev/gold_dev";
            IOUtils.mkdir(golddir);
            IOUtils.cleanDir(golddir);
            for(IInstance ins:devset.instanceList){
                TemporalDocument doc = ((temporalInstance) ins).doc;
                doc.temporalDocumentToText(golddir+File.separator+doc.getDocID()+".tml");
            }
            for (int iter : iters) {
                for (float rate : rates) {
                    String modelPath = "SL_models/dev/trained" + (usePriorInTrain ? "_usePrior" : "") + "_" + iter + "_" + String.valueOf(rate) + ".model";
                    System.out.println(modelPath);
                    /*-----TRAINING-----*/
                    if (retrain) {
                        SLModel model = SL_EE_Exp.train(trainset, configFilePath, null, iter, rate);
                        model.saveModel(modelPath);
                    }
                    /*-----TESTING ON DEV-----*/
                    for (double kl : thresholds) {
                        String outdir = "./output/dev/pred" + "_" + iter + "_" + String.valueOf(rate) + "_" + String.valueOf(kl);
                        IOUtils.mkdir(outdir);
                        System.out.println("Testing on Test Data");
                        SL_EE_Exp.test(devset, modelPath, outdir, true, kl);
                        /*Evaluation*/
                        Runtime rt = Runtime.getRuntime();
                        String cmd = "sh scripts/evaluate_dev.sh "+iter + "_" + String.valueOf(rate) + "_" + String.valueOf(kl);
                        Process pr = rt.exec(cmd);
                    }
                }
            }
        }
        else{
            String modelPath = "SL_models/trained" + (usePriorInTrain ? "_usePrior" : "") + "_" + bestiter + "_" + String.valueOf(bestrate) + ".model";
            System.out.println(modelPath);
            /*-----TRAINING-----*/
            if (retrain) {
                SLModel model = SL_EE_Exp.train(trainset_all, configFilePath, null, bestiter, bestrate);
                model.saveModel(modelPath);
            }
            /*-----TESTING-----*/
            String outdir = "./output/pred" + "_" + bestiter + "_" + String.valueOf(bestrate) + "_" + String.valueOf(bestth);
            IOUtils.mkdir(outdir);
            System.out.println("Testing on Test Data");
            SLProblem testset = getStructuredTestData();
            SL_EE_Exp.test(testset, modelPath, outdir, false, bestth);
            /*Evaluation*/
            Runtime rt = Runtime.getRuntime();
            String cmd = "sh scripts/evaluate_test.sh "+bestiter + "_" + String.valueOf(bestrate) + "_" + String.valueOf(bestth);
            Process pr = rt.exec(cmd);
        }
        //SL_EE_Exp.testLocal(newmodel);
    }
    public static SLModel train(SLProblem problem, String configFilePath, WeightVector init, int maxiter, float learning_rate) throws Exception{
        SLModel model = new SLModel();
        model.lm = new Lexiconer();
        if (model.lm.isAllowNewFeatures())
            model.lm.addFeature("W:unknownword");
        model.featureGenerator = new tempFeatureGenerator(model.lm,usePriorInTrain);
        ((tempFeatureGenerator)model.featureGenerator).sentId_or_not = sentId_or_not;
        ((tempFeatureGenerator)model.featureGenerator).setEdges(none_edge,ignore_edge,include_edge);
        //SLProblem problem = getStructuredTrainData();
        pre_extract(model, problem);
        model.infSolver = new temporalDecoder(model.featureGenerator,usePriorInTrain);
        ((temporalDecoder)model.infSolver).sentId_or_not = sentId_or_not;
        ((temporalDecoder)model.infSolver).setEdges(none_edge,ignore_edge,include_edge);
        ((temporalDecoder)model.infSolver).verbose = true;
        SLParameters para = new SLParameters();
        para.loadConfigFile(configFilePath);
        para.TOTAL_NUMBER_FEATURE = model.lm.getNumOfFeature();
        para.MAX_NUM_ITER = maxiter;
        para.LEARNING_RATE = learning_rate;
        Learner learner = LearnerFactory.getLearner(model.infSolver,
                model.featureGenerator, para);
        if(init!=null)
            model.wv = learner.train(problem,init);
        else
            model.wv = learner.train(problem);

        model.lm.setAllowNewFeatures(false);
        return model;
    }
    public static void testLocal(String modelPath) throws Exception {
        SLModel model = SLModel.loadModel(modelPath);
        SLProblem sp = getStructuredTrainData();
        PrecisionRecallManager evaluator = new PrecisionRecallManager();
        int corr = 0;
        int total = 0;

        for (int i = 0; i < sp.instanceList.size(); i++) {
            temporalInstance docInst = (temporalInstance) sp.instanceList.get(i);
            temporalStructure gold = (temporalStructure) sp.goldStructureList.get(i);
            temporalDecoder decoder = (temporalDecoder) model.infSolver;

            ((temporalDecoder)model.infSolver).usePrior = usePriorInTest;

            temporalStructure prediction = (temporalStructure) decoder
                    .getBestLocalStructure(model.wv, docInst);
            int tmp_total = gold.nE*(gold.nE-1);
            corr += tmp_total - 2*model.infSolver.getLoss(docInst,gold,prediction);
            total += tmp_total;
            for(int e1=0;e1<gold.nE;e1++){
                for(int e2=e1+1;e2<gold.nE;e2++){
                    String predlabel = prediction.getRelStr()[e1][e2];
                    String goldlabel = gold.getRelStr()[e1][e2];
                    if(!goldlabel.equals(TLINK.TlinkType.UNDEF.toStringfull())){
                        TemporalLog.println("IBT_L",
                                docInst.doc.getDocID()+
                                        " (e"+docInst.events[e1].getEid()+
                                        ","+
                                        "e"+docInst.events[e2].getEid()+"):"+
                                        goldlabel+"-->"+predlabel+" "+
                                        (predlabel.equals(goldlabel)?"CORR":"WRONG"));
                    }
                    evaluator.addPredGoldLabels(predlabel,goldlabel);
                }
            }
            TemporalDocument docPred = docInst.tempstruct2tempinst(prediction);
            /*Get local E-T*/
            LocalETClassifierExp.addPredETlinks(docInst.doc,docPred,usePriorInTest);

            docPred.temporalDocumentToText("./output/pred/"+docPred.getDocID()+".tml");
        }
        System.out.println("corr " + corr);
        System.out.println("total " + total);
        evaluator.printPrecisionRecall(new String[] {TLINK.TlinkType.UNDEF.toStringfull()});
        evaluator.printConfusionMatrix();
        System.out.println("Done with testing!");
    }
    public static temporalStructure test(SLModel model, temporalInstance ins)
            throws Exception {
        return (temporalStructure) model.infSolver.getBestStructure(model.wv,ins);
    }
    public static void test(SLProblem sp, String modelPath, String outdir, boolean gold_et_or_not, double kl_th)
            throws Exception {
        boolean force_update = true;
        String cacheDir = "serialized_data/McNemar-known/sl";

        SLModel model = SLModel.loadModel(modelPath);
        PrecisionRecallManager evaluator = new PrecisionRecallManager();
        int corr = 0;
        int total = 0;

        List<TemporalDocument> localets = new ArrayList<>();
        if(gold_et_or_not){
            for(int i = 0; i < sp.instanceList.size(); i++){
                localets.add(((temporalInstance) sp.instanceList.get(i)).doc);
            }
        }
        else{
            if(usePriorInTest){

            }
            else {
                localets = TempEval3Reader.deserialize("./serialized_data/ClearTK_Output");
            }
        }
        List<TemporalDocument> distantpairs = TempEval3Reader.deserialize("./serialized_data/LpI_farpairs");
        for (int i = 0; i < sp.instanceList.size(); i++) {
            temporalInstance docInst = (temporalInstance) sp.instanceList.get(i);
            System.out.printf("Solving: [" + (i + 1) + "/" + sp.instanceList.size() + "]:" + docInst.doc.getDocID() + "...");
            TemporalDocument docPred;
            if(!force_update) {
                docPred = TemporalDocument.deserialize(cacheDir, docInst.doc.getDocID(), true);
                if (docPred != null){
                    docPred.temporalDocumentToText(outdir+File.separator+docPred.getDocID()+".tml");
                    continue;
                }
            }
            ExecutionTimeUtil timer = new ExecutionTimeUtil();
            timer.start();
            temporalStructure gold = (temporalStructure) sp.goldStructureList.get(i);
            ((temporalDecoder)model.infSolver).verbose = false;

            ((temporalDecoder)model.infSolver).usePrior = usePriorInTest;

            temporalStructure prediction = (temporalStructure) model.infSolver
                    .getBestStructure(model.wv, docInst);
            double[][][] localScores = ((temporalDecoder)model.infSolver).localScores;
            int tmp_total = gold.nE*(gold.nE-1);
            corr += tmp_total - 2*model.infSolver.getLoss(docInst,gold,prediction);
            total += tmp_total;
            for(int e1=0;e1<gold.nE;e1++){
                for(int e2=e1+1;e2<gold.nE;e2++){
                    evaluator.addPredGoldLabels(prediction.getRelStr()[e1][e2],gold.getRelStr()[e1][e2]);
                    if(!prediction.getRelStr()[e1][e2].equals(gold.getRelStr()[e1][e2])
                            &&!prediction.getRelStr()[e1][e2].equals("undef")){
                        double[] p = localScores[e1][e2];
                        double[] q = new double[]{1.0/6,1.0/6,1.0/6,1.0/6,1.0/6,1.0/6};
                        double kl = KLDiv.kldivergence(p,q);
                        /*System.out.printf("pred=%s, gold=%s\n\tb=%.2f,a=%.2f,e=%.2f,icd=%.2f,icd'd=%2f,kl=%.2f\n",
                                prediction.getRelStr()[e1][e2],gold.getRelStr()[e1][e2],
                                localScores[e1][e2][0],localScores[e1][e2][1],localScores[e1][e2][2],
                                localScores[e1][e2][3],localScores[e1][e2][4],kl);*/
                    }
                }
            }
            for(int e1=0;e1<gold.nE;e1++){
                for(int e2=e1+1;e2<gold.nE;e2++){
                    if(prediction.getRelStr()[e1][e2].equals(gold.getRelStr()[e1][e2])
                            &&!prediction.getRelStr()[e1][e2].equals("undef")){
                        double[] p = localScores[e1][e2];
                        double[] q = new double[]{1.0/6,1.0/6,1.0/6,1.0/6,1.0/6,1.0/6};
                        double kl = KLDiv.kldivergence(p,q);
                        /*System.out.printf("pred=%s, gold=%s\n\tb=%.2f,a=%.2f,e=%.2f,icd=%.2f,icd'd=%2f,kl=%.2f\n",
                                prediction.getRelStr()[e1][e2],gold.getRelStr()[e1][e2],
                                localScores[e1][e2][0],localScores[e1][e2][1],localScores[e1][e2][2],
                                localScores[e1][e2][3],localScores[e1][e2][4],kl);*/
                    }
                }
            }
            docPred = docInst.tempstruct2tempinst(prediction);
            /*Filter out distant event pairs*/
            int dist_filter = 1;
            List<TLINK> filtered = new ArrayList<>();
            for(TLINK tlink:docPred.getBodyTlinks()){
                if(!tlink.getSourceType().equals(TempEval3Reader.Type_Event)
                        ||!tlink.getTargetType().equals(TempEval3Reader.Type_Event)){
                    filtered.add(tlink);
                    continue;
                }
                EventChunk ec1 = docPred.getEventMentionFromEIID(tlink.getSourceId());
                EventChunk ec2 = docPred.getEventMentionFromEIID(tlink.getTargetId());
                int sentId1 = docPred.getSentId(ec1);
                int sentId2 = docPred.getSentId(ec2);
                if(!usePriorInTest) {
                    if (Math.abs(sentId1 - sentId2) >= dist_filter)
                        continue;
                    double[] p = localScores[docPred.getBodyEventMentions().indexOf(ec1)][docPred.getBodyEventMentions().indexOf(ec2)];
                    double[] q = new double[]{1.0/6,1.0/6,1.0/6,1.0/6,1.0/6,1.0/6};
                    double kl = KLDiv.kldivergence(p,q);
                    if(kl<kl_th)
                        continue;
                }
                filtered.add(tlink);
            }
            /*Add distant EE predictions from L+I*/
            for(TemporalDocument doc:distantpairs){
                if(doc.getDocID().equals(docPred.getDocID())){
                    List<TLINK> distant_et = doc.getEElinks();
                    for(TLINK tl:distant_et) {
                        EventChunk ec1 = doc.getEventMentionFromEIID(tl.getSourceId());
                        EventChunk ec2 = doc.getEventMentionFromEIID(tl.getTargetId());
                        int sentId1 = doc.getSentId(ec1);
                        int sentId2 = doc.getSentId(ec2);
                        if(!usePriorInTest){
                            if (Math.abs(sentId1 - sentId2) >= dist_filter){
                                filtered.add(tl);
                            }
                        }
                    }
                    break;
                }
            }
            /*Add cleartk ET*/
            for(TemporalDocument doc:localets){
                if(doc.getDocID().equals(docInst.doc.getDocID())){
                    filtered.addAll(doc.getETlinks());
                    System.out.println("Replacing with ET from cleartk.");
                    break;
                }
            }
            docPred.setBodyTlinks(filtered);

            /*write to disk for evaluation*/
            docPred.serialize(cacheDir,docPred.getDocID(),true);
            docPred.temporalDocumentToText(outdir+File.separator+docPred.getDocID()+".tml");
            timer.end();
            timer.print();
        }
        System.out.println("corr " + corr);
        System.out.println("total " + total);
        evaluator.printPrecisionRecall(new String[] {TLINK.TlinkType.UNDEF.toStringfull()});
        evaluator.printConfusionMatrix();
        System.out.println("Done with testing!");
    }

    public static Pair<IInstance,IStructure> getSLPair(TemporalDocument doc){
        temporalInstance docInst = new temporalInstance(doc);
        temporalStructure docStruct = new temporalStructure(docInst);
        return new Pair<IInstance,IStructure>(docInst,docStruct);
    }
    public static SLProblem getStructuredTrainData() throws Exception{
        SLProblem problem = new SLProblem();
        List<TemporalDocument> docs = TempEval3Reader.deserialize(new String[]{
                    TempEval3Reader.label2dir("timebank"),
                    TempEval3Reader.label2dir("aquaint")});
        for(TemporalDocument doc:docs){
            if(doc.getBodyEventMentions().size()>MAX_EVENTS_Train)
                continue;
            Pair<IInstance, IStructure> pair = getSLPair(doc);
            problem.addExample(pair.getFirst(), pair.getSecond());
        }
        return problem;
    }
    public static SLProblem getStructuredTestData() throws Exception{
        SLProblem problem = new SLProblem();
        List<TemporalDocument> docs = TempEval3Reader.deserialize(TempEval3Reader.label2dir("platinum"));
        for(TemporalDocument doc:docs){
            if(doc.getBodyEventMentions().size()>MAX_EVENTS_Test)
                continue;
            Pair<IInstance, IStructure> pair = getSLPair(doc);
            problem.addExample(pair.getFirst(), pair.getSecond());
        }
        return problem;
    }
    private static void pre_extract(SLModel model, SLProblem problem) {
        // there shld be a better way, feature extraction
        for(int i=0;i<problem.size();i++)
        {
            model.featureGenerator.getFeatureVector(problem.instanceList.get(i),problem.goldStructureList.get(i));
        }
    }
}
