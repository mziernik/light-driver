package driver.channels;

import com.utils.TDate;
import driver.animations.Animation;
import driver.protocol.ProtocolException;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import mlogger.Log;

/**
 * Miłosz Ziernik 2014/08/03
 */
public abstract class Channel {

    public Animation animation;
    public final int id;
    public int currentValue;
    public int savedValue = 4095;
    public final String key;

    public static boolean isDay() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new TDate());
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= 6 && hour < 22;
    }

    public final static Map<String, Channel> map = new LinkedHashMap<>();

    public Channel(int id, String key) {
        this.id = id;
        this.key = key;

        if (map.containsKey(key))
            throw new RuntimeException("Klucz " + key + " nie jest unikalny");

        map.put(key, this);
    }

    public static Channel get(String key) {
        Channel channel = map.get(key);

        if (channel == null)
            throw new NoSuchFieldError("Nie znaleziono kanału \"" + key + "\"");

        return channel;
    }

    public int getValue() {
        return currentValue;
    }

    public Boolean isOn() {
        return currentValue == 0 ? Boolean.FALSE
                : currentValue == 4095 ? Boolean.TRUE
                        : null;
    }

    public void setState(boolean on) throws ProtocolException {
        setValue(on ? currentValue : 0, 2);
    }

    protected abstract void doSetValue(int value, int speed) throws ProtocolException;

    long prevStep = System.currentTimeMillis();

    public Channel changeValue(int value, int speed) throws ProtocolException {
        setValue(value, speed);
        saveValue();
        return this;
    }

    public final Channel setValue(int value, int speed) throws ProtocolException {
        if (value < 0)
            value = 0;
        if (value > 4095)
            value = 4095;

        if (value == currentValue)
            return this;

        doSetValue(value, speed);
        Log.event("Request", key + ", setValue "
                + value + ", speed " + speed);

        //System.out.println("step " + currentValue + "  " + (System.currentTimeMillis() - prevStep));
        prevStep = System.currentTimeMillis();
        currentValue = value;
        return this;
    }

    public Channel saveValue() {
        if (isDay())
            savedValue = currentValue;
        return this;
    }

    public int loadValue() {
        int val = isDay() ? savedValue : 13;

        if (val < 5)
            val = 5;

        if (val > 4095)
            val = 4095;
        return val;
    }
}
