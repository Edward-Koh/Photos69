package photos.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import photos.model.Album;
import photos.model.Photo;
import photos.model.Tag;
import photos.model.UserManager;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * Controller for the photo view screen.
 * Displays a single photo at full size along with its features.
 * Handles adding and deleting tags.
 */
public class PhotoViewController {

    @FXML private ImageView        photoImageView;
    @FXML private Label            captionLabel;
    @FXML private Label            dateLabel;
    @FXML private ListView<Tag>    tagListView;
    @FXML private ComboBox<String> tagTypeComboBox;
    @FXML private TextField        tagValueField;

    private Stage stage;
    private Photo photo;
    private Album sourceAlbum;

    private static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("MM/dd/yyyy hh:mm a");

    //Called after loading photoView.fxml to provide stage reference.
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Initializes this controller with the photo to display and its source album.
     * Source album is needed to navigate back correctly.
     */
    public void initData(Photo photo, Album sourceAlbum) {
        this.photo       = photo;
        this.sourceAlbum = sourceAlbum;

        loadPhoto();
        refreshTagList();
        populateTagTypeComboBox();
    }

    //Loads the photo image and sets caption and date labels.
    private void loadPhoto() {
        try {
            photoImageView.setImage(new Image(new FileInputStream(photo.getFilePath())));
        }catch(Exception e) {
            photoImageView.setImage(null);
        }

        captionLabel.setText(photo.getCaption().isEmpty() ? "(no caption)" : photo.getCaption());
        dateLabel.setText("Taken: " + DATE_FORMAT.format(photo.getDateTaken().getTime()));
    }

    // Populates the tag type ComboBox from UserManager's tag type map
    private void populateTagTypeComboBox() {
        Map<String, Boolean> tagTypes = UserManager.getInstance().getTagTypes();
        tagTypeComboBox.setItems(FXCollections.observableArrayList(tagTypes.keySet()));
        if(!tagTypes.isEmpty()) {
            tagTypeComboBox.getSelectionModel().selectFirst();
        }
    }

    // Refreshes the tag ListView from the photo's current tag list.
    private void refreshTagList() {
        tagListView.setItems(FXCollections.observableArrayList(photo.getTags()));
    }

    //----------------Tag Operations----------------------

    /**
     * Handles adding a tag to the current photo.
     * Enforces single-value constraint via UserManager.allowsMultiple().
     */
    @FXML
    private void handleAddTag() {
        String tagType  = tagTypeComboBox.getValue();
        String tagValue = tagValueField.getText().trim();

        if(tagType == null || tagType.isEmpty()) {
            showError("Please select a tag type.");
            return;
        }
        if(tagValue.isEmpty()) {
            showError("Please enter a tag value.");
            return;
        }

        // Enforce single-value constraint using UserManager map
        if(!UserManager.getInstance().allowsMultiple(tagType)) {
            if(!photo.getTagsByName(tagType).isEmpty()) {
                showError("Tag type \"" + tagType + "\" can only have one value per photo.");
                return;
            }
        }

        Tag tag = new Tag(tagType, tagValue);
        boolean added = photo.addTag(tag);

        if(!added) {
            showError("This tag already exists on the photo.");
            return;
        }

        tagValueField.clear();
        refreshTagList();
        saveData();
    }

    /**
     * Handles deleting the selected tag from the current photo.
     * Confirms before deleting.
     */
    @FXML
    private void handleDeleteTag() {
        Tag selected = tagListView.getSelectionModel().getSelectedItem();

        if(selected == null) {
            showError("Please select a tag to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Tag");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete tag \"" + selected + "\"?");

        confirm.showAndWait().ifPresent(response -> {
            if(response == ButtonType.OK) {
                photo.removeTag(selected);
                refreshTagList();
                saveData();
            }
        });
    }

    /**
     * Handles adding a new custom tag type.
     * Asks the user whether the new type allows single or multiple values.
     */
    @FXML
    private void handleAddCustomTagType() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Tag Type");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter new tag type name:");

        dialog.showAndWait().ifPresent(raw -> {
            final String typeName = raw.trim();
            if(typeName.isEmpty()) {
                showError("Tag type name cannot be empty.");
                return;
            }

            // Ask whether this tag type allows multiple values
            Alert choiceAlert = new Alert(Alert.AlertType.CONFIRMATION);
            choiceAlert.setTitle("Tag Value Policy");
            choiceAlert.setHeaderText("Can \"" + typeName + "\" have multiple values per photo?");
            choiceAlert.setContentText("Choose Single if only one value is allowed (like location),\nor Multiple if many values are allowed (like person).");

            ButtonType singleBtn   = new ButtonType("Single");
            ButtonType multipleBtn = new ButtonType("Multiple");
            ButtonType cancelBtn   = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            choiceAlert.getButtonTypes().setAll(singleBtn, multipleBtn, cancelBtn);

            choiceAlert.showAndWait().ifPresent(response -> {
                if(response == cancelBtn) return;

                boolean allowMultiple = (response == multipleBtn);
                boolean added = UserManager.getInstance().addTagType(typeName, allowMultiple);

                if(!added) {
                    showError("That tag type already exists.");
                    return;
                }

                populateTagTypeComboBox();
                saveData();
            });
        });
    }

    // Returns to the album view from the source album.
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/photos/view/albumView.fxml"));
            stage.setScene(new Scene(loader.load()));
            AlbumViewController controller = loader.getController();
            controller.setStage(stage);
            controller.initData(sourceAlbum);
            stage.setTitle("Album: " + sourceAlbum.getName());
            stage.show();
        }catch(Exception e) {
            showError("Failed to return to album view.");
        }
    }

    // Saves all user data to disk.
    private void saveData() {
        try {
            UserManager.getInstance().save();
        }catch(Exception e) {
            showError("Failed to save data: " + e.getMessage());
        }
    }

    // Shows an error alert with the given message.

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}