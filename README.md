This is to support the crosswalk browser engine in android. If no crosswalk engine, it fails back to the original webView browser. For iOS you still need use the plugin:cordova-plugin-inappbrowser   

# What is the enhancement (android only)
1. support the crosswalk engine
2. remove the url input box (normally on one want to have the url input box for the in app browser)

# How to install
     cordova plugin add dtworkshop-inappcrossbrowser

# How to use it 
Same as org.apache.cordova.inappbrowser. Just call the window.open like this

     window.open("www.google.com",
           '_blank',
           'hidden=no,location=no,transitionstyle=fliphorizontal,presentationstyle=pagesheet');
