package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.ReaderConfigurator;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import net.didion.jwnl.data.Exc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by qning2 on 4/4/17.
 * sieve means CAVEO. sieve_output means the output from CAVEO
 */
public class sieve_output {
    public static HashMap<String, List<TLINK>> get_CAVEO_output() throws Exception{
        return get_CAVEO_output("serialized_data/Chambers/sieve_output.ser","timebank");
    }
    public static HashMap<String, List<TLINK>> get_CAVEO_output(String dir, String label) throws Exception{
        HashMap<String,List<TLINK>> tlinks = new HashMap<>();
        try {
            FileInputStream fileIn = new FileInputStream(dir);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            tlinks = (HashMap<String,List<TLINK>>) in.readObject();
            in.close();
            fileIn.close();
        }catch(IOException i) {
            i.printStackTrace();
            return tlinks;
        }catch(ClassNotFoundException c) {
            System.out.println("Employee class not found");
            c.printStackTrace();
            return tlinks;
        }
        return validateTlinks(tlinks, label);
    }
    public static HashMap<String,List<TLINK>> validateTlinks(HashMap<String,List<TLINK>> tlinks) throws Exception{
        return validateTlinks(tlinks,"timebank");
    }
    public static HashMap<String,List<TLINK>> validateTlinks(HashMap<String,List<TLINK>> tlinks, String label) throws Exception{
        HashMap<String,List<TLINK>> newtlinks = new HashMap<>();
        for(String docId : tlinks.keySet()){
            newtlinks.put(docId,new ArrayList<>());
            String dir = TempEval3Reader.label2dir(label, true);
            TemporalDocument doc = TemporalDocument.deserialize(dir,docId.replaceAll(".tml",""),true);
            for(TLINK tl : tlinks.get(docId)){
                if(doc.validateTlink(tl)){
                    newtlinks.get(docId).add(tl);
                }
            }
        }
        return newtlinks;
    }
}
