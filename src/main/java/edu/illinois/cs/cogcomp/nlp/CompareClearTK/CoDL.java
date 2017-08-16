package edu.illinois.cs.cogcomp.nlp.CompareClearTK;

import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.MultiClassifiers;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ee.ee_perceptron;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.ReaderConfigurator;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.timeline.LocalEEClassifierExp;
import edu.illinois.cs.cogcomp.nlp.timeline.test.sieve_output;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by qning2 on 4/10/17.
 */

public class CoDL {
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
    public CoDL(double lambda, double kl_threshold) {
        this.lambda = lambda;
        this.kl_threshold = kl_threshold;
        ee_comb = new MultiClassifiers(lambda);
    }
    public void loadModelOnL() throws Exception{
        String dir = rm.getString("eeModelDirPath")+"CompClearTK/";
        String name = rm.getString("eeModelName")+"_TB_Bethard";
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
        List<TemporalDocument> testdoc = TempEval3Reader.deserialize(TempEval3Reader.label2dir("platinum"));
        for(TemporalDocument doc:testdoc){
            Platinum.add(doc);
        }
    }
    public void processUnlabeled(int iter) throws Exception{
        Unlabeled_T = new ArrayList<>();
        GlobalEE exp;
        exp = new GlobalEE(Unlabeled,ee_comb);
        GlobalEE.knownNONEs = false;
        GlobalEE.useBaselineProb = false;
        GlobalEE.use_cleartk_et = false;
        exp.serialize = true;
        if(iter==0)
            exp.cacheDir = "serialized_data/aug_Bethard/CoDL/"+"iter"+iter+"/"+"kl"+kl_threshold+"/";
        else
            exp.cacheDir = "serialized_data/aug_Bethard/CoDL/"+"iter"+iter+"/"+"kl"+kl_threshold+"/"+"lambda"+lambda+"/";
        exp.force_update = false;
        exp.kl_threshold = kl_threshold;
        exp.sentDistFilter = true;
        List<TemporalDocument> solvedDocs = exp.solve();
        /*Remove null docs*/

        for(TemporalDocument doc:solvedDocs){
            if(doc==null)
                continue;
            Unlabeled_T.add(doc);
        }
    }
    public ee_perceptron trainModelOnT(int iter, int silver, int round){
        LocalEEClassifierExp.vague_portion = 0.05;
        LocalEEClassifierExp.addVagueTlinks = false;
        LocalEEClassifierExp.feat_filter_on = true;
        LocalEEClassifierExp.ignore_edge = 2;
        String dir = rm.getString("eeModelDirPath")
                +File.separator+"codl_cleartk"
                +File.separator+String.valueOf(kl_threshold)+"_"+String.valueOf(lambda);
        IOUtils.mkdir(dir);
        LocalEEClassifierExp tester = new LocalEEClassifierExp(Unlabeled_T, null,
                dir, ee_on_T_modelName+iter+"_"+silver);
        tester.trainClassifier(round);
        tester.writeModelsToDisk();
        System.out.println("-----Performance of local EE-----");
        System.out.println("Iter="+iter);
        tester.TestOnTrain();
        return tester.getClassifier();
    }
    public void testModelOnP(String dir) throws Exception{
        GlobalEE exp;
        exp = new GlobalEE(Platinum,ee_comb);
        GlobalEE.knownNONEs = false;
        GlobalEE.useBaselineProb = false;
        GlobalEE.use_cleartk_et = true;
        exp.serialize = true;
        exp.cacheDir = "serialized_data/aug_Bethard/CoDL/Platinum/"+"lambda"+String.valueOf(lambda)+"_"+
                "kl"+String.valueOf(kl_threshold)+"/";
        exp.force_update = true;
        exp.kl_threshold = kl_threshold;
        exp.sentDistFilter = true;
        exp.sentDist = 1;

        exp.ClearTKOutput = TempEval3Reader.deserialize("./serialized_data/ClearTK_Output");

        List<TemporalDocument> predPlatinum = exp.solve();
        for(TemporalDocument doc:predPlatinum) {
            doc.temporalDocumentToText(dir + File.separator + doc.getDocID() + ".tml");
        }
    }

    public static void main(String[] args) throws Exception{
        double lambda = Double.valueOf(args[0]);
        double kl_th = Double.valueOf(args[1]);
        int maxIter = Integer.valueOf(args[2]);
        int silver = Integer.valueOf(args[3]);// silver has to be 1~6
        boolean retrain = Boolean.valueOf(args[4]);

        CoDL exp = new CoDL(lambda,kl_th);
        System.out.printf("--------Configuration--------\nlambda=%.2f\nKL threshold=%.2f\nmaxIter=%d\n",
                lambda,kl_th,maxIter);

        int train_rounds = 10;
        exp.loadModelOnL();
        int startIter = 0;
        if(retrain) {
            ResourceManager rm = new ReaderConfigurator().getDefaultConfig();
            //load U
            exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver0_label"));
            if(silver>=2)
                exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver1_label"));
            if(silver>=3)
                exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver2_label"));
            if(silver>=4)
                exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver3_label"));
            if(silver>=5)
                exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver4_label"));
            if(silver>=6)
                exp.loadUnlabeled(rm.getString("ser_dir") + File.separator + rm.getString("silver5_label"));
            //load existing model on T
            if(startIter>0)
                exp.loadModelOnT(startIter-1);
            for (int iter = startIter; iter < maxIter; iter++) {
                //proc U
                exp.processUnlabeled(iter);
                //exp.Unlabeled_T = exp.Unlabeled;

                //train on T
                ee_perceptron newclassifier = exp.trainModelOnT(iter, silver, train_rounds);

                //update multiclassifier
                exp.ee_comb.dropClassifier();
                exp.ee_comb.addClassifier(newclassifier);
            }
        }
        else{
            String savedir = exp.rm.getString("eeModelDirPath")
                    +File.separator+"codl_cleartk"
                    +File.separator+String.valueOf(kl_th)+"_"+String.valueOf(lambda);
            String modelpath = savedir+File.separator+exp.ee_on_T_modelName+(maxIter-1)+"_"+silver+".lc";
            String lexpath = savedir+File.separator+exp.ee_on_T_modelName+(maxIter-1)+"_"+silver+".lex";
            exp.ee_comb.dropClassifier();
            exp.ee_comb.addClassifier(new ee_perceptron(modelpath,lexpath));
        }
        String dir;
        dir = "./output/ClearTK/codl/Platinum"+File.separator+
                String.valueOf(lambda) + "_" +
                String.valueOf(kl_th) + "_"  +
                String.valueOf(maxIter);
        IOUtils.mkdir(dir);
        exp.loadPlatinum();
        exp.testModelOnP(dir);

        /*Evaluation*/
        String golddir = "./data/TempEval3/Evaluation/te3-platinum";
        String tag = "tbvc_codl_platinum_"+
                String.valueOf(lambda) + "_" +
                String.valueOf(kl_th) + "_"  +
                String.valueOf(maxIter);
        String logdir = "ClearTK_test/codl";
        IOUtils.mkdir("./logs/"+logdir);

        Runtime rt = Runtime.getRuntime();
        String cmd = "sh scripts/evaluate_general_dir.sh "+golddir+" "+dir+" "+tag+" "+logdir;
        Process pr = rt.exec(cmd);
    }
}
