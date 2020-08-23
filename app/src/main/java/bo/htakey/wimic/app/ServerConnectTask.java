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

package bo.htakey.wimic.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.AsyncTask;

import java.util.ArrayList;

import bo.htakey.rimic.RimicService;
import bo.htakey.rimic.model.Server;
import bo.htakey.wimic.BuildConfig;
import bo.htakey.wimic.R;
import bo.htakey.wimic.Settings;
import bo.htakey.wimic.db.WimicDatabase;
import bo.htakey.wimic.service.WimicService;
import bo.htakey.wimic.util.WimicTrustStore;

/**
 * Constructs an intent for connection to a WimicService and executes it.
 * Created by andrew on 20/08/14.
 */
public class ServerConnectTask extends AsyncTask<Server, Void, Intent> {
    private Context mContext;
    private WimicDatabase mDatabase;
    private Settings mSettings;

    public ServerConnectTask(Context context, WimicDatabase database) {
        mContext = context;
        mDatabase = database;
        mSettings = Settings.getInstance(context);
    }

    @Override
    protected Intent doInBackground(Server... params) {
        Server server = params[0];

        /* Convert input method defined in settings to an integer format used by Rimic. */
        int inputMethod = mSettings.getRimicInputMethod();

        int audioSource = mSettings.isHandsetMode() ?
                MediaRecorder.AudioSource.DEFAULT : MediaRecorder.AudioSource.MIC;
        int audioStream = mSettings.isHandsetMode() ?
                AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC;

        String applicationVersion = "";
        String flavour = "";
        String appname = mContext.getString(R.string.app_name);

        if (BuildConfig.FLAVOR.equals("betainternal")) {
            flavour = "-betainternal";
        }
        if (BuildConfig.FLAVOR.equals("beta")) {
            flavour = "-beta";
        }
        if (BuildConfig.FLAVOR.equals("official")) {
            flavour = "-official";
        }
        if (BuildConfig.FLAVOR.equals("donation")) {
            flavour = "-donation";
        }

        try {
            applicationVersion = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Intent connectIntent = new Intent(mContext, WimicService.class);
        connectIntent.putExtra(RimicService.EXTRAS_SERVER, server);
        connectIntent.putExtra(RimicService.EXTRAS_CLIENT_NAME, appname+flavour+" "+applicationVersion);
        connectIntent.putExtra(RimicService.EXTRAS_TRANSMIT_MODE, inputMethod);
        connectIntent.putExtra(RimicService.EXTRAS_DETECTION_THRESHOLD, mSettings.getDetectionThreshold());
        connectIntent.putExtra(RimicService.EXTRAS_AMPLITUDE_BOOST, mSettings.getAmplitudeBoostMultiplier());
        connectIntent.putExtra(RimicService.EXTRAS_AUTO_RECONNECT, mSettings.isAutoReconnectEnabled());
        connectIntent.putExtra(RimicService.EXTRAS_AUTO_RECONNECT_DELAY, WimicService.RECONNECT_DELAY);
        connectIntent.putExtra(RimicService.EXTRAS_USE_OPUS, !mSettings.isOpusDisabled());
        connectIntent.putExtra(RimicService.EXTRAS_INPUT_RATE, mSettings.getInputSampleRate());
        connectIntent.putExtra(RimicService.EXTRAS_INPUT_QUALITY, mSettings.getInputQuality());
        connectIntent.putExtra(RimicService.EXTRAS_FORCE_TCP, mSettings.isTcpForced());
        connectIntent.putExtra(RimicService.EXTRAS_USE_TOR, mSettings.isTorEnabled());
        connectIntent.putStringArrayListExtra(RimicService.EXTRAS_ACCESS_TOKENS, (ArrayList<String>) mDatabase.getAccessTokens(server.getId()));
        connectIntent.putExtra(RimicService.EXTRAS_AUDIO_SOURCE, audioSource);
        connectIntent.putExtra(RimicService.EXTRAS_AUDIO_STREAM, audioStream);
        connectIntent.putExtra(RimicService.EXTRAS_FRAMES_PER_PACKET, mSettings.getFramesPerPacket());
        connectIntent.putExtra(RimicService.EXTRAS_TRUST_STORE, WimicTrustStore.getTrustStorePath(mContext));
        connectIntent.putExtra(RimicService.EXTRAS_TRUST_STORE_PASSWORD, WimicTrustStore.getTrustStorePassword());
        connectIntent.putExtra(RimicService.EXTRAS_TRUST_STORE_FORMAT, WimicTrustStore.getTrustStoreFormat());
        connectIntent.putExtra(RimicService.EXTRAS_HALF_DUPLEX, mSettings.isHalfDuplex());
        connectIntent.putExtra(RimicService.EXTRAS_ENABLE_PREPROCESSOR, mSettings.isPreprocessorEnabled());
        if (server.isSaved()) {
            ArrayList<Integer> muteHistory = (ArrayList<Integer>) mDatabase.getLocalMutedUsers(server.getId());
            ArrayList<Integer> ignoreHistory = (ArrayList<Integer>) mDatabase.getLocalIgnoredUsers(server.getId());
            connectIntent.putExtra(RimicService.EXTRAS_LOCAL_MUTE_HISTORY, muteHistory);
            connectIntent.putExtra(RimicService.EXTRAS_LOCAL_IGNORE_HISTORY, ignoreHistory);
        }

        if (mSettings.isUsingCertificate()) {
            long certificateId = mSettings.getDefaultCertificate();
            byte[] certificate = mDatabase.getCertificateData(certificateId);
            if (certificate != null)
                connectIntent.putExtra(RimicService.EXTRAS_CERTIFICATE, certificate);
            // TODO(acomminos): handle the case where a certificate's data is unavailable.
        }

        connectIntent.setAction(RimicService.ACTION_CONNECT);
        return connectIntent;
    }

    @Override
    protected void onPostExecute(Intent intent) {
        super.onPostExecute(intent);
        mContext.startService(intent);
    }
}
