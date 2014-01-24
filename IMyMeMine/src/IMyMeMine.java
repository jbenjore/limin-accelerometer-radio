import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.limn.accelerometer_radio.AccelerometerData;

import com.google.common.collect.Maps;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;

/**
 * A channel-switching music track.
 *
 * @author Josh ben Jore
 */
public class IMyMeMine extends AccelerometerSketch {
    private final static Logger LOGGER = Logger.getLogger(IMyMeMine.class);
    private static final double SMOOTHNESS = 0.1D;

    private static enum Players {
        POLYSICS,
        PIAF,
        QUEEN,
        STATIC
    }

    private static Map<Players, String> FILES = Maps.newEnumMap(Players.class);
    static {
        FILES.put(Players.POLYSICS, "C:\\Users\\Josh\\Downloads\\tumblr_lrj5m5M5Oh1qk2yqto1.mp3");
        FILES.put(Players.PIAF, "C:\\Users\\Josh\\Downloads\\EdithPiafNonjeneregretterie.mp3");
        FILES.put(Players.QUEEN, "C:\\Users\\Josh\\Downloads\\Queen-I Want to Ride My Bicycle.mp3");
        FILES.put(Players.STATIC, "C:\\Users\\Josh\\Downloads\\tv-static-01.mp3");
    }

    private Minim minim;
    private Map<Players, AudioPlayer> players;
    private double gxd, gyd, gzd;
    private boolean switching;
    private boolean prevX, prevY, prevZ;
    private AudioPlayer staticNoise;
    private Thread switchingThread;
    private long prevTime;

    @Override
    public void setup() {
        minim = new Minim(this);
        players = Maps.newEnumMap(Players.class);
        for (Entry<Players, String> entry : FILES.entrySet()) {
            AudioPlayer player = minim.loadFile(entry.getValue());
            if (entry.getKey() == Players.STATIC) {
                staticNoise = player;
            }
            else {
                player.mute();
                player.loop();
            }
            players.put(entry.getKey(), player);
        }
        for (AudioPlayer player : players.values()) {
            if (player != staticNoise) {
                player.play();
            }
        }

        gxd = gyd = gzd = 0D;

//        playTranscript(
//                "C:\\Users\\Josh\\Desktop\\RadioCapture-1389754237019.csv",
//                DateTime.parse("2014-01-14T19:44:00.000-08"),
//                24);
        playRadio(
                "COM7",
                9600,
                "C:\\Users\\Josh\\Desktop\\RadioCapture-" + System.currentTimeMillis()+".csv");
    }

    @Override
    public void draw() {
        AccelerometerData data = take();

        // Occasionally log something to convince me that it's working.
        if (data.getTime() > prevTime + 1000L) {
            LOGGER.info(data);
        }
        prevTime = data.getTime();

        double xd = data.getX();
        double yd = data.getY();
        double zd = data.getZ();

        // Retrieve only smoothed values.
        gxd = smooth(gxd, xd);
        gyd = smooth(gyd, yd);
        gzd = smooth(gzd, zd);

        // Find dominating dimension.
        double sum = Math.abs(gxd) + Math.abs(gyd) + Math.abs(gzd);
        float x = (float) Math.abs(gxd / sum);
        float y = (float) Math.abs(gyd / sum);
        float z = (float) Math.abs(gzd / sum);

        if (x > Math.max(y,  z)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("PIAF x:" + x + " y:" + y + " z:" + z);
            }
            if (!prevX) {
                prevX = true;
                prevY = prevZ = false;
                flipChannel(Players.PIAF);
            }
        }
        else if (y > Math.max(x, z)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("POLYSICS y:" + y + " x:" + x + " z:" + z);
            }
            if (!prevY) {
                prevY = true;
                prevX = prevZ = false;
                flipChannel(Players.POLYSICS);
            }
        }
        else if (z >= Math.max(x, y)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("QUEEN z:" + z + " x:" + x + " y:" + y);
            }
            if (!prevZ) {
                prevZ = true;
                prevX = prevY = false;
                flipChannel(Players.QUEEN);
            }
        }
    }

    private double smooth(double gd, double d) {
        return (1D - SMOOTHNESS)*gd + SMOOTHNESS*d;
    }

    private void flipChannel(final Players toPlay) {
        if (switching) {
            LOGGER.debug("Tried to switch to " + toPlay + " but still switching");
            return;
        }
        switching = true;
        final AudioPlayer playerToUnMute = players.get(toPlay);
        for (AudioPlayer player : players.values()) {
            if (player == staticNoise) {
            }
            else if (player != playerToUnMute) {
                player.mute();
            }
        }
        LOGGER.info(Players.STATIC);
        staticNoise.cue(0);
        staticNoise.play();
        switchingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (staticNoise.isPlaying()) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        LOGGER.error(e);
                    }
                }
                LOGGER.info(toPlay);
                playerToUnMute.unmute();
                if (playerToUnMute.position() >= playerToUnMute.length()) {
                    LOGGER.info("Rewind");
                    playerToUnMute.cue(0);
                }
                switching = false;
            }
        });
        switchingThread.start();
    }

    @Override
    public void stop() {
        for (AudioPlayer player : players.values()) {
            player.close();
        }
        minim.stop();
        super.stop();
    }
}
