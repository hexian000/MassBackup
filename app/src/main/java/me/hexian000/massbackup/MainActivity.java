package me.hexian000.massbackup;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {
	EditText editPath;
	Button backup, restore;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		editPath = findViewById(R.id.editPath);
		editPath.setText(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
				           .getAbsolutePath());

		backup = findViewById(R.id.backupButton);
		restore = findViewById(R.id.restoreButton);
	}

	@Override
	protected void onResume() {
		setUIEnabled(!MassBackup.busy);
		super.onResume();
	}

	public void onBackupClick(View view) {
		Intent intent = new Intent(this, BackupService.class);
		intent.putExtra("isRestore", false);
		intent.putExtra("backupDir", editPath.getText().toString());
		startForegroundServiceCompat(intent);

		setUIEnabled(false);
	}

	public void onRestoreClick(View view) {
		Intent intent = new Intent(this, BackupService.class);
		intent.putExtra("isRestore", true);
		intent.putExtra("backupDir", editPath.getText().toString());
		startForegroundServiceCompat(intent);

		setUIEnabled(false);
	}

	private void setUIEnabled(boolean enabled) {
		backup.setEnabled(enabled);
		restore.setEnabled(enabled);
	}

	private void startForegroundServiceCompat(Intent intent) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(intent);
		} else {
			startService(intent);
		}
	}
}
