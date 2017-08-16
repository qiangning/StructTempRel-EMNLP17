package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import net.didion.jwnl.data.Exc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 4/10/17.
 */
public class EvalClearTK {
    public static void main(String[] args) throws Exception{
        List<TemporalDocument> cleartk = TempEval3Reader.deserialize("./serialized_data/ClearTK_Output");// now the E-E is from ours
        String dir = "./output/ClearTK/cleartk/Platinum";
        for(TemporalDocument doc : cleartk){
            doc.temporalDocumentToText(dir+ File.separator+doc.getDocID()+".tml");
        }

        Runtime rt = Runtime.getRuntime();
        String cmd = "sh scripts/evaluate_general.sh ./data/TempEval3/Evaluation/te3-platinum " + dir + " "+
                "cleartk";
        Process pr = rt.exec(cmd);
    }
}
