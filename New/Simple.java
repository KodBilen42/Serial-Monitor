package New;

import com.fazecast.jSerialComm.*;

import java.util.Arrays;

public class Simple {
    public static void main(String[] args){

        SerialPort[] ports = SerialPort.getCommPorts();
        for(SerialPort port: ports)
            System.out.println(port.getDescriptivePortName());

        int port_index = 0;

        ports[port_index].addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
            }

            @Override
            public void serialEvent(SerialPortEvent serialPortEvent) {
                byte[] data = serialPortEvent.getReceivedData();
                String data_s = "";
                for (byte piece: data)
                    data_s += (char)piece;
                System.out.print(data_s);
            }
        });

        ports[port_index].openPort();
    }
}
