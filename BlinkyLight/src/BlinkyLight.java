import org.apache.log4j.Logger;
import org.limn.accelerometer_radio.AccelerometerData;
import org.limn.accelerometer_radio.AccelerometerRawData;

/**
 * The full screen changes colors when the dancer moves in three dimensions.
 *
 * @author Josh ben Jore
 */
public class BlinkyLight extends AccelerometerSketch {
    private final static Logger LOGGER = Logger.getLogger(BlinkyLight.class);

    private static final int FRAME_RATE = 24;

    @Override
    public boolean sketchFullScreen() {
        return true;
    }

    @Override
    public void setup() {
        size(displayWidth, displayHeight);
        colorMode(HSB, 350);

        // String fileName = "C:\\Users\\Josh\\Desktop\\RadioCapture-" + System.currentTimeMillis() + ".csv";
        // iterator = new RadioDataSource("COM7", 9600, fileName);
        frameRate(FRAME_RATE);
        playTranscript("C:\\Users\\Josh\\Desktop\\RadioCapture-1389754237019.csv",
                null,
                FRAME_RATE);

        background(0, 0, 0);
    }

    @Override
    public void draw() {
        AccelerometerData data = take();
        AccelerometerRawData rawData = data.getRawData();
        int x = rawData.getX();
        int y = rawData.getY();
        int z = rawData.getZ();
        background(x, y, z);
    }
}
