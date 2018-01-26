package net.groboclown.groborss.action;

import android.app.Activity;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.widget.Toast;

import net.groboclown.groborss.R;
import net.groboclown.groborss.provider.FeedData;
import net.groboclown.groborss.provider.JsonState;

import static net.groboclown.groborss.RSSOverview.DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE;
import static net.groboclown.groborss.RSSOverview.DIALOG_ERROR_FEEDEXPORT;

/**
 * Backup the state of the database to a JSON file.
 */
public class JsonBackupAction {
    public static void backupJson(@NonNull Activity source) {
        // TODO allow selecting an output directory and name.
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            try {
                String filename = Environment.getExternalStorageDirectory().toString() + "/grobo_rss_" + System.currentTimeMillis() + ".json";

                JsonState.writeJsonFile(filename, FeedData.getActivityFactory(source), FeedData.getDbTables());
                Toast.makeText(source, String.format(source.getString(R.string.message_exportedto), filename), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                source.showDialog(DIALOG_ERROR_FEEDEXPORT);
            }
        } else {
            source.showDialog(DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE);
        }
    }
}
