import java.util.Iterator;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.limn.accelerometer_radio.AccelerometerData;
import org.limn.accelerometer_radio.AccelerometerRawData;
import org.limn.accelerometer_radio.FileDataSource;
import org.limn.accelerometer_radio.FrameLockedIterator;
import org.limn.accelerometer_radio.IDataSource;
import org.limn.accelerometer_radio.RadioDataSource;
import org.limn.accelerometer_radio.util.Clock;

import com.google.common.base.Joiner;

import processing.core.PApplet;

public class WormySquare extends AccelerometerSketch {
    private final static Logger LOGGER = Logger.getLogger(WormySquare.class);
    private static final DateTimeZone TZ = DateTimeZone.forID("America/Los_Angeles");
    private static final int FRAME_RATE = 24;

   
    private float posX;
    private float posY;
    private float posZ;

    private long cutoff;

    @Override
    public boolean sketchFullScreen() {
        return true;
    }

    @Override
    public void setup() {
        size(displayWidth, displayHeight, P3D);

        posX = posY = posZ = (int) (((float) Math.min(displayWidth, displayHeight)) / 2F);

        colorMode(HSB, 6);

        frameRate((float) FRAME_RATE);
        playTranscript(
                "C:\\Users\\Josh\\Desktop\\RadioCapture-1389754237019.csv",
                null,
                FRAME_RATE);
        //readRadio("COM7", 9600,
        //        "C:\\Users\\Josh\\Desktop\\RadioCapture-" + System.currentTimeMillis() + ".csv");

        background(0, 0, 0);
        
        textSize(32);
    }

    @Override
    public void draw() {
        AccelerometerData data = take();
        if (data == null) {
            return;
        }

        long time = data.getTime();
        float x = (float) data.getX();
        float y = (float) data.getY();
        float z = (float) data.getZ();

        // DateTime dateTime = new DateTime(time, TZ);

//        fill(0);
//        rect(0, 0, 700, 42 + 8);
//        fill(6);
//        text(dateTime.toString(), 10, 42);

        //background(x + 3, y + 3, z + 3);

        // "To the color X!"
        float colorX = Math.abs(x * 6);
        float colorY = Math.abs(y * 6);
        float colorZ = Math.abs(z * 6);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("fill: " + Joiner.on(", ").join(colorX, colorY, colorZ));
        }
        fill(colorX, colorY, colorZ);

        // "Increase the maximum resonator three more levels!"
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stroke: " + Joiner.on(", ").join(colorZ, colorX, colorY));
        }
        stroke(colorZ, colorX, colorY);

        posX = move(posX, x, 0, displayWidth);
        posY = move(posY, y, 0, displayHeight);
        posZ += z;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("pos: " + Joiner.on(", ").join(posX, posY, posZ));
        }
        translate(posX, posY, posZ);

        float rotation = Math.max(Math.max(x, y), z);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("rotate: " + rotation);
        }
        rotate(rotation);

        float sizeX = Math.abs(50F * x);
        float sizeY = Math.abs(50F * y);
        float sizeZ = Math.abs(50F * z);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("box: " + Joiner.on(", ").join(sizeX, sizeY, sizeZ));
        }
        box(sizeX, sizeY, sizeZ);
    }

    private float move(float sum, float delta, int min, int max) {
        float newSum = sum + delta;
        if (newSum > max) {
            return min;
        }
        else if (newSum < min) {
            return max;
        }
        else {
            return newSum;
        }
    }
}
