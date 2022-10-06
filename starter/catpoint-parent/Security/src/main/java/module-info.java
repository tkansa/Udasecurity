module Security {

    exports com.udacity.catpoint.security.data;
    exports com.udacity.catpoint.security.service;
    exports com.udacity.catpoint.security.application;
    requires java.desktop;
    requires com.google.gson;
    requires com.google.common;
    requires java.prefs;
    requires Image;
}