package edu.illinois.cs.cogcomp.nlp.CompareClearTK;

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
        String dir = "./output/ClearTK/TBAQVCTD";
        List<TemporalDocument> trainset;
        trainset = TempEval3Reader.deserialize(new String[]{
                TempEval3Reader.label2dir("timebank",true),
                TempEval3Reader.label2dir("aquaint",true)});
        for(TemporalDocument doc:trainset){
            doc.temporalDocumentToText(dir + File.separator + doc.getDocID() + ".tml");
        }
    }
}
