/*
 * GroboRSS
 *
 * Copyright (c) 2018 Groboclown
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

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
