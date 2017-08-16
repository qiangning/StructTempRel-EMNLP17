package edu.illinois.cs.cogcomp.nlp.timeline;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 2/5/17.
 */
public class LocalCVExp {
    public List<TemporalDocument> TBAQ;
    public List<TemporalDocument> trainset;
    public List<TemporalDocument> testset;
    public LocalEEClassifierExp tester;
    public void loadTBAQ()throws Exception{
        TBAQ = TempEval3Reader.deserialize(new String[]{
                TempEval3Reader.label2dir("timebank"),
                TempEval3Reader.label2dir("aquaint")});
    }
    public void loadTrain(int fold) throws Exception{
        trainset = new ArrayList<>();

        String dir = "output/cvfold_filename";
        String fname = "cvfold"+fold;
        File fin = new File(dir+File.separator+fname);
        BufferedReader br = new BufferedReader(new FileReader(fin));
        String line = null;
        while ((line = br.readLine()) != null) {
            if(line.equals("TRAIN"))
                continue;
            if(line.equals("TEST"))
                break;
            for(TemporalDocument doc:TBAQ){
                if(doc.getDocID().equals(line)) {
                    trainset.add(doc);
                    break;
                }
            }
        }
        br.close();

        /*Roll back to original tlinks (deser returns saturated tlinks)*/
        for(TemporalDocument doc:trainset){
            doc.setBodyTlinks(doc.getBodyTlinks_original());
        }
    }
    public void loadTest(int fold) throws Exception{
        testset = new ArrayList<>();

        String dir = "output/cvfold_filename";
        String fname = "cvfold"+fold;
        File fin = new File(dir+File.separator+fname);
        BufferedReader br = new BufferedReader(new FileReader(fin));
        String line = null;
        boolean flag = false;
        while ((line = br.readLine()) != null) {
            if(!flag){
                if(!line.equals("TEST"))
                    continue;
                else{
                    flag = true;
                    continue;
                }
            }
            else{
                for(TemporalDocument doc:TBAQ){
                    if(doc.getDocID().equals(line)) {
                        testset.add(doc);
                        break;
                    }
                }
            }
        }
        br.close();
        /*Roll back to original tlinks (deser returns saturated tlinks)*/
        for(TemporalDocument doc:testset){
            doc.setBodyTlinks(doc.getBodyTlinks_original());
        }
    }
    public void saturate(List<TemporalDocument> docs, int deg_sat) throws Exception{
        assert deg_sat>0;
        for(TemporalDocument doc:docs){
            doc.saturateTlinks(deg_sat,false,true);
        }
    }
    public void train(String modelDir, String modelName, int iter){
        tester = new LocalEEClassifierExp(trainset, null,modelDir,modelName);
        tester.trainClassifier(iter);
        tester.writeModelsToDisk();
        tester.TestOnTrain();
    }
    public void test(){
        tester.setTestDocs(testset);
        tester.testClassifier();
    }
    public static void main(String[] args) throws Exception{
        boolean retrain = args.length>0?Boolean.valueOf(args[0]):true;
        int fold = args.length>1?Integer.valueOf(args[1]):1;
        boolean sat = true;
        int deg_sat = 1;

        ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
        String modelDir = rm.getString("eeModelDirPath") + File.separator + "cv";
        String modelName = "ee_fold" + fold;
        LocalEEClassifierExp.addVagueTlinks = true;
        LocalEEClassifierExp.vague_portion = 0.05;
        LocalEEClassifierExp.feat_filter_on = false;
        LocalEEClassifierExp.test_filter_on = true;
        LocalEEClassifierExp.knownNONEs = false;

        /*Load data*/
        LocalCVExp exp = new LocalCVExp();
        exp.loadTBAQ();
        exp.loadTest(fold);
        if(retrain) {
            exp.loadTrain(fold);
            /*Saturation*/
            if (sat) {
                exp.saturate(exp.trainset, deg_sat);
            }
            /*Train local classifier*/
            exp.train(modelDir, modelName, 30);
        }
        else{
            exp.tester = new LocalEEClassifierExp(null, null,modelDir,modelName);
        }
        /*Evaluate on original platinum, only on EE*/
        exp.test();
    }
}
