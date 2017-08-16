package edu.illinois.cs.cogcomp.nlp.timeline;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ParamLBJ;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.util.List;

/**
 * Created by qning2 on 1/31/17.
 */
public class LocalSatExp {
    public List<TemporalDocument> trainset;
    public List<TemporalDocument> testset;
    public LocalEEClassifierExp tester;
    public void loadTrain() throws Exception{
        trainset = TempEval3Reader.deserialize(new String[]{
                TempEval3Reader.label2dir("timebank"),
                TempEval3Reader.label2dir("aquaint")});
        /*Roll back to original tlinks (deser returns saturated tlinks)*/
        for(TemporalDocument doc:trainset){
            doc.setBodyTlinks(doc.getBodyTlinks_original());
        }
    }
    public void loadTest() throws Exception{
        testset = TempEval3Reader.deserialize(TempEval3Reader.label2dir("platinum"));
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
        boolean retrain = args.length>0?Boolean.valueOf(args[0]):false;
        boolean sat = args.length>1?Boolean.valueOf(args[1]):true;
        int deg_sat = args.length>2?Integer.valueOf(args[2]):1;

        ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
        String modelDir = rm.getString("eeModelDirPath") + File.separator + "sat";
        String modelName = "ee_" + (sat ? ("sat_" + String.valueOf(deg_sat)) : "unsat");
        LocalEEClassifierExp.addVagueTlinks = false;
        LocalEEClassifierExp.feat_filter_on = false;

        /*Load data*/
        LocalSatExp exp = new LocalSatExp();
        exp.loadTest();
        if(retrain) {
            exp.loadTrain();
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
