import limn.radio.AccelerometerData;

import com.google.common.base.Joiner;

public class WormySquare extends AccelerometerSketch {
    private static final int FRAME_RATE = 24;

   
    private float posX;
    private float posY;
    private float posZ;

    @Override
    public boolean sketchFullScreen() {
        return true;
    }

    @Override
    public void setup() {
        size(this.displayWidth, this.displayHeight, P3D);

        this.posX = this.posY = this.posZ = (int) ((Math.min(this.displayWidth, this.displayHeight)) / 2F);

        colorMode(HSB, 6);

        frameRate(FRAME_RATE);
        accelerometerData();
        background(0, 0, 0);
        
        textSize(32);
    }

    @Override
    public void draw() {
        AccelerometerData data = take();
        if (data == null) {
            return;
        }

        float x = (float) data.getX();
        float y = (float) data.getY();
        float z = (float) data.getZ();

        // DateTime dateTime = new DateTime(time, TZ);

//        fill(0);
//        rect(0, 0, 700, 42 + 8);
//        fill(6);
//        text(dateTime.toString(), 10, 42);

        //background(x + 3, y + 3, z + 3);

        // "To the color XA!"
        float colorX = Math.abs(x * 6);
        float colorY = Math.abs(y * 6);
        float colorZ = Math.abs(z * 6);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("fill: " + Joiner.on(", ").join(
                    Float.toString(colorX),
                    Float.toString(colorY),
                    Float.toString(colorZ)));
        }
        fill(colorX, colorY, colorZ);

        // "Increase the maximum resonator three more levels!"
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stroke: " + Joiner.on(", ").join(
                    Float.toString(colorX),
                    Float.toString(colorY),
                    Float.toString(colorZ)));
        }
        stroke(colorZ, colorX, colorY);

        this.posX = move(this.posX, x, 0, this.displayWidth);
        this.posY = move(this.posY, y, 0, this.displayHeight);
        this.posZ += z;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("pos: " + Joiner.on(", ").join(
                    Float.toString(this.posX),
                    Float.toString(this.posY),
                    Float.toString(this.posZ)));
        }
        translate(this.posX, this.posY, this.posZ);

        float rotation = Math.max(Math.max(x, y), z);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("rotate: " + rotation);
        }
        rotate(rotation);

        float sizeX = Math.abs(50F * x);
        float sizeY = Math.abs(50F * y);
        float sizeZ = Math.abs(50F * z);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("box: " + Joiner.on(", ").join(
                    Float.toString(sizeX),
                    Float.toString(sizeY),
                    Float.toString(sizeZ)));
        }
        box(sizeX, sizeY, sizeZ);
    }

    private static float move(float sum, float delta, int min, int max) {
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
