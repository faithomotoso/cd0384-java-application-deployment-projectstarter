package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private SecurityService securityService;
    private ImageService imageService;


    @Mock
    private SecurityRepository securityRepository;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
    }

    /**
     * 1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
     */
    @ParameterizedTest
    @DisplayName("If alarm is armed and sensor becomes activated, put system into pending alarm status")
    @MethodSource(value = "activeSensorStream")
    public void testAlarmStatus_withArmedAlarm_andActivatedSensor(Sensor sensor) {
        // Mock arming status to be armed
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        // Set mock in repository for getAlarmStatus
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        // Verify that system was updated through the repository
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    private static Stream<Arguments> activeSensorStream() {
        List<Sensor> sensors = List.of(
                new Sensor("Fire Sensor", SensorType.DOOR),
                new Sensor("Water Sensor", SensorType.MOTION),
                new Sensor("Light Sensor", SensorType.WINDOW)
        );

        return sensors.stream().map(Arguments::of);
    }

    /**
     * 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm,
     *    set the alarm status to alarm.
     */
    @ParameterizedTest
    @MethodSource(value = "activeSensorStream")
    public void testAlarmStatus_whenAlarmIsArmed_withSystemInPendingAlarm_andSensorBecomesActivated(Sensor sensor) {
        // Mock security repository and set arming status
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        // Mock alarm status to pending alarm
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * 3. If pending alarm and all sensors are inactive, return to no alarm state.
     */
    @ParameterizedTest
    @MethodSource(value = "activeSensorStream")
    public void testAlarmStatus_whenSystemInPendingAlarm_andSensorBecomesInactive(Sensor sensor) {
        // Set sensor to active
        sensor.setActive(true);

        // Mock alarm status to PendingAlarm
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // Deactivate sensor
        securityService.changeSensorActivationStatus(sensor, false);

        // Verify system change
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * 4. If alarm is active, change in sensor state should not affect the alarm state.
     */
    @ParameterizedTest
    @MethodSource(value = "activeSensorStream")
    public void testAlarmState_whenAlarmIsActive_andSensorStateChanges(Sensor sensor) {
        // Mock alarm status to be active
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        // Change alarm status to either true or false
        Random random = new Random();
        securityService.changeSensorActivationStatus(sensor, random.nextBoolean());

        // Assert system alarm status is still set to Alarm
        assertEquals(securityService.getAlarmStatus(), AlarmStatus.ALARM);
    }
}
