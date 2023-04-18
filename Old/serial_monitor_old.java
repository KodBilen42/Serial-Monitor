import com.fazecast.jSerialComm.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.Collections;

public class serial_monitor_old extends Application{
    static ArrayList<SerialPort> ports = new ArrayList<>();
    static ArrayList<canvas_line_chart> charts = new ArrayList<>();
    static ArrayList<Button> buttons = new ArrayList<>();
    static ArrayList<data_label> labels = new ArrayList<>();

    // index of the listening port
    static int listening_port;

    // current data block e.g.: ";name:value"
    static String data = " ";

    // expected data names from data source
    // any other data with a different name will be assumed corrupted during data transmission and will be ignored
    static String[] data_names = new String[] {"voltage1", "voltage2", "voltage3", "current1", "rpm1", "temperature1", "temperature2", "temperature3", "pwm1"};

    public static void main(String[] args){
        // store all ports at arraylist ports
        Collections.addAll(ports, SerialPort.getCommPorts());

        // run start() method
        launch(args);
    }

    @Override
    public void start(Stage primary_stage){
        primary_stage.setTitle("Alected Serial Data Monitor");

        // create scene structure
        primary_stage.setScene(setup_scene());

        // display scene
        primary_stage.show();
    }

    private Scene setup_scene(){
        Pane root = new Pane();

        // create buttons to select listening port
        for (int i = 0; i < SerialPort.getCommPorts().length; i++){

            // create new button with description of port name
            Button button = new Button(SerialPort.getCommPorts()[i].getDescriptivePortName());

            // locate buttons from top to button
            button.setLayoutX(50);
            button.setLayoutY(80 + 30*i);

            // set font size
            button.setFont(new Font(11));

            // add button to buttons list
            buttons.add(button);

            // define button action
            button.setOnAction(e -> {
                // switch listening port to new value
                listening_port = buttons.indexOf(button);
                switch_listening_port();
            });

            // add buttons to the scene
            root.getChildren().addAll(button);
        }

        // create general port input graph at the bottom of the screen
        Canvas canvas = new Canvas();
        canvas_line_chart general_data_chart = new canvas_line_chart(canvas.getGraphicsContext2D(), Color.rgb(2, 154, 16), 800, 100, 0, 300);
        general_data_chart.data_name = "general_data";
        general_data_chart.max_value = 128;

        // add general graph to the scene
        root.getChildren().add(canvas);

        // add chart to charts list
        charts.add(general_data_chart);

        // create data reading labels and graphs
        for (int i = 0; i < data_names.length; i++){

            // create data labels for every data   e.g.: temperature:    59
            data_label line = new data_label(root, data_names[i], 150, 40, 400,  80 + 30 * i);

            // create graphs for every data
            Canvas canvas_data_chart = new Canvas();
            canvas_line_chart data_chart = new canvas_line_chart(canvas_data_chart.getGraphicsContext2D(), Color.RED, 1000, 80,  900, 100 * i - 40);
            data_chart.data_name = data_names[i];
            data_chart.data_width = 1.2;
            data_chart.max_value = 1099;
            data_chart.horizontal_line_distance = 100;
            data_chart.delta_y = 500;

            // add graphs to the scene and to the list
            root.getChildren().add(canvas_data_chart);
            charts.add(data_chart);
        }

        // return constructed scene
        return new Scene(root, 800, 500);
    }

    private static void switch_listening_port(){

        // close all ports
        for(SerialPort port : ports) {
            port.removeDataListener();
            port.closePort();
        }

        // reset ignored data
        file.ignored_data = 50;

        // open listening port
        SerialPort comPort = ports.get(listening_port);
        comPort.openPort();
        comPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
            }

