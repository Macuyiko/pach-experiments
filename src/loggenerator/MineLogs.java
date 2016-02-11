package loggenerator;

import java.io.File;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.processmining.xeslite.external.XFactoryExternalStore;

import discovery.HybridILPMining;
import discovery.ILPMining;
import experiment.DirectoryExperiment;
import experiment.Globals;
import experiment.Utils;

public class MineLogs extends DirectoryExperiment {
	
	private File outputDirHybrid;
	private File outputDirOld;
	private HybridILPMining hybridilpminer;
	private ILPMining oldilpminer;

	public MineLogs(File directory, String pattern, File outputDirectory, boolean skipIfExists) throws Exception {
		super(directory, pattern, outputDirectory, skipIfExists);
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		outputDirectory.mkdirs();
		
		outputDirHybrid = new File(outputDirectory.getAbsolutePath() + "/hybridIlpMiner/");
		outputDirOld = new File(outputDirectory.getAbsolutePath() + "/ilpMiner/");
		hybridilpminer = new HybridILPMining(null, null, outputDirHybrid, true);
		oldilpminer = new ILPMining(null, null, outputDirOld, true);
		
	}

	@Override
	public void run(File file, File outputDirectory, File outputTxt) {
		File outputTxtHybrid = new File(Utils.replaceExtension(outputTxt.getAbsolutePath(), "hybridilp.txt"));
		System.out.println(outputDirHybrid.getAbsolutePath());
		try {
			hybridilpminer.run(file, outputDirHybrid, outputTxtHybrid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		File outputTxtOld = new File(Utils.replaceExtension(outputTxt.getAbsolutePath(), "oldilp.txt"));
		System.out.println(outputDirOld.getAbsolutePath());
		try {
			oldilpminer.run(file, outputDirOld, outputTxtOld);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(final String... args) throws Exception {
		DirectoryExperiment miner = new MineLogs(
				new File(Globals.basedir + "runningManufacturingExample/logs/"),
				"\\.xes$", 
				new File(Globals.basedir + "runningManufacturingExample/models/"), 
				true);
		miner.go();
		System.out.println("Experiment finished ---------------");
	}
	
}
