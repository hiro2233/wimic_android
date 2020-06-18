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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import bo.htakey.rimic.net.RimicCertificateGenerator;
import bo.htakey.wimic.R;
import bo.htakey.wimic.db.DatabaseCertificate;
import bo.htakey.wimic.db.WimicDatabase;
import bo.htakey.wimic.db.WimicSQLiteDatabase;

public class WimicCertificateGenerateTask extends AsyncTask<Void, Void, DatabaseCertificate> {
    private static final String DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss";

    private Context context;
    private ProgressDialog loadingDialog;

    public WimicCertificateGenerateTask(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        loadingDialog = new ProgressDialog(context);
        loadingDialog.setIndeterminate(true);
        loadingDialog.setMessage(context.getString(R.string.generateCertProgress));
        loadingDialog.setCancelable(false);
        loadingDialog.show();
    }
    @Override
    protected DatabaseCertificate doInBackground(Void... params) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            RimicCertificateGenerator.generateCertificate(baos);

            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            String fileName = context.getString(R.string.certificate_export_format, dateFormat.format(new Date()));

            WimicDatabase database = new WimicSQLiteDatabase(context);
            DatabaseCertificate dc = database.addCertificate(fileName, baos.toByteArray());
            database.close();
            return dc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(DatabaseCertificate result) {
        super.onPostExecute(result);
        if(result == null) {
            Toast.makeText(context, R.string.generateCertFailure, Toast.LENGTH_SHORT).show();
        }

        loadingDialog.dismiss();
    }
}
