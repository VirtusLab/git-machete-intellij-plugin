package com.virtuslab;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StatusTreeItem extends TreeItem<Text> {

    private final List<String> branchCommits;

    public StatusTreeItem(String... branchCommits) {
        this.branchCommits = Arrays.asList(branchCommits);
        setValue(this.branchCommits);
    }

    private void setValue(List<String> items) {
        this.getChildren().clear();
        List<TreeItem<Text>> statusItems = items
                .stream()
                .map(Text::new)
                .map(TreeItem::new)
                .collect(Collectors.toList());

        int lastIdx = statusItems.size() - 1;
        Text lastCommitText = statusItems.get(lastIdx).getValue();
        boldStyle(lastCommitText);

        Text branchTitleText = new Text(statusItems.get(lastIdx).getValue().getText());
        lightStyle(branchTitleText);

        statusItems.get(lastIdx).setValue(lastCommitText);
        this.setValue(branchTitleText);

        this.getChildren().addAll(statusItems);
    }

    private void lightStyle(Text branchNameText) {
        branchNameText.setFill(Color.GRAY);
        branchNameText.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.THIN, Font.getDefault().getSize()));
    }

    private void boldStyle(Text lastCommitText) {
        lastCommitText.setFill(Color.BLACK);
        lastCommitText.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, Font.getDefault().getSize()));
    }

    public void setListCommits(boolean value) {
        for (TreeItem<Text> item : this.getChildren()) {
            if (!item.isLeaf()) {
                ((StatusTreeItem) item).setListCommits(value);
            }
        }

        if (value) {
            lightStyle(this.getValue());
            ObservableList<TreeItem<Text>> children = this.getChildren().get(this.getChildren().size() - 1).getChildren();
            this.getChildren().clear();
            this.getChildren().addAll(children);
        }
        else {
            boldStyle(this.getValue());
            setValue(this.branchCommits);
        }

    }

//    public void showCommits() {
//        if (this.getParent() !=
//    }

    public StatusTreeItem getLastChild() {
        return (StatusTreeItem) this.getChildren().get(this.getChildren().size()  - 1);
    }

    public boolean addBranch(TreeItem<Text> treeItem) {
        return this.getChildren().get(this.getChildren().size() - 1).getChildren().add(treeItem);
    }
}
