package com.udacity.catpoint.security.data;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * FakeSecurityRepository written solely for integration testing purposes
 * A minor reflection of PretendDatabaseSecurityRepositoryImpl
 */
public class FakeSecurityRepository implements SecurityRepository {
    private Set<Sensor> sensors = new TreeSet<>();

    private AlarmStatus alarmStatus;

    private ArmingStatus armingStatus;

    public FakeSecurityRepository() {
        // Pre-populate fields
        alarmStatus = AlarmStatus.NO_ALARM;

        armingStatus = ArmingStatus.DISARMED;

        sensors.addAll(List.of(new Sensor("Door Sensor", SensorType.DOOR, false),
                new Sensor("Grass Sensor", SensorType.MOTION, true)));
    }

    @Override
    public void addSensor(Sensor sensor) {
        Objects.requireNonNull(sensor);
        sensors.add(sensor);
    }

    @Override
    public void removeSensor(Sensor sensor) {
        sensors.remove(Objects.requireNonNull(sensor));
    }

    @Override
    public void updateSensor(Sensor sensor) {
        Objects.requireNonNull(sensor);
        sensors.remove(sensor);
        sensors.add(sensor);
    }

    @Override
    public void setAlarmStatus(AlarmStatus alarmStatus) {
        this.alarmStatus = Objects.requireNonNull(alarmStatus);
    }

    @Override
    public void setArmingStatus(ArmingStatus armingStatus) {
        this.armingStatus = Objects.requireNonNull(armingStatus);
    }

    @Override
    public Set<Sensor> getSensors() {
        return this.sensors;
    }

    @Override
    public AlarmStatus getAlarmStatus() {
        return this.alarmStatus;
    }

    @Override
    public ArmingStatus getArmingStatus() {
        return this.armingStatus;
    }

    @Override
    public ArmingStatus getOldArmingStatus() {
        return this.armingStatus;
    }
}
