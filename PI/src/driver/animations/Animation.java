package driver.animations;

import driver.channels.Group;
import driver.channels.PwmChannel;
import driver.protocol.ProtocolException;
import java.util.LinkedHashMap;
import java.util.Map;
import mlogger.Log;

public abstract class Animation {

    public final static Map<String, Animation> all = new LinkedHashMap<>();

    public int speed; // 0...4095
    public final Group group;
    public final PwmChannel[] channels;
    public final String key;
    public final String name;
    private AnimThread thread;
    public int value;
    int prevValue;

    public Animation(String key, String name, Group group, PwmChannel... channels) {
        this.key = key;
        this.name = name;
        this.group = group;
        this.channels = channels;
        prevValue = value = group.savedValue;
        all.put(key, this);
    }

    protected abstract void process() throws Exception;

    protected void sleep() throws ProtocolException, InterruptedException {
        for (int i = 0; i < 5; i++) {
            Thread.sleep(100);
            if (prevValue != value) {
                for (PwmChannel cha : channels)
                    if (cha.getValue() > 0)
                        cha.setValue(value, 4);

                prevValue = value;
            }
        }
    }

    public boolean isRunning() {
        return thread != null;
    }

    public void click() {
        if (thread != null)
            stop();
        else
            start();
    }

    public void start() {
        if (thread != null)
            return;
        
        if (group.animation != null)
            group.animation.stop();
        
        thread = new AnimThread();
        thread.setName("Animation: " + name);
        thread.setPriority(Thread.MIN_PRIORITY);
        group.animation = this;
        thread.start();
    }

    public void stop() {
        if (thread == null)
            return;
        thread.interrupt();
        thread = null;
        group.animation = null;
    }

    class AnimThread extends Thread {

        @Override
        public void run() {
            try {
                for (PwmChannel cha : channels)
                    cha.setValue(0, 4);
                process();
            } catch (Exception ex) {
                Log.error(ex);
            } finally {
                thread = null;
            }
        }

    }
}
