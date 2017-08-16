package edu.uw.cs.lil.uwtime.chunking;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.uwtime.chunking.chunks.IChunk;
import edu.uw.cs.utils.composites.Pair;

public class ChunkSequence<CHUNK extends IChunk<MR>, MR> implements Serializable, Iterable<CHUNK> {
	private static final long serialVersionUID = -872797768769392408L;
	private List<CHUNK> chunks;
	private Map<Pair<Integer, Integer>, CHUNK> spanMap;

	public ChunkSequence() {
		this.chunks = new ArrayList<CHUNK>();
	}

	public List<CHUNK> getChunks() {
		return chunks;
	}

	public int getNumTotalTokens() {
		int sum = 0;
		for (CHUNK chunk : chunks)
			sum += (chunk.getEnd() + 1) - chunk.getStart();
		return sum;
	}

	public Set<Pair<Integer, Integer>> getSpanSet() {
		return getSpanMap().keySet();
	}

	public Map<Pair<Integer, Integer>, CHUNK> getSpanMap() {
		if (spanMap == null)
			spanMap = new HashMap<Pair<Integer, Integer>, CHUNK> ();
		for (CHUNK c : this)
			spanMap.put(Pair.of(c.getStart(),  c.getEnd()), c);
		return spanMap;
	}

	public String toString() {
		return chunks.toString();
	}

	public void insertChunk(CHUNK chunk) {
	    for (int i = chunks.size() - 1; i >= 0; i--) {
	        if (chunk.getStart() > chunks.get(i).getEnd()) {
	            chunks.add(i + 1, chunk);
	            return;
	        }  
	    }
	    chunks.add(0, chunk);
	}

	@Override
	public Iterator<CHUNK> iterator() {
		return chunks.iterator();
	}

	public int size() {
		return chunks.size();
	}
}
