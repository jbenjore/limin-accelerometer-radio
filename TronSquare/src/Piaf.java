import java.io.File;
import java.io.FilenameFilter;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import processing.core.PConstants;
import limn.radio.AccelerometerRawData;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;

/**
 * A channel-switching music track.
 * 
 * @author Josh ben Jore
 */
public class Piaf extends AccelerometerSketch {
  private static enum Dimension {
    XA, YA, ZA
  }

  private static enum Direction {
    UPA, SIDEWAYSA, DOWNA
  }

  private static final int SONG_BAR_HEIGHT = 100;
  private static final Map<Direction, String> SONG_DIRS = Maps
      .newEnumMap(Direction.class);
  static {
    SONG_DIRS
        .put(
            Direction.UPA,
            "C:\\Users\\josh\\Music\\Amazon MP3\\Philip Glass Ensemble, Yo-Yo Ma, Philip Glass\\Naqoyqatsi (Original Motion Picture Soundtrack)");
    SONG_DIRS.put(Direction.SIDEWAYSA,
        "C:\\Users\\josh\\Music\\Amazon MP3\\Lady Gaga\\The Fame");
    SONG_DIRS
        .put(
            Direction.DOWNA,
            "C:\\Users\\josh\\Music\\Amazon MP3\\The Chemical Brothers\\Hanna (Original Motion Picture Soundtrack)");
  }
  private static String STATIC_FILE = "C:\\Users\\Josh\\Downloads\\tv-static-01.mp3";

  private static class RankablePlayer {
    private final Direction direction;
    private final AudioPlayer player;
    private final Song song;

    private double similarity;
    private double rawSimilarity;

    public RankablePlayer(Direction direction, AudioPlayer player, Song song) {
      this.direction = direction;
      this.player = player;
      this.song = song;
    }

    public AudioPlayer getPlayer() {
      return this.player;
    }

    public Song getSong() {
      return this.song;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).add("d", this.direction)
          .add("s", this.song).add("player", this.player).toString();
    }

    public void setSimilarity(double similarity) {
      this.similarity = similarity;
    }

    public void setRawSimilarity(double rawSimilarity) {
      this.rawSimilarity = rawSimilarity;
    }

    public double getSimilarity() {
      return this.similarity;
    }

    public double getRawSimilarity() {
      return this.rawSimilarity;
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
      return Objects.toStringHelper(this).add("name", this.getName())
          .add("file", this.file).toString();
    }

    private static final Pattern PREFIX_RX = Pattern.compile("^[^a-zA-Z]+");
    private static final Pattern SUFFIX_RX = Pattern.compile("\\.mp3$");

    public String getName() {
      String a = PREFIX_RX.matcher(this.name).replaceFirst("");
      return SUFFIX_RX.matcher(a).replaceFirst("");
    }

