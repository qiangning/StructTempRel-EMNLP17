package edu.illinois.cs.cogcomp.nlp.CompareCAVEO;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.nlp.classifier.sl.tempFeatureGenerator;
import edu.illinois.cs.cogcomp.nlp.classifier.sl.temporalDecoder;
import edu.illinois.cs.cogcomp.nlp.classifier.sl.temporalInstance;
import edu.illinois.cs.cogcomp.nlp.classifier.sl.temporalStructure;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.timeline.test.sieve_output;
import edu.illinois.cs.cogcomp.nlp.util.ExecutionTimeUtil;
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by qning2 on 4/11/17.
 */
public class SL_exp {
    private static int MAX_EVENTS_Train = 60;//60 will ignore 2/20 in platinum
    private static int MAX_EVENTS_Test = 80;
    private static final boolean usePriorInTrain = false;
    private static final boolean usePriorInTest = false;
    private static final boolean useDistantPairsLpI = false;
    private static int none_edge = Integer.MAX_VALUE;
    private static int ignore_edge = 2;
    private static int include_edge = 1;
    private static boolean sentId_or_not = true;
    public static String MODELDIR = "SL_models/CAVEO";
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
            /*bestiter=3;
            bestrate=0.05f;
            bestth=0.8;*/
            bestiter = Integer.parseInt(args[0]);
            bestrate = Float.valueOf(args[1]);
            bestth = Double.valueOf(args[2]);
        }

        boolean retrain = true;
        boolean development = false;
        StructuredPerceptron.random = new Random(0);
        String configFilePath = "config/StructuredPerceptron.config";
        /*-----SPLITTING-----*/
        double devratio = 0.1;
        SLProblem trainset_all = getStructuredTrainData();
        int numberOfTrainInstance = (int)Math.floor((1-devratio)*trainset_all.size());
        if(development) {
            SLProblem trainset = trainset_all;
            SLProblem devset = getStructuredDevData();
            String golddir = "./output/Chambers/SL/dev/gold_dev";
            String logdir = "CAVEO/dev";
            IOUtils.mkdir(golddir);
            IOUtils.cleanDir(golddir);
            IOUtils.mkdir("./logs/"+logdir);
            IOUtils.mkdir(SL_exp.MODELDIR+File.separator+"dev");
            for(IInstance ins:devset.instanceList){
                TemporalDocument doc = ((temporalInstance) ins).doc;
                doc.temporalDocumentToText(golddir+ File.separator+doc.getDocID()+".tml");
            }
            for (int iter : iters) {
                for (float rate : rates) {
                    String modelPath = SL_exp.MODELDIR+File.separator+"dev/trained" + (usePriorInTrain ? "_usePrior" : "") + "_" + iter + "_" + String.valueOf(rate) + ".model";
                    System.out.println(modelPath);
                    /*-----TRAINING-----*/
                    if (retrain) {
                        SLModel model = SL_exp.train(trainset, configFilePath, null, iter, rate);
                        model.saveModel(modelPath);
                    }
                    /*-----TESTING ON DEV-----*/
                    for (double kl : thresholds) {
                        String tag = iter + "_" + String.valueOf(rate) + "_" + String.valueOf(kl);
                        String outdir = "./output/Chambers/SL/dev/pred" + "_" + tag;
                        IOUtils.mkdir(outdir);
                        System.out.println("Testing on Test Data");
                        SL_exp.test(devset, modelPath, outdir, true, kl);
                        /*Evaluation*/
                        Runtime rt = Runtime.getRuntime();
                        String cmd = "sh scripts/evaluate_general_dir.sh "+golddir+" "+outdir+" "+tag+" "+logdir;
                        Process pr = rt.exec(cmd);
                    }
                }
            }
        }
        else{
            String modelPath = SL_exp.MODELDIR+File.separator+"trained" + (usePriorInTrain ? "_usePrior" : "") + "_" + bestiter + "_" + String.valueOf(bestrate) + ".model";
            System.out.println(modelPath);
            /*-----TRAINING-----*/
            if (retrain) {
                SLModel model = SL_exp.train(trainset_all, configFilePath, null, bestiter, bestrate);
                model.saveModel(modelPath);
            }
            /*-----TESTING-----*/
            String outdir = "./output/Chambers/SL/pred" + "_" + bestiter + "_" + String.valueOf(bestrate) + "_" + String.valueOf(bestth);
            IOUtils.mkdir(outdir);
            System.out.println("Testing on Test Data");
            SLProblem testset = getStructuredTestData();
            SL_exp.test(testset, modelPath, outdir, false, bestth);
            /*Evaluation*/
            String golddir = "./output/Chambers/gold";
            String tag = bestiter + "_" + String.valueOf(bestrate) + "_" + String.valueOf(bestth);
            String logdir = "CAVEO";
            IOUtils.mkdir("./logs/"+logdir);
            Runtime rt = Runtime.getRuntime();
            String cmd = "sh scripts/evaluate_general_dir.sh "+golddir+" "+outdir+" "+tag+" "+logdir;
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
    public static temporalStructure test(SLModel model, temporalInstance ins)
            throws Exception {
        return (temporalStructure) model.infSolver.getBestStructure(model.wv,ins);
    }
    public static void test(SLProblem sp, String modelPath, String outdir, boolean gold_et_or_not, double kl_th)
            throws Exception {
        boolean force_update = true;
        String cacheDir = "serialized_data/TDTrain_SL";

        SLModel model = SLModel.loadModel(modelPath);
        PrecisionRecallManager evaluator = new PrecisionRecallManager();
        int corr = 0;
        int total = 0;

        HashMap<String,List<TLINK>> CAVEO_output = sieve_output.get_CAVEO_output();
        if(gold_et_or_not){
            for(int i = 0; i < sp.instanceList.size(); i++){
                //localets.add(((temporalInstance) sp.instanceList.get(i)).doc);
            }
        }
        else{
            if(usePriorInTest){

            }
            else {
                //localets = TempEval3Reader.deserialize("./serialized_data/ClearTK_Output");
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
            if(SL_exp.useDistantPairsLpI) {
                for (TemporalDocument doc : distantpairs) {
                    if (doc.getDocID().equals(docPred.getDocID())) {
                        List<TLINK> distant_et = doc.getEElinks();
                        for (TLINK tl : distant_et) {
                            EventChunk ec1 = doc.getEventMentionFromEIID(tl.getSourceId());
                            EventChunk ec2 = doc.getEventMentionFromEIID(tl.getTargetId());
                            int sentId1 = doc.getSentId(ec1);
                            int sentId2 = doc.getSentId(ec2);
                            if (!usePriorInTest) {
                                if (Math.abs(sentId1 - sentId2) >= dist_filter) {
                                    filtered.add(tl);
                                }
                            }
                        }
                        break;
                    }
                }
            }
            /*Add CAVEO ET*/
            filtered.addAll(CAVEO_output.get(docPred.getDocID()+".tml"));
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
        List<TemporalDocument> docs = TempEval3Reader.deserialize("./serialized_data/Chambers/raw");
        for(TemporalDocument doc:docs){
            if(doc.getBodyEventMentions().size()>MAX_EVENTS_Train)
                continue;
            if(TBDense_split.findDoc(doc.getDocID())!=1)
                continue;
            doc.saturateTlinks(1, false, true, true);
            Pair<IInstance, IStructure> pair = getSLPair(doc);
            problem.addExample(pair.getFirst(), pair.getSecond());
        }
        return problem;
    }
    public static SLProblem getStructuredDevData() throws Exception{
        SLProblem problem = new SLProblem();
        List<TemporalDocument> docs = TempEval3Reader.deserialize("./serialized_data/Chambers/raw");
        for(TemporalDocument doc:docs){
            if(doc.getBodyEventMentions().size()>MAX_EVENTS_Train)
                continue;
            if(TBDense_split.findDoc(doc.getDocID())!=2)
                continue;
            doc.saturateTlinks(1, false, true, true);
            Pair<IInstance, IStructure> pair = getSLPair(doc);
            problem.addExample(pair.getFirst(), pair.getSecond());
        }
        return problem;
    }
    public static SLProblem getStructuredTestData() throws Exception{
        SLProblem problem = new SLProblem();
        List<TemporalDocument> docs = TempEval3Reader.deserialize("./serialized_data/Chambers/raw");
        for(TemporalDocument doc:docs){
            if(doc.getBodyEventMentions().size()>MAX_EVENTS_Test)
                continue;
            if(TBDense_split.findDoc(doc.getDocID())!=3)
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
