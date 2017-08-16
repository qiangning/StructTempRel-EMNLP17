package edu.illinois.cs.cogcomp.nlp.classifier.sl;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK.TlinkType;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;

/**
 * Created by qning2 on 12/26/16.
 */
public class temporalStructure implements IStructure {
    public int nE;
    public EventChunk[] events;
    public TlinkType[][] tlinks;
    private String[][] rels;

    public temporalStructure(temporalInstance tempInst){
        this.nE = tempInst.nE;
        events = tempInst.events;
        tlinks = new TlinkType[nE][nE];
        rels = null;
        for(int k=0; k<nE;k++){
            tlinks[k][k] = TlinkType.EQUAL;
        }
        for(int k=0; k<nE;k++){
            for(int m=k+1;m<nE;m++){
                TLINK tlink = tempInst.doc.getTlink(events[k],events[m]);
                tlinks[k][m] = tlink==null?TlinkType.UNDEF:tlink.getReducedRelType();
            }
            for(int m=0;m<=k;m++){
                tlinks[k][m] = tlinks[m][k].reverse();
            }
        }
    }
    public temporalStructure(int nE, String[][] rels){
        this.nE = nE;
        this.rels = rels;
        tlinks = new TlinkType[nE][nE];
        for(int k=0;k<nE;k++){
            tlinks[k][k] = TlinkType.EQUAL;
        }
        for(int k=0; k<nE;k++){
            for(int m=k+1;m<nE;m++){
                tlinks[k][m] = TlinkType.str2TlinkType(rels[k][m]);
            }
            for(int m=0;m<=k;m++){
                tlinks[k][m] = tlinks[m][k].reverse();
            }
        }
    }
    public String[][] getRelStr(){
        if(rels==null){
            rels = new String[nE][nE];
            for(int k=0;k<nE;k++){
                for(int m=0;m<nE;m++){
                    rels[k][m] = tlinks[k][m].toStringfull();
                }
            }
        }
        return rels;
    }
}
