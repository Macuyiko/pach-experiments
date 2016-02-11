package kfold;

import java.io.File;
import java.io.IOException;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.kutoolbox.utils.ExportUtils;
import org.processmining.xeslite.external.XFactoryExternalStore;

import experiment.Globals;
import experiment.Utils;

public class CollapseLogs extends FoldedDirectoryMiner {
	
	public CollapseLogs(File directory, String pattern, File outputDirectory, boolean skipIfExists, 
			File foldedLogsDir) {
		super(directory, pattern, outputDirectory, skipIfExists, foldedLogsDir);
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		outputDirectory.mkdirs();
	}

	@Override
	public void foldedRun(File file, File outputDirectory, File outputTxt, File foldedLog, File foldedNegativeLog) {
		XLog poslog = Utils.readLog(foldedLog.getAbsolutePath());
		XLog neglog = Utils.readLog(foldedNegativeLog.getAbsolutePath());
		
		try {
			ExportUtils.exportLog(poslog, new File(outputDirectory.getAbsolutePath() + "/keep_" +
					foldedLog.getName()));
			ExportUtils.exportLog(neglog, new File(outputDirectory.getAbsolutePath() + "/keep_" +
					foldedNegativeLog.getName()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String... args) throws Exception {
		FoldedDirectoryMiner miner = new CollapseLogs(
				new File(Globals.poslogsdir),
				"\\.xes$", 
				new File(Globals.basedir + "kfold/collapsedLogs/"), 
				true,
				new File(Globals.basedir + "kfold/logs/"));
		miner.go();
		System.out.println("Experiment finished ---------------");
	}

}
