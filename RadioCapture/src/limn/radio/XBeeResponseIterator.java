package limn.radio;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.log4j.Logger;

import com.google.common.collect.AbstractIterator;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeTimeoutException;

public class XBeeResponseIterator<T> extends AbstractIterator<T> {
    private final static Logger LOGGER = Logger.getLogger(XBeeResponseIterator.class);

    private final XBee xbee;

    public XBeeResponseIterator(XBee xbee) {
        checkNotNull(xbee, "xbee");
        this.xbee = xbee;
    }

    @Override
    protected T computeNext() {
        if (!this.xbee.isConnected()) {
            return endOfData();
        }

        while (true) {
            try {
                T resp = (T) this.xbee.getResponse(1000);
                if (resp != null) {
                    LOGGER.debug("Got" + resp);
                    return resp;
                }
                LOGGER.warn("Timed out waiting for XBee response");
            }
            catch (XBeeTimeoutException e) {
                LOGGER.warn(e);
            } catch (XBeeException e) {
                LOGGER.error(e);
            }
        }
    }

    public void disconnect() {
        if (this.xbee.isConnected()) {
            this.xbee.close();
        }
    }

    @Override
    public void finalize() {
        this.disconnect();
    }
}
