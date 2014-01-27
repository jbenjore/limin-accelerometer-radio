package limn.radio;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.apache.log4j.Logger;

import com.google.common.collect.AbstractIterator;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.zigbee.ZNetRxIoSampleResponse;

public class ZNetRxIoSampleResponseStream extends AbstractIterator<ZNetRxIoSampleResponse> {
    private final static Logger LOGGER = Logger.getLogger(RadioDataSource.class);
    private final Iterator<XBeeResponse> iterator;

    /**
     * 
     * @param iterator
     */
    public ZNetRxIoSampleResponseStream(Iterator<XBeeResponse> iterator) {
        this.iterator = checkNotNull(iterator, "iterator");
    }

    @Override
    protected ZNetRxIoSampleResponse computeNext() {
        while (this.iterator.hasNext()) {
            XBeeResponse xBeeResponse = this.iterator.next();
            try {
                return (ZNetRxIoSampleResponse) xBeeResponse;
            }
            catch (ClassCastException e) {
                LOGGER.warn("Unexpected radio packet: " + xBeeResponse);
            }
        }
        return endOfData();
    }
}
