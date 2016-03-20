package statistics;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import org.processmining.plugins.kutoolbox.utils.ImportUtils;

public class PetrinetComparison {
	public static final String basedir = "C:\\Users\\u0078555\\Google Drive\\KU Leuven - Papers\\"
			+ "Supervised Process Discovery and Simplification (2015, IS)\\ponce\\Experiments\\"
			+ "strangeILPInvestigation\\";
	public static final String net1 = basedir + "complex.ori.pnml";
	public static final String net2 = basedir + "complex.enc.pnml";
	
	public static void main(final String... args) throws Exception {
		
		Object[] r = ImportUtils.openPetrinet(new File(net1));
		Petrinet pet1 = (Petrinet) r[0];
		Marking mar1 = (Marking) r[1];
		
		r = ImportUtils.openPetrinet(new File(net2));
		Petrinet pet2 = (Petrinet) r[0];
		Marking mar2 = (Marking) r[1];
		
		PetrinetSemantics semantics1 = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
		semantics1.initialize(pet1.getTransitions(), mar1);
		
		PetrinetSemantics semantics2 = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
		semantics2.initialize(pet2.getTransitions(), mar2);
		
		Set<Transition> seen = new HashSet<Transition>();
		for (Transition t1 : pet1.getTransitions()) {
			String t1s = t1.getLabel().replace("+complete","");
			for (Transition t2 : pet2.getTransitions()) {
				String t2s = t2.getLabel().replace("+complete","");
				if (!t1s.equals(t2s)) continue;
				seen.add(t1); seen.add(t2);
				System.out.println("\n" + t1.getLabel()+" found in both nets --------------------------------");
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> in1 = pet1.getInEdges(t1);
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> in2 = pet2.getInEdges(t2);
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> out1 = pet1.getOutEdges(t1);
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> out2 = pet2.getOutEdges(t2);
				System.out.println(in1.size()+" , "+in2.size()+" |<>| "+out1.size()+" , "+out2.size());
				System.out.println(semantics1.getExecutableTransitions().contains(t1) + "  vs  " +
						semantics2.getExecutableTransitions().contains(t2));
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : in1) {
					System.out.println("IN 1   " + ((Place) edge.getSource()).getLabel() + 
							" has tokens: " + semantics1.getCurrentState().occurrences(edge.getSource()));
				}
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : in2) {
					System.out.println("IN 2   " + ((Place) edge.getSource()).getLabel()+ 
							" has tokens: " + semantics2.getCurrentState().occurrences(edge.getSource()));
				}
			}
		}
		for (Transition t1 : pet1.getTransitions())
			if (!seen.contains(t1)) System.out.println(t1.getLabel() + " from first net was not matched");
		for (Transition t2 : pet2.getTransitions())
			if (!seen.contains(t2)) System.out.println(t2.getLabel() + " from second net was not matched");
		
	}
	
	
	
}