            // this function runs every time data arrives from port
            @Override
            public void serialEvent(SerialPortEvent event) {

                // ignore the first 50 characters of data to sync with data source
                if (file.ignored_data > 0){
                    file.ignored_data--;
                    return;
                }

                // get all data available as byte array
                byte[] newData = event.getReceivedData();
                for (int i = 0; i < newData.length; i++) {
                    if ((char) newData[i] == ';') {
                        if(data.charAt(0) == ';') {
                            // if new data is ';' and data block is not empty, update the value
                            try {
                                // ignore the initial ';' at the begining of the data block
                                data = data.substring(1);

                                // split the name and the value parts of the block
                                String[] parts = data.split(":");
                                String data_label = parts[0];
                                String value = parts[1];

                                // for every character update general chart
                                for (char character : data.toCharArray()) {
                                    Platform.runLater(() -> {
                                        canvas_line_chart general_chart = canvas_line_chart.find_chart_by_data_name("general_data");
                                        general_chart.update(character);
                                    });
                                }

                                // find corresponding data graph and update
                                Platform.runLater(() -> {
                                    canvas_line_chart data_chart = canvas_line_chart.find_chart_by_data_name(data_label);
                                    try {
                                        data_chart.update(Integer.parseInt(value));
                                    } catch (NumberFormatException ignored) {
                                    }
                                });

                                // find corresponding label and update
                                for (data_label datalabel : labels) {
                                    if (datalabel.label_name.getText().equals(data_label)) {
                                        Platform.runLater(() -> {
                                            datalabel.update(value);
                                        });
                                    }
                                }
                            }catch (Exception ignored) {}
                        }
                        // clear the data
                        data = ";";
                    } else {
                        // if new data is not ';' then add new char to data string
                        data += (char) newData[i];
                    }
                }
            }
        });
    }

    private static class data_label{
        Pane root;
        String name;
        String value;
        Label label_name;
        Label label_value;
        int x;
        int y;
        int height;
        int width;
        public data_label(Pane root, String name, int width, int height, int x, int y){
            labels.add(this);
            this.root = root;
            this.name = name;
            this.x = x;
            this.y = y;
            this.height = height;
            this.width = width;

            // create and locate name and value labels
            label_name = new Label(this.name);
            label_name.setLayoutX(x);
            label_name.setLayoutY(y);
            root.getChildren().add(label_name);

            label_value = new Label(this.value);
            label_value.setLayoutX(x + width * 2 / 3.0);
            label_value.setLayoutY(y);
            root.getChildren().add(label_value);
        }

        public void update(String new_value){
            // update function is used to update the value of the data_label

            this.value = new_value;
            //render
            label_name.setText(name);
            label_value.setText(value);
        }
    }

    private static class canvas_line_chart{
        GraphicsContext g;                                          // graphics context of the canvas where graph is located at
        Color color;                                                // color of the curve
        ArrayList<Character> buffer = new ArrayList<Character>();   // data buffer of the graph
        int height, width, x = 0, y = 0;
        double data_width = 1.2;                                    // number of pixels in x-axis for every value
        int buffer_size;                                            // maximum number of values plotted at graph
        String data_name = "";                                      // name of the graph
        int max_value = 128;
        int horizontal_line_distance = 50;                          // distance between two rows of the grid (in terms of pixel)
        int vertical_line_distance = 50;                            // distance between two columns of the grid (in terms of pixel)
        int delta_x = 50;                                           // distance between two rows of the grid (in terms of value)
        int delta_y = 50;                                           // distance between two columns of the grid (in terms of value)
        int border_width = 20;                                      // width of border for axis labels and name at corners (in terms of pixel)

        public canvas_line_chart(GraphicsContext g, Color color, int width, int height, int x, int y){
            this.g = g;
            this.color = color;
            this.width = width;
            this.height = height;
            this.x = x;
            this.y = y;
            this.buffer_size = (int)(this.width / data_width);

            // locate and resize the canvas
            g.getCanvas().setLayoutX(x);
            g.getCanvas().setLayoutY(y + height);
            g.getCanvas().setHeight(height);
            g.getCanvas().setWidth(width);

            // draw background and border
            g.setStroke(Color.BLACK);
            g.setFill(Color.rgb(20, 13, 59));
            g.fillRect(0, 0, width, height);
            g.strokeRect(0, 0, width, height);

            // draw grid, graph name and axis values
            draw_graph_elements();
        }

        public void update(char data){
            // this function is used to add a new value to graph

            // recalculate buffer_size (data_width may be changed after initialization, so we have to recalculate buffer_size)
            this.buffer_size = (int)(this.width / data_width);

            // draw blank graph
            g.setStroke(Color.BLACK);
            g.setFill(Color.rgb(20, 13, 59));
            g.fillRect(0, 0, width, height);
            g.strokeRect(0, 0, width, height);

            // add new value to buffer, if necessary shift the buffer by removing the first element
            while (buffer.size() > buffer_size){
                buffer.remove(0);
            }
            buffer.add(data);

            // draw grid, graph name and axis values
            draw_graph_elements();

            // render graph

            // set parameters for the graph
            g.setLineWidth(1.5);
            g.setLineDashes(0);
            g.setStroke(color);

            // draw graph
            for(int i = 0; i < buffer.size() - 1; i++)
                g.strokeLine(+ border_width + i * data_width, -border_width + height - (double)buffer.get(i) * (height - 2 * border_width) / max_value,border_width + (i+1) * data_width,  -border_width + height - (double)buffer.get(i+1) * (height - 2 * border_width) / max_value);
        }

        private void draw_graph_elements(){
            // draws grid, name, axis indexes of the graph


            // horizontal lines
            for(int i = border_width, index = 0; index * horizontal_line_distance <= max_value && i < height; i += horizontal_line_distance * (height - 2 * border_width) / max_value, index++){
                g.setLineWidth(1);
                g.setLineDashes(4);
                if( i % 2 * horizontal_line_distance * height / max_value == 0) {
                    g.setStroke(Color.DARKGRAY);
                    g.strokeLine(border_width, height - i, width, height - i);
                }
                else {
                    g.setStroke(Color.GRAY);
                    g.strokeLine(border_width, height - i, width, height - i);
                }
            }

            // vertical lines
            for(int i = border_width; i < width; i += vertical_line_distance * data_width){
                g.setLineWidth(1);
                g.setLineDashes(4);
                if( i % 2 * vertical_line_distance * data_width == 0) {
                    g.setStroke(Color.DARKGRAY);
                    g.strokeLine(i, border_width, i, height - border_width);
                }
                else {
                    g.setStroke(Color.GRAY);
                    g.strokeLine(i, border_width, i, height - border_width);
                }
            }

            // x labels
            for(int x = border_width, index = 0; x < width; x += delta_x * data_width, index++){
                g.setFont(new Font(11));
                g.setFill(Color.WHITE);
                g.fillText(Integer.toString(index * delta_x), x, height);
            }

            // y labels
            for(int y = border_width, index = 0; index * delta_y <= max_value && y < height; y += delta_y * (height - 2 * border_width) / max_value, index++){
                g.setFont(new Font(11));
                g.setFill(Color.WHITE);
                g.fillText(Integer.toString(index * delta_y), 0, height - y);
            }

            g.setFont(new Font(12));
            g.setFill(Color.WHITE);
            g.fillText(this.data_name, 30, border_width);
        }

        public void update(int data){
            this.update((char)data);
        }

        // find chart with a specific data name
        public static canvas_line_chart find_chart_by_data_name(String data_name){
            for(canvas_line_chart chart : charts){
                if (chart.data_name.equals(data_name)){
                    return chart;
                }
            }
            // return blank object instead of null to prevent errors
            return new canvas_line_chart(new Canvas().getGraphicsContext2D(), Color.BLACK, 0, 0, 0, 0);
        }
    }
}