    public String getFile() {
      return this.file;
    }
  }

  private static Map<Direction, Song> SONGS = Maps.newEnumMap(Direction.class);
  static {
    randomizeSongs();
  }

  private static void randomizeSongs() {
    for (Direction direction : Direction.values()) {
      SONGS.put(direction, randomSongInDir(SONG_DIRS.get(direction)));
    }
  }

  private static Song randomSongInDir(String dirName) {
    File dir = new File(dirName);
    String[] mp3s = dir.list(new FilenameFilter() {
      @Override
      public boolean accept(@SuppressWarnings("hiding") File dir, String name) {
        return name.matches(".+\\.mp3")
            && name
                .matches(".+(escape wavefold|Poker Face|Primacy of Number).+");
      }
    });
    int idx = (int) (Math.random() * mp3s.length);
    String mp3 = mp3s[idx];
    return new Song(mp3, dirName + "\\" + mp3);
  }

  private Minim minim;
  private AudioPlayer staticPlayer;
  private Map<Direction, RankablePlayer> songPlayers;
  private Map<Dimension, Double> rawDimensions;
  private Map<Dimension, Double> smoothedDimensions;
  private RankablePlayer currentPlayer;
  private long postSwitchStarted;
  private boolean playStatic;
  private long delay = 7000L;
  private int smoothness;
  private boolean loopSongs;

  @Override
  public void draw() {
    AccelerometerRawData data = take().getRawData();

    /**
     * | -3G | -2G | -1G | 0G | 1G | 2G | 3G |
     * +-----+-----+-----+---------+-----+-----+-----+ x | | | 231 | (288.5) |
     * 346 | | | range(-1..1): 57.5 y | | | 230 | (289) | 348 | | |
     * range(-1..1): 59 z | | | 242 | (300) | 358 | | | range(-1..1): 58
     */
    int x = data.getX();
    int y = data.getY();
    int z = data.getZ();

    double xd = (x - 288.5D) / 57.5D;
    double yd = (y - 289D) / 59D;
    double zd = (z - 300D) / 58D;

    this.rawDimensions.put(Dimension.XA, Double.valueOf(xd));
    this.rawDimensions.put(Dimension.YA, Double.valueOf(yd));
    this.rawDimensions.put(Dimension.ZA, Double.valueOf(zd));
    this.smoothedDimensions.put(Dimension.XA, Double.valueOf(smooth(
        this.smoothedDimensions.get(Dimension.XA).doubleValue(), xd)));
    this.smoothedDimensions.put(Dimension.YA, Double.valueOf(smooth(
        this.smoothedDimensions.get(Dimension.YA).doubleValue(), yd)));
    this.smoothedDimensions.put(Dimension.ZA, Double.valueOf(smooth(
        this.smoothedDimensions.get(Dimension.ZA).doubleValue(), zd)));

    if (this.frameCount < 30) {
      return;
    }

    // Remove and close songs that have ended.
    boolean endedCurrent = false;
    for (Entry<Direction, RankablePlayer> entry : this.songPlayers.entrySet()) {
      RankablePlayer rPlayer = entry.getValue();
      AudioPlayer player = rPlayer.getPlayer();
      if (player.position() >= player.length() || !player.isPlaying()) {
        LOGGER.info("Player " + rPlayer.getSong().getName() + " ended");

        if (this.loopSongs) {
          if (!player.isPlaying()) {
            player.play(0);
          }
        } else {
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
      background(0);
      drawSongNames();
      drawColorBars(
          this.songPlayers.get(Direction.UPA).getSimilarity(),
          this.songPlayers.get(Direction.SIDEWAYSA).getSimilarity(),
          this.songPlayers.get(Direction.DOWNA).getSimilarity(),
          1D);
      drawColorBars(
          this.songPlayers.get(Direction.UPA).getRawSimilarity(),
          this.songPlayers.get(Direction.SIDEWAYSA).getRawSimilarity(),
          this.songPlayers.get(Direction.DOWNA).getRawSimilarity(),
          0.7D);
      drawLegend();
      return;
    }

    // Only rank when there are >1 players.
    switch (this.songPlayers.size()) {
    case 0:
      LOGGER.info("Stopping");
      stop();
      return;
    case 1:
      background(0);
      drawSongNames();
      drawColorBars(
          this.songPlayers.get(Direction.UPA).getSimilarity(),
          this.songPlayers.get(Direction.SIDEWAYSA).getSimilarity(),
          this.songPlayers.get(Direction.DOWNA).getSimilarity(),
          1D);
      drawColorBars(
          this.songPlayers.get(Direction.UPA).getRawSimilarity(),
          this.songPlayers.get(Direction.SIDEWAYSA).getRawSimilarity(),
          this.songPlayers.get(Direction.DOWNA).getRawSimilarity(),
          0.7D);
      drawLegend();
      return;
    default:
    }

    long now = System.currentTimeMillis();
    if (this.currentPlayer != null
        && this.currentPlayer.getPlayer().isPlaying()) {
      if (0L == this.postSwitchStarted) {
        // For the next three seconds, play the selected audio or something
        // else if it isn't available
        this.postSwitchStarted = now;
        AudioPlayer player = this.currentPlayer.getPlayer();
        if (player.isMuted()) {
          LOGGER.info(this.currentPlayer.getSong().getName());
          player.unmute();
        }
        return;
      } else if (now < (this.postSwitchStarted + this.delay)) {
        // We're inside the three second grace period so don't switch
        // away.
        background(0);
        drawSongNames();
        // drawColorBars(
        // this.songPlayers.get(Direction.UPA).getSimilarity(),
        // this.songPlayers.get(Direction.SIDEWAYSA).getSimilarity(),
        // this.songPlayers.get(Direction.DOWNA).getSimilarity(),
        // 1D);
        // drawColorBars(
        // this.songPlayers.get(Direction.UPA).getRawSimilarity(),
        // this.songPlayers.get(Direction.SIDEWAYSA).getRawSimilarity(),
        // this.songPlayers.get(Direction.DOWNA).getRawSimilarity(),
        // 0.7D);
        drawLegend();
        return;
      }
    }

    background(0);
    drawSongNames();
    drawColorBars(this.songPlayers.get(Direction.UPA).getSimilarity(),
        this.songPlayers.get(Direction.SIDEWAYSA).getSimilarity(),
        this.songPlayers.get(Direction.DOWNA).getSimilarity(), 1D);
    drawColorBars(this.songPlayers.get(Direction.UPA).getRawSimilarity(),
        this.songPlayers.get(Direction.SIDEWAYSA).getRawSimilarity(),
        this.songPlayers.get(Direction.DOWNA).getRawSimilarity(), 0.7D);
    drawLegend();

    // Either the selected player went away or we're free to pick a new player.
    // Rank them and pick one.

    double smoothedHorizontal = Math.max(
        Math.abs(this.smoothedDimensions.get(Dimension.XA).doubleValue()),
        Math.abs(this.smoothedDimensions.get(Dimension.ZA).doubleValue()));
    double smoothedVertical = this.smoothedDimensions.get(Dimension.YA)
        .doubleValue();
    double rawVertical = this.rawDimensions.get(Dimension.YA).doubleValue();
    double rawHorizontal = Math.max(
        Math.abs(this.rawDimensions.get(Dimension.XA).doubleValue()),
        Math.abs(this.rawDimensions.get(Dimension.ZA).doubleValue()));

    RankablePlayer upPlayer = this.songPlayers.get(Direction.UPA);
    upPlayer.setSimilarity(Math.max(0D, smoothedVertical));
    upPlayer.setRawSimilarity(Math.max(0D, rawVertical));

    RankablePlayer downPlayer = this.songPlayers.get(Direction.DOWNA);
    downPlayer.setSimilarity(Math.max(0D, smoothedVertical));
    downPlayer.setRawSimilarity(Math.max(0D, rawVertical));

    RankablePlayer sidewaysPlayer = this.songPlayers.get(Direction.SIDEWAYSA);
    sidewaysPlayer.setSimilarity(smoothedHorizontal);
    sidewaysPlayer.setRawSimilarity(rawHorizontal);

    Direction smoothedOrientation = Piaf.orientation(smoothedHorizontal,
        smoothedVertical);

    // Choose a players and possibly flip.
    RankablePlayer selectedPlayer = this.songPlayers.get(smoothedOrientation);
    if (selectedPlayer == this.currentPlayer) {
      return;
    } else if (this.currentPlayer == null) {
      LOGGER.info(selectedPlayer.getSong().getName());
      selectedPlayer.getPlayer().unmute();
      this.currentPlayer = selectedPlayer;
    } else {
      this.currentPlayer.getPlayer().mute();
      this.currentPlayer = selectedPlayer;
      this.postSwitchStarted = 0L;
      if (this.playStatic) {
        this.staticPlayer.play(0);
      }
      background(1F, 1F, 1F);
    }
  }

  private void drawColorBars(double x, double y, double z, double rectWidthPct) {
    double barHeight = this.displayHeight - SONG_BAR_HEIGHT;

    // Each bar is up to 3Gs tall, either positive or negative.
    int xHeight = (int) Math
        .floor((Math.max(Math.min(Math.abs(x), 3D), 0D) / 9D) * barHeight);
    int yHeight = (int) Math
        .floor((Math.max(Math.min(Math.abs(y), 3D), 0D) / 9D) * barHeight);
    int zHeight = (int) Math
        .floor((Math.max(Math.min(Math.abs(z), 3D), 0D) / 9D) * barHeight);

    int zeroLine = SONG_BAR_HEIGHT
        + (int) Math.floor((this.displayHeight - SONG_BAR_HEIGHT) / 2D);

    int xStart = x < 0 ? zeroLine : zeroLine - xHeight;
    int yStart = y < 0 ? zeroLine : zeroLine - yHeight;
    int zStart = z < 0 ? zeroLine : zeroLine - zHeight;

    double third = oneThirdWidth(1D);
    double middle = third / 2D;
    double diff = middle * (1D - rectWidthPct);

    stroke(0);
    fill(1F, 0.5F, 0F);
    rect((float) diff, xStart, (float) (third * rectWidthPct), xHeight);

    fill(1F, 1F, 0F);
    rect((float) (third + diff), yStart, (float) (third * rectWidthPct),
        yHeight);

    fill(0.2941176470588235F, 0, 0.5098039215686275F);
    rect((float) ((2D * third) + diff), zStart, (float) (third * rectWidthPct),
        zHeight);

    // // Write *current* values
    // fill(0);
    // stroke(0);
    // text(String.format("%.1f", Double.valueOf(xd)), 30F + (float) diff, (xd >
    // 0 ? 30F : -30F) + xStart);
    // text(String.format("%.1f", Double.valueOf(yd)), 30F + (float) (third +
    // diff), (yd > 0 ? 30F : -30F) + yStart);
    // text(String.format("%.1f", Double.valueOf(zd)), 30F + (float) ((2D *
    // third) + diff), (zd > 0 ? 30F : -30F) + zStart);
  }

  private static Direction orientation(double horizontal, double vertical) {
    double absVertical = Math.abs(vertical);
    if (horizontal > absVertical) {
      return Direction.SIDEWAYSA;
    } else if (vertical >= 0) {
      return Direction.UPA;
    } else {
      return Direction.DOWNA;
    }
  }

  @Override
  public boolean sketchFullScreen() {
    return true;
  }

  private void drawLegend() {
    stroke(1);
    fill(1);
    String legend = "\u2191 \u2193: "
        + (this.delay > 0 ? new DecimalFormat("#.#").format(Float
            .valueOf(this.delay / 1000F)) + " seconds" : "no") + " delay   "
        + "\u2190 \u2192: " + Integer.toString(100 - this.smoothness)
        + "% smoothness   "
        + (this.playStatic ? "SPC: static   " : "SPC: no static   ") + "L: "
        + (this.loopSongs ? "looping" : "play once");
    text(legend, 30, this.displayHeight - 30);
  }

  private void drawSongNames() {
    // fill(0);
    // stroke(0);
    // rect(0F, 0F, 100F, this.displayWidth);

    drawSongName(Direction.UPA, 0F, 0F, (float) oneThirdWidth(1D),
        SONG_BAR_HEIGHT);
    drawSongName(Direction.SIDEWAYSA, (float) oneThirdWidth(1D), 0F,
        (float) (2D * oneThirdWidth(1D)), SONG_BAR_HEIGHT);
    drawSongName(Direction.DOWNA, (float) (2D * oneThirdWidth(1D)), 0F,
        this.displayWidth, SONG_BAR_HEIGHT);
  }

  private void drawSongName(Direction direction, float a, float b, float c,
      float d) {
    RankablePlayer rPlayer = this.songPlayers.get(direction);
    if (rPlayer == null) {
      return;
    }
    String song = rPlayer.getSong().getName();
    final int bgColor, fgColor;
    if (rPlayer.getPlayer().isMuted()) {
      bgColor = 0;
      fgColor = 1;
    } else {
      bgColor = 1;
      fgColor = 0;
    }

    stroke(fgColor);
    fill(bgColor);
    rect(a, b, c, d);

    fill(fgColor);
    stroke(fgColor);
    textSize(20);
    text(song, a + 5F, b + 30F);

    long wait = (this.postSwitchStarted + this.delay)
        - System.currentTimeMillis();
    if (!rPlayer.getPlayer().isMuted() && wait > 0L) {
      float radius = SONG_BAR_HEIGHT / 2F;
      float e = c - radius;
      float f = d - radius;
      ellipse(e, f, SONG_BAR_HEIGHT, SONG_BAR_HEIGHT);

      fill(bgColor);
      stroke(bgColor);

      float percentile = (float) wait / (float) this.delay;
      if (1F >= percentile && percentile > 0.875F) {
        triangle(c - radius, b, c - radius, b + radius,
            c - (radius * norm(percentile, 0.875F, 1F)), b);
      } else if (0.875F >= percentile && percentile > 0.75F) {
        triangle(c - radius, b, c - radius, b + radius, c, b);
        triangle(c - radius, b + radius, c, b, c,
            b + (radius * (1F - norm(percentile, 0.75F, 0.875F))));
      } else if (0.75F >= percentile && percentile > 0.625F) {
        rect(c - radius, b, radius, radius);
        triangle(c - radius, b + radius, c, b + radius, c, b + radius
            + (radius * (1F - norm(percentile, 0.625F, 0.75F))));
      } else if (0.625F >= percentile && percentile > 0.5F) {
        rect(c - radius, b, radius, radius);
        triangle(c - radius, b + radius, c, b + radius, c, d);
        triangle(c - radius, b + radius, c, d,
            c - (radius * (1F - norm(percentile, 0.5F, 0.625F))), d);
      } else if (0.5F >= percentile && percentile > 0.375F) {
        rect(c - radius, b, d, d);
        triangle(c - radius, b + radius, c - radius, d, c - radius
            - (radius * (1F - norm(percentile, 0.375F, 0.5F))), d);
      } else if (0.375 >= percentile && percentile > 0.25F) {
        rect(c - radius, b, d, d);
        triangle(c - radius, b + radius, c - radius, d, c - d, d);
        triangle(c - radius, b + radius, c - d, d, c - d, d
            - (radius * (1F - norm(percentile, 0.25F, 0.375F))));
      } else if (0.25F >= percentile && percentile > 0.125F) {
        rect(c - radius, b, d, d);
        rect(c - d, b + radius, radius, radius);
        triangle(c - d, b + radius, c - radius, b + radius, c - d, b
            + (radius * (norm(percentile, 0.125F, 0.25F))));
      } else {
        rect(c - radius, b, d, d);
        rect(c - d, b + radius, radius, radius);
        triangle(c - d, b + radius, c - radius, b + radius, c - d, b);
        triangle(c - d, b, c - radius, b + radius, c - radius
            - (radius * (norm(percentile, 0F, 0.125F))), b);
      }
    }
  }

  private double oneThirdWidth(double w) {
    return (((this.displayWidth) - 10D) / 3D) * w;
  }

  @Override
  public void setup() {
    this.minim = new Minim(this);
    this.staticPlayer = this.minim.loadFile(STATIC_FILE);
    this.songPlayers = Maps
        .<Direction, RankablePlayer> newEnumMap(Direction.class);
    this.smoothedDimensions = Maps
        .<Dimension, Double> newEnumMap(Dimension.class);
    this.rawDimensions = Maps.<Dimension, Double> newEnumMap(Dimension.class);
    this.delay = 2000L;
    this.smoothness = 30;
    this.playStatic = true;
    this.loopSongs = true;

    for (Direction direction : Direction.values()) {
      Song song = SONGS.get(direction);
      AudioPlayer player = this.minim.loadFile(song.getFile());
      player.mute();
      player.play();
      this.songPlayers.put(direction, new RankablePlayer(direction, player,
          song));
    }
    for (Dimension dimension : Dimension.values()) {
      this.rawDimensions.put(dimension, Double.valueOf(0D));
      this.smoothedDimensions.put(dimension, Double.valueOf(0D));
    }
    this.currentPlayer = null;
    accelerometerData();
    size(this.displayWidth, this.displayHeight);
    colorMode(RGB, 1F);
    textSize(20F);
    background(0F, 0F, 0F);
  }

  private double smooth(double gd, double d) {
    double s = this.smoothness / 100D;
    return (1D - s) * gd + s * d;
  }

  @Override
  public void stop() {
    this.currentPlayer = null;
    this.staticPlayer.close();

    for (Entry<Direction, RankablePlayer> entry : this.songPlayers.entrySet()) {
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

  @Override
  public void keyPressed() {
    switch (this.key) {
    case PConstants.CODED:
      switch (this.keyCode) {
      case PConstants.UP:
        this.delay += 500L;
        break;
      case PConstants.DOWN:
        this.delay = Math.max(this.delay - 500L, 0L);
        break;
      case PConstants.LEFT:
        this.smoothness = Math.min(this.smoothness + 5, 100);
        break;
      case PConstants.RIGHT:
        this.smoothness = Math.max(this.smoothness - 5, 0);
        break;
      default:
        return;
      }
      break;
    case 'l':
      this.loopSongs = !this.loopSongs;
      break;
    case ' ':
      this.playStatic = !this.playStatic;
      break;
    default:
    }
  }
}
