package discovery.mains;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.agnesminer.AGNEsMinerPlugin;
import org.processmining.plugins.agnesminer.settings.MinerSettings;
import org.processmining.plugins.agnesminer.ui.MinerUI;
import org.processmining.plugins.kutoolbox.exceptions.OperationCancelledException;
import org.processmining.plugins.kutoolbox.utils.ExportUtils;
import org.processmining.plugins.kutoolbox.utils.FakePluginContext;
import experiment.Utils;

public class AgnesMiningMain {
	
	public static void main(final String...args) {
		File file = new File(args[0]);
		File outputDirectory = new File(args[1]);
		File outputTxt = new File(args[2]);
		
		String inputLog = file.getAbsolutePath();
		String outputModel = outputDirectory.getAbsolutePath() + "/" + Utils.replaceExtension(file.getName(), "pnml");
		
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
		long time3 = System.currentTimeMillis();
		Object[] results;
		try {
			results = AGNEsMinerPlugin.runMiner(new FakePluginContext(), log, settings);
			time3 = System.currentTimeMillis();
			
			Petrinet net = (Petrinet) results[0];
			Marking mar = (Marking) results[1];
			ExportUtils.exportPetriNet(net, mar, new File(outputModel));
		} catch (OperationCancelledException | IOException e) {
			e.printStackTrace();
		}
		
		long time4 = System.currentTimeMillis();
			
		Utils.writeLineToFile(outputTxt, "Time to read log and setup: "+(time2-time1));
		Utils.writeLineToFile(outputTxt, "Time to mine: "+(time3-time2));
		Utils.writeLineToFile(outputTxt, "Time to save net: "+(time4-time3));				
	}

}
