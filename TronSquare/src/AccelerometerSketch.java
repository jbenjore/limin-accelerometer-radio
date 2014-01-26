

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import javax.annotation.Nullable;

import limn.radio.AccelerometerData;
import limn.radio.FileDataSource;
import limn.radio.FrameLockedIterator;
import limn.radio.RadioDataSource;
import limn.radio.util.Clock;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.joda.time.ReadableInstant;

import processing.core.PApplet;

import com.google.common.base.Throwables;
import com.google.common.collect.Queues;

public class AccelerometerSketch extends PApplet {
    final static Logger LOGGER = Logger.getLogger(AccelerometerSketch.class);

    protected Iterator<AccelerometerData> iterator;
    protected BlockingQueue<AccelerometerData> queue;
    protected Thread queueThread;
    protected AccelerometerData prevData;

    private long prevTime;

    public Iterator<AccelerometerData> iterator() {
        return iterator;
    }

    public BlockingQueue<AccelerometerData> queue() {
        return queue;
    }

    public Thread queueThread() {
        return queueThread;
    }

    protected void iterator(Iterator<AccelerometerData> iterator) {
        this.iterator = iterator;
    }

    protected void accelerometerData() {
        Limn.Opts opts = null;
        try {
            opts = Limn.parseArgs(args);
        } catch (ParseException e) {
            Throwables.propagate(e);
        }

        String file = sketchPath + "\\RadioCapture-" + System.currentTimeMillis() + ".csv";
        if (opts.replay) {
            playTranscript(
                    file,
                    opts.skipTil,
                    opts.frameLocked ? 24 : 0);
        }
        else {
            playRadio(opts.port, opts.baudRate, file, Clock.systemUTC());
        }
    }

    protected void queue(BlockingQueue<AccelerometerData> queue) {
        this.queue = queue;
    }

    protected void queueThread(Thread queueThread) {
        this.queueThread = queueThread;
    }

    protected void playTranscript(String fileName, @Nullable ReadableInstant skipTil, int frameRateLock) {
        // Skip to 7:40 for Lisa's class.
        iterator = new FileDataSource(
                "C:\\Users\\Josh\\Desktop\\RadioCapture-1389754237019.csv");
        if (skipTil != null) {
            skipTil(skipTil);
        }
        if (frameRateLock > 0) {
            frameLock(frameRateLock);
        }
        iterator(iterator);
        queue(Queues.<AccelerometerData>newSynchronousQueue());
        queueThread(new Thread(new QueuePublisher(this)));
        queueThread.start();
    }

    protected void playRadio(String port, int baudRate, String fileName, Clock clock) {
        LOGGER.info(port + "@" + baudRate + " -> " + fileName);
        iterator = new RadioDataSource(port, baudRate, fileName, clock);
        queue = Queues.<AccelerometerData>newArrayBlockingQueue(1000);
        queueThread = new Thread(new QueuePublisher(this));
        queueThread.start();
    }

    @Nullable
    protected AccelerometerData take() {
        AccelerometerData data = null;
        try {
            data = queue.take();
        } catch (InterruptedException e) {
            LOGGER.error(e);
            data = prevData;
        }
        if (data != null ) {
            return prevData = data;
        }
        else {
            // Might be null.
            return prevData;
        }
    }

    protected void arrowOfTime(long time) {
        if (prevTime > time) {
            LOGGER.warn("Time has looped from " + new Instant(prevTime) +
                    " to " + new Instant(time) + "; exiting");
            System.exit(0);
        }
        prevTime = time;
    }

    protected void skipTil(ReadableInstant skipTil) {
        long millis = skipTil.getMillis();
        while (iterator != null && iterator.hasNext()) {
            AccelerometerData data = iterator.next();
            if (data.getTime() >= millis) {
                break;
            }
        }
    }
    
    protected void frameLock(int frameRate) {
        iterator = new FrameLockedIterator(
                iterator, frameRate);
    }
}
