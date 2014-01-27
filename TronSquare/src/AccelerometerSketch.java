

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
        return this.iterator;
    }

    public BlockingQueue<AccelerometerData> queue() {
        return this.queue;
    }

    public Thread queueThread() {
        return this.queueThread;
    }

    @SuppressWarnings("hiding")
    protected void iterator(Iterator<AccelerometerData> iterator) {
        this.iterator = iterator;
    }

    @SuppressWarnings("null")
    protected void accelerometerData() {
        Limn.Opts opts = null;
        try {
            opts = Limn.parseArgs(this.args);
        } catch (ParseException e) {
            Throwables.propagate(e);
        }

        String file = this.sketchPath + "\\RadioCapture-" + System.currentTimeMillis() + ".csv";
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

    @SuppressWarnings("hiding")
    protected void queue(BlockingQueue<AccelerometerData> queue) {
        this.queue = queue;
    }

    @SuppressWarnings("hiding")
    protected void queueThread(Thread queueThread) {
        this.queueThread = queueThread;
    }

    protected void playTranscript(String fileName, @Nullable ReadableInstant skipTil, int frameRateLock) {
        // Skip to 7:40 for Lisa's class.
        this.iterator = new FileDataSource(
                "C:\\Users\\Josh\\Desktop\\RadioCapture-1389754237019.csv");
        if (skipTil != null) {
            skipTil(skipTil);
        }
        if (frameRateLock > 0) {
            frameLock(frameRateLock);
        }
        iterator(this.iterator);
        queue(Queues.<AccelerometerData>newSynchronousQueue());
        queueThread(new Thread(new QueuePublisher(this)));
        this.queueThread.start();
    }

    protected void playRadio(String port, int baudRate, String fileName, Clock clock) {
        LOGGER.info(port + "@" + baudRate + " -> " + fileName);
        this.iterator = new RadioDataSource(port, baudRate, fileName, clock);
        this.queue = Queues.<AccelerometerData>newArrayBlockingQueue(1000);
        this.queueThread = new Thread(new QueuePublisher(this));
        this.queueThread.start();
    }

    @Nullable
    protected AccelerometerData take() {
        AccelerometerData data = null;
        try {
            data = this.queue.take();
        } catch (InterruptedException e) {
            LOGGER.error(e);
            data = this.prevData;
        }
        if (data != null ) {
            return this.prevData = data;
        }
        
        // Might be null.
        return this.prevData;
    }

    protected void arrowOfTime(long time) {
        if (this.prevTime > time) {
            LOGGER.warn("Time has looped from " + new Instant(this.prevTime) +
                    " to " + new Instant(time) + "; exiting");
            System.exit(0);
        }
        this.prevTime = time;
    }

    protected void skipTil(ReadableInstant skipTil) {
        long millis = skipTil.getMillis();
        while (this.iterator != null && this.iterator.hasNext()) {
            AccelerometerData data = this.iterator.next();
            if (data.getTime() >= millis) {
                break;
            }
        }
    }

    @SuppressWarnings("hiding")
    protected void frameLock(int frameRate) {
        this.iterator = new FrameLockedIterator(
                this.iterator, frameRate);
    }
}
