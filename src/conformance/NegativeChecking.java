package conformance;
import java.io.File;
import java.util.ArrayList;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.processmining.xeslite.external.XFactoryExternalStore;

import conformance.mains.NegativeCheckingMain;
import experiment.DirectoryExperiment;
import experiment.Globals;
import experiment.Subprocess;
import experiment.Utils;

public class NegativeChecking extends DirectoryExperiment {
	
	public NegativeChecking(File directory, String pattern, File outputDirectory, boolean skipIfExists) {
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
			
			File logFile = new File(logDirectory.getAbsolutePath() + "/" +
					Utils.replaceExtension(
							pnml.getName()
							.replace("_no_smt", "")
							.replace("_smt_iter", "")
							.replace("_smt_matrix", ""), "xes"));
			
			if (!logFile.exists()) {
				System.err.println("NOT FOUND (1): "+logFile.getName());
				logFile = new File(logFile.getAbsolutePath().replace(".xes", ".enc.xes"));
				System.err.println("CHANGED: "+logFile.getName());
			}
				
			if (!logFile.exists()) {
				System.err.println("NOT FOUND (2): "+logFile.getName());
				continue;
			}
			
			try {
				Subprocess.startSecondJVM(NegativeCheckingMain.class, 
						new ArrayList<String>() {{
							add("-Xmx4G");
						}},
						new ArrayList<String>() {{
							add(logDirectory.getAbsolutePath());
							add(outputTxt.getAbsolutePath());
							add(pnml.getAbsolutePath());
							}}, 600000 * 12);
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
		DirectoryExperiment miner = new NegativeChecking(
				new File(Globals.modelsdir), // Take care, this one starts from directories
				"", 
				new File(Globals.poslogsdir),// And takes logs from the outputdir
				true);
		miner.go();
		System.out.println("Experiment finished ---------------");
	}
}
