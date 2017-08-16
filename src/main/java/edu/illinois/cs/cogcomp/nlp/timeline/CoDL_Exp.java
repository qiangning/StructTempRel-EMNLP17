package edu.illinois.cs.cogcomp.nlp.timeline;

import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.MultiClassifiers;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ee.ee_perceptron;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.ReaderConfigurator;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.util.IOManager;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import org.apache.xpath.operations.Bool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Created by qning2 on 1/20/17.
 */
public class CoDL_Exp {
    public int MAX_EVENTS = 60;
    public boolean debug = false;
    public double lambda;
    public double kl_threshold;
    public ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
    public String ee_on_T_modelName = "ee_on_T";
    public MultiClassifiers ee_comb;
    public List<TemporalDocument> Unlabeled = new ArrayList<>();
    public List<TemporalDocument> Unlabeled_T = new ArrayList<>();
    public List<TemporalDocument> Platinum = new ArrayList<>();
    public CoDL_Exp(double lambda, double kl_threshold) {
        this.lambda = lambda;
        this.kl_threshold = kl_threshold;
        ee_comb = new MultiClassifiers(lambda);
    }
    public void loadModelOnL() throws Exception{
        /*String dir = rm.getString("eeModelDirPath");
        String name = rm.getString("eeModelName_none")+"_0.05";*/
        String dir = rm.getString("eeModelDirPath");
        String name = rm.getString("eeModelName");
        String modelPath = dir+name+".lc";
        String lexPath = dir+name+".lex";
        ee_comb.addClassifier(new ee_perceptron(modelPath,lexPath));
        ee_comb.addClassifier(new ee_perceptron(modelPath,lexPath));
    }
    public void loadModelOnT(int iter) throws Exception{
        // the model path might be wrong
        String dir = rm.getString("eeModelDirPath");
        String modelPath = dir+ee_on_T_modelName+iter+".lc";
        String lexPath = dir+ee_on_T_modelName+iter+".lex";
        ee_comb.dropClassifier();
        ee_comb.addClassifier(new ee_perceptron(modelPath,lexPath));
    }
    public void loadUnlabeled(String dir) throws Exception{
        if(Unlabeled == null)
            Unlabeled = new ArrayList<>();
        File file = new File(dir);
        File[] filelist = file.listFiles();
        if(filelist==null) {
            System.out.println(dir+" is empty.");
            return;
        }
        System.out.println("Loading from "+dir+". In total "+filelist.length+" files.");
        int cnt = 0;
        for(File f:filelist){
            if(f.isFile()) {
                //System.out.println(f.getName());
                //deserialize doc
                TemporalDocument doc = TemporalDocument.deserialize(dir,f.getName().substring(0,f.getName().indexOf(".ser")),false);
                //<max events
                if(doc.getBodyEventMentions().size()>MAX_EVENTS){
                    System.out.println("Too many events. "+doc.getDocID()+" is skipped.");
                    continue;
                }
                //annotation exists
                if(doc.getTextAnnotation()==null){
                    System.out.println("TextAnnotation doesn't exist. "+doc.getDocID()+" is skipped.");
                    continue;
                }
                if(debug) {
                    cnt++;
                    if(cnt>6)
                        break;
                }

                Unlabeled.add(doc);
            }
        }
    }
    public void loadPlatinum() throws Exception{
        Platinum = new ArrayList<>();
        ResourceManager rm = new ReaderConfigurator().getDefaultConfig();
        String dir = rm.getString("ser_dir")+ File.separator+rm.getString("platinum_label");
        File file = new File(dir);
        File[] filelist = file.listFiles();
        if(filelist==null) {
            System.out.println(dir+" is empty.");
            return;
        }
        int cnt = 0;
        for(File f:filelist){
            if(!f.isFile())
                continue;
            TemporalDocument doc = TemporalDocument.deserialize(dir,f.getName().substring(0,f.getName().indexOf(".ser")),false);
            if(debug) {
                cnt++;
                if(cnt>3)
                    break;
            }
            Platinum.add(doc);
        }
    }
    public void processUnlabeled(int iter) throws Exception{
        Unlabeled_T = new ArrayList<>();
        GlobalEEClassifierExp exp;
        exp = new GlobalEEClassifierExp(Unlabeled,ee_comb);
        GlobalEEClassifierExp.knownNONEs = false;
        GlobalEEClassifierExp.useBaselineProb = false;
        exp.serialize = true;
        if(iter==0)
            exp.cacheDir = "serialized_data/CoDL/"+"iter"+iter+"/"+"kl"+kl_threshold+"/";
        else
            exp.cacheDir = "serialized_data/CoDL/"+"iter"+iter+"/"+"kl"+kl_threshold+"/"+"lambda"+lambda+"/";
        exp.force_update = false;
        exp.kl_threshold = kl_threshold;
        exp.sentDistFilter = true;
        List<TemporalDocument> solvedDocs = exp.solve();
        /*Remove null docs*/
        for(TemporalDocument doc:solvedDocs){
            if(doc==null)
                continue;
            Unlabeled_T.add(doc);

            /*Apply filter: only pairs that are consistent with silver are kept*/
            /*List<TLINK> predlinks = doc.getBodyTlinks();
            TemporalDocument silverdoc = Unlabeled.get(solvedDocs.indexOf(doc));
            List<TLINK> filteredlinks = new ArrayList<>();
            for(TLINK tlink:predlinks){
                if(!tlink.getSourceType().equals(TempEval3Reader.Type_Event)
                        ||!tlink.getTargetType().equals(TempEval3Reader.Type_Event)) {
                    filteredlinks.add(tlink);
                    continue;
                }
                EventChunk ec1 = doc.getEventMentionFromEIID(tlink.getSourceId());
                EventChunk ec2 = doc.getEventMentionFromEIID(tlink.getTargetId());
                TLINK silvertlink = silverdoc.getTlink(ec1,ec2);
                if(silvertlink!=null
                        &&silvertlink.getReducedRelType()==tlink.getReducedRelType())
                    filteredlinks.add(tlink);
            }
            doc.setBodyTlinks(filteredlinks);*/
        }
    }
    public ee_perceptron trainModelOnT(int iter, int round){
        LocalEEClassifierExp.vague_portion = 0.05;
        LocalEEClassifierExp.addVagueTlinks = false;
        LocalEEClassifierExp.feat_filter_on = true;
        LocalEEClassifierExp.ignore_edge = 2;
        String dir = rm.getString("eeModelDirPath")
                +File.separator+"codl"
                +File.separator+String.valueOf(kl_threshold)+"_"+String.valueOf(lambda);
        IOUtils.mkdir(dir);
        LocalEEClassifierExp tester = new LocalEEClassifierExp(Unlabeled_T, null,
                dir, ee_on_T_modelName+iter);
        tester.trainClassifier(round);
        tester.writeModelsToDisk();
        System.out.println("-----Performance of local EE-----");
        System.out.println("Iter="+iter);
        tester.TestOnTrain();
        return tester.getClassifier();
    }
    public void testModelOnP(String dir) throws Exception{
        GlobalEEClassifierExp exp;
        exp = new GlobalEEClassifierExp(Platinum,ee_comb);
        GlobalEEClassifierExp.knownNONEs = false;
        GlobalEEClassifierExp.useBaselineProb = false;
        exp.serialize = true;
        exp.cacheDir = "serialized_data/CoDL/Platinum/"+"lambda"+String.valueOf(lambda)+"_"+
            "kl"+String.valueOf(kl_threshold)+"/";
        exp.force_update = true;
        exp.kl_threshold = kl_threshold;
        exp.sentDistFilter = true;
        List<TemporalDocument> predPlatinum = exp.solve();

        List<TemporalDocument> cleartk = TempEval3Reader.deserialize("./serialized_data/ClearTK_Output");
        List<TemporalDocument> distantpairs = TempEval3Reader.deserialize("./serialized_data/LpI_farpairs");
        for(TemporalDocument doc:predPlatinum) {
            doc.removeETlinks();
            doc.removeTTlinks();
            for(TemporalDocument tkdoc:cleartk){
                if(tkdoc.getDocID().equals(doc.getDocID())) {
                    doc.getBodyTlinks().addAll(tkdoc.getETlinks());
                    break;
                }
            }
            /*for(TemporalDocument lpidoc:distantpairs){
                if(lpidoc.getDocID().equals(doc.getDocID())){
                    List<TLINK> distant_et = lpidoc.getEElinks();
                    for(TLINK tl:distant_et) {
                        EventChunk ec1 = lpidoc.getEventMentionFromEIID(tl.getSourceId());
                        EventChunk ec2 = lpidoc.getEventMentionFromEIID(tl.getTargetId());
                        int sentId1 = lpidoc.getSentId(ec1);
                        int sentId2 = lpidoc.getSentId(ec2);
                        if (exp.sentDistFilter && Math.abs(sentId1 - sentId2) >= 1){
                            doc.getBodyTlinks().add(tl);
                        }
                    }
                    break;
                }
            }*/
            doc.temporalDocumentToText(dir + File.separator + doc.getDocID() + ".tml");
        }
    }

