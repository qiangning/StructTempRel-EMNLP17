package edu.illinois.cs.cogcomp.nlp.CompareCAVEO;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ParamLBJ;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.timeline.LocalEEClassifierExp;
import edu.illinois.cs.cogcomp.nlp.timeline.LocalETClassifierExp;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 4/3/17.
 */
public class localET {
    public static double vague_portion = 0.05;
    public static boolean addVagueTlinks = false;
    public static boolean filter_on = true;
    public static boolean knownNONEs = false;

    public static void init(){
        LocalETClassifierExp.vague_portion = vague_portion;
        LocalETClassifierExp.addVagueTlinks = addVagueTlinks;
        LocalETClassifierExp.filter_on = filter_on;
        LocalETClassifierExp.knownNONEs = knownNONEs;
    }
    public static void main(String[] args) throws Exception {
        localET.init();
        boolean retrain = false;

        /*loading train, dev, and test*/
        List<TemporalDocument> chambers = TempEval3Reader.deserialize(TempEval3Reader.label2dir("chambers_only"));
        List<TemporalDocument> testset = new ArrayList<>(), devset = new ArrayList<>(), trainset = new ArrayList<>();
        for(TemporalDocument doc:chambers){
            switch(TBDense_split.findDoc(doc.getDocID())){
                case 1:
                    //saturateTlinks(int maxIter, boolean debug, boolean force_update, boolean serialize)
                    doc.saturateTlinks(1,false,true,false);
                    trainset.add(doc);
                    break;
                case 2:
                    devset.add(doc);
                    break;
                case 3:
                    testset.add(doc);
                    break;
                default:
            }
        }
        ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
        String modelname = LocalETClassifierExp.addVagueTlinks?
                (rm.getString("etModelName_none")+"_"+LocalETClassifierExp.vague_portion)
                :rm.getString("etModelName");
        modelname += "_dense";
        LocalETClassifierExp tester = new LocalETClassifierExp(trainset, testset,rm.getString("etModelDirPath"),modelname);
        if (retrain) {
            tester.trainClassifier(ParamLBJ.etLearningRound);
            tester.writeModelsToDisk();
            tester.TestOnTrain();
        }
        tester.testClassifier();
    }
}
