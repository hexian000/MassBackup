package me.hexian000.massbackup;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import static me.hexian000.massbackup.MassBackup.CHANNEL_BACKUP_STATE;
import static me.hexian000.massbackup.MassBackup.LOG_TAG;

public class BackupService extends Service implements WorkCallback {
	private static PowerManager.WakeLock wakeLock = null;
	Handler handler = new Handler();
	Notification.Builder builder;
	NotificationManager notificationManager = null;
	int startId;

	void initNotification(@StringRes int title) {
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		builder = new Notification.Builder(this.getApplicationContext());

		builder.setContentIntent(null)
		       .setContentTitle(getResources().getString(title))
		       .setSmallIcon(R.drawable.ic_backup_black_24dp)
		       .setWhen(System.currentTimeMillis())
		       .setOngoing(true)
		       .setOnlyAlertOnce(true)
		       .setVisibility(Notification.VISIBILITY_PUBLIC);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			// Android 8.0+
			if (notificationManager != null) {
				MassBackup.createNotificationChannels(notificationManager, getResources());
				builder.setChannelId(CHANNEL_BACKUP_STATE);
			}
		} else {
			// Android 7.1
			builder.setPriority(Notification.PRIORITY_DEFAULT)
			       .setLights(0, 0, 0)
			       .setVibrate(null)
			       .setSound(null);
		}

		Notification notification = builder.build();
		startForeground(startId, notification);
	}

	@Override
	public void onDestroy() {
		releaseLocks();
		MassBackup.busy = false;
		super.onDestroy();
	}

	private String loadScript() {
		try (InputStream scriptStream = getResources().openRawResource(R.raw.backup)) {
			byte[] buf = new byte[65536];
			int n = scriptStream.read(buf);
			return new String(buf, 0, n);
		} catch (IOException e) {
			Log.wtf(LOG_TAG, "script not available");
		}
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.startId = startId;
		final boolean isRestore = intent.getBooleanExtra("isRestore", false);
		final String backupDir = intent.getStringExtra("backupDir");
		String scripts = loadScript();
		String action;
		if (isRestore) {
			action = "restore";
			initNotification(R.string.restore_in_progress);
		} else {
			action = "backup";
			initNotification(R.string.backup_in_progress);
		}
		if (backupDir == null || scripts == null) {
			Log.wtf(LOG_TAG, "empty backupDir");
			stopSelf(startId);
			return START_NOT_STICKY;
		}

		MassBackup.busy = true;
		acquireLocks();
		new Thread(new Work(scripts, action, this,
				getApplicationInfo().nativeLibraryDir, backupDir)).start();
		return START_NOT_STICKY;
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		throw new IllegalStateException("BackupService.onBind");
	}

	@Override
	public void finished() {
		handler.post(() -> this.stopSelf(startId));
	}

	@Override
	public void output(String message) {
		handler.post(() -> {
			if (builder != null && notificationManager != null) {
				builder.setContentText(message)
				       .setWhen(System.currentTimeMillis());
				notificationManager.notify(startId, builder.build());
			}
		});
	}

	void acquireLocks() {
		releaseLocks();
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		if (powerManager != null) {
			wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					LOG_TAG + ":BackupService");
			wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
			Log.d(LOG_TAG, "WakeLock acquired for 10 minutes");
		}
	}

	void releaseLocks() {
		if (wakeLock != null) {
			if (wakeLock.isHeld()) {
				wakeLock.release();
				Log.d(LOG_TAG, "WakeLock released");
			}
			wakeLock = null;
		}
	}
}
