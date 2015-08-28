package com.dt_workshop.InAppCrossBrowser;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import org.apache.cordova.CordovaWebView;
import org.chromium.net.NetError;
import org.crosswalk.engine.XWalkCordovaResourceClient;
import org.crosswalk.engine.XWalkCordovaUiClient;
import org.crosswalk.engine.XWalkWebViewEngine;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;

import java.util.HashMap;

public class InAppCrossBrowser extends WebViewBrowser {

    private XWalkView xWalkView = null;

    /**
     * Closes the dialog
     */
    public void closeDialog() {
        final WebView childView = this.inAppWebView;
        // The JS protects against multiple calls, so this should happen only when
        // closeDialog() is called by other native code.
        if (childView == null) {
            return;
        }
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                childView.setWebViewClient(new WebViewClient() {
                    // NB: wait for about:blank before dismissing
                    public void onPageFinished(WebView view, String url) {
                        if (dialog != null) {
                            {
                                dialog.dismiss();
                                dialog = null;

                                xWalkView.onDestroy();
                                xWalkView = null;

                                cordova.getActivity()
                                        .getWindow()
                                        .getDecorView()
                                        .findViewById(android.R.id.content)
                                        .postInvalidateDelayed(300);
                            }
                        }
                    }
                });

                // NB: From SDK 19: "If you call methods on WebView from any thread
                // other than your app's UI thread, it can cause unexpected results."
                // http://developer.android.com/guide/webapps/migrating.html#Threads
                childView.loadUrl("about:blank");
                inAppWebView = null;
            }
        });

        try {
            JSONObject obj = new JSONObject();
            obj.put("type", EXIT_EVENT);
            sendUpdate(obj, false);
        } catch (JSONException ex) {
            Log.d(LOG_TAG, "Should never happen");
        }
    }

    protected void loadURL(String url) {
        if (xWalkView != null) {
            xWalkView.load(url, null);
        } else {
            inAppWebView.loadUrl(url);
        }
    }

    protected WebView createBuildInWebView() {
        final CordovaWebView thatWebView = this.webView;
        // Edit Text Box
        edittext = new EditText(cordova.getActivity());
        RelativeLayout.LayoutParams textLayoutParams =
                new RelativeLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
        textLayoutParams.addRule(RelativeLayout.RIGHT_OF, 1);
        textLayoutParams.addRule(RelativeLayout.LEFT_OF, 5);
        edittext.setLayoutParams(textLayoutParams);
        edittext.setId(4);
        edittext.setSingleLine(true);
        edittext.setText("");
        edittext.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        edittext.setImeOptions(EditorInfo.IME_ACTION_GO);
        edittext.setInputType(InputType.TYPE_NULL); // Will not except input... Makes the text NON-EDITABLE
        edittext.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    //                    navigate(edittext.getText().toString());
                    return true;
                }
                return false;
            }
        });

        inAppWebView = new WebView(cordova.getActivity());
        inAppWebView.setLayoutParams(new LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT));
        inAppWebView.setWebChromeClient(new InAppChromeClient(thatWebView));
        WebViewClient client = new InAppBrowserClient(thatWebView, edittext);
        inAppWebView.setWebViewClient(client);
        WebSettings settings = inAppWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setBuiltInZoomControls(true);
        settings.setPluginState(android.webkit.WebSettings.PluginState.ON);

        //Toggle whether this is enabled or not!
        Bundle appSettings = cordova.getActivity().getIntent().getExtras();
        boolean enableDatabase =
                appSettings == null ? true : appSettings.getBoolean("InAppBrowserStorageEnabled", true);
        if (enableDatabase) {
            String databasePath = cordova.getActivity()
                    .getApplicationContext()
                    .getDir("inAppBrowserDB", Context.MODE_PRIVATE)
                    .getPath();
            settings.setDatabasePath(databasePath);
            settings.setDatabaseEnabled(true);
        }
        settings.setDomStorageEnabled(true);

        if (clearAllCache) {
            CookieManager.getInstance().removeAllCookie();
        } else if (clearSessionCache) {
            CookieManager.getInstance().removeSessionCookie();
        }

        inAppWebView.setId(6);
        inAppWebView.getSettings().setLoadWithOverviewMode(true);
        inAppWebView.getSettings().setUseWideViewPort(true);
        inAppWebView.requestFocus();
        inAppWebView.requestFocusFromTouch();

        return inAppWebView;
    }

    /**
     * copy from XWalkCordovaResourceClient.convertErrorCode
     */
    public static int convertErrorCode(int netError) {
        // Note: many NetError.Error constants don't have an obvious mapping.
        // These will be handled by the default case, ERROR_UNKNOWN.
        switch (netError) {
            case NetError.ERR_UNSUPPORTED_AUTH_SCHEME:
                return XWalkCordovaResourceClient.ERROR_UNSUPPORTED_AUTH_SCHEME;

            case NetError.ERR_INVALID_AUTH_CREDENTIALS:
            case NetError.ERR_MISSING_AUTH_CREDENTIALS:
            case NetError.ERR_MISCONFIGURED_AUTH_ENVIRONMENT:
                return XWalkCordovaResourceClient.ERROR_AUTHENTICATION;

            case NetError.ERR_TOO_MANY_REDIRECTS:
                return XWalkCordovaResourceClient.ERROR_REDIRECT_LOOP;

            case NetError.ERR_UPLOAD_FILE_CHANGED:
                return XWalkCordovaResourceClient.ERROR_FILE_NOT_FOUND;

            case NetError.ERR_INVALID_URL:
                return XWalkCordovaResourceClient.ERROR_BAD_URL;

            case NetError.ERR_DISALLOWED_URL_SCHEME:
            case NetError.ERR_UNKNOWN_URL_SCHEME:
                return XWalkCordovaResourceClient.ERROR_UNSUPPORTED_SCHEME;

            case NetError.ERR_IO_PENDING:
            case NetError.ERR_NETWORK_IO_SUSPENDED:
                return XWalkCordovaResourceClient.ERROR_IO;

            case NetError.ERR_CONNECTION_TIMED_OUT:
            case NetError.ERR_TIMED_OUT:
                return XWalkCordovaResourceClient.ERROR_TIMEOUT;

            case NetError.ERR_FILE_TOO_BIG:
                return XWalkCordovaResourceClient.ERROR_FILE;

            case NetError.ERR_HOST_RESOLVER_QUEUE_TOO_LARGE:
            case NetError.ERR_INSUFFICIENT_RESOURCES:
            case NetError.ERR_OUT_OF_MEMORY:
                return XWalkCordovaResourceClient.ERROR_TOO_MANY_REQUESTS;

            case NetError.ERR_CONNECTION_CLOSED:
            case NetError.ERR_CONNECTION_RESET:
            case NetError.ERR_CONNECTION_REFUSED:
            case NetError.ERR_CONNECTION_ABORTED:
            case NetError.ERR_CONNECTION_FAILED:
            case NetError.ERR_SOCKET_NOT_CONNECTED:
                return XWalkCordovaResourceClient.ERROR_CONNECT;

            case NetError.ERR_INTERNET_DISCONNECTED:
            case NetError.ERR_ADDRESS_INVALID:
            case NetError.ERR_ADDRESS_UNREACHABLE:
            case NetError.ERR_NAME_NOT_RESOLVED:
            case NetError.ERR_NAME_RESOLUTION_FAILED:
                return XWalkCordovaResourceClient.ERROR_HOST_LOOKUP;

            case NetError.ERR_SSL_PROTOCOL_ERROR:
            case NetError.ERR_SSL_CLIENT_AUTH_CERT_NEEDED:
            case NetError.ERR_TUNNEL_CONNECTION_FAILED:
            case NetError.ERR_NO_SSL_VERSIONS_ENABLED:
            case NetError.ERR_SSL_VERSION_OR_CIPHER_MISMATCH:
            case NetError.ERR_SSL_RENEGOTIATION_REQUESTED:
            case NetError.ERR_CERT_ERROR_IN_SSL_RENEGOTIATION:
            case NetError.ERR_BAD_SSL_CLIENT_AUTH_CERT:
            case NetError.ERR_SSL_NO_RENEGOTIATION:
            case NetError.ERR_SSL_DECOMPRESSION_FAILURE_ALERT:
            case NetError.ERR_SSL_BAD_RECORD_MAC_ALERT:
            case NetError.ERR_SSL_UNSAFE_NEGOTIATION:
            case NetError.ERR_SSL_WEAK_SERVER_EPHEMERAL_DH_KEY:
            case NetError.ERR_SSL_CLIENT_AUTH_PRIVATE_KEY_ACCESS_DENIED:
            case NetError.ERR_SSL_CLIENT_AUTH_CERT_NO_PRIVATE_KEY:
                return XWalkCordovaResourceClient.ERROR_FAILED_SSL_HANDSHAKE;

            case NetError.ERR_PROXY_AUTH_UNSUPPORTED:
            case NetError.ERR_PROXY_AUTH_REQUESTED:
            case NetError.ERR_PROXY_CONNECTION_FAILED:
            case NetError.ERR_UNEXPECTED_PROXY_AUTH:
                return XWalkCordovaResourceClient.ERROR_PROXY_AUTHENTICATION;

            // The certificate errors are handled by onReceivedSslError
            // and don't need to be reported here.
            case NetError.ERR_CERT_COMMON_NAME_INVALID:
            case NetError.ERR_CERT_DATE_INVALID:
            case NetError.ERR_CERT_AUTHORITY_INVALID:
            case NetError.ERR_CERT_CONTAINS_ERRORS:
            case NetError.ERR_CERT_NO_REVOCATION_MECHANISM:
            case NetError.ERR_CERT_UNABLE_TO_CHECK_REVOCATION:
            case NetError.ERR_CERT_REVOKED:
            case NetError.ERR_CERT_INVALID:
            case NetError.ERR_CERT_WEAK_SIGNATURE_ALGORITHM:
            case NetError.ERR_CERT_NON_UNIQUE_NAME:
                return XWalkCordovaResourceClient.ERROR_OK;

            default:
                return XWalkCordovaResourceClient.ERROR_UNKNOWN;
        }
    }

    protected void attachWebView(ViewGroup viewGroup) {
        View view = createBuildInWebView();

        if (webView.getEngine() instanceof XWalkWebViewEngine) {
            xWalkView = new XWalkView(cordova.getActivity(), (AttributeSet) null);

            xWalkView.setUIClient(new XWalkCordovaUiClient((XWalkWebViewEngine) webView.getEngine()) {
                InAppBrowserClient inAppBrowserClient = new InAppBrowserClient(webView, edittext);

                @Override
                public void onPageLoadStarted(XWalkView view, String url) {
                    inAppBrowserClient.onPageStarted(inAppWebView, url, null);
                }

                @Override
                public void onPageLoadStopped(XWalkView view, String url, LoadStatus status) {
                    inAppBrowserClient.onPageFinished(inAppWebView, view.getUrl());
                }
            });

            xWalkView.setResourceClient(new XWalkResourceClient(xWalkView) {
                InAppBrowserClient inAppBrowserClient = new InAppBrowserClient(webView, edittext);

                @Override
                public void onReceivedLoadError(XWalkView view,
                                                int errorCode,
                                                String description,
                                                String failingUrl) {
                    inAppBrowserClient.onReceivedError(inAppWebView,
                            convertErrorCode(errorCode),
                            description,
                            failingUrl);
                }
            });

            view = xWalkView;
        }

        viewGroup.addView(view);
    }

    /**
     * Convert our DIP units to Pixels
     *
     * @return int
     */
    private int dpToPixels(int dipValue) {
        int value = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                (float) dipValue,
                cordova.getActivity().getResources().getDisplayMetrics());

        return value;
    }

    protected InAppBrowserDialog createBrowserDialog() {
        // Let's create the main dialog
        InAppBrowserDialog dialog = new InAppBrowserDialog(cordova.getActivity(), android.R.style.Theme_NoTitleBar);
        dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setInAppBroswer(getInAppBrowser());

        // Main container layout
        final LinearLayout main = new LinearLayout(cordova.getActivity());
        main.setOrientation(LinearLayout.VERTICAL);

        // Toolbar layout
        RelativeLayout toolbar = new RelativeLayout(cordova.getActivity());
        //Please, no more black!
        toolbar.setBackgroundColor(android.graphics.Color.LTGRAY);
        toolbar.setLayoutParams(new RelativeLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                this.dpToPixels(44)));
        toolbar.setPadding(this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2));
        toolbar.setHorizontalGravity(Gravity.LEFT);
        toolbar.setVerticalGravity(Gravity.TOP);

        // Action Button Container layout
        RelativeLayout actionButtonContainer = new RelativeLayout(cordova.getActivity());
        actionButtonContainer.setLayoutParams(new RelativeLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT));
        actionButtonContainer.setHorizontalGravity(Gravity.LEFT);
        actionButtonContainer.setVerticalGravity(Gravity.CENTER_VERTICAL);
        actionButtonContainer.setId(1);

        Resources activityRes = cordova.getActivity().getResources();

        // Back button
        //        Button back = new Button(cordova.getActivity());
        //        RelativeLayout.LayoutParams backLayoutParams = new RelativeLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.MATCH_PARENT);
        //        backLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
        //        back.setLayoutParams(backLayoutParams);
        //        back.setContentDescription("Back Button");
        //        back.setId(2);
        //        int backResId = activityRes.getIdentifier("ic_action_previous_item", "drawable", cordova.getActivity().getPackageName());
        //        Drawable backIcon = activityRes.getDrawable(backResId);
        //        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
        //            back.setBackgroundDrawable(backIcon);
        //        } else {
        //            back.setBackground(backIcon);
        //        }
        //
        //        back.setOnClickListener(new View.OnClickListener() {
        //            public void onClick(View v) {
        //                goBack();
        //            }
        //        });

        // Close/Done button
        Button close = new Button(cordova.getActivity());
        RelativeLayout.LayoutParams closeLayoutParams =
                new RelativeLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
        closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        close.setLayoutParams(closeLayoutParams);
        close.setContentDescription("Close Button");
        close.setId(5);
        int closeResId =
                activityRes.getIdentifier("ic_action_remove", "drawable", cordova.getActivity().getPackageName());
        Drawable closeIcon = activityRes.getDrawable(closeResId);
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            close.setBackgroundDrawable(closeIcon);
        } else {
            close.setBackground(closeIcon);
        }
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeDialog();
            }
        });

        // Add the back and forward buttons to our action button container layout
        //        actionButtonContainer.addView(back);
        toolbar.addView(actionButtonContainer);
        toolbar.addView(close);

        // Add our toolbar to our main view/layout
        main.addView(toolbar);

        // Add main webview to our main view/layout
        attachWebView(main);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        dialog.setContentView(main);
        dialog.getWindow().setAttributes(lp);

        return dialog;
    }

    @Override
    public String showWebPage(final String url, HashMap<String, Boolean> features) {

        // Determine if we should hide the location bar.
        showLocationBar = true;
        openWindowHidden = false;
        if (features != null) {
            Boolean show = features.get(LOCATION);
            if (show != null) {
                showLocationBar = show.booleanValue();
            }
            Boolean hidden = features.get(HIDDEN);
            if (hidden != null) {
                openWindowHidden = hidden.booleanValue();
            }
            Boolean cache = features.get(CLEAR_ALL_CACHE);
            if (cache != null) {
                clearAllCache = cache.booleanValue();
            } else {
                cache = features.get(CLEAR_SESSION_CACHE);
                if (cache != null) {
                    clearSessionCache = cache.booleanValue();
                }
            }
        }

        dialog = createBrowserDialog();

        // Create dialog in new thread
        Runnable runnable = new Runnable() {
            //            @SuppressLint("NewApi")
            public void run() {

                loadURL(url);

                if (openWindowHidden) {
                    dialog.hide();
                } else {
                    dialog.show();
                }

            }
        };

        this.cordova.getActivity().runOnUiThread(runnable);

        return "";
    }

}
