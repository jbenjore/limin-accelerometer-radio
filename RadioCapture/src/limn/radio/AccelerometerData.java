package limn.radio;
import javax.annotation.Nullable;

import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import com.google.common.base.Objects;

public class AccelerometerData {
    private final long time;
    private final double x;
    private final double y;
    private final double z;
    private final AccelerometerRawData rawData;

    AccelerometerData(long time, double x, double y, double z, @Nullable AccelerometerRawData rawData) {
        this.time = time;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rawData = rawData;
    }

    public long getTime() {
        return this.time;
    }
    
    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    
    public double getZ() {
        return this.z;
    }

    public AccelerometerRawData getRawData() {
        return this.rawData;
    }
    
    @Override
    public String toString() {
        Objects.ToStringHelper str = Objects.toStringHelper(this)
            .add("time", new Instant(this.time).toDateTime(DateTimeZone.forOffsetHours(-8)))
            .add("x", this.x)
            .add("y", this.y)
            .add("z", this.z);
        if (rawData != null) {
            str.add("raw", rawData);
        }
        return str.toString();
    }
}