package me.hexian000.massbackup;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.res.Resources;
import android.os.Build;

public class MassBackup extends Application {
	public final static String LOG_TAG = "MassBackup";
	public final static String CHANNEL_BACKUP_STATE = "backup_state";
	public static boolean busy = false;

	static void createNotificationChannels(NotificationManager manager, Resources res) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(CHANNEL_BACKUP_STATE,
					res.getString(R.string.channel_backup_state),
					NotificationManager.IMPORTANCE_DEFAULT);
			channel.enableLights(false);
			channel.enableVibration(false);
			channel.setSound(null, null);

			manager.createNotificationChannel(channel);
		}
	}

}
