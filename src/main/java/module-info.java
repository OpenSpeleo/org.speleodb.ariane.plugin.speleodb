import com.arianesline.ariane.plugin.api.DataServerPlugin;
import com.arianesline.ariane.plugin.speleodb.SpeleoDBPlugin;


module com.arianesline.ariane.plugin.speleoDB {
    requires transitive com.arianesline.ariane.plugin.api;
    requires transitive com.arianesline.cavelib.api;

    requires transitive javafx.base;
    requires transitive javafx.controls;
    requires javafx.web;
    requires java.prefs;
    requires javafx.fxml;
    requires jakarta.xml.bind;
    requires transitive jakarta.json;
    requires java.net.http;

    exports com.arianesline.ariane.plugin.speleodb to javafx.graphics,javafx.fxml,javafx.web;
    opens com.arianesline.ariane.plugin.speleodb to javafx.fxml,javafx.graphics,java.base,javafx.web;

    provides DataServerPlugin
            with SpeleoDBPlugin;

}