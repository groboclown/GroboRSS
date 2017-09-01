/**
 * GroboRSS
 *
 * Copyright (c) 2017 Groboclown
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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;

import net.groboclown.groborss.R;

import java.io.File;
import java.io.FilenameFilter;

import net.groboclown.groborss.RSSOverview;
import net.groboclown.groborss.provider.OPML;

public class OpmlImportAction {
    public static void importOpml(final Activity source) {
        // TODO allow for browsing the files, or at the very least
        // show the actual path.
        // TODO allow for importing the OPML from a URL.
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
                            OPML.importFromFile(Environment.getExternalStorageDirectory().toString() + File.separator + fileNames[which], source);
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
