package limn.radio;

import static com.google.common.base.Preconditions.checkNotNull;
import limn.radio.util.Clock;

import org.apache.log4j.Logger;

public class DataDumper {
    private final static Logger LOGGER = Logger.getLogger(DataDumper.class);
    public static void replay(final String fileName, boolean frameLocked) {
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
