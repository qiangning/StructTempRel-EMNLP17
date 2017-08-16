package edu.illinois.cs.cogcomp.nlp.timeline;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.lbjava.classify.ScoreSet;
import edu.illinois.cs.cogcomp.lbjava.learn.Lexicon;
import edu.illinois.cs.cogcomp.lbjava.learn.SparseNetworkLearner;
import edu.illinois.cs.cogcomp.nlp.classifier.FeatureExtractor;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.LearningObj;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ParamLBJ;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.util.ExecutionTimeUtil;
import edu.illinois.cs.cogcomp.nlp.util.PrecisionRecallManager;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by qning2 on 12/12/16.
 */
public abstract class LocalClassifier {
    protected List<String> trainfeats = new ArrayList<>();
    protected List<String> testfeats = new ArrayList<>();
    protected SparseNetworkLearner classifier;
    protected HashMap<String,FeatureExtractor> featureExtractor = new HashMap<>();
    protected String modelDirPath;
    protected String modelName;
    protected PrecisionRecallManager evaluator = new PrecisionRecallManager();
    public LocalClassifier(String modelDirPath, String modelName){
        this.modelDirPath = modelDirPath;
        this.modelName = modelName;
    }
    public FeatureExtractor getFeatureExtractor(TemporalDocument doc){
        FeatureExtractor extractor;
        if(featureExtractor.containsKey(doc.getDocID())) {
            extractor = featureExtractor.get(doc.getDocID());
        }
        else{
            extractor = new FeatureExtractor(doc);
            featureExtractor.put(doc.getDocID(),extractor);
        }
        return extractor;
    }
    public abstract List<String> ExtractFeatures(TemporalDocument doc);

    public void CleanFeatures(){
        CleanTrainFeats();
        CleanTestFeats();
    }
    public void CleanTrainFeats(){
        trainfeats = new ArrayList<>();
    }
    public void CleanTestFeats(){
        testfeats = new ArrayList<>();
    }
    public void addTrainFeats(TemporalDocument doc){
        trainfeats.addAll(ExtractFeatures(doc));
    }
    public void addTestFeats(TemporalDocument doc){
        testfeats.addAll(ExtractFeatures(doc));
    }

    protected LearningObj[] feat2learnobj(List<String> feats){
        int n = feats.size();
        LearningObj[] learningObjs = new LearningObj[n];
        for (int i = 0; i < n; i++) {
            String ex = feats.get(i);
            int pos = ex.lastIndexOf(ParamLBJ.FEAT_DELIMITER);
            String featString = ex.substring(0, pos);
            String label = ex.substring(pos + 1).trim();
            LearningObj obj = new LearningObj(featString, label);
            learningObjs[i] = obj;
        }
        return learningObjs;
    }
    public void trainClassifier(int Round){
        ExecutionTimeUtil timer = new ExecutionTimeUtil();
        timer.start();
        classifier.forget();
        LearningObj[] trainObjs = feat2learnobj(trainfeats);
        System.out.print("--Round: ");
        for (int round = 0; round < Round; round++) {
            System.out.print((round + 1) + " ");
            classifier.learn(trainObjs);
        }
        classifier.doneLearning();
        timer.end();
        System.out.println("Training time: " + timer.getTimeSeconds());

        Lexicon lexicon = classifier.getLexicon();
        System.out.println("# of features: " + lexicon.size());
    }
    public void TestOnTrain(){
        testClassifier(trainfeats);
    }
    public void testClassifier(){
        testClassifier(testfeats);
    }
    public Pair<String,String> testClassifier(String feat){
        int pos = feat.lastIndexOf(ParamLBJ.FEAT_DELIMITER);
        String goldLabel = feat.substring(pos+1).trim();
        LearningObj obj = new LearningObj(feat.substring(0,pos).trim(),goldLabel);
        ScoreSet scores = classifier.scores(obj);
        String predLabel = scores.highScoreValue();
        return new Pair<>(goldLabel,predLabel);
    }
    public void testClassifier(List<String> feats){
        evaluator.reset();
        for(String feat:feats){
            Pair<String,String> result = testClassifier(feat);
            String goldLabel = result.getFirst();
            String predLabel = result.getSecond();
            evaluator.addPredGoldLabels(predLabel,goldLabel);
        }
        evaluator.printPrecisionRecall(new String[] {TLINK.TlinkType.UNDEF.toStringfull()});
        evaluator.printConfusionMatrix();
    }
    public void writeModelsToDisk() {
        IOUtils.mkdir(modelDirPath);
        classifier.save();
        System.out.println("Done training, models are in " + modelDirPath);
    }
    public void writeModelsToDisk(String dir, String modelName){
        IOUtils.mkdir(dir);
        classifier.write(dir + File.separator + modelName + ".lc", dir + File.separator + modelName + ".lex");
        System.out.println("Done training, models are in " + dir+File.separator+modelName+".lc (.lex)");
    }
}
