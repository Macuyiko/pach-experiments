package discovery;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.processmining.xeslite.external.XFactoryExternalStore;

import discovery.mains.AgnesMiningMain;
import experiment.DirectoryExperiment;
import experiment.Globals;
import experiment.Subprocess;
import experiment.Utils;

public class AgnesMining extends DirectoryExperiment {
	
	public AgnesMining(File directory, String pattern, File outputDirectory, boolean skipIfExists) {
		super(directory, pattern, outputDirectory, skipIfExists);
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		outputDirectory.mkdirs();
	}

	@Override
	public void run(final File file, final File outputDirectory, final File outputTxt) {
		try {
			Subprocess.startSecondJVM(AgnesMiningMain.class, new ArrayList<String>(), 
					new ArrayList<String>() {{
						add(file.getAbsolutePath());
						add(outputDirectory.getAbsolutePath());
						add(outputTxt.getAbsolutePath());
						}}, 600000 * 6 * 4);
		} catch (IOException | TimeoutException | InterruptedException e) {
			e.printStackTrace();
			// A bit dangerous in case there would be other processes running but so be it for now
			Utils.killProcess("ace");
			Utils.killProcess("swipl");
		}		
	}

	public static void main(final String... args) throws Exception {
		DirectoryExperiment miner = new AgnesMining(
				new File(Globals.poslogsdir),
				"\\.xes$", 
				new File(Globals.modelsdir + "agnesMiner\\"), 
				true);
		miner.go();
		System.out.println("Experiment finished ---------------");
	}
	
}
