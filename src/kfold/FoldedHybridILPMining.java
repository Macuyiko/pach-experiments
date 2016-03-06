package kfold;

import java.io.File;

import org.processmining.hybridilpminer.algorithms.decorators.HybridILPDecoratorImpl;

import discovery.HybridILPMining;
import experiment.DirectoryExperiment;
import experiment.Globals;

public class FoldedHybridILPMining extends FoldedDirectoryMiner {
	private DirectoryExperiment miner;

	public FoldedHybridILPMining(File directory, String pattern, File outputDirectory, boolean skipIfExists, 
			File foldedLogsDir) throws Exception {
		super(directory, pattern, outputDirectory, skipIfExists, foldedLogsDir);
		miner = new HybridILPMining(null, null, outputDirectory, true);
	}

	@Override
	public void foldedRun(File file, File outputDirectory, File outputTxt, File foldedLog, File foldedNegativeLog) {
		miner.run(foldedLog, outputDirectory, outputTxt);
	}

	public static void main(final String... args) throws Exception {
		HybridILPDecoratorImpl.USE_CUSTOM_SETUP = true;
		FoldedDirectoryMiner miner = new FoldedHybridILPMining(
				new File(Globals.poslogsdir),
				"\\.xes$", 
				new File(Globals.basedir + "kfold/hybridilp_positiveonly/"), 
				true,
				new File(Globals.basedir + "kfold/logs/"));
		miner.go();
		System.out.println("Experiment finished ---------------");
	}

}
