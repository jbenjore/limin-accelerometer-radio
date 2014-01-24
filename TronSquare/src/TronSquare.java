import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.limn.accelerometer_radio.AccelerometerData;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;

/**
 * A tron-like scape where crystals grow based on motion.
 *
 * @author Josh ben Jore
 */
public class TronSquare extends AccelerometerSketch {
    final static Logger LOGGER = Logger.getLogger(TronSquare.class);
    private static final DateTimeFormatter FORMATTER =
        new DateTimeFormatterBuilder()
            .appendYearOfEra(4, 4)
            .appendLiteral('-')
            .appendMonthOfYear(2)
            .appendLiteral('-')
            .appendDayOfMonth(2)
            .appendLiteral(' ')
            .appendHourOfHalfday(1)
            .appendLiteral(':')
            .appendMinuteOfHour(2)
            .appendLiteral(':')
            .appendSecondOfMinute(2)
            .appendLiteral(' ')
            .appendHalfdayOfDayText()
            .toFormatter();
    private static final int H = 640;
    private static final int H_MAX = H - 100;
    private static final int H_HALF = (int) Math.floor((H) / 2F);
    private static final int H_MIN = 100;

    private static final int W = 480;
    private static final int W_MAX = 380;
    private static final int W_HALF = (int) Math.floor((H) / 2F);
    private static final int W_MIN = 100;

    private static final int Z_MAX = 500;
    private static final int Z_HALF = 300;
    private static final int Z_MIN = 100;
    
    private static final int X1 = 340;
    private static final int X0 = 286;
    private static final int X_1 = 228;
    private static final int X_RANGE = (X1 - X_1) / 2;

    private static final int Y1 = 342;
    private static final int Y0 = 286;
    private static final int Y_1 = 227;
    private static final int Y_RANGE = (Y1 - Y_1) / 2;

    private static final int Z1 = 356;
    private static final int Z0 = 295;
    private static final int Z_1 = 239;
    private static final int Z_RANGE = (Z1 - Z_1) / 2;

    private static final DateTimeZone TZ = DateTimeZone.forID("America/Los_Angeles");

    private static final int FRAME_RATE = 24;

    private float X, Y, Z;
    private float gxf, gyf, gzf;
    private long prevTime;
    private Minim minim;
    private AudioPlayer player;
    private boolean playing;
    private AudioPlayer player2;

    @Override
    public void setup() {
        X = H_HALF;
        Y = W_HALF;
        Z = Z_HALF;
        gxf = gyf = gzf = 0F;

        size(H, W, P3D);
        smooth();
        colorMode(HSB, 6);
        background(0, 0, 0);
        stroke(6, 6, 6);
        textSize(32F);
        frameRate(FRAME_RATE);

        // Replay from disk or play live.
//        readTranscript(
//                "C:\\Users\\Josh\\Desktop\\RadioCapture-1389754237019.csv",
//                DateTime.parse("2014-01-14T19:40:00.000-08"),
//                FRAME_RATE
//        );
        playRadio("COM7", 9600,
                "C:\\Users\\Josh\\Desktop\\RadioCapture-" + System.currentTimeMillis() + ".csv");
    }

    @Override
    public void draw() {
        AccelerometerData data = take();
        if (data == null) {
            return;
        }

        long time = data.getTime();
        arrowOfTime(time);

//        AccelerometerRawData rawData = data.getRawData();
//        int x = rawData.getX();
//        int y = rawData.getY();
//        int z = rawData.getZ();

        fill(0);
        stroke(0);
        rect(0, 0, 400, 42 + 8);

        stroke(6, 6, 6);
        fill(6);
        text(new DateTime(time, TZ).toString(FORMATTER), 10, 42);

//        float xf = ((float) (x - X0)) / X_RANGE;
//        float yf = ((float) (y - Y0)) / Y_RANGE;
//        float zf = ((float) (z - Z0)) / Z_RANGE;
        float xf = (float) data.getX();
        float yf = (float) data.getY();
        float zf = (float) data.getZ();

        // Low pass filter.
        gxf = 0.9F*gxf + 0.1F*xf;
        gyf = 0.9F*gyf + 0.1F*yf;
        gzf = 0.9F*gzf + 0.1F*zf;

        //background(x + 3, y + 3, z + 3);
        X = ADD(X, -gyf, W_MAX, W_MIN);
        Y = ADD(Y,  gxf, H_MAX, H_MIN);
        Z = ADD(Z,  gzf, Z_MAX, Z_MIN);
        fill(gzf, gyf, gxf);
        translate(X, Y, Z);
        box(
            (Math.abs(gyf) / 3) * H,
            (Math.abs(gxf) / 3) * W,
            (Math.abs(gzf) / 3) * Z
        );

//        saveFrame("C:\\Users\\Josh\\Desktop\\RadioCapture-1389754237019\\"
//                + String.format("%010d", frameCount)
//                + ".tif");
    }

    float ADD(float origValue, float delta, int max, int min) {
        float newValue = origValue + delta;
        if (newValue > max - min) {
            return min;
        }
        else if (newValue < min) {
            return max - min;
        }
        else {
            return newValue;
        }
    }
}
