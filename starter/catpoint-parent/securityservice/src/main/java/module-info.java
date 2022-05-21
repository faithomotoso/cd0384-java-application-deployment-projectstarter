module com.udacity.catpoint.securityservice {
    requires imageservice;
    requires java.prefs;
    requires com.google.common;
    requires gson;
    requires java.desktop;

    exports com.udacity.catpoint.security.data;
    exports com.udacity.catpoint.security.service;
}