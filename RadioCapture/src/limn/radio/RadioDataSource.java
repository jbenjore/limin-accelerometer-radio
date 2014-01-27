package limn.radio;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

import limn.radio.util.Clock;

import org.apache.log4j.Logger;

import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.XBeeTimeoutException;
import com.rapplogic.xbee.api.zigbee.ZNetRxIoSampleResponse;

public class RadioDataSource extends AbstractIterator<AccelerometerData> implements IDataSource {
    private final static Logger LOGGER = Logger.getLogger(RadioDataSource.class);

    private static class TimestampedIo {
        public final long time;
        public final ZNetRxIoSampleResponse io;
        public TimestampedIo(long time, ZNetRxIoSampleResponse io) {
            this.time = time;
            this.io = io;
        }
    }

    private static final long MODEM_RECONNECT_TIMEOUT = 1000L;
    private final String port;
    private final int baudRate;
    private final String fileName;
    private final Clock clock;

    private Writer writer;
    private XBee xbee;
    private Iterator<AccelerometerData> iterator;
    private boolean reconnecting;

    private Thread radioReader;

    private boolean finalizing;

    public RadioDataSource(String port, int baudRate, String fileName, Clock clock) {
        this.port = checkNotNull(port, "port");
        this.baudRate = baudRate;
        this.fileName = checkNotNull(fileName, "fileName");
        this.clock = checkNotNull(clock, "clock");

        this.reconnecting = false;
    }

    @Override
    public void finalize() {
        this.finalizing = true;
        if (this.radioReader != null) {
            this.radioReader.interrupt();
            this.radioReader = null;
        }
        if (this.xbee != null) {
            if (this.xbee.isConnected()) {
                LOGGER.info("Closing radio on " + this.port);
                this.xbee.close();
            }
            this.xbee = null;
        }
        if (this.writer != null) {
            LOGGER.info("Closing transcript in " + this.fileName);
            try {
                this.writer.close();
            } catch (IOException e) {
                LOGGER.error(e);
            }
            try {
                Path path = Paths.get(this.fileName);
                BasicFileAttributes readAttributes = Files.readAttributes(
                        path, BasicFileAttributes.class);
                long size = readAttributes.size();
                LOGGER.debug(this.fileName + " is " + size + "bytes");
                if (0L == size) {
                    LOGGER.warn("Deleting empty transcript");
                    File file = path.toFile();
                    file.delete();
                }
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }
    }


    private static void waitForModemReconnect() {
        try {
            Thread.currentThread();
            Thread.sleep(MODEM_RECONNECT_TIMEOUT);
        } catch (InterruptedException e) {
            LOGGER.error(e);
        }
    }

    private void ensureOpen() {
        this.xbee = new XBee();
        try {
            this.xbee.open(this.port, this.baudRate);
        } catch (XBeeException e) {
            LOGGER.error(e);
            this.xbee = null;
            this.iterator = null;
            this.iterator = Iterators.emptyIterator();
            return;
        }

        Path path = Paths.get(this.fileName);
        try {
            this.writer = Files.newBufferedWriter(path, Charset.defaultCharset(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            LOGGER.error(e);
            this.xbee = null;
            this.iterator = null;
            this.iterator = Iterators.emptyIterator();
            return;
        }

        final RadioDataSource obj = this;
        final Queue<TimestampedIo> queue = new ConcurrentLinkedQueue<TimestampedIo>();
        this.radioReader = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!obj.isFinalizing()) {
                    try {
                        XBeeResponse response = obj.getXbee().getResponse(1000);
                        if (response != null && response.getClass() == ZNetRxIoSampleResponse.class) {
                            queue.add(new TimestampedIo(
                                    obj.getClock().millis(),
                                    (ZNetRxIoSampleResponse) response));
                        }
                    } catch (XBeeTimeoutException e) {
                        RadioDataSource.getLogger().warn(e);
                    } catch (XBeeException e) {
                        RadioDataSource.getLogger().error(e);
                    }
                }
            }
        });

        this.iterator = new AbstractIterator<AccelerometerData>() {
            @Override
            protected AccelerometerData computeNext() {
                while (!obj.isFinalizing()) {
                    TimestampedIo timestampedIo = queue.poll();
                    if (timestampedIo == null) {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            RadioDataSource.getLogger().warn(e);
                        }
                        continue;
                    }

                    AccelerometerRawData rawData = new AccelerometerRawData(
                            timestampedIo.time,
                            timestampedIo.io);
                    saveData(rawData);
                    return rawData.toAccelerometerData();
                }
                return endOfData();
            }

            private void saveData(AccelerometerRawData rawData) {
                Integer voltage = rawData.getVoltage();
                StringBuilder sb = new StringBuilder()
                    .append(rawData.getTime())
                    .append(',').append(rawData.getX())
                    .append(',').append(rawData.getY())
                    .append(',').append(rawData.getZ());
                if (voltage != null) {
                    sb.append(',').append(voltage.toString());
                }

                try {
                    sb.append("\r\n");
                    obj.getWriter().write(sb.toString());
                    obj.getWriter().flush();
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
            }
        };

        this.radioReader.start();
    }

    public Writer getWriter() {
        return this.writer;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public Clock getClock() {
        return this.clock;
    }

    public XBee getXbee() {
        return this.xbee;
    }

    public boolean isFinalizing() {
        return this.finalizing;
    }

    @Nullable @Override
    protected AccelerometerData computeNext() {
        while (this.iterator == null || !this.iterator.hasNext()) {
            if (this.reconnecting) {
                waitForModemReconnect();
            }
            this.ensureOpen();
            this.reconnecting = true;
        }
        return this.iterator.next();
    }
}
