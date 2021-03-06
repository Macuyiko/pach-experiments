package org.processmining.hybridilpminer.algorithms.decorators;

import org.processmining.hybridilpminer.models.abstraction.interfaces.LPLogAbstraction;
import org.processmining.hybridilpminer.models.lp.variablemapping.interfaces.HybridVariableMapping;
import org.processmining.hybridilpminer.parameters.HybridILPMinerParametersImpl;
import org.processmining.lpengines.interfaces.LPEngine.VariableType;

public class HybridILPDecoratorImpl<T extends HybridVariableMapping<Integer>> extends HybridLPDecoratorImpl<T> {

	public static boolean USE_CUSTOM_SETUP = true;
	
	public HybridILPDecoratorImpl(T varMap, HybridILPMinerParametersImpl configuration,
			LPLogAbstraction<?> logAbstraction) {
		super(varMap, configuration, logAbstraction);
	}

	protected void setupVariables() {
		if (!USE_CUSTOM_SETUP) {
			super.setupVariables();
			return;
		}
		System.err.println("Custom setupVars called");
		engine.setType(varMap.getMarkingVariableLPIndex(), VariableType.BOOLEAN);
		for (int i : varMap.getSingleVariableIndices()) {
			engine.setType(i, VariableType.INTEGER);
			engine.setLowerBound(i, Double.MIN_VALUE);
			engine.setUpperBound(i, Double.MAX_VALUE);
		}
		for (int i : varMap.getXVariableIndices()) {
			engine.setType(i, VariableType.INTEGER);
			engine.setLowerBound(i, Double.MIN_VALUE);
			engine.setUpperBound(i, Double.MAX_VALUE);
		}
		for (int i : varMap.getYVariableIndices()) {
			engine.setType(i, VariableType.INTEGER);
			engine.setLowerBound(i, Double.MIN_VALUE);
			engine.setUpperBound(i, Double.MAX_VALUE);
		}
	}
}
