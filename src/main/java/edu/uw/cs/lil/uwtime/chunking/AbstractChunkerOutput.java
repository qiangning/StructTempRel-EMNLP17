package edu.uw.cs.lil.uwtime.chunking;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.uwtime.chunking.chunks.IChunk;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryClassifierOutput;

public abstract class AbstractChunkerOutput<CHUNK extends IChunk<?>> implements IDataItem<AbstractChunkerOutput<CHUNK>>{
	private static final long serialVersionUID = 2063112961477645669L;

	public abstract IBinaryClassifierOutput<? extends AbstractChunkerOutput<CHUNK>> getClassifierOutput();
	public abstract CHUNK getChunk();

	@Override
	public AbstractChunkerOutput<CHUNK> getSample() {
		return this;
	}
}
