package photos.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import photos.model.Album;
import photos.model.User;
import photos.model.UserManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Controller for the album list screen.
 * The main area for the user subsystem.
 * Displays all albums for the current user with name, photo count, and date range.
 * Handles album function and navigation to album view and search.
 */
public class AlbumListController {

    @FXML private TableView<Album>           albumTable;
    @FXML private TableColumn<Album, String> nameColumn;
    @FXML private TableColumn<Album, String> countColumn;
    @FXML private TableColumn<Album, String> dateRangeColumn;

    private Stage stage;
    private User  currentUser;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");

    //Called after loading albumList.fxml to provide stage reference.
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Initializes the controller with the current user's data.
     * Called by LoginController right after setStage().
     * Sets up table columns and loads album data.
     */
    public void initData() {
        currentUser = UserManager.getInstance().getCurrentUser();

        // Name column binds to album name
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Count column — computed from album, needs custom cell factory
        countColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                String.valueOf(cellData.getValue().getPhotoCount())
            )
        );

        // Date range column — uses getEarliestDate/getLatestDate from Album
        dateRangeColumn.setCellValueFactory(cellData -> {
            Album album = cellData.getValue();
            Calendar earliest = album.getEarliestDate();
            Calendar latest   = album.getLatestDate();

            String range;
            if(earliest == null) {
                range = "No photos";
            }else if(earliest.equals(latest)) {
                range = DATE_FORMAT.format(earliest.getTime());
            }else {
                range = DATE_FORMAT.format(earliest.getTime())
                      + " – "
                      + DATE_FORMAT.format(latest.getTime());
            }
            return new javafx.beans.property.SimpleStringProperty(range);
        });

        albumTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && albumTable.getSelectionModel().getSelectedItem() != null) {
                handleOpenAlbum();
            }
        });

        refreshAlbumTable();
    }

    //Reloads the table from the current user's album list.
    private void refreshAlbumTable() {
        albumTable.setItems(FXCollections.observableArrayList(currentUser.getAlbums()));
        albumTable.refresh();
    }

    //---------------------ALBUM FUNCTIONS---------------------
    /**
     * Handles creating a new album.
     * Prompts for a name and rejects duplicates.
     */
    @FXML
    private void handleCreateAlbum() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Album");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter album name:");

        dialog.showAndWait().ifPresent(name -> {
            name = name.trim();
            if(name.isEmpty()) {
                showError("Album name cannot be empty.");
                return;
            }

            boolean added = currentUser.addAlbum(name);

            if(!added) {
                showError("An album with that name already exists.");
                return;
            }

            refreshAlbumTable();
            saveData();
        });
    }

    /**
     * Handles deleting the selected album.
     * Confirms with the user before deleting.
     */
    @FXML
    private void handleDeleteAlbum() {
        Album selected = albumTable.getSelectionModel().getSelectedItem();

        if(selected == null) {
            showError("Please select an album to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Album");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete album \"" + selected.getName() + "\"?");

        confirm.showAndWait().ifPresent(response -> {
            if(response == ButtonType.OK) {
                currentUser.removeAlbum(selected.getName());
                refreshAlbumTable();
                saveData();
            }
        });
    }

    /**
     * Handles renaming the selected album.
     * Rejects duplicate names via User.renameAlbum().
     */
    @FXML
    private void handleRenameAlbum() {
        Album selected = albumTable.getSelectionModel().getSelectedItem();

        if(selected == null) {
            showError("Please select an album to rename.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Rename Album");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter new name:");

        dialog.showAndWait().ifPresent(newName -> {
            newName = newName.trim();
            if(newName.isEmpty()) {
                showError("Album name cannot be empty.");
                return;
            }

            boolean renamed = currentUser.renameAlbum(selected.getName(), newName);
            if(!renamed) {
                showError("An album with that name already exists.");
                return;
            }

            refreshAlbumTable();
            saveData();
        });
    }

    /**
     * Handles opening the selected album.
     * Switches scene to albumView.fxml and passes the selected album.
     */
    @FXML
    private void handleOpenAlbum() {
        Album selected = albumTable.getSelectionModel().getSelectedItem();

        if(selected == null) {
            showError("Please select an album to open.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/photos/view/albumView.fxml"));
            stage.setScene(new Scene(loader.load()));
            AlbumViewController controller = loader.getController();
            controller.setStage(stage);
            controller.initData(selected);
            stage.setTitle("Album: " + selected.getName());
            stage.show();
        }catch(Exception e) {
            showError("Failed to open album.");
        }
    }

    //Handles navigating to the search screen
    @FXML
    private void handleSearch() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/photos/view/search.fxml"));
            stage.setScene(new Scene(loader.load()));
            SearchController controller = loader.getController();
            controller.setStage(stage);
            controller.initData();
            stage.setTitle("Search Photos");
            stage.show();
        }catch(Exception e) {
            showError("Failed to load search screen.");
        }
    }

    // Handles logout. Saves data and returns to login screen.
    @FXML
    private void handleLogout() {
        saveData();
        UserManager.getInstance().setCurrentUser(null);
        switchToLogin();
    }

    //Switches back to the login screen.
    private void switchToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/photos/view/login.fxml"));
            stage.setScene(new Scene(loader.load()));
            LoginController controller = loader.getController();
            controller.setStage(stage);
            stage.setTitle("Photos Login");
            stage.show();
        }catch(Exception e) {
            showError("Failed to return to login.");
        }
    }

    //Saves all user data to disk.
    private void saveData() {
        try {
            UserManager.getInstance().save();
        }catch(Exception e) {
            showError("Failed to save data: " + e.getMessage());
        }
    }

    //Shows an error alert with the given message.
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}