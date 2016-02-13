package experiment;

import java.io.File;

public abstract class DirectoryExperiment {
	protected File directory;
	protected String pattern;
	protected File outputDirectory;
	protected boolean skipIfExists;

	public DirectoryExperiment(File directory, String pattern, 
			File outputDirectory, boolean skipIfExists) {
		this.directory = directory;
		this.pattern = pattern;
		this.outputDirectory = outputDirectory;
		this.skipIfExists = skipIfExists;
	}
	
	public void go() {
		for (File file : Utils.getDirectoryFiles(directory, pattern)) {
			System.out.println(file.getName());
			String outputTxt = null;
			if (file.isDirectory()) {
				outputTxt = file.getAbsolutePath() + "/" + file.getName() + ".txt";
			} else {
				outputTxt = outputDirectory.getAbsolutePath() + "/" + 
					Utils.replaceExtension(file.getName(), "txt");
			}
			File outputTxtFile = new File(outputTxt);
			
			if (skipIfExists && outputTxtFile.exists()) {
				System.out.println("  SKIPPED: "+file.getAbsolutePath());
				continue;
			}
			outputTxtFile.delete();
			run(file, outputDirectory, outputTxtFile);
		}
	}
	
	public abstract void run(File file, File outputDirectory, File outputTxt);
}
