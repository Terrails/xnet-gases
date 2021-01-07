package terrails.xnetgases.logic;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class LogicUtils {

    private static Map<String, XGSensor.SensorMode> sensorModeCache;
    private static Map<String, XGSensor.Operator> operatorCache;

    @Nonnull
    public static XGSensor.SensorMode getSensorModeFrom(String s) {
        if (sensorModeCache == null) {
            sensorModeCache = new HashMap<>();
            for (XGSensor.SensorMode mode : XGSensor.SensorMode.values()) {
                sensorModeCache.put(mode.name(), mode);
            }
        }
        return sensorModeCache.get(s);
    }

    @Nonnull
    public static XGSensor.Operator getOperatorFrom(String s) {
        if (operatorCache == null) {
            operatorCache = new HashMap<>();
            for (XGSensor.Operator mode : XGSensor.Operator.values()) {
                operatorCache.put(mode.name(), mode);
            }
        }
        return operatorCache.get(s);
    }

}
