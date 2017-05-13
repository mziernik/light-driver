package driver.animations;

import driver.channels.Group;
import driver.channels.PwmChannel;
import java.util.Random;

public class Random1 extends Animation {

    public Random1(Group group) {
        super("rnd" + group.key, "Losowy " + group.name, group, group.channels);
    }

    @Override
    protected void process() throws Exception {
        final Random random = new Random();

        while (true) {
            sleep();
            PwmChannel channel = channels[random.nextInt(channels.length)];

            channel.setValue(channel.getValue() > 0 ? 0 : value, 4);

        }

    }

}
