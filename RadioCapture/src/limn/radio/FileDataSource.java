package limn.radio;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import javax.annotation.Nullable;

import com.google.common.collect.AbstractIterator;

public class FileDataSource extends AbstractIterator<AccelerometerData> implements IDataSource {
    private final String fileName;
    private Iterator<AccelerometerData> iterator;
    
    public FileDataSource(String fileName) {
        this.fileName = checkNotNull(fileName, "fileName");
    }

    @Nullable
    @Override
    protected AccelerometerData computeNext() {
        while (this.iterator == null || !this.iterator.hasNext()) {
            this.reopen();
        }
        return this.iterator.next();
    }

    private void reopen() {
        final Iterator<String> lineReadingIterator = new LineReadingIterator(this.fileName);
        final Iterator<AccelerometerRawData> decodingIterator = new DecodingIterator(lineReadingIterator);
        this.iterator = new AbstractIterator<AccelerometerData>() {
            @Override
            protected AccelerometerData computeNext() {
                while (decodingIterator.hasNext()) {
                    return decodingIterator.next().toAccelerometerData();
                }
                return endOfData();
            }
        };
    }
}
