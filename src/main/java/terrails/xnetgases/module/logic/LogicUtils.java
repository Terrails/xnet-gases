package terrails.xnetgases.module.logic;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class LogicUtils {

    private static Map<String, XGSensor.SensorMode> sensorModeCache;
    private static Map<String, XGSensor.Operator> operatorCache;
    private static Map<String, XGLogicConnectorSettings.LogicMode> logicModeCache;

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

    @Nonnull
    public static XGLogicConnectorSettings.LogicMode getLogicModeFrom(String s) {
        if (logicModeCache == null) {
            logicModeCache = new HashMap<>();
            for (XGLogicConnectorSettings.LogicMode mode : XGLogicConnectorSettings.LogicMode.values()) {
                logicModeCache.put(mode.name(), mode);
            }
        }
        return logicModeCache.get(s);
    }

}
