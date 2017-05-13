package driver.switches;

import com.utils.TDate;
import driver.Location;
import driver.Terminal;
import driver.channels.Channel;
import driver.channels.Group;
import driver.switches.ClickDetector.ClickEvent;
import java.util.*;

public class Switch {

    public final static Map<String, Switch> all = new LinkedHashMap<>();

    //---------------------------------------------------------
    public final int upId;
    public final int downId;
    public final Terminal terminal;
    public final Group group;
    public final String name;
    public final String key;
    public final Location location;

    public Switch(Location location, String name, Terminal terminal, int upId, int downId, Group group) {
        this.upId = upId;
        this.downId = downId;
        this.terminal = terminal;
        this.location = location;
        this.name = name;
        this.group = group;
        this.key = location.key + name;
        if (all.containsKey(key))
            throw new RuntimeException("Switch " + key + " już istnieje");

        for (Switch sw : all.values())
            if (sw.terminal == terminal && (sw.upId == upId || sw.downId == downId))
                throw new RuntimeException("Zduplikowany switch (" + terminal
                        + ", " + upId + ", " + downId + ")");
        all.put(key, this);
    }

    public void onSwitchDownEvent(boolean upSw) throws Throwable {
        upDownChange(upSw, group);
    }

    private int getSpeed(int val) {

        return 2;
    }

    protected void onClickEvent(ClickEvent event) throws Exception {

        //    System.out.println("onClickEvent");
        if (event.channel.animation != null)
            event.channel.animation.stop();

        boolean down = event.initValue > 0;

        int val = event.channel.loadValue();

        /*     if (event.channel instanceof Group) {
         event.channel.animation = new RandomGroupAnimation((Group) event.channel, down);
         event.channel.animation.start();
         return;
         }*/
        event.channel.setValue(down ? 0 : val, getSpeed(val));
    }

    protected void upDownChange(boolean upSw, Channel channel) throws Throwable {
        ClickEvent event = ClickDetector.onSwitchDown(channel, this, upSw, channel.getValue());

        final long now = System.currentTimeMillis();

        if (now - event.created < ClickDetector.CLICK_TIME) {

            if (event.initValue == 0) {  // jesli jest 0 to wlacz natychmiast

                if (channel.animation != null)
                    channel.animation.stop();

                int val = channel.loadValue();
                channel.setValue(val, getSpeed(val));
            }
            return;
        }

        int value = channel.getValue();

        if (channel.animation != null)
            value = channel.animation.value;

        int step = (int) Math.ceil((value + 1) / 10d);

        if (upSw && value < 4095)
            value = range(value + step, 0, 4095);

        if (!upSw && value > 1)
            value = range(value - step, 0, 4095);

        if (channel.animation != null) {
            channel.animation.value = value;
            return;
        }

        if (upSw)
            channel.changeValue(value, 0);

        if (!upSw)
            channel.changeValue(value, 1);

        lastTrcking = now;
    }
    private long lastTrcking = System.currentTimeMillis();

    public int range(int value, int min, int max) {
        return value < min ? min : value > max ? max : value;
    }
}
