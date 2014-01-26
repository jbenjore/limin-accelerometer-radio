package limn.radio;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import javax.annotation.Nullable;

import com.google.common.collect.AbstractIterator;

public class DecodingIterator extends AbstractIterator<AccelerometerRawData> {
    private final Iterator<String> iterator;
    public DecodingIterator(Iterator<String> iterator) {
        this.iterator = checkNotNull(iterator, "iterator");
    }

    @Nullable
    @Override
    protected AccelerometerRawData computeNext() {
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line != null) {
                AccelerometerRawData rawData = AccelerometerRawData.fromLine(line);
                if (rawData != null) {
                    return rawData;
                }
            }
            return null;
        }
        return endOfData();
    }
}
