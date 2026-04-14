package photos.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import photos.model.Album;
import photos.model.Photo;
import photos.model.User;
import photos.model.UserManager;

import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.List;

/**
 * Controller for the album view screen
 * Displays all photos in the open album as thumbnails with captions
 * Handles add, remove, caption, copy, move, and slideshow functionality
*/
public class AlbumViewController {

    @FXML private TilePane   photoTilePane;
    @FXML private Label      captionLabel;
    @FXML private ImageView  slideshowImageView;
    @FXML private Label      slideshowCaption;

    private Stage  stage;
    private Album  currentAlbum;
    private User   currentUser;
    private int    slideshowIndex = 0;

    private static final int THUMB_SIZE = 120;

    //Called after loading albumView.fxml to provide stage reference
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    
    // initializes this controller with the album to display
    public void initData(Album album) {
        this.currentAlbum = album;
        this.currentUser  = UserManager.getInstance().getCurrentUser();
        refreshPhotoGrid();

        if(!currentAlbum.getPhotos().isEmpty()) {
            slideshowIndex = 0;
            showSlideshowPhoto();
        }
    }


    // Refreshes the thumbnail grid with the current album
    private void refreshPhotoGrid() {
        photoTilePane.getChildren().clear();
        selectedPhoto = null;
        selectedCell  = null;

        for(Photo photo : currentAlbum.getPhotos()) {
            try {
                ImageView thumb = new ImageView(
                    new Image(new FileInputStream(photo.getFilePath()),
                              THUMB_SIZE, THUMB_SIZE, true, true)
                );

                Label caption = new Label(photo.getCaption().isEmpty() ? "(no caption)" : photo.getCaption());
                caption.setMaxWidth(THUMB_SIZE);
                caption.setWrapText(true);

                VBox cell = new VBox(5, thumb, caption);
                cell.setStyle("-fx-cursor: hand; -fx-padding: 5;");

                //click function to see photo
                cell.setOnMouseClicked(e -> {
                    setSelectedPhoto(photo, cell);
                    if (e.getClickCount() == 2) {
                        openPhotoView(photo);
                    }
                });

                photoTilePane.getChildren().add(cell);
            } catch (Exception e) {

                //if image can't be loaded (file moved/deleted), show placeholder label
                Label broken = new Label("[Missing]\n" + photo.getCaption());
                photoTilePane.getChildren().add(broken);
            }
        }

        if(!currentAlbum.getPhotos().isEmpty()) {
            slideshowIndex = Math.min(slideshowIndex, currentAlbum.getPhotos().size() - 1);
            showSlideshowPhoto();
        }else {
            slideshowImageView.setImage(null);
            slideshowCaption.setText("");
        }
    }

