package com.desperate.pez_android.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.androidadvance.topsnackbar.TSnackbar;
import com.desperate.pez_android.R;
import com.desperate.pez_android.other.MyJsonPostRequest;
import com.desperate.pez_android.other.NetworkReceiver;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class ForgotPassword extends AppCompatActivity {

    private static final int REQUEST_SIGNUP = 0;
    private static final int REQUEST_LOGIN = 1;
    private static final String LOST_LOGIN_URL = "http://www.poe.pl.ua:8080/pezREST/abon/lostlogin";
    private static final String LOST_PASSWORD_URL = "http://www.poe.pl.ua:8080/pezREST/abon/lostpassw";


    BroadcastReceiver broadcastReceiver;
    TSnackbar snackbar;
    boolean showSnackbar = false;
    boolean lostLogin = false;
    boolean lostPassword = false;

    @BindView(R.id.account_layout)
    TextInputLayout _accountLayout;
    @BindView(R.id.email_layout)
    TextInputLayout _emailLayout;
    @BindView(R.id.input_account)
    EditText _accountText;
    @BindView(R.id.input_email)
    EditText _emailText;
    @BindView(R.id.btn_get_login)
    Button _getLoginButton;
    @BindView(R.id.btn_get_password)
    Button _getPasswordButton;
    @BindView(R.id.link_signup)
    TextView _signupLink;
    @BindView(R.id.main_frame)
    LinearLayout mainFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        ButterKnife.bind(this);

        _getLoginButton.setOnClickListener(v -> {
            lostLogin = true;
            getLostData();
        });

        _getPasswordButton.setOnClickListener(v -> {
            lostPassword = true;
            getLostData();
        });

        _signupLink.setOnClickListener(v -> {
            // Start the Signup activity
            Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
            startActivityForResult(intent, REQUEST_SIGNUP);
            finish();
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        });

        broadcastReceiver = new NetworkReceiver() {
            @Override
            public void onNetworkChange() {
                controlSnackbar();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (broadcastReceiver != null) {
            showSnackbar = false;
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    public void onBackPressed() {
        //  going back to the LoginActivity
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    public void controlSnackbar() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > 22) {
            //cm.getNetworkCapabilities(cm.getActiveNetwork()).hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            if (cm.getActiveNetwork() != null) {
                if ( showSnackbar == true) {
                    snackbar = TSnackbar.make(mainFrame, "Підключено", TSnackbar.LENGTH_SHORT);
                    generateSnackbar(snackbar);
                }
            } else {
                showSnackbar = true;
                snackbar = TSnackbar.make(mainFrame, "Відсутнє з'єднання з інтернетом", TSnackbar.LENGTH_INDEFINITE);
                generateSnackbar(snackbar);
            }
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null) {
                if ( showSnackbar == true) {
                    snackbar = TSnackbar.make(mainFrame, "Підключено", TSnackbar.LENGTH_SHORT);
                    generateSnackbar(snackbar);
                }
            } else {
                showSnackbar = true;
                snackbar = TSnackbar.make(mainFrame, "Відсутнє з'єднання з інтернетом", TSnackbar.LENGTH_INDEFINITE);
                generateSnackbar(snackbar);
            }
        }
    }

    public void generateSnackbar(TSnackbar snackbar) {
        View view = snackbar.getView();
        TextView snackbarText = view.findViewById(com.google.android.material.R.id.snackbar_text);
        snackbarText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        snackbarText.setTextColor(ContextCompat.getColor(getApplicationContext(),
                R.color.colorAccent));
        snackbar.show();
    }


    public void getLostData() {

        if (!validate()) {
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(ForgotPassword.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Перевірка...");
        progressDialog.show();

        final String account = _accountText.getText().toString().trim();
        final String emeail = _emailText.getText().toString().trim();

        final JSONObject ob  = new JSONObject();
        try {
            ob.put("personal_account", account);
            ob.put("email", emeail);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (lostLogin) {
            lostLogin = false;
            MyJsonPostRequest myJsonPostRequest = new MyJsonPostRequest(Request.Method.POST, LOST_LOGIN_URL, ob, response -> {
                try {
                    JSONObject obj = response.getJSONObject(0);
                    String username = obj.getString("message");
                    Toast.makeText(this, "Ваш логін: " + username, Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }, error -> {
                parseError(error);
                progressDialog.dismiss();
            });
            myJsonPostRequest.setRetryPolicy(new DefaultRetryPolicy(
                    10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            RequestQueue requestQueue= Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(myJsonPostRequest);
        } else if (lostPassword) {
            lostPassword = false;
            MyJsonPostRequest myJsonPostRequest = new MyJsonPostRequest(Request.Method.POST, LOST_PASSWORD_URL, ob, response -> {
                try {
                    JSONObject obj = response.getJSONObject(0);
                    String message = obj.getString("message");
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }, error -> {
                parseError(error);
                progressDialog.dismiss();
            });
            myJsonPostRequest.setRetryPolicy(new DefaultRetryPolicy(
                    10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            RequestQueue requestQueue= Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(myJsonPostRequest);
        }



    }

    private String parseError(VolleyError error) {
        String message = "";
        try {
            String responseBody = new String(error.networkResponse.data, "utf-8");
            JSONArray data = new JSONArray(responseBody);
            JSONObject jsonMessage = data.getJSONObject(0);
            message = jsonMessage.getString("message");
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        } catch (UnsupportedEncodingException | JSONException e) {
            e.printStackTrace();
        }
        return message;
    }

    public boolean validate() {
        boolean valid = true;

        String account = _accountText.getText().toString();
        String email = _emailText.getText().toString();

        if (account.isEmpty()) {
            _accountLayout.setError("Введіть номер особового рахунку");
            valid = false;
        } else {
            _accountLayout.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailLayout.setError("Введіть коректний e-mail");
            valid = false;
        } else {
            _emailLayout.setError(null);
        }

        return valid;
    }
}
