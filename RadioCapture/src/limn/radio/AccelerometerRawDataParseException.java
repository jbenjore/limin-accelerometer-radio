package limn.radio;

/**
 * An exception 
 * @author josh
 *
 */
public class AccelerometerRawDataParseException extends Exception {
  public AccelerometerRawDataParseException(Throwable cause) {
    super(cause);
  }

  public AccelerometerRawDataParseException(String message) {
    super(message);
  }
}
