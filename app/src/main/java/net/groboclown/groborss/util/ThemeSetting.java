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

package net.groboclown.groborss.util;

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceManager;

import net.groboclown.groborss.R;

import net.groboclown.groborss.Strings;

public class ThemeSetting {
    // Can be cached, becase we require a restart in order to change the theme.
    private static Boolean LIGHT_THEME;

    public static boolean isLightColorMode(Context context) {
        return (ThemeSetting.isLightTheme(context) ||
                PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Strings.SETTINGS_BLACKTEXTONWHITE, false));
    }


    public static void setTheme(Activity activity) {
        if (isLightTheme(activity)) {
            activity.setTheme(R.style.Theme_Light);
        } else {
            activity.setTheme(R.style.Theme_Dark);
        }
    }


    private static boolean isLightTheme(Context context) {
        if (LIGHT_THEME == null) {
            LIGHT_THEME = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Strings.SETTINGS_LIGHTTHEME, false);
        }
        return LIGHT_THEME;
    }
}
