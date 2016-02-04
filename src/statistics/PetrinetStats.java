package statistics;
import java.io.File;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.plugins.kutoolbox.utils.ImportUtils;
import experiment.Globals;
import experiment.Utils;

public class PetrinetStats {
	public static void main(final String... args) throws Exception {
		
		for (File file : Utils.getDirectoryFiles(new File(Globals.modelsdir + "hybridilp_positiveonly"), "\\.pnml$")) {
			System.out.println(file.getName());
			Object[] r = ImportUtils.openPetrinet(file);
			Petrinet net = (Petrinet) r[0];
			
			System.out.println("#places:" + net.getPlaces().size());
			System.out.println("#transitions:" + net.getTransitions().size());
			System.out.println("#edges:" + net.getEdges().size());
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : net.getEdges()) {
				if (!e.getLabel().equals("1") || net.getArc(e.getSource(), e.getTarget()).getWeight() > 1) {
					System.out.println("  --> Net contains non unary arcs");
					break;
				}
			}
		}
	}
	
	
	
}