    public static void main(String[] args) throws Exception{
        /*ResourceManager rm = new ReaderConfigurator().getDefaultConfig();
        CoDL_Exp exp = new CoDL_Exp();
        exp.loadModelOnL();
        exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver0_label"));
        exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver1_label"));
        exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver2_label"));
        exp.processUnlabeled(0);
        ee_perceptron newclassifier = exp.trainModelOnT(0, 10);
        exp.ee_comb.dropClassifier();
        exp.ee_comb.addClassifier(newclassifier);
        exp.loadPlatinum();
        exp.testModelOnP();*/

        // args = lambda kl_th maxIter retrain
        double lambda = Double.valueOf(args[0]);
        double kl_th = Double.valueOf(args[1]);
        int maxIter = Integer.valueOf(args[2]);
        boolean retrain = Boolean.valueOf(args[3]);
        CoDL_Exp exp = new CoDL_Exp(lambda,kl_th);
        //exp.lambda = lambda;
        //exp.kl_threshold = kl_th;
        System.out.printf("--------Configuration--------\nlambda=%.2f\nKL threshold=%.2f\nmaxIter=%d\n",
                lambda,kl_th,maxIter);

        //boolean retrain = true;
        int train_rounds = 10;
        exp.loadModelOnL();
        //int maxIter = 1;
        int startIter = 0;
        if(retrain) {
            ResourceManager rm = new ReaderConfigurator().getDefaultConfig();
            //load U
            exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver0_label"));
            exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver1_label"));
            exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver2_label"));
            exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver3_label"));
            exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver4_label"));
            exp.loadUnlabeled(rm.getString("ser_dir") + File.separator + rm.getString("silver5_label"));
            //load existing model on T
            if(startIter>0)
                exp.loadModelOnT(startIter-1);
            for (int iter = startIter; iter < maxIter; iter++) {
                //proc U
                exp.processUnlabeled(iter);
                //exp.Unlabeled_T = exp.Unlabeled;

                //train on T
                ee_perceptron newclassifier = exp.trainModelOnT(iter, train_rounds);

                //update multiclassifier
                exp.ee_comb.dropClassifier();
                exp.ee_comb.addClassifier(newclassifier);
            }
        }
        else{
            String savedir = exp.rm.getString("eeModelDirPath")
                    +File.separator+"codl"
                    +File.separator+String.valueOf(kl_th)+"_"+String.valueOf(lambda);
            String modelpath = savedir+File.separator+exp.ee_on_T_modelName+(maxIter-1)+".lc";
            String lexpath = savedir+File.separator+exp.ee_on_T_modelName+(maxIter-1)+".lex";
            exp.ee_comb.dropClassifier();
            exp.ee_comb.addClassifier(new ee_perceptron(modelpath,lexpath));
        }
        String dir = "./output/pred_"+
                String.valueOf(lambda) + "_" +
                String.valueOf(kl_th) + "_"  +
                String.valueOf(maxIter);
        IOUtils.mkdir(dir);
        exp.loadPlatinum();
        exp.testModelOnP(dir);

        /*Evaluation*/
        Runtime rt = Runtime.getRuntime();
        String cmd = "sh scripts/evaluate_test.sh " +
                String.valueOf(lambda) + "_" +
                String.valueOf(kl_th) + "_"  +
                String.valueOf(maxIter);
        Process pr = rt.exec(cmd);
    }
}
