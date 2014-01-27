import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import limn.radio.AccelerometerRawData;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Doubles;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;

/**
 * A channel-switching music track.
 *
 * @author Josh ben Jore
 */
public class IMyMeMine extends AccelerometerSketch {
    private static enum Dimension {
        XA, YA, ZA
    }
    private static class RankablePlayer {
        private final Dimension dimension;
        private final AudioPlayer player;
        private final Song song;
        private double magnitude;
        public RankablePlayer(Dimension dimension, AudioPlayer player, Song song, double magnitude) {
            this.dimension = dimension;
            this.player = player;
            this.song = song;
            this.magnitude = magnitude;
        }

        public Dimension getDimension() {
            return this.dimension;
        }

        public double getMagnitude() {
            return this.magnitude;
        }

        public AudioPlayer getPlayer() {
            return this.player;
        }

        public Song getSong() {
            return this.song;
        }

        public void setMagnitude(double magnitude) {
            this.magnitude = magnitude;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("d", this.dimension)
                    .add("m", this.magnitude)
                    .add("s", this.song)
                    .add("player", this.player)
                    .toString();
        }
    }
    private static class Song {
        private String file;
        private String name;
        public Song(String name, String file) {
            this.name = name;
            this.file = file;
        }
        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("name", this.name)
                    .add("file", this.file)
                    .toString();
        }
        public String getName() {
            return this.name;
        }
        public String getFile() {
            return this.file;
        }
    }
    private static final double SMOOTHNESS = 0.1D;
    private static final long DELAY = 7000L;
    private static final boolean PLAYSTATIC = false;

    private static final String XDIR = "Louis Armstrong\\The Best Of Louis Armstrong Vol 1";

    private static final String YDIR = "Ruth Wallis\\Boobs [Explicit]";
    private static final String ZDIR = "Jimi Hendrix\\Are You Experienced [+video]";

    private static final double INITIAL_MAGNITUDE = 0D;

    private static String MUSICDIR = "C:\\Users\\josh\\Music\\Amazon MP3\\";
    private static String STATIC_FILE = "C:\\Users\\Josh\\Downloads\\tv-static-01.mp3";
    private static Map<Dimension, Song> SONGS = Maps.newEnumMap(Dimension.class);
    static {
        SONGS.put(Dimension.XA, randomSongInDir(MUSICDIR + XDIR));
        SONGS.put(Dimension.YA, randomSongInDir(MUSICDIR + YDIR));
        SONGS.put(Dimension.ZA, randomSongInDir(MUSICDIR + ZDIR));
    }

    private static Song randomSongInDir(String dirName) {
        File dir = new File(dirName);
        String[] mp3s = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(
                    @SuppressWarnings("hiding") File dir,
                    String name) {
                return name.matches(".+\\.mp3");
            }
        });
        int idx = (int) (Math.random() * mp3s.length);
        String mp3 = mp3s[idx];
        return new Song(mp3, dirName + "\\" + mp3);
    }
    private Minim minim;
    private AudioPlayer staticPlayer;
    private Map<Dimension, RankablePlayer> songPlayers;
    private Map<Dimension, Double> smoothed;
    private RankablePlayer currentPlayer;
    private long postSwitchDeadline;

    private static final Comparator<Dimension> DIMENSION_ORDERING = Ordering.explicit(Dimension.XA, Dimension.YA, Dimension.ZA);
    private static final Comparator<RankablePlayer> MAGNITUDE_COMPARATOR = new Ordering<RankablePlayer>() {
        @Override
        public int compare(RankablePlayer left, RankablePlayer right) {
            return Doubles.compare(left.getMagnitude(), right.getMagnitude());
        }
    }.reverse();
    private static final Ordering<RankablePlayer> PLAYER_COMPARATOR = Ordering.compound(ImmutableList.<Comparator<RankablePlayer>>of(
            MAGNITUDE_COMPARATOR,
            new Ordering<RankablePlayer>() {
                @SuppressWarnings("synthetic-access")
                @Override
                public int compare(RankablePlayer left, RankablePlayer right) {
                    return DIMENSION_ORDERING.compare(
                            left.getDimension(),
                            right.getDimension());
                }
            }));

    @Override
    public void draw() {
        AccelerometerRawData data = take().getRawData();

        /**
         * NOPE!
         *   | -3G | -2G | -1G |   0G  |  1G |  2G |  3G |
         *   +-----+-----+-----+-------+-----+-----+-----+
         * x |     |     | 148 | (184) | 221 |     |     | range(-1..1): 73
         * y |     |     | 146 | (184) | 222 |     |     | range(-1..1): 76
         * z |     |     | 154 | (192) | 230 |     |     | range(-1..1): 76
         */

        /**
         *   | -3G | -2G | -1G |    0G   |  1G |  2G |  3G |
         *   +-----+-----+-----+---------+-----+-----+-----+
         * x |     |     | 231 | (288.5) | 346 |     |     | range(-1..1): 57.5
         * y |     |     | 230 | (289)   | 348 |     |     | range(-1..1): 59
         * z |     |     | 242 | (300)   | 358 |     |     | range(-1..1): 58
         */
        int x = data.getX();
        int y = data.getY();
        int z = data.getZ();

        double xd = (x - 288.5D) / 57.5D;
        double yd = (y - 289D) / 59D;
        double zd = (z - 300D) / 58D;

        this.smoothed.put(Dimension.XA, Double.valueOf(smooth(this.smoothed.get(Dimension.XA).doubleValue(), xd)));
        this.smoothed.put(Dimension.YA, Double.valueOf(smooth(this.smoothed.get(Dimension.YA).doubleValue(), yd)));
        this.smoothed.put(Dimension.ZA, Double.valueOf(smooth(this.smoothed.get(Dimension.ZA).doubleValue(), zd)));

        if (this.frameCount < 30) {
            return;
        }

        // Remove and close songs that have ended.
        boolean endedCurrent = false;
        for (Entry<Dimension, RankablePlayer> entry : this.songPlayers.entrySet()) {
            RankablePlayer rPlayer = entry.getValue();
            AudioPlayer player = rPlayer.getPlayer();
            if (player.position() >= player.length() || !player.isPlaying()) {
                LOGGER.info("Player " + rPlayer.getSong().getName() + " ended");
                player.close();
                this.songPlayers.remove(entry.getKey());
                // Unhook the current player.
                if (rPlayer == this.currentPlayer) {
                    LOGGER.info("Also ended current player");
                    this.currentPlayer = null;
                    endedCurrent = true;
                }
            }
        }
        if (endedCurrent && !this.songPlayers.isEmpty()) {
            this.currentPlayer = this.songPlayers.values().iterator().next();
            this.currentPlayer.getPlayer().unmute();
        }

        // Nothing happens while static is playing.
        if (this.staticPlayer.isPlaying()) {
            if (this.staticPlayer.position() >= 500) {
                this.staticPlayer.pause();
                this.staticPlayer.cue(0);
            }
            return;
        }

        // Only rank when there are >1 players.
        switch (this.songPlayers.size()) {
        case 0:
            LOGGER.info("Stopping");
            stop();
            return;
        case 1:
            return;
        default:
        }

        long now = System.currentTimeMillis();
        if (this.currentPlayer != null && this.currentPlayer.getPlayer().isPlaying()) {
            if (0L == this.postSwitchDeadline) {
                // For the next three seconds, play the selected audio or something
                // else if it isn't available
                this.postSwitchDeadline = now + DELAY;
                AudioPlayer player = this.currentPlayer.getPlayer();
                if (player.isMuted()) {
                    LOGGER.info(this.currentPlayer.getSong().getName());
                    player.unmute();
                }
                return;
            }
            else if (now < this.postSwitchDeadline) {
                // We're inside the three second grace period so don't switch
                // away.
                drawFadingBackground(now);
                drawColorBars(
                        this.smoothed.get(Dimension.XA).doubleValue(),
                        this.smoothed.get(Dimension.YA).doubleValue(),
                        this.smoothed.get(Dimension.ZA).doubleValue(),
                        1D);
                drawColorBars(xd, yd, zd, 0.5D);
                return;
            }
        }

        background(0);
        drawColorBars(
                this.smoothed.get(Dimension.XA).doubleValue(),
                this.smoothed.get(Dimension.YA).doubleValue(),
                this.smoothed.get(Dimension.ZA).doubleValue(),
                1D);
        drawColorBars(xd, yd, zd, 0.5D);

        // Either the selected player went away or we're free to pick a new player.
        // Rank them and pick one.
        Collection<RankablePlayer> rankablePlayers = this.songPlayers.values();

        // Update player smoothed.
        for (Entry<Dimension, RankablePlayer> entry : this.songPlayers.entrySet()) {
            Dimension dimension = entry.getKey();
            RankablePlayer rPlayer = entry.getValue();
            double magnitude = this.smoothed.get(dimension).doubleValue();
            rPlayer.setMagnitude(magnitude);
        }

        // Rank the players and possibly flip.
        List<RankablePlayer> rankedPlayers = PLAYER_COMPARATOR.sortedCopy(rankablePlayers);
        final RankablePlayer selectedPlayer = rankedPlayers.get(0);
        if (selectedPlayer == this.currentPlayer) {
            return;
        }
        else if (this.currentPlayer == null) {
            LOGGER.info(selectedPlayer.getSong().getName());
            selectedPlayer.getPlayer().unmute();
            this.currentPlayer = selectedPlayer;
        }
        else {
            this.currentPlayer.getPlayer().mute();
            this.currentPlayer = selectedPlayer;
            this.postSwitchDeadline = 0L;
            if (PLAYSTATIC) {
                this.staticPlayer.play(0);
            }
            background(1F, 1F, 1F);
        }
    }

    private void drawColorBars(double xd, double yd, double zd, double w) {
        double barHeight = this.displayHeight - 100D;

        // Each bar is up to 3Gs tall, either positive or negative.
        int xHeight = (int) Math.floor((Math.max(Math.min(Math.abs(xd), 3D), 0D) / 3D) * barHeight); 
        int yHeight = (int) Math.floor((Math.max(Math.min(Math.abs(yd), 3D), 0D) / 3D) * barHeight); 
        int zHeight = (int) Math.floor((Math.max(Math.min(Math.abs(zd), 3D), 0D) / 3D) * barHeight); 

        int zeroLine = 100 + (int) Math.floor((this.displayHeight - 100D) / 2D);

        int xStart = xd > 0 ? zeroLine : zeroLine - xHeight;
        int yStart = yd > 0 ? zeroLine : zeroLine - yHeight;
        int zStart = zd > 0 ? zeroLine : zeroLine - zHeight;

        double third = oneThirdWidth(1D);
        double middle = third / 2D;
        double diff = middle * (1D - w);

        stroke(0);
        fill(1F, 0.5F, 0F);
        rect((float) diff, xStart, (float) (third * w), xHeight);

        fill(1F, 1F, 0F);
        rect((float) (third + diff), yStart, (float) (third * w), yHeight);

        fill(0.2941176470588235F, 0, 0.5098039215686275F);
        rect((float) ((2D * third) + diff), zStart, (float) (third * w), zHeight);

        // Write *current* values
        fill(0);
        stroke(0);
        text(String.format("%.1f", Double.valueOf(xd)), 30F + (float) diff, (xd > 0 ? 30F : -30F) + xStart);
        text(String.format("%.1f", Double.valueOf(yd)), 30F + (float) (third + diff), (yd > 0 ? 30F : -30F) + yStart);
        text(String.format("%.1f", Double.valueOf(zd)), 30F + (float) ((2D * third) + diff), (zd > 0 ? 30F : -30F) + zStart);
    }

    private void drawFadingBackground(long now) {
        long gray = this.postSwitchDeadline - now;
        float delta = (float) gray / (float) DELAY;
        fill(1F, 1F, 1F);
        background(delta, delta, delta);
    }

    private double oneThirdWidth(double w) { 
        return (((this.displayWidth) - 10D) / 3D) * w;
    }

    @Override
    public void setup() {
        this.minim = new Minim(this);
        this.staticPlayer = this.minim.loadFile(STATIC_FILE);
        this.songPlayers = Maps.<Dimension, RankablePlayer>newEnumMap(Dimension.class);
        this.smoothed = Maps.<Dimension, Double>newEnumMap(Dimension.class);
        for (Dimension dimension : Dimension.values()) {
            Song song = SONGS.get(dimension);
            AudioPlayer player = this.minim.loadFile(song.getFile());
            player.mute();
            player.play();
            this.songPlayers.put(dimension, new RankablePlayer(
                    dimension, player, song, INITIAL_MAGNITUDE));
            this.smoothed.put(dimension, Double.valueOf(INITIAL_MAGNITUDE));
        }
        this.currentPlayer = null;
        accelerometerData();
        size(this.displayWidth - 5, this.displayHeight - 70);
        colorMode(RGB, 1F);
        textSize(64);
        background(0F, 0F, 0F);
    }

    private static double smooth(double gd, double d) {
        return (1D - SMOOTHNESS)*gd + SMOOTHNESS*d;
    }

    @Override
    public void stop() {
        this.currentPlayer = null;
        this.staticPlayer.close();

        for (Entry<Dimension, RankablePlayer> entry : this.songPlayers.entrySet()) {
            RankablePlayer rPlayer = entry.getValue();
            AudioPlayer player = rPlayer.getPlayer();
            player.close();

            this.songPlayers.remove(entry.getKey());
        }

        this.minim.stop();
        this.minim = null;

        super.stop();
        System.exit(0);
    }
}
