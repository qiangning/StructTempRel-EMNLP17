package edu.uw.cs.lil.uwtime.data;

import java.util.Iterator;
import java.util.LinkedList;

import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;

public class TemporalChunkDataset implements IDataCollection<TemporalJointChunk>{
	private LinkedList<TemporalJointChunk> chunks;

	public TemporalChunkDataset() {
		chunks = new LinkedList<TemporalJointChunk>();
	}	

	public Iterator<TemporalJointChunk> iterator() {
		return chunks.iterator();
	}

	public int size() {
		return chunks.size();
	}
	
	public String toString() {
		return chunks.toString();
	}

	public static TemporalChunkDataset getGoldChunkDataset(IDataCollection<TemporalSentence> dataset) {
		TemporalChunkDataset chunkDataset = new TemporalChunkDataset();
		for (TemporalSentence sentence : dataset)
			for (TemporalJointChunk chunk : sentence.getLabel())
				chunkDataset.chunks.add(chunk);
		return chunkDataset;
	}
}
