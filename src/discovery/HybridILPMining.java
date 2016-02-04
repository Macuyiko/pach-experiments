package discovery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.boot.Boot;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Bootable;
import org.processmining.hybridilpminer.parameters.LPObjectiveType;
import org.processmining.hybridilpminer.parameters.XLogHybridILPMinerParametersImpl;
import org.processmining.hybridilpminer.plugins.HybridILPMinerPlugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.ilpminer.ILPModelJavaILP;
import org.processmining.plugins.kutoolbox.utils.ExportUtils;
import org.processmining.plugins.kutoolbox.utils.FakePluginContext;
import org.processmining.xeslite.external.XFactoryExternalStore;

import experiment.Globals;
import experiment.Utils;

public class HybridILPMining {
	public String inputLog;
	public String outputModel;
	public String outputTxt;
	public FakePluginContext context;

	public static File outputdir = new File(Globals.modelsdir + "hybridilp_positiveonly\\");

	public static void main(final String... args) throws Exception {
		ILPModelJavaILP.loadSLibraries();
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		outputdir.mkdirs();
		Boot.boot(HybridILPMining.class, PluginContext.class);
		FakePluginContext context = new FakePluginContext();
		
		for (File file : Utils.getDirectoryFiles(new File(Globals.poslogsdir), "\\.xes$")) {
			HybridILPMining miner = new HybridILPMining();
			miner.inputLog = file.getAbsolutePath();
			miner.outputModel = outputdir.getAbsolutePath() + "\\" + file.getName().replace(".xes", ".pnml");
			miner.outputTxt = outputdir.getAbsolutePath() + "\\" + file.getName().replace(".xes", ".txt");
			miner.context = context;
			System.out.println(miner.outputModel);
			miner.run();
		}
	}

	@Bootable
	public static void kicker(Object o) {
	}
	
	public void run() throws Exception {
		PrintStream pos = new PrintStream(new FileOutputStream(outputTxt));

		long time1 = System.currentTimeMillis();

		XLog log = Utils.readLog(inputLog);
		
		XLogHybridILPMinerParametersImpl config = new XLogHybridILPMinerParametersImpl(context, log, 
				XLogInfoImpl.STANDARD_CLASSIFIER);
		config.setObjectiveType(LPObjectiveType.MINIMIZE_ARCS);
		
		long time2 = System.currentTimeMillis();

		Object[] r = HybridILPMinerPlugin.mine(context, log, config);

		long time3 = System.currentTimeMillis();

		ExportUtils.exportPetriNet((Petrinet) r[0], (Marking) r[1], new File(outputModel));

		long time4 = System.currentTimeMillis();

		pos.println("Time to read log and setup: " + (time2 - time1));
		pos.println("Time to mine: " + (time3 - time2));
		pos.println("Time to save net: " + (time4 - time3));
		pos.close();
	}
}
