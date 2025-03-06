package com.arianesline.ariane.plugin.speleodb;


import com.arianesline.ariane.plugin.api.DataServerCommands;
import com.arianesline.ariane.plugin.api.DataServerPlugin;
import com.arianesline.ariane.plugin.api.PluginInterface;
import com.arianesline.ariane.plugin.api.PluginType;
import com.arianesline.cavelib.api.CaveSurveyInterface;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.util.Objects;


public class SpeleoDBPlugin implements DataServerPlugin {

    private final StringProperty commandProperty = new SimpleStringProperty();
    private CaveSurveyInterface survey;
    private File surveyFile;

    @Override
    public File getSurveyFile() {
        return surveyFile;
    }

    @Override
    public void setSurveyFile(File file) {
        surveyFile = file;
    }

    @Override
    public StringProperty getCommandProperty() {
        return commandProperty;
    }

    @Override
    public CaveSurveyInterface getSurvey() {
        return survey;
    }

    @Override
    public void setSurvey(CaveSurveyInterface survey) {
        this.survey = survey;

    }

    @Override
    public void showUI() {
        SpeleoDBController controller = new SpeleoDBController();
        controller.parentPlugin = this;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/SpeleoDB.fxml"));
        fxmlLoader.setController(controller);
        Parent root1 = null;
        try {
            root1 = fxmlLoader.load();

        } catch (IOException e) {
            e.printStackTrace();
        }
        Stage stage = new Stage();
        stage.initStyle(StageStyle.DECORATED);
        stage.setScene(new Scene(root1));
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))));
        stage.setTitle("SpeleoDB");
        stage.show();
    }

    @Override
    public Node getUINode() {
        SpeleoDBController controller = new SpeleoDBController();
        controller.parentPlugin = this;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/SpeleoDB.fxml"));
        fxmlLoader.setController(controller);
        Parent root1 = null;
        try {
            root1 = fxmlLoader.load();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return root1;
    }

    @Override
    public Image getIcon() {
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png")));
    }


    public void saveSurvey() {
        commandProperty.set(DataServerCommands.SAVE.name());
    }
    public void loadSurvey(File file) {
        surveyFile=file;
        commandProperty.set(DataServerCommands.LOAD.name());
    }

    @Override
    public PluginInterface getInterfaceType() {
        return PluginInterface.LEFT_TAB;
    }

    @Override
    public String getName() {
        return "SPELEO_DB";
    }

    @Override
    public PluginType getType() {
        return PluginType.DATASERVER;
    }

    @Override
    public void showSettings() {


    }
}