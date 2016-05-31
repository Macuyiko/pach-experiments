package kfold;

import java.io.File;
import discovery.UmaSimplify;
import experiment.DirectoryExperiment;
import experiment.Globals;

public class FoldedUmaSimplify extends FoldedDirectoryMiner {
	private DirectoryExperiment miner;

	public FoldedUmaSimplify(File directory, String pattern, File outputDirectory, boolean skipIfExists, 
			File foldedLogsDir, File originalModelsDir) throws Exception {
		super(directory, pattern, outputDirectory, skipIfExists, foldedLogsDir);
		miner = new UmaSimplify(null, null, outputDirectory, true, originalModelsDir);
	}

	@Override
	public void foldedRun(File file, File outputDirectory, File outputTxt, File foldedLog, File foldedNegativeLog) {
		miner.run(foldedLog, outputDirectory, outputTxt);
	}

	public static void main(final String... args) throws Exception {
		FoldedUmaSimplify miner = new FoldedUmaSimplify(
				new File(Globals.poslogsdir), "\\.xes$", 
				new File(Globals.basedir + "kfold/models/mining/ilp_uma/"), true,
				new File(Globals.basedir + "kfold/logs/"),
				new File(Globals.basedir + "kfold/models/mining/ilp/"));
		miner.go();
		System.out.println("Experiment finished ---------------");
	}

}
