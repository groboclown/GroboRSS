/*
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

package net.groboclown.groborss.theme;

import android.content.Context;
import android.support.annotation.NonNull;

import net.groboclown.groborss.BuildConfig;

/**
 * Color used by the themes.  Must be HTML compatible colors
 */
public class EntryThemeColor {
    // Remember: color orders must match EntryTheme enum ordinal order
    public static final EntryThemeColor BACKGROUND = new EntryThemeColor(
            "lightgray", "black", "darkblue"
    );
    public static final EntryThemeColor TEXT = new EntryThemeColor(
            "black", "lightgray", "lightgray"
    );
    public static final EntryThemeColor LINK_NORMAL = new EntryThemeColor(
            "blue", "blue", "lightblue"
    );
    public static final EntryThemeColor LINK_HOVER = new EntryThemeColor(
            "blue", "blue", "lightblue"
    );
    public static final EntryThemeColor LINK_VISITED = new EntryThemeColor(
            "darkblue", "lightblue", "blue"
    );


    private final String[] perTheme;

    public EntryThemeColor(String... perTheme) {
        if (BuildConfig.DEBUG) {
            if (perTheme.length != EntryTheme.values().length) {
                throw new IllegalArgumentException("Must have color list match entry theme list");
            }
            for (String color : perTheme) {
                if (color == null) {
                    throw new IllegalArgumentException("Null colors not allowed");
                }
            }
        }
        this.perTheme = perTheme;
    }

    @NonNull
    public String getColor(@NonNull EntryTheme t) {
        return perTheme[t.ordinal()];
    }

    @NonNull
    public String getColor(@NonNull Context context) {
        return perTheme[ThemeSetting.getEntryTheme(context).ordinal()];
    }
}
