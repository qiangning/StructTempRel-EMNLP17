package edu.uw.cs.lil.uwtime.chunking;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.utils.composites.Pair;

public class TemporalChunkerOutputDataset implements IDataCollection<TemporalChunkerOutput>{
    private List<TemporalChunkerOutput> allOutputs;
    private List<Pair<TemporalSentence, List<TemporalChunkerOutput>>> outputsBySentence;

    public TemporalChunkerOutputDataset() {
        outputsBySentence = new LinkedList<Pair<TemporalSentence, List<TemporalChunkerOutput>>> ();
        allOutputs = new LinkedList<TemporalChunkerOutput>();
    }

    @Override
    public Iterator<TemporalChunkerOutput> iterator() {
        return allOutputs.iterator();
    }

    @Override
    public int size() {
        return allOutputs.size();
    }

    public List<Pair<TemporalSentence, List<TemporalChunkerOutput>>> getOutputsBySentence() {
        return outputsBySentence;
    }

    public void addChunks(TemporalSentence sentence, List<TemporalChunkerOutput> outputs) {
        outputsBySentence.add(Pair.of(sentence, outputs));
        allOutputs.addAll(outputs);
    }
}
