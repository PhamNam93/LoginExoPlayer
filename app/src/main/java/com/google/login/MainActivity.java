package com.google.login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.login.exoplayer2.PlayerActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "GoogleActivity";
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    FirebaseAuth mAuth;
    private String hls = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8";
    private String dash = "http://www.youtube.com/api/manifest/dash/id/bf5bb2419360daf1/source/youtube?as=fmp4_audio_clear,fmp4_sd_hd_clear&sparams=ip,ipbits,expire,source,id,as&ip=0.0.0.0&ipbits=0&expire=19000000000&signature=51AF5F39AB0CEC3E5497CD9C900EBFEAECCCB5C7.8506521BFC350652163895D4C26DEE124209AA9E&key=ik0";
    private String mp4 = "https://html5demos.com/assets/dizzy.mp4";
    private String mp42 = "https://www.googleapis.com/drive/v3/files/1U49byA3hPC94Y6f28vnoMtLp1EzNjeOq?alt=media";
    private String mkv = "https://www.googleapis.com/drive/v3/files/1TStyqQfhXqayN5upVIFC1v7jQBpste0v?alt=media";

    Button btn1,btn2,btn3,btn4,btnplay;
    EditText et_url;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestSignIn();
        et_url = (EditText) findViewById(R.id.et_url);
        btn1 = (Button)findViewById(R.id.btn1);
        btn2 = (Button)findViewById(R.id.btn2);
        btn3 = (Button)findViewById(R.id.btn3);
        btn4 = (Button)findViewById(R.id.btn4);
        btnplay = (Button)findViewById(R.id.btn_play);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                et_url.setText(hls);
            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                et_url.setText(dash);
            }
        });
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                et_url.setText(mp42);
            }
        });
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                et_url.setText(mkv);
            }
        });
        btnplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(et_url.getText().toString() == ""){
                    Toast.makeText(MainActivity.this,"Url khong dc de trong", Toast.LENGTH_LONG).show();
                    return;
                }
                boolean preferExtensionDecoders = false;
                String videoUrl = et_url.getText().toString();
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                intent.putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS_EXTRA, preferExtensionDecoders);
                intent.putExtra(PlayerActivity.ABR_ALGORITHM_EXTRA, PlayerActivity.ABR_ALGORITHM_DEFAULT);
                intent.setData(Uri.parse(videoUrl));
                intent.setAction(PlayerActivity.ACTION_VIEW);
                startActivity(intent);
            }
        });

    }

    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestServerAuthCode(getString(R.string.default_web_client_id))
                .requestScopes(
                        new Scope("https://www.googleapis.com/auth/drive.activity.readonly")
                        , new Scope("https://www.googleapis.com/auth/drive")
                        , new Scope("https://www.googleapis.com/auth/drive.file")
                        , new Scope("https://www.googleapis.com/auth/drive.appdata")
                        , new Scope("https://www.googleapis.com/auth/drive.metadata.readonly")
                )
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();
        signIn();
        // The result of the sign-in Intent is handled in onActivityResult.
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);


                RequestBody requestBody = new FormBody.Builder()
                        .add("grant_type", "authorization_code")
                        .add("client_id", getString(R.string.default_web_client_id))
                        .add("client_secret", "3l3AHARUlNPcflefZ6TaV3r-")
                        .add("redirect_uri","")
                        .add("code", account.getServerAuthCode())
                        .add("id_token", account.getIdToken()) // Added this extra parameter here
                        .build();
                final Request request = new Request.Builder()
                        .url("https://www.googleapis.com/oauth2/v4/token")
                        .post(requestBody)
                        .build();
                OkHttpClient client = new OkHttpClient();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        try {
                            JSONObject jsonObject = new JSONObject(response.body().string());
                            Global.TokenGoogleDriver = jsonObject.getString("access_token");
                            final String message = jsonObject.toString(5);

                            Log.i(TAG, "firebaseAuthWithGoogle:" + message);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        Log.e(TAG, e.toString());
                    }
                });



//                List<String> scope = new ArrayList<String>();
//                scope.add("https://www.googleapis.com/auth/drive.activity.readonly");
//                scope.add("https://www.googleapis.com/auth/drive");
//                scope.add("https://www.googleapis.com/auth/drive.file");
//                scope.add("https://www.googleapis.com/auth/drive.appdata");
//                scope.add("https://www.googleapis.com/auth/drive.metadata.readonly");

//                GoogleAccountCredential credential =
//                        GoogleAccountCredential.usingOAuth2(
//                                this, scope);
//                credential.setSelectedAccount(account.getAccount());
//                String tk = GoogleAuthUtil.getToken(MainActivity.this, account.getAccount(), credential.getScope());
//                Log.d(TAG, "firebaseAuthWithGoogle:" + tk);

            } catch (Exception e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
            }
        }

    }

}
