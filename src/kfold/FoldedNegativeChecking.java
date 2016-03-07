package kfold;
import java.io.File;
import java.util.ArrayList;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.processmining.xeslite.external.XFactoryExternalStore;

import conformance.mains.NegativeCheckingMain;
import experiment.DirectoryExperiment;
import experiment.Globals;
import experiment.Subprocess;
import experiment.Utils;

public class FoldedNegativeChecking extends DirectoryExperiment {
	
	public FoldedNegativeChecking(File directory, String pattern, File outputDirectory, boolean skipIfExists) {
		super(directory, pattern, outputDirectory, skipIfExists);
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		outputDirectory.mkdirs();
	}
	
	public void runDir(File modelDirectory, final File logDirectory) {
		if (!modelDirectory.isDirectory()) return;
		
		final File outputTxt = new File(modelDirectory.getAbsolutePath() + "/../" + 
				modelDirectory.getName() + ".negrecprecgen.txt");
		System.out.println("\n In modelDirectory: " + modelDirectory.getAbsolutePath());
		System.out.println(" Outputting to: " + outputTxt.getName());
		
		if (this.skipIfExists && outputTxt.exists()) {
			System.err.println("SKIPPING: "+modelDirectory.getName());
		} else for (final File pnml : Utils.getDirectoryFiles(modelDirectory, "\\.pnml$")) {
			// For each pnml, we use the allbut_x log to replay			
			File logFile = new File(logDirectory.getAbsolutePath() + "/" +
					Utils.replaceExtension(
							pnml.getName()
							.replace("allbut_", "")
							.replace("_no_smt", "")
							.replace("_smt_iter", "")
							.replace("_smt_matrix", ""), "xes"));
			// allbut_a32.pos.0.pnml
			// a32.pos.0.xes
			if (!logFile.exists()) {
				System.err.println("NOT FOUND: "+logFile.getName());
				continue;
			}
			
			try {
				Subprocess.startSecondJVM(NegativeCheckingMain.class, new ArrayList<String>(), 
						new ArrayList<String>() {{
							add(logDirectory.getAbsolutePath());
							add(outputTxt.getAbsolutePath());
							add(pnml.getAbsolutePath());
							}}, 600000 * 6);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Recurse into subdirs
		for (File pnml : Utils.getDirectoryFiles(modelDirectory, "")) {
			runDir(pnml, logDirectory);
		}
	}
	
	@Override
	public void run(File modelDirectory, File logDirectory, File outputTxt) {
		runDir(modelDirectory, logDirectory);
	}
	
	public static void main(final String... args) throws Exception {
		DirectoryExperiment miner = new FoldedNegativeChecking(
				new File(Globals.basedir + "kfold/models/"), // Take care, this one starts from directories
				"", 
				new File(Globals.basedir + "kfold/models/logs/"), // And takes logs from the outputdir
				true);
		miner.go();
		System.out.println("Experiment finished ---------------");
	}
}
