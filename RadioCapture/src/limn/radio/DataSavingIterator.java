package limn.radio;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;

public class DataSavingIterator extends AbstractIterator<AccelerometerData> {
    private final static Logger LOGGER = Logger.getLogger(DataSavingIterator.class);

    private final Iterator<AccelerometerData> iterator;
    private final Writer writer;

    private long count = Long.MIN_VALUE;

    public DataSavingIterator(Iterator<AccelerometerData> iterator, Writer writer) {
        this.iterator = checkNotNull(iterator, "iterator");
        this.writer = checkNotNull(writer, "writer");
    }

    @Override
    protected AccelerometerData computeNext() {
        while (this.iterator.hasNext()) {
            AccelerometerData data = this.iterator.next();
            this.saveData(data);
            return data;
        }
        return endOfData();
    }

    private void saveData(AccelerometerData data) {
    }

    private void flushOccasionally() {
        ++count;
        if (Long.MAX_VALUE == count) {
            count = Long.MIN_VALUE;
        }
        if (count % 100L == 0L) {
            try {
                writer.flush();
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }
    }
}
