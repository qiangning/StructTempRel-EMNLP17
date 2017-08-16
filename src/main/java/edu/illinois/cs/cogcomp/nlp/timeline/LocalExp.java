package edu.illinois.cs.cogcomp.nlp.timeline;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 1/15/17.
 */
public class LocalExp {
    public TempEval3Reader testReader;
    public List<TemporalDocument> testset;
    public List<TemporalDocument> resultset;
    public LocalEEClassifierExp localEE;
    public LocalETClassifierExp localET;
    public LocalExp() throws Exception {
        loadTestData();
        loadClassifier();
    }
    public void loadTestData() throws Exception {
        testReader = new TempEval3Reader("TIMEML","te3-platinum","data/TempEval3/Evaluation/");
        testReader.ReadData();
        testReader.removeDuplicatedTlinks();
        testReader.createTextAnnotation();
        testset = testReader.getDataset().getDocuments();
        resultset = new ArrayList<>();
    }
    public void loadClassifier() throws Exception{
        ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
        LocalETClassifierExp.addVagueTlinks = false;
        String modelname_et = LocalETClassifierExp.addVagueTlinks?
                (rm.getString("etModelName_none")+"_"+LocalETClassifierExp.vague_portion)
                :rm.getString("etModelName");
        LocalEEClassifierExp.addVagueTlinks = true;
        LocalEEClassifierExp.vague_portion = 0.01;
        String modelname_ee = LocalEEClassifierExp.addVagueTlinks?
                (rm.getString("eeModelName_none")+"_"+LocalEEClassifierExp.vague_portion)
                :rm.getString("eeModelName");
        //modelname_ee += "noBethardChambers";
        localET = new LocalETClassifierExp(null, testset,rm.getString("etModelDirPath"),modelname_et);

        localEE = new LocalEEClassifierExp(null, testset,rm.getString("eeModelDirPath"),modelname_ee);
    }
    public void setKnownNONEs(boolean knownNONEs){
        LocalEEClassifierExp.knownNONEs = knownNONEs;
        LocalEEClassifierExp.test_filter_on = !knownNONEs;
        LocalETClassifierExp.knownNONEs = knownNONEs;
        LocalETClassifierExp.filter_on = !knownNONEs;
    }
    public void run() throws Exception{
        boolean useClearTK_ET = true;
        List<TemporalDocument> localets = new ArrayList<>();
        if(useClearTK_ET)
            localets = TempEval3Reader.deserialize("./serialized_data/ClearTK_Output");
        for(TemporalDocument docGold:testset){
            System.out.printf("Solving: [" + (testset.indexOf(docGold) + 1) + "/" + testset.size() + "]:" + docGold.getDocID() + "...");
            TemporalDocument docPred_ee = localEE.testOnDoc(docGold, TLINK.ignore_tlink);
            TemporalDocument docPred_et = localET.testOnDoc(docGold, TLINK.ignore_tlink);
            List<TLINK> newlinks = docPred_ee.getEElinks();
            if(useClearTK_ET){
                for(TemporalDocument doc:localets){
                    if(doc.getDocID().equals(docGold.getDocID())) {
                        newlinks.addAll(doc.getETlinks());
                        break;
                    }
                }
            }
            else
                newlinks.addAll(docPred_et.getETlinks());
            TemporalDocument docPred = new TemporalDocument(docGold);
            docPred.setBodyTlinks(newlinks);
            resultset.add(docPred);
        }
    }
    public void write2disk(String dir) throws Exception{
        for(TemporalDocument docPred:resultset){
            docPred.temporalDocumentToText(dir+ File.separator+docPred.getDocID()+".tml");
            //docPred.serialize("serialized_data/McNemar-known/local",docPred.getDocID(),false);
        }
    }
    public void evaluate() throws Exception{
        Runtime rt = Runtime.getRuntime();
        //String cmd = "sh scripts/evaluate_test.sh local_unknown_noBethardChambers";
        String cmd = "sh scripts/evaluate_test.sh local_unknown";
        Process pr = rt.exec(cmd);
    }
    public static void main(String[] args) throws Exception{
        LocalExp exp = new LocalExp();
        exp.setKnownNONEs(false);
        //String dir = "./output/pred_local_unknown_noBethardChambers";
        String dir = "./output/pred_local_unknown";
        exp.run();
        exp.write2disk(dir);
        exp.evaluate();
    }
}
