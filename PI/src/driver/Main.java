package driver;

import http.HServer;
import driver.protocol.*;
import api.WsServer;
import com.fazecast.jSerialComm.SerialPort;
import java.io.FileInputStream;
import mlogger.*;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Properties;
import sun.misc.Signal;

/**
 * Miłosz Ziernik 2014/07/13
 */
//   T8.ch5 oswietlenie pod szafkami w kuchni
public class Main {

    public final static boolean isWindows = System.getProperty("os.name")
            .toLowerCase().contains("windows");

    public final static boolean clietMode = isWindows;
    public final static long start = System.currentTimeMillis();
    static String hostname;

    public static Protocol protocol;
    public static Helper helper;

    public static void main(String[] args) {

        Signal.handle(new Signal("INT"), (sig) -> {
            System.out.println("Sterownik: INTERUPT Signal");
            System.exit(1);
        });

        if (isWindows)
            try {
                Class.forName(Index.class.getName());
                HServer.init();
                new WsServer().start();
                return;
            } catch (Throwable e) {
                Log.error(e);
                return;
            }

        MLogger.defaults.source = "Sterownik";
        MLogger logger = MLogger.getInstance();

        MLogger.addUdpHandler("192.168.1.255", 514);

        Log.event("Uruchamiam sterownik");

        try {

            hostname = InetAddress.getLocalHost().getHostName();

            System.out.println("HOST: " + hostname);

            Class.forName(Index.class.getName()); // inicjalizacja

            for (SerialPort sp : SerialPort.getCommPorts())
                Log.debug("Available serial port: " + sp.getSystemPortName());

            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("config.properties")) {
                props.load(fis);
            }

            /*
            Map<String, String> map = new TreeMap<>();

            try (InputStream in = Runtime.getRuntime().exec("dmesg").getInputStream()) {
                String data = IOUtils.read(in, Charset.forName("UTF-8"));

                String[] lines = data.split("\n");
                for (String s : lines)
                    if (s.contains("pl2303 converter now attached to")
                            && s.contains("] usb ")
                            && s.contains("ttyUSB")) {

                        s = s.substring(s.indexOf("] usb ") + "] usb ".length()).trim();

                        map.put(s.substring(0, s.indexOf(":")).trim(),
                                "/dev/" + s.substring(s.lastIndexOf(" ") + 1).trim());

                    }
            }
            
            switch (hostname) {
                case "ZMSERVER":
                    helper = new Helper("COM6");
                    //     protocol = new Protocol("COM6");
                    break;
                default:

                    if (map.size() == 2) {
                        ports.clear();
                        ports.addAll(map.values());
                    }

                    if (ports.size() > 1)
                        helper = new Helper(ports.get(1));
                    break;

            }
             */
            if (props.containsKey("serial.main")) {
                Log.event("Main serial: " + props.getProperty("serial.main"));
                protocol = new Protocol(props.getProperty("serial.main"));
            }
            if (props.containsKey("serial.helper")) {
                helper = new Helper(props.getProperty("serial.helper"));
                Log.event("Helper serial: " + props.getProperty("serial.helper"));
            }

            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        HServer.init();
                        new WsServer().start();
                        Thread.sleep(300);
                        protocol.sendHello();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Log.error(e);
                    }
                }
            }).start();

        } catch (Throwable e) {
            Log.error(e);
            e.printStackTrace();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
            System.exit(1);
        }

    }
}