    /**
     * Handles adding a photo to the current album
     * Opens a FileChooser to select an image file
     * Sets dateTaken from the file's last-modified timestamp
     * Rejects duplicate photos by checking same file path
     */
    @FXML
    private void handleAddPhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Photo");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files",
                "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp")
        );

        File file = chooser.showOpenDialog(stage);
        if(file == null) return;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(file.lastModified());
        cal.set(Calendar.MILLISECOND, 0);

        Photo photo = new Photo(file.getAbsolutePath(), cal);
        boolean added = currentAlbum.addPhoto(photo);

        if(!added) {
            showError("That photo is already in this album.");
            return;
        }

        refreshPhotoGrid();
        saveData();
    }

    // Only handles removing the selected photo from the current album
    @FXML
    private void handleRemovePhoto() {
        Photo selected = getSelectedPhoto();
        if(selected == null) {
            showError("Please select a photo to remove.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Photo");
        confirm.setHeaderText(null);
        confirm.setContentText("Remove this photo from the album?");

        confirm.showAndWait().ifPresent(response -> {
            if(response == ButtonType.OK) {
                currentAlbum.removePhoto(selected);
                refreshPhotoGrid();
                saveData();
            }
        });
    }

    // handles captioning/recaptioning of selected photo which updates other references of it
    @FXML
    private void handleCaptionPhoto() {
        Photo selected = getSelectedPhoto();
        if(selected == null) {
            showError("Please select a photo to caption.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getCaption());
        dialog.setTitle("Caption Photo");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter caption:");

        dialog.showAndWait().ifPresent(caption -> {
            selected.setCaption(caption.trim());
            refreshPhotoGrid();
            saveData();
        });
    }

    /**
     * Handles copying the selected photo to another album.
     * Additionally, presents a ChoiceDialog listing other albums the user owns.
     * The same Photo object reference is added to the target album,
     */
    @FXML
    private void handleCopyPhoto() {
        Photo selected = getSelectedPhoto();
        if(selected == null) {
            showError("Please select a photo to copy.");
            return;
        }

        List<Album> otherAlbums = getOtherAlbums();
        if(otherAlbums.isEmpty()) {
            showError("No other albums to copy to.");
            return;
        }

        ChoiceDialog<Album> dialog = new ChoiceDialog<>(otherAlbums.get(0), otherAlbums);
        dialog.setTitle("Copy Photo");
        dialog.setHeaderText(null);
        dialog.setContentText("Copy to album:");

        dialog.showAndWait().ifPresent(target -> {
            boolean added = target.addPhoto(selected);
            if(!added) {
                showError("That photo is already in the target album.");
                return;
            }
            saveData();
            showInfo("Photo copied to \"" + target.getName() + "\".");
        });
    }

    /**
     * Handles moving the selected photo to another album.
     * NOTE: Move = copy to target + remove from source.
     */
    @FXML
    private void handleMovePhoto() {
        Photo selected = getSelectedPhoto();
        if(selected == null) {
            showError("Please select a photo to move.");
            return;
        }

        List<Album> otherAlbums = getOtherAlbums();
        if(otherAlbums.isEmpty()) {
            showError("No other albums to move to.");
            return;
        }

        ChoiceDialog<Album> dialog = new ChoiceDialog<>(otherAlbums.get(0), otherAlbums);
        dialog.setTitle("Move Photo");
        dialog.setHeaderText(null);
        dialog.setContentText("Move to album:");

        dialog.showAndWait().ifPresent(target -> {
            boolean added = target.addPhoto(selected);
            if(!added) {
                showError("That photo is already in the target album.");
                return;
            }
            currentAlbum.removePhoto(selected);
            refreshPhotoGrid();
            saveData();
            showInfo("Photo moved to \"" + target.getName() + "\".");
        });
    }

    private List<Album> getOtherAlbums() {
        return currentUser.getAlbums().stream()
            .filter(a -> !a.equals(currentAlbum))
            .collect(java.util.stream.Collectors.toList());
    }

                // Slideshow

    // Advances to the next photo in the slideshow which can wrap around to the beginning
    @FXML
    private void handleSlideshowNext() {
        if(currentAlbum.getPhotos().isEmpty()) return;
        slideshowIndex = (slideshowIndex + 1) % currentAlbum.getPhotos().size();
        showSlideshowPhoto();
    }


    // Goes back to the previous photo in the slideshow which can wrap to the end
    @FXML
    private void handleSlideshowPrev() {
        if(currentAlbum.getPhotos().isEmpty()) return;
        slideshowIndex = (slideshowIndex - 1 + currentAlbum.getPhotos().size())
                          % currentAlbum.getPhotos().size();
        showSlideshowPhoto();
    }

    // loads current photo in the slideshow
    private void showSlideshowPhoto() {
        Photo photo = currentAlbum.getPhotos().get(slideshowIndex);
        try {
            slideshowImageView.setImage(
                new Image(new FileInputStream(photo.getFilePath()))
            );
            slideshowCaption.setText(photo.getCaption());
        }catch(Exception e) {
            slideshowImageView.setImage(null);
            slideshowCaption.setText("[Image not found]");
        }
    }


    /** Opens the photo view for the given photo 
     *  Called in thumbnail thing
     */

    private void openPhotoView(Photo photo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/photos/view/photoView.fxml"));
            stage.setScene(new Scene(loader.load()));
            PhotoViewController controller = loader.getController();
            controller.setStage(stage);
            controller.initData(photo, currentAlbum);
            stage.setTitle("Photo View");
            stage.show();
        }catch(Exception e) {
            showError("Failed to open photo view.");
        }
    }

    // returns album to list screen
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
        } catch (Exception e) {
            showError("Failed to return to album list.");
        }
    }


    /**
     * Returns the currently selected photo from the tile pane.
     * Since TilePane doesn't have built-in selection, we track
     * selection via a stored reference updated on thumbnail click.
     * For now returns null — see note below.
     *
     * NOTE: TilePane has no built-in selection model. You have two options:
     * (1) Track a selectedPhoto field updated on cell click (recommended)
     * (2) Switch to a ListView or GridView with selection support
     * This is left for the FXML/wiring phase to finalize.
     *
     * @return the selected Photo, or null if none selected
     */
    private Photo getSelectedPhoto() {
        // To be wired up during FXML phase with a selectedPhoto field
        return selectedPhoto;
    }
 
    // Tracks the currently selected photo from thumbnail clicks
    private Photo selectedPhoto = null;
 
    // Tracks the currently highlighted cell so we can clear its style on re-selection
    private VBox selectedCell = null;

    // Sets the selected photo and highlights and unhighlights photo

    private void setSelectedPhoto(Photo photo, VBox cell) {
        // Clear previous highlighted photo
        if(selectedCell != null) {
            selectedCell.setStyle("-fx-cursor: hand; -fx-padding: 5;");
        }
        this.selectedPhoto = photo;
        this.selectedCell  = cell;
        // Apply highlight to newly selected photo
        cell.setStyle("-fx-cursor: hand; -fx-padding: 5; -fx-border-color: #0096c7; -fx-border-width: 2; -fx-background-color: #e0f7ff;");
    }

    // saves all user to the disk
    private void saveData() {
        try {
            UserManager.getInstance().save();
        }catch(Exception e) {
            showError("Failed to save data: " + e.getMessage());
        }
    }

    // shows error alert
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // shows informational alert
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}