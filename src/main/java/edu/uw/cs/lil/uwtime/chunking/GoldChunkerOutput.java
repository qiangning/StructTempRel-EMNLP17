package edu.uw.cs.lil.uwtime.chunking;

import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryClassifierOutput;
import edu.uw.cs.lil.uwtime.learn.temporal.GoldClassifierOutput;

public class GoldChunkerOutput extends AbstractChunkerOutput<TemporalJointChunk> {
	private static final long serialVersionUID = -8436813077121508308L;
	
	private final TemporalJointChunk goldChunk;
	public GoldChunkerOutput(TemporalJointChunk goldChunk) {
		this.goldChunk = new TemporalJointChunk(goldChunk);
		this.goldChunk.setResult(null);
	}
	
	public IBinaryClassifierOutput<GoldChunkerOutput> getClassifierOutput() {
		return new GoldClassifierOutput();
	}
	public TemporalJointChunk getChunk() {
		return goldChunk;
	}
}
