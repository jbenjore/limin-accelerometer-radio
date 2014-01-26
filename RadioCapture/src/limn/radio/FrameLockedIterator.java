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

        counter = 0L;
        offset = Long.MIN_VALUE;
        initialized = false;
    }

    @Nullable
    @Override
    protected AccelerometerData computeNext() {
        // Collect all data frames that will be aggregated into a single synthetic
        // frame for the purposes of rendering a picture.
        ++counter;
        long frameBegins = (counter - 1L) * frameMillis;
        long nextFrameBegins = frameBegins + frameMillis;
        list.clear();

        while (iterator.hasNext()) {
            AccelerometerData data = iterator.peek();
            if (data == null) {
                break;
            }

            long time = data.getTime();
            if (initialized) {
                time -= offset;
                if (time >= nextFrameBegins) {
                    break;
                }
            }
            else {
                offset = time;
                time = 0L;
                initialized = true;
            }

            list.add(iterator.next());
        }

        switch (list.size()) {
        case 0:
            if (prevData != null) {
                LOGGER.debug("Reusing " + prevData);
                return prevData;
            }
            else {
                LOGGER.warn("Tried to reuse prev data, nothing there!");
                return null;
            }
        case 1:
            prevData = list.get(0);
            return prevData;
        default:
            double time, x, y, z, rawX, rawY, rawZ;
            time = x = y = z = rawX = rawY = rawZ = 0D;
            for (AccelerometerData data : list) {
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
                    (long) Math.floor(time / list.size()),
                    x / list.size(),
                    y / list.size(),
                    z / list.size(),
                    new AccelerometerRawData(
                            (long) Math.floor(time / list.size()),
                            (int) Math.floor(rawX / list.size()),
                            (int) Math.floor(rawY / list.size()),
                            (int) Math.floor(rawZ / list.size())));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sum of " + list.size() + " between [" + frameBegins + ".." + nextFrameBegins + "): " + aggregate);
            }
            return prevData = aggregate;
        }
    }
    
    public long getTime() {
        return offset + (long) Math.floor((frameRate * counter));
    }
}
