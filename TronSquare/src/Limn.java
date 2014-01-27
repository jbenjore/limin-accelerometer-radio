import java.util.ListIterator;

import limn.radio.DataDumper;
import limn.radio.util.Clock;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.joda.time.ReadableInstant;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

import processing.core.PApplet;


public class Limn {
    final static Logger LOGGER = Logger.getLogger(Limn.class);
    private static final Options OPTIONS = Limn.createOptions();

    public static class Opts {
        private final String[] args;
        public final boolean replay;
        public final String appName;
        public final String port;
        public final int baudRate;
        public final String file;
        public final boolean frameLocked;
        public final ReadableInstant skipTil;
        public Opts(String[] args, boolean replay, String appName, String port,
                int baudRate, String file, ReadableInstant skipTil, boolean frameLocked) {
            this.args = args;
            this.replay = replay;
            this.appName = appName;
            this.port = port;
            this.baudRate = baudRate;
            this.file = file;
            this.skipTil = skipTil;
            this.frameLocked = frameLocked;
        }
        @Override
        public String toString() {
            ToStringHelper h = Objects.toStringHelper(this);
            if (this.args.length > 0) {
                h.add("args", this.args);
            }
            if (this.replay) {
                h.add("replay", true);
            }
            if (this.appName != null) {
                h.add("appName", this.appName);
            }
            if (this.port != null) {
                h.add("port", this.port);
            }
            if (this.baudRate != 0) {
                h.add("baudRate", this.baudRate);
            }
            if (this.file != null) {
                h.add("file", this.file);
            }
            if (this.skipTil != null) {
                h.add("skipTil", this.skipTil);
            }
            if (this.frameLocked) {
                h.add("frameLocked", this.frameLocked);
            }
            return h.toString();
        }
    }
    
    public static Opts parseArgs(String[] args) throws ParseException {
        GnuParser parser = new GnuParser() {
            @Override
            protected void processOption(String arg, ListIterator iter) throws ParseException {
                if (getOptions().hasOption(arg)) {
                    super.processOption(arg, iter);
                }
            }
        };
        CommandLine parse = parser.parse(OPTIONS, args);
        return new Opts(
                parse.getArgs(),
                parse.hasOption("replay"),
                parse.getOptionValue("app"),
                parse.getOptionValue("port", "COM7"),
                Integer.parseInt(parse.getOptionValue("baudRate", "9600")),
                parse.getOptionValue("file"),
                parse.hasOption("skip") ? Instant.parse(parse.getOptionValue("skip")) : null,
                Boolean.parseBoolean(parse.getOptionValue("frameLocked", "false")));
    }
    
    public static void main(String[] args) throws ParseException {
        Opts opts = parseArgs(args);
        LOGGER.warn(opts);
        if (opts.replay) {
            if (opts.appName != null) {
                PApplet.main(opts.appName, args);
            }
            else {
                DataDumper.replay(
                        opts.file != null ? opts.file : "C:\\Users\\josh\\Desktop\\RadioCapture-" + System.currentTimeMillis() + ".csv",
                        opts.frameLocked);
            }
        }
        else {
            if (opts.appName != null) {
                PApplet.main(opts.appName, args);
            }
            else {
                DataDumper.capture(
                        opts.port,
                        opts.baudRate,
                        opts.file != null ? opts.file : "C:\\Users\\josh\\Desktop\\RadioCapture-" + System.currentTimeMillis() + ".csv",
                        Clock.systemUTC());
            }
        }
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addOption(new Option("app", true, "app"));
        options.addOption(new Option("file", true, "file"));
        options.addOption(new Option("port", true, "XBee port"));
        options.addOption(new Option("baudRate", true, "XBee baud rate"));
        options.addOption(new Option("replay", false, "replay"));
        options.addOption(new Option("skipTil", true, "skip til"));
        options.addOption(new Option("frameLocked", false, "replay at normal speed"));
        return options;
    }
}
