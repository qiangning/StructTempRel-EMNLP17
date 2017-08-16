package edu.uw.cs.lil.uwtime.chunking.chunks;

import edu.uw.cs.lil.tiny.parser.IDerivation;

public interface IChunk<MR> {
	int getStart();
	int getEnd();
	boolean strictlyMatches(IChunk<MR> other);
	boolean overlapsWith(IChunk<MR> other);
	boolean contains(IChunk<MR> other);
	int getSpanSize();
	IDerivation<MR> getDerivation();
}
