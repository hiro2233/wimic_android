/*
 * Copyright (C) 2014 Andrew Comminos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package bo.htakey.wimic.preference;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import bo.htakey.wimic.BuildConfig;
import bo.htakey.wimic.Constants;
import bo.htakey.wimic.R;
import bo.htakey.wimic.Settings;

/**
 * This entire class is a mess.
 * FIXME. Please.
 */
public class Preferences extends PreferenceActivity {

    public static final String ACTION_PREFS_GENERAL = "bo.htakey.wimic.app.PREFS_GENERAL";
    public static final String ACTION_PREFS_AUTHENTICATION = "bo.htakey.wimic.app.PREFS_AUTHENTICATION";
    public static final String ACTION_PREFS_AUDIO = "bo.htakey.wimic.app.PREFS_AUDIO";
    public static final String ACTION_PREFS_APPEARANCE = "bo.htakey.wimic.app.PREFS_APPEARANCE";
    public static final String ACTION_PREFS_ABOUT = "bo.htakey.wimic.app.PREFS_ABOUT";
    public static final String ACTION_PREFS_ADVANCED = "bo.htakey.wimic.app.PREFS_ADVANCED";
    public static final String ACTION_PREFS_SCODE_SCHEME = "android_secret_code";
    public static final String ACTION_PREFS_SCODE1 = "android.intent.action.SECRET_CODE";
    public static final String ACTION_PREFS_SCODE2 = "android.telephony.action.SECRET_CODE";
    private static final char[] scode = {0x34, 0x34, 0x37, 0x36, 0x37, 0x34, 0x34};

    //private static final String USE_TOR_KEY = "useTor";
    private static final String VERSION_KEY = "version";
    private static final String ADVANCED_KEY = "advanced";

    public static class advancedCode extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String dialedNumber = getResultData();
            String action = intent.getAction();
            if (ACTION_PREFS_SCODE1.equals(action) || ACTION_PREFS_SCODE2.equals(action)) {
                String uri  =  intent.getDataString();
                String[] sep = new String[0];
                if (uri != null) {
                    sep = uri.split("://");
                }
                String codes = String.valueOf(scode);
                boolean sep_ok = false;
                if (sep.length > 0) {
                    sep_ok = sep[1].equalsIgnoreCase(codes);
                }
                if (sep_ok || dialedNumber.equals("*#*#" + codes + "#*#*")) {
                    Intent send_intent = new Intent();
                    send_intent.setAction(ACTION_PREFS_ADVANCED);
                    send_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(send_intent);
                    setResultData(null);
                }
            }
            Log.i(Constants.TAG, "Advanced Preference " + intent.getAction() + " Dial: " + dialedNumber);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme(Settings.getInstance(this).getTheme());
        super.onCreate(savedInstanceState);

        // Legacy preference section handling
        String action = getIntent().getAction();
        if (action != null) {
            if (ACTION_PREFS_GENERAL.equals(action)) {
                addPreferencesFromResource(R.xml.settings_general);
                configureOrbotPreferences(getPreferenceScreen());
            } else if (ACTION_PREFS_AUTHENTICATION.equals(action)) {
                addPreferencesFromResource(R.xml.settings_authentication);
            } else if (ACTION_PREFS_AUDIO.equals(action)) {
                addPreferencesFromResource(R.xml.settings_audio);
                configureAudioPreferences(getPreferenceScreen());
            } else if (ACTION_PREFS_APPEARANCE.equals(action)) {
                addPreferencesFromResource(R.xml.settings_appearance);
            } else if (ACTION_PREFS_ABOUT.equals(action)) {
                addPreferencesFromResource(R.xml.settings_about);
                configureAboutPreferences(this, getPreferenceScreen());
            }
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);

