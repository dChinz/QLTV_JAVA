module com.example.management {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires java.sql;
    requires mysql.connector.j;
    requires jbcrypt;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome6;
    uses org.kordamp.ikonli.IkonProvider;

    opens com.example to javafx.fxml;
    opens com.example.controller to javafx.fxml;
    opens com.example.model to javafx.base;

    exports com.example;
    exports com.example.controller;
    exports com.example.model;
    exports com.example.service;
    exports com.example.dao;
    exports com.example.config;
}
