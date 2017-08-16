package edu.illinois.cs.cogcomp.nlp.CompareCAVEO;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ParamLBJ;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.ReaderConfigurator;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.timeline.LocalEEClassifierExp;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 4/3/17.
 */
public class localEE {
    public static double vague_portion = 0.02;
    public static boolean addVagueTlinks = false;
    public static boolean feat_filter_on = false;// feature with dist>ignore_edge won't be extracted
    public static boolean test_filter_on = false;
    public static boolean knownNONEs = false;
    public static int ignore_edge = 2;

    public static void init(){
        LocalEEClassifierExp.vague_portion = vague_portion;
        LocalEEClassifierExp.addVagueTlinks = addVagueTlinks;
        LocalEEClassifierExp.feat_filter_on = feat_filter_on;
        LocalEEClassifierExp.test_filter_on = test_filter_on;
        LocalEEClassifierExp.knownNONEs = knownNONEs;
        LocalEEClassifierExp.ignore_edge = ignore_edge;
    }
    public static void main(String[] args) throws Exception {
        localEE.init();
        boolean retrain = true;
        boolean chambers_only = false;
        boolean TE3_only = false;// only use TE3 in training
        boolean removeVague = true;//remove vague links in Chambers dataset

        /*loading train, dev, and test*/
        List<TemporalDocument> chambers = TempEval3Reader.deserialize(TempEval3Reader.label2dir("chambers_only"));
        List<TemporalDocument> testset = new ArrayList<>();
        List<TemporalDocument> devset = new ArrayList<>();
        List<TemporalDocument> trainset = new ArrayList<>();
        for (TemporalDocument doc : chambers) {
            switch (TBDense_split.findDoc(doc.getDocID())) {
                case 1:
                    if (retrain&&!TE3_only) {
                        if (removeVague)
                            doc.removeVagueTlinks();
                        trainset.add(doc);
                    }
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
        if(retrain){// other files in TBAQ are added
            if(!chambers_only) {
                List<TemporalDocument> TBAQ = TempEval3Reader.deserialize(new String[]{
                        TempEval3Reader.label2dir("timebank", false),
                        TempEval3Reader.label2dir("aquaint", false)});
                for (TemporalDocument doc : TBAQ) {
                    if (TBDense_split.findDoc(doc.getDocID()) == 2 || TBDense_split.findDoc(doc.getDocID()) == 3) {
                        continue;
                    } else if (TBDense_split.findDoc(doc.getDocID()) == 1) {
                        /*for (int i = 0; i < trainset.size(); i++) {
                            if (doc.getDocID().equals(trainset.get(i).getDocID())) {
                                trainset.remove(i);
                                break;
                            }
                        }*/
                        if(TE3_only){
                            trainset.add(doc);
                        }
                        continue;
                    }
                    trainset.add(doc);
                }
            }
            for(TemporalDocument doc:trainset)
                doc.saturateTlinks(1, false, true, false);
        }
        ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
        String modelname = LocalEEClassifierExp.addVagueTlinks?
                (rm.getString("eeModelName_none")+"_"+LocalEEClassifierExp.vague_portion)
                :rm.getString("eeModelName");
        if(!TE3_only) {
            modelname += "_dense";
            if (removeVague)
                modelname += "_noV";
            if (!chambers_only)
                modelname += "_augTE3";
        }
        else{
            modelname += "_te3only_noChambers";
        }
        LocalEEClassifierExp tester = new LocalEEClassifierExp(trainset, testset,rm.getString("eeModelDirPath"),modelname);
        if (retrain) {
            tester.trainClassifier(ParamLBJ.eeLearningRound);
            tester.writeModelsToDisk();
            tester.TestOnTrain();
        }
        tester.testClassifier();
    }
}
