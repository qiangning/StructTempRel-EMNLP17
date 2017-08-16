package edu.uw.cs.lil.uwtime.chunking.chunks;

public abstract class AbstractJointChunk<MR, ERESULT> extends AbstractChunk<MR>{
	public abstract ERESULT getResult();
	public abstract IChunk<MR> getBaseChunk();
}
