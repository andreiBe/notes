package com.patonki;

import com.patonki.helper.FolderHandler;
import com.patonki.javafx.AutocompletionTextField;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Controller {
    @FXML
    private HBox box; //palkki yläosassa, jossa slideri ja textfield on
    @FXML
    private TextArea area; //alue johon muistiinpanot kirjoitetaan
    @FXML
    private Slider slider; //fontin muuttamiseen tarkoitettu slideri

    private AutocompletionTextField field; //tekstiruutu, jonka avulla voi navigoida muistiinpanojen välillä

    private final Storage storage = new Storage(this::handleError);
    //ohjelman asetukset kuten ikkunan koko ja fontin koko
    private OptionsManager options;

    public void init(OptionsManager options, FolderHandler folder) {
        this.options = options;
        loadOptions();

        this.field = new AutocompletionTextField();
        //lisätään kaikki saatavilla olevat muistiinpanot listaan, josta tekstiruutu etsii ehdotettavia muistiinpanoja
        this.field.getEntries().addAll(folder.fileNames());

        box.getChildren().add(field); //lisätään tekstiruutu sliderin viereen

        setContent();
        //Kun tekstialuetta klikkaa, on mahdollisuus poistaa muistiinpano
        MenuItem deleteFileContextMenu = new MenuItem("Delete note");
        MenuItem syncWithFtp = new MenuItem("Sync with FTP server");
        MenuItem pushToFtp = new MenuItem("Push To FTP server");

        area.setContextMenu(new ContextMenu(deleteFileContextMenu, syncWithFtp, pushToFtp));


        // Event listeners
        deleteFileContextMenu.setOnAction(e -> deleteCurrentFile());
        syncWithFtp.setOnAction(e -> storage.syncWithFtpServer().ifPresent(this::handleError));
        pushToFtp.setOnAction(e -> storage.pushToFtpServer().ifPresent(this::handleError));

        field.textProperty().addListener((observableValue, s, t1) -> fieldTyped(t1));
        area.textProperty().addListener((observableValue, s, t1) -> areaTyped(t1));
        slider.valueProperty().addListener((observableValue, number, t1) -> sliderMoved(t1.doubleValue()));
    }
    private void handleError(Exception e) {
        if (e instanceof Storage.DecryptionError) {
            area.textProperty().setValue("Unable to decrypt");
            return;
        }
        field.textProperty().setValue("stack_trace");
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        area.textProperty().setValue(exceptionAsString);
    }
    private void deleteCurrentFile() {
        if (storage.deleteCurrent()) { //poistaminen onnistui
            if (storage.fileNameWithoutExtension().equals("text")) area.clear();
            field.getEntries().remove(storage.fileNameWithoutExtension());
            field.clear(); //tyhjennetään tekstiruutu, samalla tiedostoksi muuttuu oletustiedosto
        }
    }
    private void loadOptions() {
        int fontSize = options.getIntProperty("fontSize");
        storage.loadOptions(options);
        area.setFont(Font.font("Arial",fontSize));
        slider.setValue(fontSize);
    }
    private void setContent() {
        if (storage.exists()) {
            area.setText(storage.currentContent());
        }else {
            area.setText("");
        }
    }
    private void setCurrentFile(String name, String password) {
        storage.setCurrentFile(name, password);
        setContent();
    }
    private void fieldTyped(String value) { //suoritetaan kun ikkunan yläreunassa olevaan tekstiruutuun kirjoitetaan
        if (value.isEmpty()) { //oletus tiedosto on text.txt
            value = "text";
        }
        String[] splitted = value.split("%");
        String filename = splitted[0];

        String password = splitted.length > 1 ? splitted[1] : null;
        setCurrentFile(filename, password);
    }
    private void areaTyped(String value) { //suoritetaan kun isoon tekstialueeseen kirjoitetaan
        if (!storage.exists()) { // luodaan uusi tiedosto, jos jotain kirjoitetaan
            if (value.trim().isEmpty()) return;
            field.getEntries().add(storage.fileNameWithoutExtension()); //lisätään autocomplete arvo
        }
        storage.write(value); //tallennetaan tiedostoon kirjoitettu asia
    }
    //slideria siirrettäessä fontin koko muuttuu
    private void sliderMoved(double value) {
        area.setFont(Font.font("Arial", value));
    }


    public int getFontSize() {
        return (int) area.getFont().getSize();
    }

    public void onClose() {
        this.storage.onClose();
    }
}