        String action = getIntent().getAction();
        if (ACTION_PREFS_ADVANCED.equals(action)) {
            Header header = new Header();
            Context context = getApplicationContext();
            header.title = context.getString(R.string.advanced);
            header.summary = context.getString(R.string.advanced_sum);
            header.fragment = WimicPreferenceFragment.class.getName();

            Bundle b = new Bundle();
            b.putString("settings", ADVANCED_KEY);
            header.fragmentArguments = b;
            target.add(header);
        }

    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return WimicPreferenceFragment.class.getName().equals(fragmentName);
    }

    private static void configureOrbotPreferences(PreferenceScreen screen) {
        //Preference useOrbotPreference = screen.findPreference(USE_TOR_KEY);
        //useOrbotPreference.setEnabled(OrbotHelper.isOrbotInstalled(screen.getContext()));
    }

    private static void configureAudioPreferences(final PreferenceScreen screen) {
        ListPreference inputPreference = (ListPreference) screen.findPreference(Settings.PREF_INPUT_METHOD);
        inputPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateAudioDependents(screen, (String) newValue);
                return true;
            }
        });

        // Scan each bitrate and determine if the device supports it
        ListPreference inputQualityPreference = (ListPreference) screen.findPreference(Settings.PREF_INPUT_RATE);
        String[] bitrateNames = new String[inputQualityPreference.getEntryValues().length];
        for(int x=0;x<bitrateNames.length;x++) {
            int bitrate = Integer.parseInt(inputQualityPreference.getEntryValues()[x].toString());
            boolean supported = AudioRecord.getMinBufferSize(bitrate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) > 0;
            bitrateNames[x] = bitrate+"Hz" + (supported ? "" : " (unsupported)");
        }
        inputQualityPreference.setEntries(bitrateNames);

        updateAudioDependents(screen, inputPreference.getValue());
    }

    private static void updateAudioDependents(PreferenceScreen screen, String inputMethod) {
        PreferenceCategory pttCategory = (PreferenceCategory) screen.findPreference("ptt_settings");
        PreferenceCategory vadCategory = (PreferenceCategory) screen.findPreference("vad_settings");
        pttCategory.setEnabled(Settings.ARRAY_INPUT_METHOD_PTT.equals(inputMethod));
        vadCategory.setEnabled(Settings.ARRAY_INPUT_METHOD_VOICE.equals(inputMethod));
    }

    private static void configureAboutPreferences(Context context, PreferenceScreen screen) {
        String version = "Unknown";
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = info.versionName;
            if (BuildConfig.FLAVOR.equals("beta")) {
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                f.setTimeZone(TimeZone.getTimeZone("UTC"));
                version += ("\nBeta flavor, versioncode: " + info.versionCode
                        + "\nbuildtime: " + f.format(new Date(BuildConfig.TIMESTAMP)) + " UTC");
            } else if (BuildConfig.FLAVOR.equals("donation")) {
                version += "\n\n*) " + context.getString(R.string.donation_thanks);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Preference versionPreference = screen.findPreference(VERSION_KEY);
        versionPreference.setSummary(version);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class WimicPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String section;
            if (savedInstanceState == null) {
                section = getArguments().getString("settings");
            }
            else {
                // Orientation Change
                section = savedInstanceState.getString("settings");
            }

            if ("general".equals(section)) {
                addPreferencesFromResource(R.xml.settings_general);
                configureOrbotPreferences(getPreferenceScreen());
            } else if ("authentication".equals(section)) {
                addPreferencesFromResource(R.xml.settings_authentication);
            } else if ("audio".equals(section)) {
                addPreferencesFromResource(R.xml.settings_audio);
                configureAudioPreferences(getPreferenceScreen());
            } else if ("appearance".equals(section)) {
                addPreferencesFromResource(R.xml.settings_appearance);
            } else if ("about".equals(section)) {
                addPreferencesFromResource(R.xml.settings_about);
                configureAboutPreferences(getPreferenceScreen().getContext(), getPreferenceScreen());
            } else if (ADVANCED_KEY.equals(section)) {
                addPreferencesFromResource(R.xml.settings_advanced);
            }
        }
    }
}
