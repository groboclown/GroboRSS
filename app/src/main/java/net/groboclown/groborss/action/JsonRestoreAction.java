package net.groboclown.groborss.action;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.annotation.NonNull;

import net.groboclown.groborss.R;
import net.groboclown.groborss.RSSOverview;
import net.groboclown.groborss.provider.DbTableFacadeFactory;
import net.groboclown.groborss.provider.FeedData;
import net.groboclown.groborss.provider.JsonState;
import net.groboclown.groborss.provider.OPML;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Restore the state of the system from a JSON file.
 */
public class JsonRestoreAction {
    public static void restoreJson(final Activity source) {
        // TODO allow for browsing the files, or at the very least show the actual path.
        // TODO allow for importing the file from a URL.
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(source);

            builder.setTitle(R.string.select_file);

            try {
                final String[] fileNames = Environment.getExternalStorageDirectory().list(new FilenameFilter() {
                    public boolean accept(File dir, String filename) {
                        return new File(dir, filename).isFile();
                    }
                });
                builder.setItems(fileNames, new DialogInterface.OnClickListener()  {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            JsonState.readJsonFile(
                                    Environment.getExternalStorageDirectory().toString() + File.separator + fileNames[which],
                                    FeedData.getActivityFactory(source),
                                    FeedData.TABLES);
                        } catch (Exception e) {
                            source.showDialog(RSSOverview.DIALOG_ERROR_FEEDIMPORT);
                        }
                    }
                });
                builder.show();
            } catch (Exception e) {
                source.showDialog(RSSOverview.DIALOG_ERROR_FEEDIMPORT);
            }
        } else {
            source.showDialog(RSSOverview.DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE);
        }
    }
}
