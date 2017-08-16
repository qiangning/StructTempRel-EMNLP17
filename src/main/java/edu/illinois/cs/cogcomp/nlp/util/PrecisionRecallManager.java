package edu.illinois.cs.cogcomp.nlp.util;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;

import java.util.*;

/**
 * Created by qning2 on 11/28/16.
 * Adapted from Quang's version.
 */
public class PrecisionRecallManager {
    public class ResultStruct {
        public int numCorrect;
        public int numPredict;
        public int numKeys;
        public double prec;
        public double rec;
        public double f;

        public ResultStruct() {
        }
    }

    protected Map<String, Integer> mapLabelPrediction = null;
    protected Map<String, Integer> mapLabelGoldData = null;
    protected Map<String, Integer> mapCorrectPrediction = null;
    protected int correct = 0;
    protected int total = 0;
    protected HashMap<String, HashMap<String,Integer>> confusionMat = new HashMap<>();

    /**
     *
     */
    public PrecisionRecallManager() {
        mapLabelGoldData = new HashMap<String, Integer>();
        mapLabelPrediction = new HashMap<String, Integer>();
        mapCorrectPrediction = new HashMap<String, Integer>();
        correct = 0;
        total = 0;
    }

    public void reset(){
        mapLabelGoldData = new HashMap<String, Integer>();
        mapLabelPrediction = new HashMap<String, Integer>();
        mapCorrectPrediction = new HashMap<String, Integer>();
        correct = 0;
        total = 0;
        confusionMat = new HashMap<>();
    }

    /**
     * @param label
     */
    protected void addPrediction(String label) {
        if (mapLabelPrediction.containsKey(label)) {
            int c = mapLabelPrediction.get(label);
            mapLabelPrediction.put(label, c + 1);
        } else {
            mapLabelPrediction.put(label, 1);
        }
    }

    /**
     * @param label
     */
    protected void addGoldData(String label) {
        if (mapLabelGoldData.containsKey(label)) {
            int c = mapLabelGoldData.get(label);
            mapLabelGoldData.put(label, c + 1);
        } else {
            mapLabelGoldData.put(label, 1);
        }
    }

    /**
     * @param pred
     * @param gold
     */
    public void addPredGoldLabels(String pred, String gold) {
        addPrediction(pred);
        addGoldData(gold);
        addCorrectPred(pred, gold);
        total++;

        //Add to confusion matrix
        if(confusionMat.containsKey(pred)){
            if(confusionMat.get(pred).containsKey(gold)){
                int newval = confusionMat.get(pred).get(gold);
                confusionMat.get(pred).put(gold,newval+1);
            }
            else{
                confusionMat.get(pred).put(gold,1);
            }
        }
        else{
            HashMap<String,Integer> tmpHash = new HashMap<>();
            tmpHash.put(gold,1);
            confusionMat.put(pred,tmpHash);
        }
    }

    /**
     * @param pred
     * @param gold
     */
    protected void addCorrectPred(String pred, String gold) {
        if (gold.equals(pred)) {
            if (mapCorrectPrediction.containsKey(pred)) {
                int c = mapCorrectPrediction.get(pred);
                mapCorrectPrediction.put(pred, c + 1);
            } else {
                mapCorrectPrediction.put(pred, 1);
            }
            correct++;
        }
    }

    public double getAccuracy() {
        return (double) correct / (double) total;
    }

    public Map<String, Double> getPrecision() {
        Map<String, Double> mapPrecision = new HashMap<String, Double>();
        for (String label : mapLabelPrediction.keySet()) {
            int total = mapLabelPrediction.get(label);
            int correct = (mapCorrectPrediction.containsKey(label) ? mapCorrectPrediction
                    .get(label) : 0);
            mapPrecision.put(label, (double) correct / (double) total);
        }
        return mapPrecision;
    }

    public Map<String, Double> getRecall() {
        Map<String, Double> mapRecall = new HashMap<String, Double>();
        for (String label : mapLabelGoldData.keySet()) {
            int total = mapLabelGoldData.get(label);
            int correct = (mapCorrectPrediction.containsKey(label) ? mapCorrectPrediction
                    .get(label) : 0);
            mapRecall.put(label, (double) correct / (double) total);
        }
        return mapRecall;
    }

