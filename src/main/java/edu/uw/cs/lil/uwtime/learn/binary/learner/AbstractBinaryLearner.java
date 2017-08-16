package edu.uw.cs.lil.uwtime.learn.binary.learner;


import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryModel;

public abstract class AbstractBinaryLearner<DI extends IDataItem<?>> {
	protected final IDataCollection<DI>	  							trainingData;
	protected final IValidator<DI, Boolean>							validator;

	protected AbstractBinaryLearner(
			IDataCollection<DI> trainingData,
			IValidator<DI, Boolean> validator) {
		this.trainingData = trainingData;
		this.validator = validator;
	}

	public abstract void train(IBinaryModel<DI> model);
}
