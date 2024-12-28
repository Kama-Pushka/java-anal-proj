module org.ulearnstatistics {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires org.apache.commons.lang3;
    requires java.sql;
    requires com.google.gson;
//    requires sdk;

    opens org.ulearnstatistics to javafx.fxml;
    exports org.ulearnstatistics;
}