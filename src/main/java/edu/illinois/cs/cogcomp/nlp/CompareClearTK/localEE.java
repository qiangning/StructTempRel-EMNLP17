package edu.illinois.cs.cogcomp.nlp.CompareClearTK;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.CompareCAVEO.GlobalEE;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ParamLBJ;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.timeline.LocalEEClassifierExp;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 4/10/17.
 */
public class localEE {
    public static double vague_portion = 0.2;
    public static boolean addVagueTlinks = true;
    public static boolean feat_filter_on = false;// feature with dist>=ignore_edge won't be extracted
    public static boolean test_filter_on = true;
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
        List<TemporalDocument> TB_Bethard = null;
        List<TemporalDocument> Platinum = TempEval3Reader.deserialize(TempEval3Reader.label2dir("platinum"));
        if(retrain){
            TB_Bethard = TempEval3Reader.deserialize("./serialized_data/aug_Bethard/TimeBank");
            for(TemporalDocument doc:TB_Bethard)
                doc.saturateTlinks(1, false, true, false);
        }
        ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
        String modelname = LocalEEClassifierExp.addVagueTlinks?
                (rm.getString("eeModelName_none")+"_"+LocalEEClassifierExp.vague_portion)
                :rm.getString("eeModelName");
        modelname += "_TB_Bethard";
        LocalEEClassifierExp tester = new LocalEEClassifierExp(TB_Bethard, Platinum,rm.getString("eeModelDirPath")+"CompClearTK/",modelname);
        if (retrain) {
            tester.trainClassifier(ParamLBJ.eeLearningRound);
            tester.writeModelsToDisk();
            tester.TestOnTrain();
        }
        tester.testClassifier();

        /*Write tml to disk*/
        String dir = "./output/ClearTK/local";
        List<TemporalDocument> localets = TempEval3Reader.deserialize("./serialized_data/ClearTK_Output");
        for(TemporalDocument docGold:Platinum){
            System.out.printf("Solving: [" + (Platinum.indexOf(docGold) + 1) + "/" + Platinum.size() + "]:" + docGold.getDocID() + "...");
            TemporalDocument docPred_ee = tester.testOnDoc(docGold, TLINK.ignore_tlink);
            List<TLINK> newlinks = docPred_ee.getEElinks();
            for(TemporalDocument doc:localets){
                if(doc.getDocID().equals(docGold.getDocID())) {
                    newlinks.addAll(doc.getETlinks());
                    break;
                }
            }
            TemporalDocument docPred = new TemporalDocument(docGold);
            docPred.setBodyTlinks(newlinks);
            docPred.temporalDocumentToText(dir+ File.separator+docPred.getDocID()+".tml");
        }

        /*Evaluation*/
        Runtime rt = Runtime.getRuntime();
        String cmd = "sh scripts/evaluate_general.sh ./data/TempEval3/Evaluation/te3-platinum "+dir+" cleartk_vs_local";
        Process pr = rt.exec(cmd);
    }
}
