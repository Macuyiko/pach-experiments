package discovery;

import java.io.File;
import java.io.IOException;
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

import experiment.DirectoryExperiment;
import experiment.Globals;
import experiment.Utils;

public class HybridILPMining extends DirectoryExperiment {
	private FakePluginContext context;

	public HybridILPMining(){
		super(null, null, null, false);
	}
	
	public HybridILPMining(File directory, String pattern, File outputDirectory, boolean skipIfExists) throws Exception {
		super(directory, pattern, outputDirectory, skipIfExists);
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		outputDirectory.mkdirs();
		ILPModelJavaILP.loadSLibraries();
		Boot.boot(HybridILPMining.class, PluginContext.class);
		context = new FakePluginContext();
	}

	@Override
	public void run(File file, File outputDirectory, File outputTxt) {
		String outputModel = outputDirectory.getAbsolutePath() + "/" + Utils.replaceExtension(file.getName(), "pnml");
		
		long time1 = System.currentTimeMillis();

		XLog log = Utils.readLog(file.getAbsolutePath());
		XLogHybridILPMinerParametersImpl config = new XLogHybridILPMinerParametersImpl(
				context, log, XLogInfoImpl.STANDARD_CLASSIFIER);
		config.setObjectiveType(LPObjectiveType.MINIMIZE_ARCS);
		
		long time2 = System.currentTimeMillis();
		
		Object[] r = HybridILPMinerPlugin.mine(context, log, config);

		long time3 = System.currentTimeMillis();

		try {
			ExportUtils.exportPetriNet((Petrinet) r[0], (Marking) r[1], new File(outputModel));
		} catch (IOException e) {
			e.printStackTrace();
		}

		long time4 = System.currentTimeMillis();

		Utils.writeLineToFile(outputTxt, "Time to read log and setup: " + (time2 - time1));
		Utils.writeLineToFile(outputTxt, "Time to mine: " + (time3 - time2));
		Utils.writeLineToFile(outputTxt, "Time to save net: " + (time4 - time3));
	}

	@Bootable
	public static void kicker(Object o) {
	}

	public static void main(final String... args) throws Exception {
		DirectoryExperiment miner = new HybridILPMining(
				new File(Globals.poslogsdir),
				"\\.xes$", 
				new File(Globals.modelsdir + "hybridilpmod_positiveonly\\"), 
				true);
		miner.go();
		System.out.println("Experiment finished ---------------");
	}
	
}
