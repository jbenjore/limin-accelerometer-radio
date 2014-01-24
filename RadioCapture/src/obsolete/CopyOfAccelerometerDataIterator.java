package obsolete;

public class CopyOfAccelerometerDataIterator /* extends AbstractIterator<AccelerometerData> */ {
//    private static enum LearningState {
//        UNINITIALIZED, LEARNING_FACEUP, LEARNING_FACEDOWN, ONLINE, FLIP_WAIT
//    }
//
//    private final static Logger LOGGER = Logger.getLogger(CopyOfAccelerometerDataIterator.class);
//    private static final double TRAINING_VARIANCE_THRESHOLD = 10D;
//    private static final long LEARNING_WINDOW = 3000L;;
//
//    private final String name;
//    private final Iterator<AccelerometerRawData> iterator;
//    private LearningState learningState;
//    private long learningSince;
//    private List<AccelerometerRawData> trainingData;
//    private double xFacedownMean;
//    private double yFacedownMean;
//    private double zFacedownMean;
//    private double xFaceupMean;
//    private double yFaceupMean;
//    private double zFaceupMean;
//    private double range;
//    private double xScale;
//    private double yScale;
//    private double zScale;
//
//    public CopyOfAccelerometerDataIterator(String name,
//            Iterator<AccelerometerRawData> iterator) {
//        this.name = name;
//        this.iterator = iterator;
//        this.learningState = LearningState.UNINITIALIZED;
//        this.learningSince = 0L;
//        this.trainingData = Lists.newLinkedList();
//    }
//
//    @Override
//    protected AccelerometerData computeNext() {
//        while (this.iterator.hasNext()) {
//            AccelerometerRawData rawData = iterator.next();
//            switch (this.learningState) {
//            case UNINITIALIZED:
//                LOGGER.info("Set the " + this.name + " \"face up\" sensor on a level surface");
//                this.learningState = LearningState.LEARNING_FACEUP;
//                this.learningSince = rawData.getTime();
//                this.trainingData.add(rawData);
//                continue;
//
//            case LEARNING_FACEUP:
//                this.trainingData.add(rawData);
//                this.flushOldData(this.learningSince - LEARNING_WINDOW);
//                if (this.trainingData.size() > 30 &&
//                        this.trainingVariance() < TRAINING_VARIANCE_THRESHOLD) {
//                    this.saveFaceupSummary();
//                    LOGGER.info("Flip the " + this.name + " sensor over");
//                    this.trainingData.clear();
//                    this.learningState = LearningState.FLIP_WAIT;
//                    this.learningSince = rawData.getTime();
//                    LOGGER.info("Flip out!");
//                }
//                continue;
//            case FLIP_WAIT:
//                if (rawData.getTime() > this.learningSince + 5000L) {
//                    this.learningState = LearningState.LEARNING_FACEDOWN;
//                    this.learningSince = rawData.getTime();
//                    this.trainingData.clear();
//                    LOGGER.info("Hope you flipped");
//                }
//                continue;
//
//            case LEARNING_FACEDOWN:
//                this.trainingData.add(rawData);
//                this.flushOldData(this.learningSince - LEARNING_WINDOW);
//                if (this.trainingData.size() > 30 &&
//                        this.trainingVariance() < TRAINING_VARIANCE_THRESHOLD) {
//                    this.saveFacedownSummary();
//                    this.range = Math.max(
//                        Math.max(
//                            Math.max(xFaceupMean, xFacedownMean) - Math.min(xFaceupMean, xFacedownMean),
//                            Math.max(yFaceupMean, yFacedownMean) - Math.min(yFaceupMean, yFacedownMean)),
//                        Math.max(zFaceupMean, zFacedownMean) - Math.min(zFaceupMean, zFacedownMean))
//                        / 2D;
//                    this.xScale = Math.min(xFacedownMean, xFaceupMean) + (0.5D * this.range);
//                    this.yScale = Math.min(yFacedownMean, yFaceupMean) + (0.5D * this.range);
//                    this.zScale = Math.min(zFacedownMean, zFaceupMean) + (0.5D * this.range);
//                    LOGGER.info("Online");
//                    this.trainingData = null;
//                    this.learningState = LearningState.ONLINE;
//                }
//                continue;
//
//            default:
//                return this.transformData(rawData);
//            }
//        }
//        return endOfData();
//    }
//
//    private void flushOldData(long time) {
//        while (!this.trainingData.isEmpty()) {
//            AccelerometerRawData data = this.trainingData.get(0);
//            if (data.getTime() < time) {
//                this.trainingData.remove(0);
//            }
//            else {
//                return;
//            }
//        }
//    }
//
//    private void saveFacedownSummary() {
//        DescriptiveStatistics xStats = new DescriptiveStatistics();
//        DescriptiveStatistics yStats = new DescriptiveStatistics();
//        DescriptiveStatistics zStats = new DescriptiveStatistics();
//        for (AccelerometerRawData data : this.trainingData) {
//            xStats.addValue(data.getX());
//            yStats.addValue(data.getY());
//            zStats.addValue(data.getZ());
//        }
//        this.xFacedownMean = xStats.getMean();
//        this.yFacedownMean = yStats.getMean();
//        this.zFacedownMean = zStats.getMean();
//    }
//
//    private void saveFaceupSummary() {
//        DescriptiveStatistics xStats = new DescriptiveStatistics();
//        DescriptiveStatistics yStats = new DescriptiveStatistics();
//        DescriptiveStatistics zStats = new DescriptiveStatistics();
//        for (AccelerometerRawData data : this.trainingData) {
//            xStats.addValue(data.getX());
//            yStats.addValue(data.getY());
//            zStats.addValue(data.getZ());
//        }
//        this.xFaceupMean = xStats.getMean();
//        this.yFaceupMean = yStats.getMean();
//        this.zFaceupMean = zStats.getMean();
//    }
//
//    private double trainingVariance() {
//        DescriptiveStatistics xStats = new DescriptiveStatistics();
//        DescriptiveStatistics yStats = new DescriptiveStatistics();
//        DescriptiveStatistics zStats = new DescriptiveStatistics();
//        for (AccelerometerRawData data : this.trainingData) {
//            xStats.addValue(data.getX());
//            yStats.addValue(data.getY());
//            zStats.addValue(data.getZ());
//        }
//        double xVariance = xStats.getVariance();
//        double yVariance = yStats.getVariance();
//        double zVariance = zStats.getVariance();
//        return Math.max(
//            Math.max(xVariance, yVariance),
//            zVariance);
//    }
//
//    private AccelerometerData transformData(AccelerometerRawData rawData) {
//        return new AccelerometerData(
//            rawData.getTime(),
//            (rawData.getX() - this.xScale) / this.range,
//            (rawData.getY() - this.yScale) / this.range,
//            (rawData.getZ() - this.zScale) / this.range,
//            rawData
//        );
//    }
//
//    private void waitForFlip() {
//        LOGGER.info("Flip out!?");
//        for (int i = 0; i < 5; ++i) {
//            try {
//                Thread.currentThread().sleep(1000L);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            LOGGER.info(i);
//        }
//        LOGGER.info("Hope you flipped...");
//    }
}
