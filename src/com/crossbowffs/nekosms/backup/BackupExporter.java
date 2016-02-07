package com.crossbowffs.nekosms.backup;

import android.content.Context;
import android.util.JsonWriter;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.database.CursorWrapper;
import com.crossbowffs.nekosms.database.SmsFilterDbLoader;
import com.crossbowffs.nekosms.preferences.BooleanPreference;
import com.crossbowffs.nekosms.preferences.Preferences;
import com.crossbowffs.nekosms.utils.Xlog;

import java.io.*;

/* package */ class BackupExporter implements JsonConstants, Closeable {
    private static final String TAG = BackupExporter.class.getSimpleName();
    private static final int BACKUP_VERSION = BuildConfig.BACKUP_VERSION;
    private final JsonWriter mJsonWriter;

    public BackupExporter(OutputStream out) {
        OutputStreamWriter streamWriter;
        try {
            streamWriter = new OutputStreamWriter(out, JSON_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        mJsonWriter = new JsonWriter(streamWriter);
        mJsonWriter.setIndent("\t");
    }

    public BackupExporter(File file) throws FileNotFoundException {
        this(new FileOutputStream(file));
    }

    public void write(Context context, int options) throws IOException {
        begin();
        if ((options & BackupLoader.OPTION_INCLUDE_SETTINGS) != 0) {
            writePreferences(context);
        }
        if ((options & BackupLoader.OPTION_INCLUDE_FILTERS) != 0) {
            writeFilters(context);
        }
        end();
    }

    private void begin() throws IOException {
        mJsonWriter.beginObject();
        mJsonWriter.name(KEY_VERSION).value(BACKUP_VERSION);
    }

    private void writePreference(Preferences preferences, BooleanPreference pref) throws IOException {
        mJsonWriter.name(pref.getKey()).value(preferences.get(pref));
    }

    private void writePreferences(Context context) throws IOException {
        Preferences preferences = Preferences.fromContext(context);
        mJsonWriter.name(KEY_SETTINGS).beginObject();
        writePreference(preferences, Preferences.PREF_ENABLE);
        writePreference(preferences, Preferences.PREF_DEBUG_MODE);
        writePreference(preferences, Preferences.PREF_NOTIFICATIONS_ENABLE);
        writePreference(preferences, Preferences.PREF_NOTIFICATIONS_SOUND);
        writePreference(preferences, Preferences.PREF_NOTIFICATIONS_VIBRATE);
        writePreference(preferences, Preferences.PREF_NOTIFICATIONS_LIGHTS);
        mJsonWriter.endObject();
    }

    private void writeFilter(SmsFilterData filterData) throws IOException {
        mJsonWriter.beginObject();
        mJsonWriter.name(KEY_FILTER_FIELD).value(filterData.getField().name());
        mJsonWriter.name(KEY_FILTER_MODE).value(filterData.getMode().name());
        mJsonWriter.name(KEY_FILTER_PATTERN).value(filterData.getPattern());
        mJsonWriter.name(KEY_FILTER_CASE_SENSITIVE).value(filterData.isCaseSensitive());
        mJsonWriter.endObject();
    }

    private void writeFilters(Context context) throws IOException {
        try (CursorWrapper<SmsFilterData> filterCursor = SmsFilterDbLoader.loadAllFilters(context)) {
            if (filterCursor == null) {
                Xlog.e(TAG, "Failed to load SMS filters (loadAllFilters returned null)");
                return;
            }

            mJsonWriter.name(KEY_FILTERS).beginArray();
            while (filterCursor.moveToNext()) {
                writeFilter(filterCursor.get());
            }
            mJsonWriter.endArray();
        }
    }

    private void end() throws IOException {
        mJsonWriter.endObject();
    }

    @Override
    public void close() throws IOException {
        mJsonWriter.close();
    }
}
