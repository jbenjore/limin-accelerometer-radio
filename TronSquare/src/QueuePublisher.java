
import limn.radio.AccelerometerData;

import org.apache.log4j.Logger;

final class QueuePublisher implements Runnable {
    final static Logger LOGGER = Logger.getLogger(QueuePublisher.class);

    private final AccelerometerSketch sketch;

    public QueuePublisher(AccelerometerSketch receiver) {
        this.sketch = receiver;
    }

    @Override
    public void run() {
        while (this.sketch.iterator().hasNext()) {
            AccelerometerData data = this.sketch.iterator().next();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ready " + data);
            }
            while (!this.sketch.queue().offer(data)) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    LOGGER.error(e);
                }
            }
        }
    }
}