package com.virtuslab;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;

public class GitMacheteUI extends JFXPanel {
    @FXML
    TreeView<Text> treeView;

    @FXML
    Button refreshButton;

    private AnchorPane anchorPane;

    GitMacheteUI() {
        init();
    }

    private void init() {
        URL resource = getClass().getResource("/com/virtuslab/GitMacheteUI.fxml");
        FXMLLoader loader = new FXMLLoader(resource);
        loader.setController(this);
        try {
            anchorPane = loader.load();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Platform.runLater(this::createScene);
        Platform.runLater(this::populateTree);
    }

    private void createScene() {
        Scene scene = new Scene(anchorPane);
        setScene(scene);
    }

    @FXML
    private void listCommits(ActionEvent event) {
        if (event.getSource() instanceof CheckBox) {
            CheckBox checkBox = (CheckBox) event.getSource();
            ((StatusTreeItem) treeView.getRoot()).setListCommits(checkBox.isSelected());
        }
    }

    private void populateTree() {
        StatusTreeItem root = new StatusTreeItem("root");
        StatusTreeItem develop = new StatusTreeItem("develop");
        StatusTreeItem allow = new StatusTreeItem("Allow ownership links", "1st round of fixes", "allow-ownership-link");
        StatusTreeItem build = new StatusTreeItem("Build arbitrarily long chains", "build-chain");
        StatusTreeItem call = new StatusTreeItem("Call web service", "1st round of fixes", "2nd round of fixes", "call-ws");
        StatusTreeItem master = new StatusTreeItem("master");
        StatusTreeItem hotfix = new StatusTreeItem("HOTFIX Add the trigger (amended)", "hotfix/add-trigger");

        develop.getChildren().addAll(allow, call);
        allow.getChildren().addAll(build);
        master.getChildren().addAll(hotfix);
        root.getChildren().addAll(develop, master);

        treeView.setRoot(root);
    }
}
