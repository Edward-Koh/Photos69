package photos.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import photos.model.User;
import photos.model.UserManager;

/**
 * Controller for the admin subsystem.
 * Allows the admin to list, create, and delete users.
 * Stock user is hidden from this list as it is a system user.
 */
public class AdminController {

    @FXML private ListView<User> userListView;

    private Stage stage;

    /**
     * Called after loading admin.fxml to give this controller
     * a reference to the primary stage.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
        refreshUserList();
    }

    /**
     * Refreshes the ListView to show the current user list
     * Called after any create or delete operation to update
     */
    private void refreshUserList() {
        userListView.setItems(
            FXCollections.observableArrayList(UserManager.getInstance().getUsers())
        );
    }

    /**
     * Handles creating a new user.
     * asks for a username via TextInputDialog.
     * doesn't accept empty, duplicate, or reserved usernames with an Alert.
     */
    @FXML
    private void handleCreateUser() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create User");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter new username:");

        dialog.showAndWait().ifPresent(username -> {
            username = username.trim();
            if(username.isEmpty()) {
                showError("Username cannot be empty.");
                return;
            }

            boolean added = UserManager.getInstance().addUser(username);

            if(!added) {
                showError("Username already exists or is reserved.");
                return;
            }

            refreshUserList();
            saveData();
        });
    }

    /**
     * Handles deleting the selected user.
     * Requires a user to be selected and confirms with Alert before deleting.
     * Reserved usernames (admin, stock) are blocked in UserManager.
     */
    @FXML
    private void handleDeleteUser() {
        User selected = userListView.getSelectionModel().getSelectedItem();

        if(selected == null) {
            showError("Please select a user to delete.");
            return;
        }

        String username = selected.getUsername();
        if(username.equals("admin") || username.equals("stock")) {
            showError("Cannot delete the \"" + username + "\" system user.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete user \"" + selected.getUsername() + "\"? This cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if(response == ButtonType.OK) {
                UserManager.getInstance().removeUser(selected.getUsername());
                refreshUserList();
                saveData();
            }
        });
    }

    /**
     * Handles logging out of the admin subsystem.
     * Saves data and returns to the login screen.
     */
    @FXML
    private void handleLogout() {
        saveData();
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
            showError("Failed to return to login screen.");
        }
    }

    /**
     * Saves all user data to disk.
     * Called after any create/delete operation and on logout
     */
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
        alert.setTitle("Admin Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}