package limn.radio;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.rapplogic.xbee.api.zigbee.ZNetRxIoSampleResponse;

public class AccelerometerRawData {
    /*
     * X0  288.023622047244
     * Y0  290.594488188976
     * Z_1 240
     *
     * X0  285.527777777778
     * Y0  282.014880952381
     * Z1  355.451388888889
     *
     * X1  342.324074074074
     * Y0  286.527777777778
     * Z0  305
     *
     * X_1 229
     * Y0  283.975961538462
     * Z0  292.754807692308
     * 
     * X0  289.002450980392
     * Y_1 227.948529411765
     * Z0  286.995098039216
     *
     * X0  286.185185185185
     * Y1  341.944444444444
     * Z0  304
     */

    private static final double X1 = 342.324074074074D;
    private static final double X0 = ((288.023622047244D + 285.527777777778D + 289.002450980392D + 286.185185185185D) / 4D);
    private static final double X_1 = 229D;
    private static final double X_RANGE = (X1 - X_1) / 2D;

    private static final double Y1 = 341.944444444444D;
    private static final double Y0 = ((290.594488188976D + 282.014880952381D + 286.527777777778D + 283.975961538462D) / 4D);
    private static final double Y_1 = 227.948529411765D;
    private static final double Y_RANGE = (Y1 - Y_1) / 2D;

    private static final double Z1 = 355.451388888889D;
    private static final double Z0 = ((305D + 292.754807692308D + 286.995098039216D + 304) / 4D);
    private static final double Z_1 = 240D;
    private static final double Z_RANGE = (Z1 - Z_1) / 2D;

    private static final double RANGE = (X_RANGE + Y_RANGE + Z_RANGE) / 3D;

    private final long time;
    private final int x;
    private final int y;
    private final int z;

    @Nullable
    private Integer voltage;

    @Nullable
    public static AccelerometerRawData fromLine(String line) {
        checkNotNull(line, "line");
        List<String> fields = Lists.newArrayList(Splitter.on(',').split(line));
        long time;
        int x, y, z;
        Integer supplyVoltage;

        AccelerometerRawData rawData;
        switch (fields.size()) {
        case 5:
            time = Long.parseLong(fields.get(0));
            x = Integer.parseInt(fields.get(1));
            y = Integer.parseInt(fields.get(2));
            z = Integer.parseInt(fields.get(3));
            supplyVoltage = Integer.parseInt(fields.get(4));

            return new AccelerometerRawData(time, x, y, z, supplyVoltage);
        case 4:
            time = Long.parseLong(fields.get(0));
            x = Integer.parseInt(fields.get(1));
            y = Integer.parseInt(fields.get(2));
            z = Integer.parseInt(fields.get(3));
            return new AccelerometerRawData(time, x, y, z);
        default:
            return null;
        }
    }

    public AccelerometerRawData(long time, ZNetRxIoSampleResponse io) {
        this(time,
                checkNotNull(io, "io").getAnalog(0),
                io.getAnalog(1),
                io.getAnalog(2),
                io.getSupplyVoltage());
    }

    public AccelerometerRawData(long time, int x, int y, int z) {
        this.time = time;
        this.x = x;
        this.y = y;
        this.z = z;
        this.voltage = null;
    }

    public AccelerometerRawData(long time, int x, int y, int z, @Nullable Integer voltage) {
        this(time, x, y, z);
        this.voltage = voltage;
    }

    public long getTime() {
        return time;
    }

    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Nullable
    public Integer getVoltage() {
        return this.voltage;
    }
    
    @Override
    public String toString() {
        Objects.ToStringHelper sb = Objects.toStringHelper(this)
                .add("time", time)
                .add("x", x)
                .add("y", y)
                .add("z", z);
        if (voltage != null) {
            sb.add("v", voltage);
        }
        return sb.toString();
    }

    public AccelerometerData toAccelerometerData() {
        return new AccelerometerData(
                time,
                ((x - X0) / RANGE) + 0.0035448671943733266D,
                ((y - Y0) / RANGE) -0.0525404383804913D,
                ((z - Z0) / RANGE) + 1.0178923920194956D,
                this);
    }

}
