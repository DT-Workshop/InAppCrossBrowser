This is to enhance the cordova inAppBrowser plugin to support the crosswalk browser engine in android. If no crosswalk engine, it fails back to the original webView browser.   

# What is the enhancement (android only)
1. support the crosswalk engine
2. remove the url input box (noramlly on one what to have the url input box for the in app browser)

# How to install
 cordova plugin add com.DT-Workshop.InAppBrowser

# How to use it 
Same as org.apache.cordova.inappbrowser. Just call the window.open like this

    $scope.browser = window.open("www.google.com",
                        '_blank',
                        'hidden=yes,location=no,transitionstyle=fliphorizontal,presentationstyle=pagesheet');
