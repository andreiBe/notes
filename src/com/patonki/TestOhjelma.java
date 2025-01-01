package com.patonki;

import javafx.application.Application;
import javafx.stage.Stage;

public class TestOhjelma extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.show();
        System.out.println("END: "+ System.currentTimeMillis());
    }
    public static void aloita() {
        launch(TestOhjelma.class);
    }
}
