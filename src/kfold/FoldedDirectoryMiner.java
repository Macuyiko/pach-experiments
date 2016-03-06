package kfold;

import java.io.File;
import java.io.IOException;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.kutoolbox.utils.ExportUtils;
import org.processmining.xeslite.external.XFactoryExternalStore;

import experiment.DirectoryExperiment;
import experiment.Utils;

public abstract class FoldedDirectoryMiner extends DirectoryExperiment {
	private File foldedLogsDir;

	public FoldedDirectoryMiner(File directory, String pattern, File outputDirectory, boolean skipIfExists,
			File foldedLogsDir) {
		super(directory, pattern, outputDirectory, skipIfExists);
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		outputDirectory.mkdirs();
		this.foldedLogsDir = foldedLogsDir;
	}

	@Override
	public void run(File file, File outputDirectory, File outputTxt) {
		System.out.println(outputTxt.getName());
		String baseName = file.getName().split("\\.")[0];
		for (File pos : Utils.getDirectoryFiles(foldedLogsDir, "^"+baseName+"\\.pos\\.\\d\\.xes$")) {
			File neg = new File(pos.getAbsolutePath().replace(".pos", ".neg"));
			XLog posxlog = Utils.mergeFolds(foldedLogsDir, pos, "\\.pos\\.\\d\\.xes$");
			XLog negxlog = Utils.mergeFolds(foldedLogsDir, neg, "\\.neg\\.\\d\\.xes$");
			File tmp = new File(outputDirectory.getAbsolutePath() + "/allbut_"+pos.getName());
			File tmpneg = new File(outputDirectory.getAbsolutePath() + "/allbut_"+neg.getName());
			try {
				ExportUtils.exportLog(posxlog, tmp);
				ExportUtils.exportLog(negxlog, tmpneg);
				foldedRun(file, outputDirectory, outputTxt, tmp, tmpneg);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			tmp.delete();
			tmpneg.delete();
		}
	}

	public abstract void foldedRun(File file, File outputDirectory, File outputTxt, File foldedLog, File foldedNegativeLog);

}
