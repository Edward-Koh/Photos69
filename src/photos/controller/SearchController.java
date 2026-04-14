package photos.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import photos.model.Album;
import photos.model.Photo;
import photos.model.User;
import photos.model.UserManager;

import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Controller for the search screen.
 * Supports two search modes:
 *   1. Date range — find photos taken between two dates
 *   2. Tag-based — single tag, AND combination, or OR combination of two tags
 *
 * Results can be saved as a new album.
 */
public class SearchController {

    // Date range fields
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private ComboBox<String> tagType1ComboBox;
    @FXML private TextField        tagValue1Field;
    @FXML private ComboBox<String> tagType2ComboBox;
    @FXML private TextField        tagValue2Field;

    @FXML private TilePane         resultsTilePane;
    @FXML private Label            resultsCountLabel;

    private Stage       stage;
    private User        currentUser;
    private List<Photo> searchResults = new ArrayList<>();

    private static final int THUMB_SIZE = 100;

    //Called after loading search.fxml to provide stage reference.
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Initializes the controller with the current user's tag types
     * for populating the ComboBoxes.
     */
    public void initData() {
        currentUser = UserManager.getInstance().getCurrentUser();
        List<String> tagTypes = new ArrayList<>(UserManager.getInstance().getTagTypes().keySet());
        tagType1ComboBox.setItems(FXCollections.observableArrayList(tagTypes));
        tagType2ComboBox.setItems(FXCollections.observableArrayList(tagTypes));
        if(!tagTypes.isEmpty()) {
            tagType1ComboBox.getSelectionModel().selectFirst();
            tagType2ComboBox.getSelectionModel().selectFirst();
        }
    }

    //-------------------------Search Handlers-------------------------
    /**
     * Handles searching photos by date range in user's data
     * Duplicate photos (same file in multiple albums) are deduplicated by file path.
     */
    @FXML
    private void handleDateSearch() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end   = endDatePicker.getValue();

        if(start == null || end == null) {
            showError("Please select both a start and end date.");
            return;
        }
        if(start.isAfter(end)) {
            showError("Start date must be before or equal to end date.");
            return;
        }

        Calendar startCal = toCalendar(start, false);
        Calendar endCal   = toCalendar(end, true); // end of day

        searchResults = UserManager.getInstance().searchByDate(currentUser, startCal, endCal);
        displayResults();
    }

    //------------------searching by tag-------------

    //Handles single tag search.
    @FXML
    private void handleSingleTagSearch() {
        String type  = tagType1ComboBox.getValue();
        String value = tagValue1Field.getText().trim();

        if(type == null || value.isEmpty()) {
            showError("Please select a tag type and enter a value.");
            return;
        }

        searchResults = UserManager.getInstance().searchBySingleTag(currentUser, type, value);
        displayResults();
    }

    // Handles AND search: photos must have BOTH tag1 AND tag2.
    @FXML
    private void handleAndSearch() {
        String type1  = tagType1ComboBox.getValue();
        String value1 = tagValue1Field.getText().trim();
        String type2  = tagType2ComboBox.getValue();
        String value2 = tagValue2Field.getText().trim();

        if (type1 == null || value1.isEmpty() || type2 == null || value2.isEmpty()) {
            showError("Please fill in both tag fields.");
            return;
        }

        searchResults = UserManager.getInstance()
                .searchByTwoTagsAnd(currentUser, type1, value1, type2, value2);
        displayResults();
    }

    /**
     * Searches by two tag pairs using OR logic.
     * Delegates to UserManager.searchByTwoTagsOr().
     */
    @FXML
    private void handleOrSearch() {
        String type1  = tagType1ComboBox.getValue();
        String value1 = tagValue1Field.getText().trim();
        String type2  = tagType2ComboBox.getValue();
        String value2 = tagValue2Field.getText().trim();

        if(type1 == null || value1.isEmpty() || type2 == null || value2.isEmpty()) {
            showError("Please fill in both tag fields.");
            return;
        }

        searchResults = UserManager.getInstance()
                .searchByTwoTagsOr(currentUser, type1, value1, type2, value2);
        displayResults();
    }

    //---------------Results Display--------------------

    //Renders search results as thumbnails in the TilePane.
    private void displayResults() {
        resultsTilePane.getChildren().clear();
        resultsCountLabel.setText(searchResults.size() + " result(s) found.");

        for(Photo photo : searchResults) {
            try {
                ImageView thumb = new ImageView(
                    new Image(new FileInputStream(photo.getFilePath()),
                              THUMB_SIZE, THUMB_SIZE, true, true)
                );
                Label caption = new Label(photo.getCaption().isEmpty()
                    ? "(no caption)" : photo.getCaption());
                caption.setMaxWidth(THUMB_SIZE);
                caption.setWrapText(true);

                VBox cell = new VBox(5, thumb, caption);
                cell.setStyle("-fx-padding: 5;");
                resultsTilePane.getChildren().add(cell);
            }catch(Exception e) {
                resultsTilePane.getChildren().add(new Label("[Missing]\n" + photo.getCaption()));
            }
        }
    }

    //-------------------Save Results as Album-------------------------

    /**
     * Creates a new album from the current search results.
     * Uses User.addAlbum(String) then retrieves the Album object
     * to add photos to it by reference.
     */
    @FXML
    private void handleSaveAsAlbum() {
        if(searchResults.isEmpty()) {
            showError("No results to save.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Save as Album");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter album name:");

        dialog.showAndWait().ifPresent(name -> {
            name = name.trim();
            if(name.isEmpty()) {
                showError("Album name cannot be empty.");
                return;
            }

            boolean created = currentUser.addAlbum(name);
            if(!created) {
                showError("An album with that name already exists.");
                return;
            }

            // Retrieve the newly created Album object to add photos to it
            Album newAlbum = currentUser.getAlbum(name);
            for(Photo photo : searchResults) {
                newAlbum.addPhoto(photo);
            }

            saveData();
            showInfo("Album \"" + name + "\" created with " + searchResults.size() + " photo(s).");
        });
    }

    // ------------------------Navigation-------------------------

    //Returns to the album list screen.
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/photos/view/albumList.fxml"));
            stage.setScene(new Scene(loader.load()));
            AlbumListController controller = loader.getController();
            controller.setStage(stage);
            controller.initData();
            stage.setTitle("Albums - " + currentUser.getUsername());
            stage.show();
        }catch(Exception e) {
            showError("Failed to return to album list.");
        }
    }

    // --------------------------Helpers-------------------------


    // Converts a LocalDate from DatePicker to a Calendar.

    private Calendar toCalendar(LocalDate date, boolean endOfDay) {
        Calendar cal = Calendar.getInstance();
        cal.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth(),
                endOfDay ? 23 : 0,
                endOfDay ? 59 : 0,
                endOfDay ? 59 : 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private void saveData() {
        try {
            UserManager.getInstance().save();
        } catch (Exception e) {
            showError("Failed to save data: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Done");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}