/*
 *   Copyright (C) 2017 Teclib. All rights reserved.
 *
 * This file is part of flyve-mdm-android-agent
 *
 * flyve-mdm-android-agent is a subproject of Flyve MDM. Flyve MDM is a mobile
 * device management software.
 *
 * Flyve MDM is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * Flyve MDM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * ------------------------------------------------------------------------------
 * @author    Rafael Hernandez
 * @date      02/06/2017
 * @copyright Copyright (C) 2017 Teclib. All rights reserved.
 * @license   GPLv3 https://www.gnu.org/licenses/gpl-3.0.html
 * @link      https://github.com/flyve-mdm/flyve-mdm-android-agent
 * @link      https://flyve-mdm.com
 * ------------------------------------------------------------------------------
 */

package org.flyve.mdm.agent.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.flyve.mdm.agent.R;
import org.flyve.mdm.agent.core.enrollment.EnrollmentHelper;
import org.flyve.mdm.agent.data.MqttData;
import org.flyve.mdm.agent.data.SupervisorData;
import org.flyve.mdm.agent.utils.FlyveLog;
import org.flyve.mdm.agent.utils.Helpers;

public class StartEnrollmentActivity extends Activity {

    private RelativeLayout btnEnroll;
    private TextView txtMessage;
    private TextView txtTitle;
    private ProgressBar pb;
    private static final int REQUEST_EXIT = 1;
    private boolean status = true;

    /**
     * Called when the activity is starting
     * It shows the UI to start the enrollment
     * @param savedInstanceState if the activity is being re-initialized, it contains the data it most recently supplied
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_enrollment);

        broadcastClose();

        MqttData cache = new MqttData( StartEnrollmentActivity.this );
        // if broker is on cache open the main activity
        String broker = cache.getBroker();
        if(broker != null) {
            openMain();
        }

        TextView txtIntro = (TextView) findViewById(R.id.txtIntro);
        txtIntro.setText( Html.fromHtml(StartEnrollmentActivity.this.getResources().getString(R.string.walkthrough_step_1)) );
        txtIntro.setMovementMethod(LinkMovementMethod.getInstance());

        txtMessage = (TextView) findViewById(R.id.txtMessage);
        txtTitle = (TextView) findViewById(R.id.txtTitle);
        pb = (ProgressBar) findViewById(R.id.progressBar);

        Intent intent = getIntent();
        Uri data = intent.getData();

        String deepLinkErrorMessage = getResources().getString(R.string.ERROR_DEEP_LINK);
        String deepLinkData;

        try {
            deepLinkData = Helpers.base64decode(data.getQueryParameter("data"));
        } catch(Exception ex) {
            showError( deepLinkErrorMessage );
            FlyveLog.e(deepLinkErrorMessage + " - " + ex.getMessage());
            return;
        }

        String url;
        String userToken;
        String invitationToken;
        String name;
        String phone;
        String website;
        String email;

        try {
            // CSV comma-separated values format
            // url; user token; invitation token; support name; support phone, support website; support email
            String[] csv = deepLinkData.split("\\\\;");

            if(csv.length >= 0) {

                // url
                if(!csv[0].isEmpty()) {
                    url = csv[0];
                } else {
                    deepLinkErrorMessage = "URL " + deepLinkErrorMessage;
                    showError( deepLinkErrorMessage );
                    return;
                }

                // user token
                if(!csv[1].isEmpty()) {
                    userToken = csv[1];
                } else {
                    deepLinkErrorMessage = "USER " + deepLinkErrorMessage;
                    showError( deepLinkErrorMessage );
                    return;
                }

                // invitation token
                if(!csv[2].isEmpty()) {
                    invitationToken = csv[2];
                } else {
                    deepLinkErrorMessage = "TOKEN " + deepLinkErrorMessage;
                    showError( deepLinkErrorMessage );
                    return;
                }

                SupervisorData supervisorData = new SupervisorData(StartEnrollmentActivity.this);

                // name
                if(csv.length > 3 && !csv[3].isEmpty()) {
                    name = csv[3];
                    supervisorData.setName(name);
                }

                // phone
                if(csv.length > 4 && !csv[4].isEmpty()) {
                    phone = csv[4];
                    supervisorData.setPhone(phone);
                }

                // website
                if(csv.length > 5 && !csv[5].isEmpty()) {
                    website = csv[5];
                    supervisorData.setWebsite(website);
                }

                // email
                if(csv.length > 6 && !csv[6].isEmpty()) {
                    email = csv[6];
                    supervisorData.setEmail(email);
                }

                cache.setUrl(url);
                cache.setUserToken(userToken);
                cache.setInvitationToken(invitationToken);

            } else {
                showError( deepLinkErrorMessage );
            }

        } catch (Exception ex) {
            FlyveLog.e(ex.getMessage());
            showError( deepLinkErrorMessage );
        }

        btnEnroll = (RelativeLayout) findViewById(R.id.btnEnroll);
        btnEnroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnEnroll.setVisibility(View.GONE);
                txtMessage.setText(getResources().getString(R.string.please_wait));
                pb.setVisibility(View.VISIBLE);

                EnrollmentHelper sessionToken = new EnrollmentHelper(StartEnrollmentActivity.this);
                sessionToken.getActiveSessionToken(new EnrollmentHelper.EnrollCallBack() {
                    @Override
                    public void onSuccess(String data) {
                        btnEnroll.setVisibility(View.VISIBLE);
                        pb.setVisibility(View.GONE);
                        txtMessage.setText("");
                        txtTitle.setText(getResources().getString(R.string.start_enroll));

                        // Active EnrollmentHelper Token is stored on cache
                        openActivity();
                    }

                    @Override
                    public void onError(String error) {
                        btnEnroll.setVisibility(View.VISIBLE);
                        pb.setVisibility(View.GONE);
                        showError( error );
                    }
                });
            }
        });
    }

    /**
     * Send a Broadcast with the Close Action
     */
    public void broadcastClose() {
        //send broadcast
        Intent in = new Intent();
        in.setAction("flyve.ACTION_CLOSE");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(in);
    }
    
    /**
     * Shows an error message
     * @param message
     */
    private void showError(String message) {
        txtTitle.setText(getResources().getString(R.string.fail_enroll));

        Helpers.snack(this, message, this.getResources().getString(R.string.snackbar_close), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    /**
     * Open activity
     */
    private void openActivity() {
        Intent miIntent = new Intent(StartEnrollmentActivity.this, EnrollmentActivity.class);
        StartEnrollmentActivity.this.startActivityForResult(miIntent, REQUEST_EXIT);
    }

    /**
     * Open the main activity
     */
    private void openMain() {
        Intent intent = new Intent(StartEnrollmentActivity.this, MainActivity.class);
        StartEnrollmentActivity.this.startActivity(intent);
        StartEnrollmentActivity.this.finish();
    }

    /**
     * Called when the launched activity exits
     * @param requestCode the request code originally supplied, it identifies who this result came from
     * @param resultCode the result code returned
     * @param data an intent which can return result data to the caller
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EXIT && resultCode == RESULT_OK) {
            this.finish();
        }
    }
}