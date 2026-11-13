package legend.core;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class UpdaterController {
  @FXML
  private Label updateStatus;

  public void setStatus(final String status) {
    this.updateStatus.setText(status);
  }
}
