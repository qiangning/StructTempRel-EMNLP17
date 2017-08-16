package edu.illinois.cs.cogcomp.nlp.CompareCAVEO;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import net.didion.jwnl.data.Exc;

import java.io.File;
import java.util.List;

/**
 * Created by qning2 on 4/11/17.
 */
public class GenTrainsetForClearTK {
    public static void main(String[] args) throws Exception{
        List<TemporalDocument> chambers = TempEval3Reader.deserialize(TempEval3Reader.label2dir("chambers_only"));
        String dir = "./output/ClearTK/TDTrain";
        String dir2 = "./output/ClearTK/TDTest";
        for (TemporalDocument doc : chambers) {
            if(TBDense_split.findDoc(doc.getDocID())==1){
                doc.temporalDocumentToText(dir + File.separator + doc.getDocID() + ".tml");
            }
            else if(TBDense_split.findDoc(doc.getDocID())==3){
                doc.temporalDocumentToText(dir2 + File.separator + doc.getDocID() + ".tml");
            }
        }
    }
}
