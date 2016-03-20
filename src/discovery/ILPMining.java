package discovery;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.ilpminer.ILPMinerSettings.SolverSetting;
import org.processmining.plugins.ilpminer.ILPMinerSettings.SolverType;
import org.processmining.plugins.ilpminer.ILPMinerSolution;
import org.processmining.plugins.ilpminer.ILPModelJavaILP;
import org.processmining.plugins.ilpminer.ILPModelSettings;
import org.processmining.plugins.ilpminer.PrefixClosedLanguage;
import org.processmining.plugins.ilpminer.templates.PetriNetILPModelSettings;
import org.processmining.plugins.ilpminer.templates.PureNetILPModelExtension;
import org.processmining.plugins.ilpminer.templates.javailp.PetriNetILPModel;
import org.processmining.plugins.kutoolbox.utils.ExportUtils;
import org.processmining.plugins.kutoolbox.utils.FakePluginContext;
import org.processmining.plugins.log.logabstraction.LogRelations;
import org.processmining.plugins.log.logabstraction.factories.LogRelationsFactory;
import org.processmining.xeslite.external.XFactoryExternalStore;

import experiment.DirectoryExperiment;
import experiment.Globals;
import experiment.Utils;

public class ILPMining extends DirectoryExperiment {
	public ILPMining(File directory, String pattern, File outputDirectory, boolean skipIfExists) throws Exception {
		super(directory, pattern, outputDirectory, skipIfExists);
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		outputDirectory.mkdirs();
		ILPModelJavaILP.loadSLibraries();
	}
	
	@Override
	public void run(File file, File outputDirectory, File outputTxt) {
		String inputLog = file.getAbsolutePath();
		String outputModel = outputDirectory.getAbsolutePath() + "/" + Utils.replaceExtension(file.getName(), "pnml");
		
		long time1 = System.currentTimeMillis();

		XLog log = Utils.readLog(inputLog);
		XLogInfo summary = XLogInfoFactory.createLogInfo(log);
		LogRelations relations = LogRelationsFactory.constructAlphaLogRelations(log, summary);
		XEventClasses classes = summary.getEventClasses();
		Petrinet net = PetrinetFactory.newPetrinet("Mined ILP net");
		Map<XEventClass, Integer> indices = new HashMap<XEventClass, Integer>();
		Map<Integer, Transition> transitions = new HashMap<Integer, Transition>();
		int ij = 0;
		for (XEventClass evClass : classes.getClasses()) {
			indices.put(evClass, ij);
			Transition t = net.addTransition(evClass.toString());
			transitions.put(ij, t);
			ij++;
		}
		PrefixClosedLanguage l = new PrefixClosedLanguage(log, indices, classes);
		Set<ILPMinerSolution> solutions;
		Class<PetriNetILPModel> ILPVariant = PetriNetILPModel.class;
		Class<?>[] ILPExtensions = new Class[] { PureNetILPModelExtension.class };
		PetriNetILPModelSettings modelSettings = new PetriNetILPModelSettings();
		Map<SolverSetting, Object> solverSettings = new HashMap<SolverSetting, Object>();
		solverSettings.put(SolverSetting.TYPE, SolverType.JAVAILP_LPSOLVE);
		solverSettings.put(SolverSetting.LICENSE_DIR, "c:\\ILOG\\ILM");
		
		long time2 = System.currentTimeMillis();

		ILPModelJavaILP modelJavaILP;
		try {
			Constructor<?> mc = ILPVariant.getConstructor(new Class[] { Class[].class, Map.class, ILPModelSettings.class });
			modelJavaILP = (ILPModelJavaILP) mc.newInstance(new Object[] { ILPExtensions, solverSettings, modelSettings });
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		try {
			modelJavaILP.findPetriNetPlaces(indices, l, relations, new FakePluginContext());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		solutions = modelJavaILP.getSolutions();
		ILPMinerSolution[] array = solutions.toArray(new ILPMinerSolution[0]);
		for (int ii = 0; ii < array.length; ii++) {
			for (ILPMinerSolution s : solutions) {
				if (array[ii].compareTo(s) < 0) {
					solutions.remove(array[ii]);
					break;
				}
			}
		}

		int placeId = 1;
		Marking m = new Marking();
		for (ILPMinerSolution s : solutions) {
			Place p = net.addPlace("P " + placeId++);
			for (int i = 0; i < s.getInputSet().length; i++) {
				if (s.getInputSet()[i] > 0) {
					net.addArc(transitions.get(i), p);
				}
			}
			for (int i = 0; i < s.getOutputSet().length; i++) {
				if (s.getOutputSet()[i] > 0) {
					net.addArc(p, transitions.get(i));
				}
			}
			for (int i = 0; i < s.getTokens(); i++) {
				m.add(p);
			}
		}
		Map<Transition, XEventClass> mapping = new HashMap<Transition, XEventClass>(classes.size());
		for (XEventClass clazz : indices.keySet()) {
			if (clazz != null) {
				mapping.put(transitions.get(indices.get(clazz)), clazz);
			}
		}

		long time3 = System.currentTimeMillis();

		try {
			ExportUtils.exportPetriNet(net, m, new File(outputModel));
		} catch (IOException e) {
			e.printStackTrace();
		}

		long time4 = System.currentTimeMillis();

		Utils.writeLineToFile(outputTxt, "Time to read log and setup: " + (time2 - time1));
		Utils.writeLineToFile(outputTxt, "Time to mine: " + (time3 - time2));
		Utils.writeLineToFile(outputTxt, "Time to save net: " + (time4 - time3));
	}
	
	public static void main(final String... args) throws Exception {
		DirectoryExperiment miner = new ILPMining(
				new File(Globals.poslogsdir),
				"\\.xes$", 
				new File(Globals.modelsdir + "ilpminer_positiveonly\\"), 
				true);
		miner.go();
		System.out.println("Experiment finished ---------------");
	}
}
