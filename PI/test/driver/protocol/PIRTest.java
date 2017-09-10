package driver.protocol;

import com.json.JArray;
import com.utils.TDate;
import driver.protocol.PIR.ScheduleDay;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author miloszz
 */
public class PIRTest {

    public PIRTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void calendar() {

        for (int i = 0; i < 365; i++) {
            ScheduleDay sd = new ScheduleDay(i + 1);

            System.out.println(sd.day + ".\t +" + sd.offsetHours + "\t" 
                    + sd.on.toString("dd-MM HH:mm")
                    + " - " + sd.off.toString("HH:mm") + " " + sd.duration);

        }

    }

}
