package experiment;

public class ProcessWorker extends Thread {
	private final Process process;
	public Integer exit;

	public ProcessWorker(Process process) {
		this.process = process;
	}

	public void run() {
		try {
			exit = process.waitFor();
		} catch (InterruptedException ignore) {
			return;
		}
	}
}