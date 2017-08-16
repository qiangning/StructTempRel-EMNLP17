package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.MultiClassifiers;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ScoringFunc;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ee.ee_perceptron;
import edu.illinois.cs.cogcomp.nlp.classifier.my_ee_perceptron;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.timeline.GlobalEEClassifierExp;
import edu.illinois.cs.cogcomp.nlp.timeline.LocalEEClassifierExp;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 1/17/17.
 */
public class Simple_CoDL_Exp {
    public int MAX_EVENTS = 60;
    public double lambda = 0.7;
    public ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
    private AnnotatorService pipeline;
    private String ee_on_T_modelName = "ee_on_T2";
    public ee_perceptron ee_on_L;
    public ee_perceptron ee_on_T;
    public List<TemporalDocument> Unlabeled = new ArrayList<>();
    public List<TemporalDocument> Unlabeled_T = new ArrayList<>();
    public List<TemporalDocument> Platinum = new ArrayList<>();

    public Simple_CoDL_Exp() {
    }

    public void loadModelOnL() throws Exception{
        String dir = rm.getString("eeModelDirPath");
        String name = rm.getString("eeModelName_none")+"_0.05";
        String modelPath = dir+name+".lc";
        String lexPath = dir+name+".lex";
        ee_on_L = new ee_perceptron(modelPath,lexPath);
    }
    public void loadModelOnT() throws Exception{
        String dir = rm.getString("eeModelDirPath");
        String modelPath = dir+ee_on_T_modelName+".lc";
        String lexPath = dir+ee_on_T_modelName+".lex";
        ee_on_T = new ee_perceptron(modelPath,lexPath);
    }
    public void loadUnlabeled() throws Exception{
        TempEval3Reader testReader;
        testReader = new TempEval3Reader("TIMEML", "TE3-Silver-data-0", "data/TempEval3/Training/");
        testReader.ReadData();
        testReader.removeDuplicatedTlinks();
        pipeline = testReader.getPipeline();
        for(TemporalDocument doc:testReader.getDataset().getDocuments())
            if(doc.getBodyEventMentions().size()<=MAX_EVENTS)
                Unlabeled.add(doc);

        /*testReader = new TempEval3Reader("TIMEML", "TE3-Silver-data-1", "data/TempEval3/Training/");
        testReader.ReadData();
        testReader.removeDuplicatedTlinks();
        pipeline = testReader.getPipeline();
        for(TemporalDocument doc:testReader.getDataset().getDocuments())
            if(doc.getBodyEventMentions().size()<=MAX_EVENTS)
                Unlabeled.add(doc);*/
    }
    public void processUnlabeled() throws Exception{
        GlobalEEClassifierExp exp;
        exp = new GlobalEEClassifierExp(Unlabeled,new my_ee_perceptron(ee_on_L));
        GlobalEEClassifierExp.knownNONEs = false;
        GlobalEEClassifierExp.useBaselineProb = false;
        exp.serialize = true;
        exp.cacheDir = "serialized_data/CoDL/";
        exp.force_update = false;
        Unlabeled_T = exp.solve();
    }
    public void trainModelOnU(int round){
        LocalEEClassifierExp tester = new LocalEEClassifierExp(Unlabeled_T, null,rm.getString("eeModelDirPath"),ee_on_T_modelName);
        LocalEEClassifierExp.vague_portion = 0.05;
        LocalEEClassifierExp.addVagueTlinks = true;
        tester.trainClassifier(round);
        tester.writeModelsToDisk();
        tester.TestOnTrain();
        ee_on_T = tester.getClassifier();
    }
    public void loadPlatinum() throws Exception{
        TempEval3Reader testReader;
        testReader = new TempEval3Reader("TIMEML", "te3-platinum", "data/TempEval3/Evaluation/");
        testReader.ReadData();
        testReader.removeDuplicatedTlinks();
        pipeline = testReader.getPipeline();
        Platinum = testReader.getDataset().getDocuments();
    }
    public void testModelOnP() throws Exception{
        MultiClassifiers multiClassifiers = new MultiClassifiers(lambda);
        multiClassifiers.addClassifier(ee_on_L);
        multiClassifiers.addClassifier(ee_on_T);

        GlobalEEClassifierExp exp;
        exp = new GlobalEEClassifierExp(Platinum,multiClassifiers);
        GlobalEEClassifierExp.knownNONEs = false;
        GlobalEEClassifierExp.useBaselineProb = false;
        exp.serialize = true;
        exp.cacheDir = "serialized_data/CoDL/";
        exp.force_update = true;
        List<TemporalDocument> predPlatinum = exp.solve();
        for(TemporalDocument doc:predPlatinum)
            doc.temporalDocumentToText("./output/pred/"+doc.getDocID()+".tml");
    }
    public static void main(String[] args) throws Exception{
        boolean retrain = false;
        Simple_CoDL_Exp exp = new Simple_CoDL_Exp();
        exp.loadModelOnL();
        if(retrain) {
            exp.loadUnlabeled();
            exp.processUnlabeled();
            exp.trainModelOnU(30);
        }
        else{
            exp.loadModelOnT();
        }

        exp.loadPlatinum();
        exp.testModelOnP();
    }
}
