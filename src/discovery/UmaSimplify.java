package discovery;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.kutoolbox.logmappers.PetrinetLogMapper;
import org.processmining.plugins.kutoolbox.utils.ExportUtils;
import org.processmining.plugins.kutoolbox.utils.ImportUtils;
import org.processmining.plugins.uma.UmaPromUtil;
import org.processmining.xeslite.external.XFactoryExternalStore;

import conformance.mains.AlignCheckingMain;
import experiment.DirectoryExperiment;
import experiment.Globals;
import experiment.Utils;
import hub.top.uma.InvalidModelException;
import hub.top.uma.view.MineSimplify;
import hub.top.uma.view.MineSimplify.Configuration;

public class UmaSimplify extends DirectoryExperiment {
	private File originalmodelsdir;

	public UmaSimplify(File directory, String pattern, File outputDirectory, boolean skipIfExists,
			File originalmodelsdir) throws Exception {
		super(directory, pattern, outputDirectory, skipIfExists);
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		outputDirectory.mkdirs();
		
		this.originalmodelsdir = originalmodelsdir;
	}
	
	@Override
	public void run(File file, File outputDirectory, File outputTxt) {
		String inputLog = file.getAbsolutePath();
		String inputModel = originalmodelsdir.getAbsolutePath() + "/" + Utils.replaceExtension(file.getName(), "pnml");
		String outputModel = outputDirectory.getAbsolutePath() + "/" + Utils.replaceExtension(file.getName(), "pnml");
		
		long time1 = System.currentTimeMillis();
		XLog log = Utils.readLog(inputLog);
		Object[] pnm = ImportUtils.openPetrinet(new File(inputModel));
		Petrinet net = (Petrinet) pnm[0];
		Marking initialMarking = (Marking) pnm[1];
		PetrinetLogMapper mapper = PetrinetLogMapper.getStandardMap(log, net);
		mapper.applyMappingOnTransitions();
		TransEvClassMapping map = AlignCheckingMain.getTransEvClassMapping(mapper, net);
		Configuration config = new Configuration();
		for (Transition t : map.keySet()) {
			XEventClass ev_class = map.get(t);
			config.eventToTransition.put(ev_class.getId(), t.getLabel());
		}
		XEventClassifier classifier = map.getEventClassifier();
		
		config.abstract_chains = false;
		config.filter_threshold = 0.0d;
		config.remove_flower_places = true;
		config.unfold_refold = true;
		// config.remove_implied; leave default
		
		long time2 = System.currentTimeMillis();
		Object r[] = simplifyNet(log, net, initialMarking, config, classifier);

		long time3 = System.currentTimeMillis();

		try {
			ExportUtils.exportPetriNet((Petrinet) r[0], (Marking) r[1], new File(outputModel));
		} catch (IOException e) {
			e.printStackTrace();
		}

		long time4 = System.currentTimeMillis();

		Utils.writeLineToFile(outputTxt, "Time to read log and setup: " + (time2 - time1));
		Utils.writeLineToFile(outputTxt, "Time to simplify: " + (time3 - time2));
		Utils.writeLineToFile(outputTxt, "Time to save net: " + (time4 - time3));
	}
	
	public static Object[] simplifyNet(XLog log, Petrinet net, Marking initMarking, 
			Configuration config, XEventClassifier classifier) {
		PetrinetSemantics semantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
		semantics.initialize(net.getTransitions(), initMarking);
		Map<PetrinetNode, hub.top.petrinet.Transition> transitionMap = new HashMap<PetrinetNode, hub.top.petrinet.Transition>();
		hub.top.petrinet.PetriNet originalNet = UmaPromUtil.toPNAPIFormat(net, initMarking, transitionMap);
		for (String key : config.eventToTransition.keySet()) {
			PetrinetNode n_for_key = null;
			for (PetrinetNode n : transitionMap.keySet()) {
				if (n.getLabel().equals(key)) {
					n_for_key = n;
					break;
				}
			}
			config.eventToTransition.put(key, transitionMap.get(n_for_key).getUniqueIdentifier());
		}
		LinkedList<String[]> eventLog = UmaPromUtil.toSimpleEventLog(log, classifier);
		MineSimplify simplify = new MineSimplify(originalNet, eventLog, config);
		simplify.prepareModel();
		try {
			simplify.run();
			String name = net.getLabel() + " (simplified with UMA)";
			hub.top.petrinet.PetriNet _simplifiedNet = simplify.getSimplifiedNet();
			Object[] simplifiedNet = UmaPromUtil.toPromFormat(_simplifiedNet, name);
			return simplifiedNet;
		} catch (InvalidModelException e) {
			return null;
		}
	}
	
	public static void main(final String... args) throws Exception {
		UmaSimplify miner = new UmaSimplify(
				new File(Globals.poslogsdir), "\\.xes$", 
				new File(Globals.modelsdir + "mining\\ilp_uma\\"), 
				true,
				new File(Globals.modelsdir + "mining\\ilp\\"));
		miner.go();
		System.out.println("Experiment finished ---------------");
	}
}
