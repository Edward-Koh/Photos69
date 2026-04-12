package photos.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import photos.model.User;
import photos.model.UserManager;

/**
 * Controller for the login screen.
 * Entry point of the application (choose admin or user)
 */
public class LoginController {

    @FXML private TextField usernameField;

    private Stage stage;

    /**
     * Called by Photos.java after loading login.fxml to give this
     * controller a reference to the primary stage for scene switching.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Handles the login button action.
     * Looks up the username in UserManager:
     * - "admin" routes to the admin subsystem
     * - Any valid user routes to their album list
     * - Invalid username = error Alert
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();

        if(username.isEmpty()) {
            showError("Please enter a username.");
            return;
        }

        // Special case: admin routes to admin subsystem
        if(username.equalsIgnoreCase("admin")) {
            switchToAdmin();
            return;
        }

        // Look up regular user
        UserManager um = UserManager.getInstance();
        User user = um.getUserByUsername(username);

        if(user == null) {
            showError("Username not found. Please try again.");
            return;
        }

        um.setCurrentUser(user);
        switchToAlbumList();
    }

    // swtiches to admin mode
    private void switchToAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/photos/view/admin.fxml"));
            Stage s = stage;
            s.setScene(new Scene(loader.load()));
            AdminController controller = loader.getController();
            controller.setStage(s);
            s.setTitle("Admin");
            s.show();
        }catch(Exception e) {
            showError("Failed to load admin screen.");
        }
    }

    // switches the scene to album list for the user
    private void switchToAlbumList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/photos/view/albumList.fxml"));
            Stage s = stage;
            s.setScene(new Scene(loader.load()));
            AlbumListController controller = loader.getController();
            controller.setStage(s);
            controller.initData();
            s.setTitle("Albums - " + UserManager.getInstance().getCurrentUser().getUsername());
            s.show();
        }catch(Exception e) {
            showError("Failed to load album list.");
        }
    }

    //Shows an error alert with the given message
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}