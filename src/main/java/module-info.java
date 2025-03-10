import com.arianesline.ariane.plugin.api.DataServerPlugin;
import com.arianesline.ariane.plugin.speleodb.SpeleoDBPlugin;


module com.arianesline.ariane.plugin.speleoDB {
    requires com.arianesline.ariane.plugin.api;
    requires com.arianesline.cavelib.api;

    requires javafx.controls;
    requires javafx.web;
    requires java.prefs;
    requires javafx.fxml;
    requires jakarta.xml.bind;
    requires jakarta.json;
    requires java.net.http;

    exports com.arianesline.ariane.plugin.speleodb to javafx.graphics,javafx.fxml,javafx.web;
    opens com.arianesline.ariane.plugin.speleodb to javafx.fxml,javafx.graphics,java.base,javafx.web;

    provides DataServerPlugin
            with SpeleoDBPlugin;

}