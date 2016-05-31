package conformance.mains;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import nl.tue.astar.AStarException;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.alignetc.AlignETCPlugin;
import org.processmining.plugins.alignetc.AlignETCSettings;
import org.processmining.plugins.alignetc.result.AlignETCResult;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithoutILP;
import org.processmining.plugins.connectionfactories.logpetrinet.EvClassLogPetrinetConnectionFactoryUI;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.kutoolbox.logmappers.PetrinetLogMapper;
import org.processmining.plugins.kutoolbox.utils.FakePluginContext;
import org.processmining.plugins.kutoolbox.utils.ImportUtils;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.AllSyncReplayResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.xeslite.external.XFactoryExternalStore;

import experiment.Utils;

public class AlignCheckingMain {
	
	public static void main(String... args) {
		File logDirectory = new File(args[0]);
		File outputTxt = new File(args[1]);
		File pnml = new File(args[2]);
		
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		
		System.out.print(pnml.getName() + "\t");
			
		File logFile = new File(logDirectory.getAbsolutePath() + "/" +
				Utils.replaceExtension(
						pnml.getName()
						.replace(".enc", "")
						.replace("allbut_", "")
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
			return;
		}
		
		XLog log = ImportUtils.openLog(logFile);
		Object[] i = ImportUtils.openPetrinet(pnml);
		Petrinet net = (Petrinet) i[0];
		Marking initialMarking = (Marking) i[1];
		PetrinetLogMapper mapper = PetrinetLogMapper.getStandardMap(log, net);
		mapper.applyMappingOnTransitions();
		double result = -1d;
		try {
			result = checkAlignETCSilent(log, net, initialMarking, mapper).ap;
		} catch (ConnectionCannotBeObtained | IllegalTransitionException | OutOfMemoryError e) {
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println(result+"");
		Utils.writeLineToFile(outputTxt, pnml.getName()+"\t"+result);
		System.exit(0);
	}
	
	public static TransEvClassMapping getTransEvClassMapping(PetrinetLogMapper mapping, Petrinet net) {
		TransEvClassMapping transEvClassMapping = new TransEvClassMapping(
				mapping.getEventClassifier(),
				EvClassLogPetrinetConnectionFactoryUI.DUMMY);

		for (Transition transition : net.getTransitions()) {
			if (mapping.get(transition) == null) {
				transEvClassMapping.put(transition, EvClassLogPetrinetConnectionFactoryUI.DUMMY);
			} else if (mapping.get(transition).equals(PetrinetLogMapper.BLOCKING_CLASS)) {
				transEvClassMapping.put(transition, EvClassLogPetrinetConnectionFactoryUI.DUMMY);
			} else {
				transEvClassMapping.put(transition, mapping.get(transition));
			}
		}

		return transEvClassMapping;
	}
	
	public static AlignETCResult checkAlignETCSilent(XLog log, Petrinet net, Marking iniMark, PetrinetLogMapper mapper) throws ConnectionCannotBeObtained, IllegalTransitionException {
		
		TransEvClassMapping oldMap = getTransEvClassMapping(mapper, net);
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log);
		CostBasedCompleteParam parameter = new CostBasedCompleteParam(logInfo.getEventClasses().getClasses(), oldMap.getDummyEventClass(), net.getTransitions(), 2, 5);
		parameter.getMapEvClass2Cost().remove(oldMap.getDummyEventClass());
		parameter.getMapEvClass2Cost().put(oldMap.getDummyEventClass(), 1);
			
		parameter.setGUIMode(false);
		parameter.setCreateConn(false);
		parameter.setInitialMarking(iniMark);
		parameter.setMaxNumOfStates(20000);//200000);
			
		PNLogReplayer replayer = new PNLogReplayer();
		PetrinetReplayerWithoutILP replWithoutILP = new PetrinetReplayerWithoutILP();
		PNRepResult pnRepResult = null;
		try {
			pnRepResult = replayer.replayLog(null, net, log, oldMap, replWithoutILP, parameter);
		} catch (AStarException e) {
			e.printStackTrace();
		}
			
		Collection<AllSyncReplayResult> col = new ArrayList<AllSyncReplayResult>();
		for (SyncReplayResult rep : pnRepResult) {
			List<List<Object>> nodes = new ArrayList<List<Object>>();
			nodes.add(rep.getNodeInstance());
			List<List<StepTypes>> types = new ArrayList<List<StepTypes>>();
			types.add(rep.getStepTypes());
			SortedSet<Integer> traces = rep.getTraceIndex();
			boolean rel = rep.isReliable();
			AllSyncReplayResult allRep = new AllSyncReplayResult(nodes, types, -1, rel);
			allRep.setTraceIndex(traces);
			col.add(allRep);
		}
		PNMatchInstancesRepResult alignments = new PNMatchInstancesRepResult(col);
		AlignETCResult res = new AlignETCResult();
		AlignETCSettings sett = new AlignETCSettings(res);
		AlignETCPlugin plugin = new AlignETCPlugin();
		return plugin.checkGenericAlignETC(new FakePluginContext(), log, net, iniMark, alignments, res, sett);
	}
	
	
}
