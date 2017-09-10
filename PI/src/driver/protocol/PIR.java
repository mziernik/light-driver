package driver.protocol;

import api.WsServer;
import com.json.JArray;
import com.json.JObject;

import com.utils.TDate;
import com.threads.TThread;
import java.util.Calendar;
import java.util.TimeZone;
import mlogger.Log;

public class PIR extends TThread {

    boolean pirEnabled;
    long powerOnTS;
    public final Helper helper;
    long flushTS;
    boolean bright;
    private final Object flushNotify = new Object();

    final static int POWER_ON_DELAY = 15000;
    final static int BRIGHT_TIME = 25000;

    public void flush() {
        if (!driver.State.pirEnabled) {
            flushTS = 0;
            return;
        }

        if (!(helper.pirPower && System.currentTimeMillis() - powerOnTS > POWER_ON_DELAY))
            return;

        flushTS = System.currentTimeMillis();
        synchronized (flushNotify) {
            flushNotify.notifyAll();
        }
    }

    public PIR(Helper helper) {
        this.helper = helper;
    }

    @Override
    protected void execute() throws Throwable {

        // getSchedule();
        while (!isInterrupted())
            try {

                long now = System.currentTimeMillis();
                boolean canBright = !driver.State.scheduleEnabled
                        || new ScheduleDay(Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
                                .isInRange();

                if (canBright && !helper.pirPower) {
                    // włączenie modułu
                    Log.info("Włączam moduł PIR");
                    helper.pirPower = true;
                    helper.writePowerState();
                    helper.pwmWrite(20);
                    powerOnTS = now;
                    notifyChanges();
                }

                if (!canBright && helper.pirPower) {
                    // wylaczenie modułu
                    Log.info("Wyłączam moduł PIR");
                    helper.pirPower = false;
                    helper.writePowerState();
                    helper.pwmWrite(0);
                    powerOnTS = 0;
                    notifyChanges();
                }

                if (flushTS > 0 && !bright) {
                    // wlaczenie
                    Log.info("Włączam zewnętrzne oświetlenie");

                    for (int i = helper.pwmValue; i <= 255; i++) {
                        helper.pwmWrite(i);
                        Thread.sleep(5);
                    }
                    bright = true;
                    notifyChanges();
                }

                if (bright && flushTS > 0 && now - flushTS > BRIGHT_TIME) {
                    bright = false;
                    flushTS = 0;

                    // wylaczenie
                    Log.info("Wyłączam zewnętrzne oświetlenie");
                    for (int i = helper.pwmValue; i > 0; i--) {
                        helper.pwmWrite(i);
                        Thread.sleep(15);
                        if (flushTS > 0)
                            break;
                    }

                    notifyChanges();
                    // jesli podczas sciemniania cos sie mienilo,
                    // to pomin sleepa 5000
                    if (flushTS > 0)
                        continue;
                }

                synchronized (flushNotify) {
                    flushNotify.wait(5000);
                }

            } catch (Throwable e) {
                Log.error(e);
            }

    }

    private void notifyChanges() {
        if (!WsServer.hasClients())
            return;

        JObject json = new JObject();
        json.object("PIR")
                .put("power", helper.pirPower)
                .put("state", helper.pirState)
                .put("value", helper.pwmValue);

        WsServer.boadcast(json);
    }

    public static JArray getSchedule() {
        JArray main = new JArray();
        for (int i = 1; i <= 365; i++)
            main.add(new ScheduleDay(i).getData());
        return main;
    }

    static class ScheduleDay {

        public final int day;
        public final TDate date;
        public final TDate rise;
        public final TDate set;
        public final TDate dayLength;
        public final int offsetHours;

        public TDate on;
        public TDate off;
        public TDate duration;

        public ScheduleDay(int day) {
            this.day = day;
            String sRise = PIR.SUN_RISE[day - 1];
            String sSet = PIR.SUN_SET[day - 1];

            int len = toMinutes(sSet) - toMinutes(sRise);

            TimeZone.getAvailableIDs();

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            cal.add(Calendar.DAY_OF_YEAR, day - 1);

            date = new TDate(cal.getTime());

            cal.add(Calendar.MINUTE, toMinutes(sRise));
            rise = new TDate(cal.getTime());

            cal.add(Calendar.MINUTE, len);
            set = new TDate(cal.getTime());

            cal.add(Calendar.MINUTE, -toMinutes(sRise));
            dayLength = new TDate(cal.getTime());

            int zoneOffset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (60 * 1000);
            offsetHours = zoneOffset / 60;

            on = new TDate(set).addMinutes(zoneOffset - 120);
            off = new TDate(on).addMinutes(520 - (int) (len / 2.5));

            duration = new TDate(off.getTime() - on.getTime());
        }

        /**
         * Czy bieżąca date jest w zakresie aktywności
         *
         * @return
         */
        public boolean isInRange() {
            long now = System.currentTimeMillis();
            return now >= on.getTime() && now <= off.getTime();
        }

        public JArray getData() {
            return new JArray().addAll(
                    date.toString("dd MMM"),
                    offsetHours,
                    on.toString("HH:mm"),
                    off.toString("HH:mm"),
                    duration.toString("HH:mm"),
                    dayLength.toString("HH:mm"),
                    rise.toString("HH:mm"),
                    set.toString("HH:mm")
            );

            /*
                        System.out.println(sd.day + ".\t +" + sd.offsetHours + "\t"
                    + sd.on.toString("dd-MM HH:mm")
                    + " - " + sd.off.toString("HH:mm") + " " + sd.duration);
             */
        }

        private static int toMinutes(String s) {
            String[] a = s.split(":");
            return Integer.parseInt(a[0]) * 60 + Integer.parseInt(a[1]);
        }

    }
    // godziny zachodów słońca (czas GMT) dla poszczególnych dni roku
    public final static String[] SUN_SET = {"16:31", "16:32", "16:33", "16:34",
        "16:35", "16:37", "16:38", "16:39", "16:41", "16:42", "16:43", "16:45",
        "16:47", "16:48", "16:50", "16:51", "16:53", "16:54", "16:56", "16:58",
        "17:00", "17:01", "17:03", "17:05", "17:07", "17:09", "17:10", "17:12",
        "17:14", "17:16", "17:18", "17:20", "17:22", "17:23", "17:25", "17:27",
        "17:29", "17:31", "17:33", "17:35", "17:37", "17:39", "17:41", "17:43",
        "17:45", "17:46", "17:48", "17:50", "17:52", "17:54", "17:56", "17:58",
        "18:00", "18:02", "18:03", "18:05", "18:07", "18:09", "18:11", "18:13",
        "18:15", "18:16", "18:18", "18:20", "18:22", "18:24", "18:26", "18:27",
        "18:29", "18:31", "18:33", "18:35", "18:36", "18:38", "18:40", "18:42",
        "18:43", "18:45", "18:47", "18:49", "18:51", "18:52", "18:54", "18:56",
        "18:58", "18:59", "19:01", "19:03", "19:05", "19:06", "19:08", "19:10",
        "19:12", "19:13", "19:15", "19:17", "19:18", "19:20", "19:22", "19:24",
        "19:25", "19:27", "19:29", "19:31", "19:32", "19:34", "19:36", "19:38",
        "19:39", "19:41", "19:43", "19:45", "19:46", "19:48", "19:50", "19:52",
        "19:53", "19:55", "19:57", "19:58", "20:00", "20:02", "20:03", "20:05",
        "20:07", "20:09", "20:10", "20:12", "20:13", "20:15", "20:17", "20:18",
        "20:20", "20:22", "20:23", "20:25", "20:26", "20:28", "20:29", "20:31",
        "20:32", "20:34", "20:35", "20:37", "20:38", "20:39", "20:41", "20:42",
        "20:43", "20:44", "20:46", "20:47", "20:48", "20:49", "20:50", "20:51",
        "20:52", "20:53", "20:54", "20:55", "20:56", "20:56", "20:57", "20:58",
        "20:58", "20:59", "21:00", "21:00", "21:00", "21:01", "21:01", "21:01",
        "21:01", "21:02", "21:02", "21:02", "21:02", "21:02", "21:02", "21:01",
        "21:01", "21:01", "21:01", "21:00", "21:00", "20:59", "20:59", "20:58",
        "20:58", "20:57", "20:56", "20:55", "20:55", "20:54", "20:53", "20:52",
        "20:51", "20:50", "20:48", "20:47", "20:46", "20:45", "20:44", "20:42",
        "20:41", "20:39", "20:38", "20:37", "20:35", "20:33", "20:32", "20:30",
        "20:29", "20:27", "20:25", "20:23", "20:22", "20:20", "20:18", "20:16",
        "20:14", "20:12", "20:10", "20:08", "20:06", "20:05", "20:02", "20:00",
        "19:58", "19:56", "19:54", "19:52", "19:50", "19:48", "19:46", "19:43",
        "19:41", "19:39", "19:37", "19:35", "19:32", "19:30", "19:28", "19:25",
        "19:23", "19:21", "19:19", "19:16", "19:14", "19:12", "19:09", "19:07",
        "19:05", "19:02", "19:00", "18:58", "18:55", "18:53", "18:51", "18:48",
        "18:46", "18:43", "18:41", "18:39", "18:36", "18:34", "18:32", "18:29",
        "18:27", "18:24", "18:22", "18:20", "18:17", "18:15", "18:13", "18:10",
        "18:08", "18:06", "18:03", "18:01", "17:59", "17:56", "17:54", "17:52",
        "17:50", "17:47", "17:45", "17:43", "17:41", "17:38", "17:36", "17:34",
        "17:32", "17:30", "17:28", "17:25", "17:23", "17:21", "17:19", "17:17",
        "17:15", "17:13", "17:11", "17:09", "17:07", "17:05", "17:04", "17:02",
        "17:00", "16:58", "16:56", "16:55", "16:53", "16:51", "16:50", "16:48",
        "16:47", "16:45", "16:44", "16:42", "16:41", "16:39", "16:38", "16:37",
        "16:36", "16:34", "16:33", "16:32", "16:31", "16:30", "16:29", "16:28",
        "16:27", "16:26", "16:25", "16:25", "16:24", "16:24", "16:23", "16:22",
        "16:22", "16:22", "16:21", "16:21", "16:21", "16:21", "16:21", "16:21",
        "16:21", "16:21", "16:21", "16:21", "16:22", "16:22", "16:22", "16:23",
        "16:23", "16:24", "16:25", "16:25", "16:26", "16:27", "16:28", "16:29",
        "16:30"};

    // godziny wschodów słońca (czas GMT) dla poszczególnych dni roku
    public final static String[] SUN_RISE = {"8:50", "8:50", "8:50", "8:50",
        "8:49", "8:49", "8:48", "8:48", "8:47", "8:47", "8:46", "8:46", "8:45",
        "8:44", "8:43", "8:42", "8:41", "8:40", "8:39", "8:38", "8:37", "8:36",
        "8:35", "8:34", "8:32", "8:31", "8:29", "8:28", "8:27", "8:25", "8:24",
        "8:22", "8:20", "8:19", "8:17", "8:15", "8:14", "8:12", "8:10", "8:08",
        "8:06", "8:05", "8:03", "8:01", "7:59", "7:57", "7:55", "7:53", "7:51",
        "7:49", "7:47", "7:45", "7:43", "7:40", "7:38", "7:36", "7:34", "7:32",
        "7:30", "7:27", "7:25", "7:23", "7:21", "7:18", "7:16", "7:14", "7:12",
        "7:09", "7:07", "7:05", "7:02", "7:00", "6:58", "6:55", "6:53", "6:51",
        "6:48", "6:46", "6:44", "6:41", "6:39", "6:37", "6:34", "6:32", "6:30",
        "6:27", "6:25", "6:23", "6:20", "6:18", "6:16", "6:13", "6:11", "6:08",
        "6:06", "6:04", "6:01", "5:59", "5:57", "5:55", "5:52", "5:50", "5:48",
        "5:46", "5:43", "5:41", "5:39", "5:37", "5:35", "5:32", "5:30", "5:28",
        "5:26", "5:24", "5:22", "5:20", "5:18", "5:16", "5:14", "5:12", "5:10",
        "5:08", "5:06", "5:04", "5:02", "5:00", "4:58", "4:56", "4:55", "4:53",
        "4:51", "4:50", "4:48", "4:46", "4:45", "4:43", "4:41", "4:40", "4:39",
        "4:37", "4:36", "4:34", "4:33", "4:32", "4:31", "4:30", "4:28", "4:27",
        "4:26", "4:25", "4:24", "4:23", "4:22", "4:22", "4:21", "4:20", "4:20",
        "4:19", "4:18", "4:18", "4:18", "4:17", "4:17", "4:17", "4:16", "4:16",
        "4:16", "4:16", "4:16", "4:16", "4:16", "4:16", "4:16", "4:17", "4:17",
        "4:17", "4:18", "4:18", "4:18", "4:19", "4:20", "4:20", "4:21", "4:22",
        "4:22", "4:23", "4:24", "4:25", "4:26", "4:27", "4:28", "4:29", "4:30",
        "4:31", "4:32", "4:33", "4:35", "4:36", "4:37", "4:38", "4:40", "4:41",
        "4:43", "4:44", "4:45", "4:47", "4:48", "4:50", "4:51", "4:53", "4:54",
        "4:56", "4:57", "4:59", "5:00", "5:02", "5:04", "5:05", "5:07", "5:08",
        "5:10", "5:12", "5:13", "5:15", "5:17", "5:18", "5:20", "5:22", "5:23",
        "5:25", "5:26", "5:28", "5:30", "5:31", "5:33", "5:35", "5:37", "5:38",
        "5:40", "5:41", "5:43", "5:45", "5:46", "5:48", "5:50", "5:51", "5:53",
        "5:55", "5:56", "5:58", "6:00", "6:01", "6:03", "6:05", "6:06", "6:08",
        "6:10", "6:11", "6:13", "6:15", "6:16", "6:18", "6:20", "6:21", "6:23",
        "6:25", "6:26", "6:28", "6:30", "6:31", "6:33", "6:35", "6:36", "6:38",
        "6:40", "6:42", "6:43", "6:45", "6:47", "6:48", "6:50", "6:52", "6:54",
        "6:55", "6:57", "6:59", "7:01", "7:02", "7:04", "7:06", "7:08", "7:10",
        "7:11", "7:13", "7:15", "7:17", "7:19", "7:20", "7:22", "7:24", "7:26",
        "7:28", "7:30", "7:31", "7:33", "7:35", "7:37", "7:39", "7:41", "7:43",
        "7:44", "7:46", "7:48", "7:50", "7:52", "7:54", "7:55", "7:57", "7:59",
        "8:01", "8:03", "8:04", "8:06", "8:08", "8:10", "8:11", "8:13", "8:15",
        "8:16", "8:18", "8:19", "8:21", "8:23", "8:24", "8:26", "8:27", "8:29",
        "8:30", "8:31", "8:33", "8:34", "8:35", "8:36", "8:38", "8:39", "8:40",
        "8:41", "8:42", "8:43", "8:44", "8:44", "8:45", "8:46", "8:47", "8:47",
        "8:48", "8:48", "8:49", "8:49", "8:49", "8:50", "8:50", "8:50", "8:50",
        "8:50"};
}
