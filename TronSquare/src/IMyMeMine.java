import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

import javax.media.nativewindow.util.Dimension;

import limn.radio.AccelerometerData;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Instant;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;

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
    private static final double INITIAL_MAGNITUDE = 0D;

    private static enum Dimension {
        X, Y, Z
    }

    private static class Song {
        public Song(String name, String file) {
            this.name = name;
            this.file = file;
        }
        private String file;
        private String name;
        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("name", name)
                    .add("file", file)
                    .toString();
        }
    }

    private static String STATIC_FILE = "C:\\Users\\Josh\\Downloads\\tv-static-01.mp3";
    private static Map<Dimension, Song> SONGS = Maps.newEnumMap(Dimension.class);
    static {
        SONGS.put(Dimension.X,
                new Song("Polysics - I, My, Me, Mine",
                        "C:\\Users\\Josh\\Downloads\\tumblr_lrj5m5M5Oh1qk2yqto1.mp3"));
        SONGS.put(Dimension.Y,
                new Song("Edith Piaf - Non je ne regretterie",
                        "C:\\Users\\Josh\\Downloads\\EdithPiafNonjeneregretterie.mp3"));
        SONGS.put(Dimension.Z,
                new Song("Queen - I want to ride my bicycle",
                        "C:\\Users\\Josh\\Downloads\\Queen-I Want to Ride My Bicycle.mp3"));
    }

    private Minim minim;
    private AudioPlayer staticPlayer;
    private Map<Dimension, RankablePlayer> songPlayers;
    private Map<Dimension, Double> magnitudes;
    private boolean switching;
    private boolean prevX, prevY, prevZ;
    private Thread switchingThread;
    private long prevTime;
    private RankablePlayer currentPlayer;
    private boolean startedStatic;
    private long switchingDeadline;
    private long postSwitchDeadline;

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

        public AudioPlayer getPlayer() {
            return player;
        }

        public void setMagnitude(double magnitude) {
            this.magnitude = magnitude;
        }

        public Song getSong() {
            return song;
        }

        public double getMagnitude() {
            return magnitude;
        }

        public Dimension getDimension() {
            return dimension;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("d", dimension)
                    .add("m", magnitude)
                    .add("s", song)
                    .add("player", player)
                    .toString();
        }
    }

    private static final Comparator<Dimension> DIMENSION_ORDERING = Ordering.explicit(Dimension.X, Dimension.Y, Dimension.Z);
    private static final Comparator<RankablePlayer> MAGNITUDE_COMPARATOR = new Ordering<RankablePlayer>() {
        @Override
        public int compare(RankablePlayer left, RankablePlayer right) {
            return Doubles.compare(left.getMagnitude(), right.getMagnitude());
        }
    };
    private static final Ordering<RankablePlayer> PLAYER_COMPARATOR = Ordering.compound(ImmutableList.<Comparator<RankablePlayer>>of(
            MAGNITUDE_COMPARATOR,
            new Ordering<RankablePlayer>() {
                @Override
                public int compare(RankablePlayer left, RankablePlayer right) {
                    return DIMENSION_ORDERING.compare(
                            left.getDimension(),
                            right.getDimension());
                }
            }));

    @Override
    public void setup() {
        minim = new Minim(this);
        staticPlayer = minim.loadFile(STATIC_FILE);
        songPlayers = Maps.<Dimension, RankablePlayer>newEnumMap(Dimension.class);
        magnitudes = Maps.<Dimension, Double>newEnumMap(Dimension.class);
        for (Dimension dimension : Dimension.values()) {
            Song song = SONGS.get(dimension);
            AudioPlayer player = minim.loadFile(song.file);
            player.mute();
            player.play(180000);
            songPlayers.put(dimension, new RankablePlayer(
                    dimension, player, song, INITIAL_MAGNITUDE));
            magnitudes.put(dimension, INITIAL_MAGNITUDE);
        }
        currentPlayer = null;
        accelerometerData();
    }

    @Override
    public void draw() {
        AccelerometerData data = take();

        // Occasionally log something to convince me that it's working.
        boolean logThis = data.getTime() > prevTime + 1000L;
        prevTime = data.getTime();
        if (logThis) {
            LOGGER.info(data);
        }

        final double xd = data.getX();
        final double yd = data.getY();
        final double zd = data.getZ();

        // Collect smoothed data.
        magnitudes.put(Dimension.X, smooth(magnitudes.get(Dimension.X), xd));
        magnitudes.put(Dimension.Y, smooth(magnitudes.get(Dimension.Y), yd));
        magnitudes.put(Dimension.Z, smooth(magnitudes.get(Dimension.Z), zd));
        if (logThis) {
            LOGGER.info(magnitudes);
        }

        // Remove and close songs that have ended.
        for (Entry<Dimension, RankablePlayer> entry : songPlayers.entrySet()) {
            RankablePlayer rPlayer = entry.getValue();
            AudioPlayer player = rPlayer.getPlayer();
            if (player.position() >= player.length() || !player.isPlaying()) {
                LOGGER.info("Player " + rPlayer.getSong().name + " ended");
                player.close();
                songPlayers.remove(entry.getKey());
                // Unhook the current player.
                if (rPlayer == currentPlayer) {
                    LOGGER.info("Also ended current player");
                    currentPlayer = null;
                }
            }
        }

        // Nothing happens while static is playing.
        if (staticPlayer.isPlaying()) {
            return;
        }

        // Only rank when there are >1 players.
        switch (songPlayers.size()) {
        case 0:
            LOGGER.info("Stopping");
            stop();
            return;
        case 1:
            return;
        default:
        }

        long now = System.currentTimeMillis();
        if (currentPlayer != null && currentPlayer.getPlayer().isPlaying()) {
            if (0L == postSwitchDeadline) {
                // For the next three seconds, play the selected audio or something
                // else if it isn't available
                postSwitchDeadline = now + 3000L;
                AudioPlayer player = currentPlayer.getPlayer();
                LOGGER.info("Unmuting " + currentPlayer.getSong().name);
                player.unmute();
                return;
            }
            else if (now < postSwitchDeadline) {
                // We're inside the three second grace period so don't switch
                // away.
                return;
            }
        }

        // Either the selected player went away or we're free to pick a new player.
        // Rank them and pick one.
        Collection<RankablePlayer> rankablePlayers = songPlayers.values();

        // Update player magnitudes.
        for (Entry<Dimension, RankablePlayer> entry : songPlayers.entrySet()) {
            Dimension dimension = entry.getKey();
            RankablePlayer rPlayer = entry.getValue();
            AudioPlayer player = rPlayer.getPlayer();
            double magnitude = magnitudes.get(dimension);
            rPlayer.setMagnitude(magnitude);
        }

        // Rank the players and possibly flip.
        List<RankablePlayer> rankedPlayers = PLAYER_COMPARATOR.sortedCopy(rankablePlayers);
        if (logThis) {
            LOGGER.info(rankedPlayers);
        }
        final RankablePlayer selectedPlayer = rankedPlayers.get(0);
        if (selectedPlayer == currentPlayer) {
            return;
        }
        else if (currentPlayer == null) {
            LOGGER.info(selectedPlayer.song.name);
            selectedPlayer.getPlayer().unmute();
            currentPlayer = selectedPlayer;
        }
        else {
            LOGGER.info(selectedPlayer.song.name + " -> " + currentPlayer.song.name);
            currentPlayer.getPlayer().mute();
            currentPlayer = selectedPlayer;
            postSwitchDeadline = 0L;
            staticPlayer.cue(0);
            staticPlayer.play();
        }
    }

    private double smooth(double gd, double d) {
        return (1D - SMOOTHNESS)*gd + SMOOTHNESS*d;
    }

    @Override
    public void stop() {
        currentPlayer = null;
        staticPlayer.close();

        for (Entry<Dimension, RankablePlayer> entry : songPlayers.entrySet()) {
            RankablePlayer rPlayer = entry.getValue();
            AudioPlayer player = rPlayer.getPlayer();
            player.close();

            songPlayers.remove(entry.getKey());
        }

        minim.stop();
        minim = null;

        super.stop();
        System.exit(0);
    }
}
