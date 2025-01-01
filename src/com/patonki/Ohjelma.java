package com.patonki;

import com.patonki.helper.FolderHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


public class Ohjelma extends Application {
    public static Stage STAGE;
    private OptionsManager options;

    private static final String configFilePath = "data/options.txt";
    @Override
    public void start(Stage stage) throws Exception {
        long start = System.currentTimeMillis();
        System.out.println("Starting now!");
        STAGE = stage;


        options = new OptionsManager(configFilePath);
        options.readConfig();

        FolderHandler folderHandler = new FolderHandler("data", true);

        /*Ladataan asetukset*/
        int width=options.getIntProperty("width");
        int height=options.getIntProperty("height");
        stage.setWidth(width);
        stage.setHeight(height);

        long middle = System.currentTimeMillis();
        System.out.println("Middle before fxml: " + (middle- start));

        //Ladataan fxml tiedosto, jossa ui:n elementit määritellään
        FXMLLoader loader = new FXMLLoader(Ohjelma.class.getResource("/main.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();
        System.out.println("Before init: " + (System.currentTimeMillis() - start));
        controller.init(options, folderHandler);

        System.out.println("Before scene "  + (System.currentTimeMillis() - start));
        //Asetetaan ikkunan asetukset
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.getIcons().add(new Image("/icon.png"));
        stage.setAlwaysOnTop(true);
        stage.setTitle("Notes");
        stage.show();
        stage.setOnCloseRequest(e -> {
            //Suljettaessa tallennetaan asetukset
            saveConfiguration(controller,stage);
            controller.onClose();
        });
        System.out.println("Finished " + (System.currentTimeMillis() - start));
        System.out.println("END " + System.currentTimeMillis());
    }
    //Tallennetaan tällä hetkellä voimassa olevat asetukset.
    //Esim jos käyttäjä sulkee ohjelman kun ikkunan leveys on 300px, ikkunan leveys
    //on 300px kun ohjelma avataan uudestaan
    private void saveConfiguration(Controller controller, Stage stage) {
        int fontSize = controller.getFontSize();
        int curWidth = (int) stage.getWidth();
        int curHeight = (int) stage.getHeight();

        options.setProperty("fontSize",fontSize+"");
        options.setProperty("width",curWidth+"");
        options.setProperty("height",curHeight+"");

        options.writeConfig();
    }
    public static void aloita() {
        launch(Ohjelma.class);
    }
}
