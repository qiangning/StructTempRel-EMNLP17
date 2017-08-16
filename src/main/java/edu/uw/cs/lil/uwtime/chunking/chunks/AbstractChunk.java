package edu.uw.cs.lil.uwtime.chunking.chunks;

public abstract class AbstractChunk<MR> implements IChunk<MR>{
	@Override
	public boolean strictlyMatches(IChunk<MR> other) {
		return this.getStart() == other.getStart() && this.getEnd() == other.getEnd();
	}
	
	@Override
	public boolean overlapsWith(IChunk<MR> other) {
		return this.getStart() <= other.getEnd() && other.getStart() <= this.getEnd();
	}
	
	@Override
	public boolean contains(IChunk<MR> other) {
		return this.getStart() <= other.getStart() && this.getEnd() >= other.getEnd();
	}
	
	@Override
	public int getSpanSize() {
		return this.getEnd() - this.getStart() + 1;
	}
}
