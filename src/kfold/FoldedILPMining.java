package kfold;

import java.io.File;
import discovery.ILPMining;
import experiment.DirectoryExperiment;
import experiment.Globals;

public class FoldedILPMining extends FoldedDirectoryMiner {
	private DirectoryExperiment miner;

	public FoldedILPMining(File directory, String pattern, File outputDirectory, boolean skipIfExists, 
			File foldedLogsDir) throws Exception {
		super(directory, pattern, outputDirectory, skipIfExists, foldedLogsDir);
		miner = new ILPMining(null, null, outputDirectory, true);
	}

	@Override
	public void foldedRun(File file, File outputDirectory, File outputTxt, File foldedLog, File foldedNegativeLog) {
		miner.run(foldedLog, outputDirectory, outputTxt);
	}

	public static void main(final String... args) throws Exception {
		FoldedDirectoryMiner miner = new FoldedILPMining(
				new File(Globals.poslogsdir),
				"\\.xes$", 
				new File(Globals.basedir + "kfold/ilp_positiveonly/"), 
				true,
				new File(Globals.basedir + "kfold/logs/"));
		miner.go();
		System.out.println("Experiment finished ---------------");
	}

}
