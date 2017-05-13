package driver.animations;

import driver.channels.Group;
import driver.channels.PwmChannel;
import java.util.Random;

public class Point extends Animation {

    public Point(Group group) {
        super("pt" + group.key, "Wędrujący punkt " + group.name, group, group.channels);
    }

    @Override
    protected void process() throws Exception {
        int idx = 0;
        PwmChannel channel = null;
        while (true) {
            sleep();
            if (channel != null)
                channel.setValue(0, 4);

            channel = channels[idx++];
            channel.setValue(channel.getValue() > 0 ? 0 : value, 4);
            if (idx >= channels.length)
                idx = 0;
        }

    }

}
