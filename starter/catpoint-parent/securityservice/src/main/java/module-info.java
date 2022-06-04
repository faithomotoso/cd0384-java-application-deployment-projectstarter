module com.udacity.catpoint.securityservice {
    requires imageservice;
    requires java.prefs;
    requires com.google.common;
    requires gson;
    requires java.desktop;
    requires java.sql;

    exports com.udacity.catpoint.security.service;
    exports com.udacity.catpoint.security.data;

    opens com.udacity.catpoint.security.data to gson;
}