    public Map<String, Double> getFscore(Map<String, Double> mapPrec,
                                         Map<String, Double> mapRec) {

        Set<String> labels = (mapPrec.size() > mapRec.size() ? mapPrec.keySet()
                : mapRec.keySet());

        Map<String, Double> mapFscore = new HashMap<String, Double>();
        for (String label : labels) {
            double prec = (mapPrec.containsKey(label) ? mapPrec.get(label) : 0);
            double rec = (mapRec.containsKey(label) ? mapRec.get(label) : 0);
            mapFscore.put(label, (prec + rec == 0 ? 0 : (2 * prec * rec)
                    / (prec + rec)));
        }
        return mapFscore;
    }

    public void printAccuracy() {
        System.out.println("Correct=" + correct + ", Total=" + total);
        System.out.println("Micro-averaged Accuracy: "
                + ((double) correct / (double) total));
    }

    public void printPrecisionRecall() {
        Map<String, Double> mapPrec = getPrecision();
        Map<String, Double> mapRec = getRecall();
        Map<String, Double> mapFscore = getFscore(mapPrec, mapRec);
        double totalF1 = 0.0;
        System.out
                .println("| Label | Correct prediction | Total prediction | Total gold data | Precision | Recall | F1 score |");
        System.out
                .println("|-------------------------------------------------------------------------------------------------|");
        for (String label : mapFscore.keySet()) {
            System.out
                    .println("| "
                            + label
                            + " | "
                            + (mapCorrectPrediction.containsKey(label) ? mapCorrectPrediction
                            .get(label) : 0)
                            + " | "
                            + (mapLabelPrediction.containsKey(label) ? mapLabelPrediction
                            .get(label) : 0)
                            + " | "
                            + (mapLabelGoldData.containsKey(label) ? mapLabelGoldData
                            .get(label) : 0)
                            + " | "
                            + (mapPrec.containsKey(label) ? mapPrec.get(label)
                            : 0)
                            + " | "
                            + (mapRec.containsKey(label) ? mapRec.get(label)
                            : 0) + " | " + mapFscore.get(label) + " |");
            totalF1 += mapFscore.get(label);
        }
        System.out.println("Average F1: "
                + (totalF1 / (double) mapFscore.size()));
        //
        int tCorrects = 0;
        int tPredicts = 0;
        int tKeys = 0;
        for (String label : mapCorrectPrediction.keySet()) {
            tCorrects += mapCorrectPrediction.get(label);
        }

        for (String label : mapLabelPrediction.keySet()) {
            tPredicts += mapLabelPrediction.get(label);
        }

        for (String label : mapLabelGoldData.keySet()) {
            tKeys += mapLabelGoldData.get(label);
        }

        double prec = tCorrects / (double) tPredicts;
        double rec = tCorrects / (double) tKeys;
        double f = (2 * prec * rec) / (prec + rec);
        System.out.println();
        System.out.println("Prec = " + tCorrects + "/" + tPredicts + " = "
                + prec);
        System.out.println("Rec = " + tCorrects + "/" + tKeys + " = " + rec);
        System.out.println("F-score = " + f);
        System.out.println();
    }

    public void printPrecisionRecall(String[] ignoredLabels) {
        Set<String> setIgnoredLabels = new HashSet<String>();
        for (String l : ignoredLabels) {
            setIgnoredLabels.add(l);
        }
        Map<String, Double> mapPrec = getPrecision();
        Map<String, Double> mapRec = getRecall();
        Map<String, Double> mapFscore = getFscore(mapPrec, mapRec);
        double totalF1 = 0.0;
        System.out
                .println("| Label | Correct prediction | Total prediction | Total gold data | Precision | Recall | F1 score |");
        System.out
                .println("|-------------------------------------------------------------------------------------------------|");
        for (String label : mapFscore.keySet()) {
            System.out
                    .println("| "
                            + label
                            + " | "
                            + (mapCorrectPrediction.containsKey(label) ? mapCorrectPrediction
                            .get(label) : 0)
                            + " | "
                            + (mapLabelPrediction.containsKey(label) ? mapLabelPrediction
                            .get(label) : 0)
                            + " | "
                            + (mapLabelGoldData.containsKey(label) ? mapLabelGoldData
                            .get(label) : 0)
                            + " | "
                            + (mapPrec.containsKey(label) ? mapPrec.get(label)
                            : 0)
                            + " | "
                            + (mapRec.containsKey(label) ? mapRec.get(label)
                            : 0) + " | " + mapFscore.get(label) + " |");
            totalF1 += mapFscore.get(label);
        }
        System.out.println("Average F1: "
                + (totalF1 / (double) mapFscore.size()));
        //
        int tCorrects = 0;
        int tPredicts = 0;
        int tKeys = 0;
        for (String label : mapCorrectPrediction.keySet()) {
            if (setIgnoredLabels.contains(label)) {
                continue;
            }
            tCorrects += mapCorrectPrediction.get(label);
        }

        for (String label : mapLabelPrediction.keySet()) {
            if (setIgnoredLabels.contains(label)) {
                continue;
            }
            tPredicts += mapLabelPrediction.get(label);
        }

        for (String label : mapLabelGoldData.keySet()) {
            if (setIgnoredLabels.contains(label)) {
                continue;
            }
            tKeys += mapLabelGoldData.get(label);
        }

        double prec = tCorrects / (double) tPredicts;
        double rec = tCorrects / (double) tKeys;
        double f = (2 * prec * rec) / (prec + rec);
        System.out.println();
        System.out.println("Prec = " + tCorrects + "/" + tPredicts + " = "
                + prec);
        System.out.println("Rec = " + tCorrects + "/" + tKeys + " = " + rec);
        System.out.println("F-score = " + f);
        System.out.println();
    }

