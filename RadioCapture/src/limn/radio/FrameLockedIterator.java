package limn.radio;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;

public class FrameLockedIterator extends AbstractIterator<AccelerometerData> {
    private final static Logger LOGGER = Logger.getLogger(FrameLockedIterator.class);

    private final PeekingIterator<AccelerometerData> iterator;
    private final double frameRate;
    private final long frameMillis;
    private final List<AccelerometerData> list;
    private long counter;
    private long offset;
    private boolean initialized;

    private AccelerometerData prevData;

    public FrameLockedIterator(Iterator<AccelerometerData> iterator, int frameRate) {
        this.iterator = Iterators.peekingIterator(checkNotNull(iterator, "iterator"));
        this.frameRate = frameRate;
        this.frameMillis = (long) Math.floor(1000D / frameRate);
        this.list = Lists.newArrayList();

        this.counter = 0L;
        this.offset = Long.MIN_VALUE;
        this.initialized = false;
    }

    @Nullable
    @Override
    protected AccelerometerData computeNext() {
        // Collect all data frames that will be aggregated into a single synthetic
        // frame for the purposes of rendering a picture.
        ++this.counter;
        long frameBegins = (this.counter - 1L) * this.frameMillis;
        long nextFrameBegins = frameBegins + this.frameMillis;
        this.list.clear();

        while (this.iterator.hasNext()) {
            AccelerometerData data = this.iterator.peek();
            if (data == null) {
                break;
            }

            long time = data.getTime();
            if (this.initialized) {
                time -= this.offset;
                if (time >= nextFrameBegins) {
                    break;
                }
            }
            else {
                this.offset = time;
                time = 0L;
                this.initialized = true;
            }

            this.list.add(this.iterator.next());
        }

        switch (this.list.size()) {
        case 0:
            if (this.prevData != null) {
                LOGGER.debug("Reusing " + this.prevData);
                return this.prevData;
            }
            LOGGER.warn("Tried to reuse prev data, nothing there!");
            return null;
        case 1:
            this.prevData = this.list.get(0);
            return this.prevData;
        default:
            double time, x, y, z, rawX, rawY, rawZ;
            time = x = y = z = rawX = rawY = rawZ = 0D;
            for (AccelerometerData data : this.list) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Summing: " + data);
                }
                time += data.getTime();
                x += data.getX();
                y += data.getY();
                z += data.getZ();
                AccelerometerRawData rawData = data.getRawData();
                rawX += rawData.getX();
                rawY += rawData.getY();
                rawZ += rawData.getZ();
            }
            AccelerometerData aggregate = new AccelerometerData(
                    (long) Math.floor(time / this.list.size()),
                    x / this.list.size(),
                    y / this.list.size(),
                    z / this.list.size(),
                    new AccelerometerRawData(
                            (long) Math.floor(time / this.list.size()),
                            (int) Math.floor(rawX / this.list.size()),
                            (int) Math.floor(rawY / this.list.size()),
                            (int) Math.floor(rawZ / this.list.size())));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sum of " + this.list.size() + " between [" + frameBegins + ".." + nextFrameBegins + "): " + aggregate);
            }
            return this.prevData = aggregate;
        }
    }
    
    public long getTime() {
        return this.offset + (long) Math.floor((this.frameRate * this.counter));
    }
}
