package me.hexian000.massbackup;

public interface WorkCallback {
	void finished();

	void output(String message);
}
