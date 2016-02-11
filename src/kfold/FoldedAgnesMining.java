package kfold;

import java.io.File;

import discovery.AgnesMining;
import experiment.DirectoryExperiment;
import experiment.Globals;

public class FoldedAgnesMining extends FoldedDirectoryMiner {
	
	private DirectoryExperiment miner;

	public FoldedAgnesMining(File directory, String pattern, File outputDirectory, boolean skipIfExists, 
			File foldedLogsDir) {
		super(directory, pattern, outputDirectory, skipIfExists, foldedLogsDir);
		miner = new AgnesMining(null, null, outputDirectory, true);
	}

	@Override
	public void foldedRun(File file, File outputDirectory, File outputTxt, File foldedLog, File foldedNegativeLog) {
		miner.run(foldedLog, outputDirectory, outputTxt);
	}

	public static void main(final String... args) throws Exception {
		FoldedDirectoryMiner miner = new FoldedAgnesMining(
				new File(Globals.poslogsdir),
				"\\.xes$", 
				new File(Globals.basedir + "kfold/agnesMiner/"), 
				true,
				new File(Globals.basedir + "kfold/logs/"));
		miner.go();
		System.out.println("Experiment finished ---------------");
	}

}
