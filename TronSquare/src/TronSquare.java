import limn.radio.AccelerometerData;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 * A tron-like scape where crystals grow based on motion.
 *
 * @author Josh ben Jore
 */
public class TronSquare extends AccelerometerSketch {
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
    
    private static final DateTimeZone TZ = DateTimeZone.forID("America/Los_Angeles");

    private static final int FRAME_RATE = 24;

    private float xa, ya, za;
    private float gxf, gyf, gzf;
    @Override
    public void setup() {
        this.xa = H_HALF;
        this.ya = W_HALF;
        this.za = Z_HALF;
        this.gxf = this.gyf = this.gzf = 0F;

        size(H, W, P3D);
        smooth();
        colorMode(HSB, 6);
        background(0, 0, 0);
        stroke(6, 6, 6);
        textSize(32F);
        frameRate(FRAME_RATE);

        accelerometerData();
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
        this.gxf = 0.9F*this.gxf + 0.1F*xf;
        this.gyf = 0.9F*this.gyf + 0.1F*yf;
        this.gzf = 0.9F*this.gzf + 0.1F*zf;

        //background(x + 3, y + 3, z + 3);
        this.xa = ADD(this.xa, -this.gyf, W_MAX, W_MIN);
        this.ya = ADD(this.ya,  this.gxf, H_MAX, H_MIN);
        this.za = ADD(this.za,  this.gzf, Z_MAX, Z_MIN);
        fill(this.gzf, this.gyf, this.gxf);
        translate(this.xa, this.ya, this.za);
        box(
            (Math.abs(this.gyf) / 3) * H,
            (Math.abs(this.gxf) / 3) * W,
            (Math.abs(this.gzf) / 3) * this.za
        );

//        saveFrame("C:\\Users\\Josh\\Desktop\\RadioCapture-1389754237019\\"
//                + String.format("%010d", frameCount)
//                + ".tif");
    }

    private static float ADD(float origValue, float delta, int max, int min) {
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
