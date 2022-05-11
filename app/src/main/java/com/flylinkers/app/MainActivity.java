package com.flylinkers.app;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;


public class MainActivity extends AppCompatActivity {
    private RequestQueue queue;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    int RC_SIGN_IN = 1;
    private Button btnSignIn;
    String TAG = "GoogleSignIn";
    String url = "https://app.flylinkers.com";
    SwipeRefreshLayout mySwipeRefreshLayout;
    WebView myWebView;

    final Context context = this;
    private final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0;
    private final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0;
    private final int MY_PERMISSIONS_REQUEST_READ_CAMERA = 0;
    public static final int INPUT_FILE_REQUEST_CODE = 1;

    public ValueCallback<Uri[]> uploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;

    private String mCameraPhotoPath;
    private ValueCallback<Uri> mUploadMessage;
    private final static int FILECHOOSER_RESULTCODE = 1;

    //private final Handler handler = new Handler(this);
    private static final int CLICK_ON_WEBVIEW = 1;
    private static final int CLICK_ON_URL = 2;

    @Override
    protected void onCreate(Bundle SavedInstancesState){
        super.onCreate(SavedInstancesState);
        setContentView(R.layout.activity_main);

        //init request test
        queue = Volley.newRequestQueue(this);

        //login
        GoogleSignInOptions gso = new GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();
        //end login

        myWebView = (WebView) findViewById(R.id.webview);
        assert myWebView != null;

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setSupportZoom(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        myWebView.getSettings().setAppCacheEnabled(true);
        myWebView.getSettings().setLoadsImagesAutomatically(true);
        myWebView.getSettings().setDatabaseEnabled(true);
        myWebView.getSettings().setDomStorageEnabled(true);
        myWebView.getSettings().setSupportMultipleWindows(true);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        myWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);


        if(Build.VERSION.SDK_INT >= 22){
            webSettings.setMixedContentMode(0);
            myWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }else if(Build.VERSION.SDK_INT >= 19){
            myWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }else if(Build.VERSION.SDK_INT < 19){
            myWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        myWebView.setWebViewClient(new WebViewClient());
        myWebView.setWebChromeClient(new WebChromeClient());
        myWebView.setWebViewClient(new MyWebViewClient());
        myWebView.setVerticalScrollBarEnabled(false);

        myWebView.loadUrl(url);
        myWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        mySwipeRefreshLayout = this.findViewById(R.id.swipeContainer);
        mySwipeRefreshLayout.setOnRefreshListener(
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    //findViewById(R.id.loaderwebview).setVisibility(View.VISIBLE);
                    myWebView.reload();
                    mySwipeRefreshLayout.setRefreshing(false);
                }
            }
        );

        myWebView.setWebChromeClient(new WebChromeClient() {
            // For 3.0+ Devices (Start)
            // onActivityResult attached before constructor
            protected void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
            }

            // For Lollipop 5.0+ Devices
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }
                uploadMessage = filePathCallback;
                Intent i = fileChooserParams.createIntent();
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");

                try {
                    startActivityForResult(i, REQUEST_SELECT_FILE);
                } catch (ActivityNotFoundException e) {
                    uploadMessage = null;
                    System.out.println("error loading file: "+e);
                    return false;
                }
                return true;
            }

            protected void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
            }
        });
    };


    private void getUserToken(String user){
        String api = "https://api.flylinkers.com/user/loginapp/?user="+user;

        /*JSONObject object = new JSONObject();
        try {
            //input your API parameters
            object.put("email", "david38595@gmail.com");
            object.put("password","115401861950237523615");
            object.put("photo","https://lh3.googleusercontent.com/a-/AOh14GgJLy8zdjyfC0haY-vzjRaQ3Jg3Kmiq2ujlJCbcDQ=s96-c");
            object.put("name","Omar Lamprea");
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        //post
        StringRequest postRequest = new StringRequest(Request.Method.POST, api,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    System.out.println("Responseeee: "+response);
                    String url = "";
                    String created = "";
                    int ward = 0;
                    char[] charResponse = response.toCharArray();

                    for (int i = 0; i < response.length(); i++) {
                        if (charResponse[i] == 34){
                            ward++;
                            i++;
                        }
                        if (ward == 3 && charResponse[i] != 34){
                            url += charResponse[i];
                        }

                        if (ward == 6 && charResponse[i] != 58 && charResponse[i] != 44){
                            created += charResponse[i];
                        }
                    }
                    if (created.equals("true")){
                        myWebView.loadUrl("https://app.flylinkers.com/?user=" + url + "&newUser=" + created);
                    }else{
                        myWebView.loadUrl("https://app.flylinkers.com/?user=" + url);
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    System.out.println("Error:"+error);
                }
            }
        );

        queue.add(postRequest);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Resultado devuelto al iniciar el Intent de GoogleSignInApi.getSignInIntent (...);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_SELECT_FILE) {
                if (uploadMessage == null)
                    return;
                uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                uploadMessage = null;
            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage)
                return;
            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
            // Use RESULT_OK only if you're implementing WebView inside an Activity
            Uri result = data == null || resultCode != this.RESULT_OK ? null : data.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        } else
            Toast.makeText(this, "Failed to Upload Image", Toast.LENGTH_LONG).show();

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            if(task.isSuccessful()){
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                    firebaseAuthWithGoogle(
                            account.getIdToken(),
                            account.getEmail(),
                            account.getDisplayName(),
                            account.getPhotoUrl(),
                            account.getId()
                    );
                } catch (ApiException e) {
                    // Google Sign In fallido, actualizar GUI
                    Log.w(TAG, "Google sign in failed", e);
                }
            }else{
                Log.d(TAG, "Error, login no exitoso:" + task.getException().toString());
                Toast.makeText(this, "Ocurrio un error. "+task.getException().toString(), Toast.LENGTH_LONG).show();
            }
        }

    }

    private void firebaseAuthWithGoogle(
            String idToken,
            String email,
            String displayName,
            Uri photo,
            String Id
            ) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success");
                    String infoUser = "{'email': '" + email + "', 'password': '" + Id + "', 'name': '" + displayName + "', 'photo': '" + photo + "'}";
                    System.out.println(infoUser);
                     getUserToken(infoUser);
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                }
            }
        });
    }


    //login
    private void signIn(){
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    //action: back page
    @Override
    public void onBackPressed(){
        if (myWebView != null && myWebView.canGoBack()){
            myWebView.goBack();
        }else{
            super.onBackPressed();
        }
    }

    private class MyWebViewClient extends WebViewClient{
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon){
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url){
            findViewById(R.id.loaderwebview).setVisibility(View.GONE);
            findViewById(R.id.webview).setVisibility(View.VISIBLE);
        }

        /*@Override
        public boolean shouldOverrideUrlLoading(WebView view, String url){

            if (url.indexOf("mailto:") > -1){
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                return true;
            }/*else if(url.startsWith("https://www.youtube.com")){
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                return true;
            }else{
                view.loadUrl(url);
                return true;
            }
        }*/
    }


    public class WebAppInterface {
        Context mContext;
        /* Instantiate the interface and set the context*/
        WebAppInterface(Context c) {
            mContext = c;
        }

        /* start function from the web page*/
        @JavascriptInterface
        public void showToast(String toast) {
            signIn();
        }
    }
}