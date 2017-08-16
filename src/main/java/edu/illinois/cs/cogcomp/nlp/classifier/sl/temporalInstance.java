package edu.illinois.cs.cogcomp.nlp.classifier.sl;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 12/26/16.
 */
public class temporalInstance implements IInstance {
    public int nE;
    public EventChunk[] events;
    public TemporalDocument doc;

    public temporalInstance(TemporalDocument doc){
        this.doc = doc;
        nE = doc.getBodyEventMentions().size();
        events = new EventChunk[nE];
        for(int k=0; k<nE;k++){
            events[k] = doc.getBodyEventMentions().get(k);
        }
    }
    public TemporalDocument tempstruct2tempinst(temporalStructure predStruct){
        TemporalDocument docPred = new TemporalDocument(doc);
        docPred.setBodyTlinks(null);
        List<TLINK> predlinks = new ArrayList<>();
        int lid = 0;
        for(int k=0;k<nE;k++){
            EventChunk ec1 = events[k];
            for(int m=k+1;m<nE;m++){
                EventChunk ec2 = events[m];
                if(predStruct.tlinks[k][m] == TLINK.TlinkType.UNDEF)
                    continue;
                TLINK tlink = new TLINK(lid++,"", TempEval3Reader.Type_Event,TempEval3Reader.Type_Event,ec1.getEiid(),ec2.getEiid(),predStruct.tlinks[k][m]);
                predlinks.add(tlink);
            }
        }
        docPred.setBodyTlinks(predlinks);
        return docPred;
    }
}
