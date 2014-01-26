
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
        while (sketch.iterator().hasNext()) {
            AccelerometerData data = sketch.iterator().next();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ready " + data);
            }
            while (!sketch.queue().offer(data)) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    LOGGER.error(e);
                }
            }
        }
    }
}