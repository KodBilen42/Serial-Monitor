import com.fazecast.jSerialComm.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.Collections;

public class file extends Application{
    static int ignored_data = 50;
    static int listening_port;
    static String data = "";
    static String[] expected_data_labels = new String[]{"temperature1", "temperature2", "temperature3", "data1", "data2", "data3", "data4"};
    static ArrayList<Button> buttons = new ArrayList<Button>();
    static ArrayList<Label> labels = new ArrayList<Label>();
    static ArrayList<SerialPort> ports = new ArrayList<SerialPort>();

    public static void main(String[] args) {
        ports.clear();
        Collections.addAll(ports, SerialPort.getCommPorts());
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        primaryStage.setScene(_create_scene());
        primaryStage.setTitle("Serial Data Monitor");
        primaryStage.show();
    }

    private Scene _create_scene(){
        Pane root = new Pane();
        Label label1 = new Label("Select Listening Port");
        label1.setLayoutX(50);
        label1.setLayoutY(50);
        label1.setFont(new Font(16));

        Label label2 = new Label("Data Readings");
        label2.setLayoutX(350);
        label2.setLayoutY(50);
        label2.setFont(new Font(16));

        root.getChildren().addAll(label1, label2);

        for(int i = 0; i < expected_data_labels.length; i++){
            Label label = new Label(expected_data_labels[i]);
            label.setLayoutX(350);
            label.setLayoutY(80 + 30*i);
            label.setFont(new Font(14));
            labels.add(label);
            root.getChildren().add(label);
        }

        for (int i = 0; i < SerialPort.getCommPorts().length; i++){
            Button button = new Button(SerialPort.getCommPorts()[i].getDescriptivePortName());
            button.setLayoutX(50);
            button.setLayoutY(80 + 30*i);
            button.setFont(new Font(10.5));
            buttons.add(button);
            button.setOnAction(e -> {
                listening_port = buttons.indexOf(button);
                _switch_listening_port(listening_port);
            });
            System.out.println("Listening Port: " + listening_port);
            root.getChildren().addAll(button);
        }


        return new Scene(root, 600, 400);
    }

    private void _switch_listening_port(int listening_port_index){
        for(SerialPort port : ports) {
            port.removeDataListener();
            port.closePort();
        }
        ignored_data = 50;
        SerialPort comPort = ports.get(listening_port);
        comPort.openPort();
        comPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (file.ignored_data > 0){
                    file.ignored_data--;
                    return;
                }
                byte[] newData = event.getReceivedData();
                for (int i = 0; i < newData.length; i++) {
                    if ((char) newData[i] == ';') {
                        if(data.charAt(0) == ';') {
                            data = data.substring(1);
                            String[] parts = data.split(":");
                            String data_label = parts[0];
                            String value = parts[1];
                            for (Label label : labels){
                                if (label.getText().split(" ")[0].equals(data_label)){
                                    Platform.runLater(() ->{
                                        label.setText(label.getText().split(" ")[0] + "       " + value);
                                    });
                                }
                            }
                        }
                        data = ";";
                    } else {
                        data += (char) newData[i];
                    }
                }
            }
        });
    }
}
