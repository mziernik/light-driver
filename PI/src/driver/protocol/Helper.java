package driver.protocol;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.util.Arrays;
import mlogger.Log;

public class Helper {

    private final SerialPort serial;
    private static long lastSend = System.currentTimeMillis();
    private long lastWrited;

    public boolean pirState;
    public boolean batState;
    public boolean pirPower;
    public boolean batPower;
    public int pwmValue;

    public final PIR pir;

    public Helper(String comPortName) throws Exception {

        serial = SerialPort.getCommPort(comPortName);

        boolean openPort2 = serial.openPort();

        serial.setBaudRate(9600);
        serial.setParity(SerialPort.NO_PARITY);
        serial.setNumDataBits(8);

        serial.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                    return;

                try {

                    int avail = serial.bytesAvailable();
                    if (avail < 0)
                        return;

                    byte[] bytes = new byte[avail];
                    serial.readBytes(bytes, bytes.length);

                    if (bytes == null || bytes.length == 0)
                        return;

                    int v = bytes[0] & 0xFF;

                    pirState = (v & 0x01) != 0;
                    batState = (v & 0x02) == 0;

                    //   System.out.println("PIR: " + pirState + ", BAT: " + batState);
                    Log.info("status", "PIR: " + pirState + ", BAT: " + batState);

                    if (pirState)
                        pir.flush();

                } catch (Throwable ex) {
                    Log.error(ex);
                }
            }
        });

        /*
        Log.debug("Otwieram port " + comPortName);
        serial = new SerialPort(comPortName);

        if (serial.openPort())
            Log.debug("Połączono");

        serial.setParams(SerialPort.BAUDRATE_9600, 8, 1, SerialPort.PARITY_NONE);
        serial.addEventListener(this);
         */
        pir = new PIR(this);
        pir.start();

        writePowerState();
        pwmWrite(0);
    }

    /*
    @Override
    public void serialEvent(SerialPortEvent spe) {

        if (spe.getEventType() != SerialPortEvent.RXCHAR) {
            System.out.println("Zdarzenie " + spe.getEventType());
        }

        if (spe.getEventType() == SerialPortEvent.RXCHAR) {
            try {

                byte[] bytes = serial.readBytes();
                if (bytes == null || bytes.length == 0) {
                    return;
                }

                int v = bytes[0] & 0xFF;

                pirState = (v & 0x01) != 0;
                batState = (v & 0x02) == 0;

                //   System.out.println("PIR: " + pirState + ", BAT: " + batState);
                Log.info("status", "PIR: " + pirState + ", BAT: " + batState);

                if (pirState)
                    pir.flush();

            } catch (Throwable ex) {
                Log.error(ex);
            }
        }
    }
     */
    public void pwmWrite(int value) {
        if (value == 256)
            value = 255;
        pwmValue = value;

      //  System.out.println("PIR pwm " + (value & 0xFF));

        byte[] data = new byte[]{(byte) 1, (byte) value};
        serial.writeBytes(data, data.length);
    }

    public void writePowerState() {
        int v = 0;
        if (pirPower)
            v |= 0x01;
        if (batPower)
            v |= 0x02;

        byte[] data = new byte[]{(byte) 2, (byte) v};
        serial.writeBytes(data, data.length);
    }

}