    public void printConfusionMatrix(){
        Set<String> keys = confusionMat.keySet();
        for(String key:keys){
            Set<String> keys2 = confusionMat.get(key).keySet();
            int total = 0;
            for(String key2:keys2)
                total+=confusionMat.get(key).get(key2);
            confusionMat.get(key).put("total",total);
            System.out.println(key+"="+confusionMat.get(key).toString());
        }
    }

    public HashMap<String, HashMap<String, Integer>> getConfusionMat() {
        return confusionMat;
    }

    public double getAveragedFscore() {
        Map<String, Double> mapPrec = getPrecision();
        Map<String, Double> mapRec = getRecall();
        Map<String, Double> mapFscore = getFscore(mapPrec, mapRec);
        double totalF1 = 0.0;
        for (String label : mapFscore.keySet()) {
            totalF1 += mapFscore.get(label);
        }
        return (totalF1 / (double) mapFscore.size());
    }

    public double getFscore(String[] ignoredLabels) {
        Set<String> setIgnoredLabels = new HashSet<String>();
        for (String l : ignoredLabels) {
            setIgnoredLabels.add(l);
        }
        int tCorrects = 0;
        int tPredicts = 0;
        int tKeys = 0;
        for (String label : mapCorrectPrediction.keySet()) {
            if (setIgnoredLabels.contains(label)) {
                continue;
            }
            tCorrects += mapCorrectPrediction.get(label);
            tPredicts += mapLabelPrediction.get(label);
            tKeys += mapLabelGoldData.get(label);
        }
        double prec = (tPredicts > 0 ? tCorrects / (double) tPredicts : 0.0);
        double rec = (tKeys > 0 ? tCorrects / (double) tKeys : 0.0);
        double f = (prec + rec > 0 ? (2 * prec * rec) / (prec + rec) : 0.0);
        return f;
    }

    public ResultStruct getResultStruct(String[] ignoredLabels) {
        Set<String> setIgnoredLabels = new HashSet<String>();
        for (String l : ignoredLabels) {
            setIgnoredLabels.add(l);
        }
        int tCorrects = 0;
        int tPredicts = 0;
        int tKeys = 0;
        for (String label : mapCorrectPrediction.keySet()) {
            if (setIgnoredLabels.contains(label)) {
                continue;
            }
            tCorrects += mapCorrectPrediction.get(label);
            tPredicts += mapLabelPrediction.get(label);
            tKeys += mapLabelGoldData.get(label);
        }
        double prec = (tPredicts > 0 ? tCorrects / (double) tPredicts : 0.0);
        double rec = (tKeys > 0 ? tCorrects / (double) tKeys : 0.0);
        double f = (prec + rec > 0 ? (2 * prec * rec) / (prec + rec) : 0.0);
        ResultStruct rs = new ResultStruct();
        rs.numCorrect = tCorrects;
        rs.numPredict = tPredicts;
        rs.numKeys = tKeys;
        rs.prec = prec;
        rs.rec = rec;
        rs.f = f;
        return rs;
    }

    /*Pair<Gold,Pred>*/
    public static PrecisionRecallManager ListPairs2PRM(List<Pair<String,String>> results){
        PrecisionRecallManager evaluator = new PrecisionRecallManager();
        for(Pair<String,String> res:results){
            evaluator.addPredGoldLabels(res.getSecond(),res.getFirst());
        }
        return evaluator;
    }
    public void addListPairs(List<Pair<String,String>> results){
        for(Pair<String,String> res:results){
            addPredGoldLabels(res.getSecond(),res.getFirst());
        }
    }
}
