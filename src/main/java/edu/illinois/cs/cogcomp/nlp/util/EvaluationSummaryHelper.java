package edu.illinois.cs.cogcomp.nlp.util;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by qning2 on 4/2/17.
 */
public class EvaluationSummaryHelper {
    public static void main(String[] args) throws Exception{
        String dir = "logs/ClearTK_dev";
        String prefix = "evaluation_process_";
        String suffix = ".txt";
        //evaluation_process_chambers_codl_0.6_1.4_1
        int[] iters = {3,6,9,12};
        float[] rates = {0.005f,0.05f,0.5f};
        double[] kls = {0,0.2,0.4,0.6,0.8,1.0,1.2,1.4};
        for(int iter:iters) {
            for (float r : rates) {
                for (double kl : kls) {
                    try {
                        String fname = prefix + iter + "_" + String.valueOf(r) + "_" + String.valueOf(kl)+suffix;
                        printEval(dir, fname);
                    }
                    catch(Exception e){
                        continue;
                    }
                }
            }
        }
        /*String[] fname =
                {"chambers_codl_0.1_1.4_1",
                "chambers_codl_0.2_1.4_1",
                "chambers_codl_0.3_1.4_1",
                "chambers_codl_0.4_1.4_1",
                "chambers_codl_0.5_1.4_1",
                "chambers_codl_0.6_1.4_1"};
        for(String f:fname){
            System.out.println("--------Config: "+f+"--------");
            printEval(dir,prefix+f+suffix);*/

    }
    public static void printEval(String dir, String fname) throws Exception{
        Scanner in = new Scanner(new FileReader(dir+ File.separator+fname));
        System.out.println(dir+ File.separator+fname);
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if(line.equals("=== Temporal Awareness Score ==="))
                break;
        }
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if(line.isEmpty())
                continue;
            System.out.println(line);
        }
        System.out.println("");
    }
}
