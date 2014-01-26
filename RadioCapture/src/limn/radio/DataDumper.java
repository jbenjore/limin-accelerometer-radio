package limn.radio;

import static com.google.common.base.Preconditions.checkNotNull;
import limn.radio.util.Clock;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

// TODO: Rename CommandLine
public class DataDumper {
    private final static Logger LOGGER = Logger.getLogger(DataDumper.class);
    private static final Clock CLOCK = Clock.systemUTC();

    public static void replay(final String fileName, boolean timeLocked) {
        checkNotNull(fileName, "fileName");
        FileDataSource iterator = new FileDataSource(fileName);
        play(iterator);
    }

    private static void play(IDataSource iterator) {
        checkNotNull(iterator, "iterator");
        while (iterator.hasNext()) {
            AccelerometerData data = iterator.next();
            LOGGER.info(data);
        }
    }

    public static void capture(String port, int baudRate, String fileName, Clock clock) {
        RadioDataSource iterator = new RadioDataSource(port, baudRate, fileName, clock);
        play(iterator);
    }
}
