package discovery;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.agnesminer.AGNEsMinerPlugin;
import org.processmining.plugins.agnesminer.settings.MinerSettings;
import org.processmining.plugins.agnesminer.ui.MinerUI;
import org.processmining.plugins.kutoolbox.exceptions.OperationCancelledException;
import org.processmining.plugins.kutoolbox.utils.ExportUtils;
import org.processmining.plugins.kutoolbox.utils.FakePluginContext;
import org.processmining.xeslite.external.XFactoryExternalStore;

import experiment.Globals;
import experiment.Utils;

public class AgnesMining {
	public String inputLog;
	public String outputModel;
	public String outputTxt;
	
	public static File outputdir = new File(Globals.modelsdir + "agnesMiner\\");

	public static void main(final String... args) throws Exception {
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		outputdir.mkdirs();

		for (File file : Utils.getDirectoryFiles(new File(Globals.poslogsdir), "\\.xes$")) {
			AgnesMining miner = new AgnesMining();
			miner.inputLog = file.getAbsolutePath();
			miner.outputModel = outputdir.getAbsolutePath() + "\\" + file.getName().replace(".xes", ".pnml");
			miner.outputTxt = outputdir.getAbsolutePath() + "\\" + file.getName().replace(".xes", ".txt");
			System.out.println(miner.outputModel);
			miner.run();
		}
	}
	
	public void run() throws OperationCancelledException, IOException {
		PrintStream pos = new PrintStream(new FileOutputStream(outputTxt));
	
		long time1 = System.currentTimeMillis();
			
		XLog log = Utils.readLog(inputLog);
		MinerUI parameters = new MinerUI(log);
		MinerSettings settings = parameters.getSettings();
		settings.binAce = "C:\\ACE-1.2.8-b1\\windows\\bin\\ACE.exe";
		settings.binSwipl = "C:\\Program Files\\swipl\\bin\\swipl.exe";
		settings.directoryWorking = new File("C:\\AGNEs6\\mining-"+new Random().nextInt(10000));
		settings.newGeneration = true;
		settings.weightThreshold = 0.8;
		settings.includeParVar = false;
		
		long time2 = System.currentTimeMillis();
			
		Object[] results = AGNEsMinerPlugin.runMiner(new FakePluginContext(), log, settings);
		long time3 = System.currentTimeMillis();
			
		Petrinet net = (Petrinet) results[0];
		Marking mar = (Marking) results[1];
		ExportUtils.exportPetriNet(net, mar, new File(outputModel));
		
		long time4 = System.currentTimeMillis();
			
		pos.println("Time to read log and setup: "+(time2-time1));
		pos.println("Time to mine: "+(time3-time2));
		pos.println("Time to save net: "+(time4-time3));				
		pos.close();
	}
}
