package me.hexian000.massbackup;

import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

import static me.hexian000.massbackup.MassBackup.LOG_TAG;

public class Work implements Runnable {
	private final String scripts;
	private final String action;
	private final WorkCallback callback;
	private final String binDir;
	private final String backupsDir;

	Work(String scripts, String action, WorkCallback callback, String binDir, String backupsDir) {
		this.scripts = scripts;
		this.action = action;
		this.callback = callback;
		this.binDir = binDir;
		this.backupsDir = backupsDir;
		Log.d(LOG_TAG, "Work action=" + action);
		Log.d(LOG_TAG, "Work binDir=" + binDir);
		Log.d(LOG_TAG, "Work backupsDir=" + backupsDir);
	}

	@Override
	public void run() {
		try {
			Process shell = Runtime.getRuntime().exec("su");
			Scanner in = new Scanner(shell.getInputStream());
			PrintStream out = new PrintStream(shell.getOutputStream());
			setup(out);
			out.println(action + " 2>/cache/MassBackup.log");
			out.println("exit");
			out.flush();
			while (in.hasNextLine()) {
				callback.output(in.nextLine());
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, "run()", e);
		} finally {
			callback.finished();
		}
	}

	private String chooseABI() {
		for (String abi : Build.SUPPORTED_ABIS) {
			switch (abi) {
				case "arm64-v8a":
				case "armeabi-v7a":
				case "armeabi":
					return abi;
			}
		}
		return "";
	}

	private void setup(PrintStream out) {
		String abi = chooseABI();
		if ("".equals(abi)) {
			throw new UnsupportedOperationException("Unsupported ABI");
		}
		String zstd = binDir + "/" + abi + "/zstd";
		Log.d(LOG_TAG, "zstd=" + zstd);
		out.println("zstd=\'" + zstd + "\'");
		out.println("backupPath=\'" + backupsDir + "\'");

		out.println(scripts);
	}
}
