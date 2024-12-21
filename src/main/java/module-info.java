module org.ulearnstatistic {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.sql;
    requires org.apache.commons.lang3;
    requires org.xerial.sqlitejdbc;
    requires java.desktop;
    requires com.google.gson;
//    requires sdk;

    opens org.ulearnstatistic to javafx.fxml;
    exports org.ulearnstatistic;
}