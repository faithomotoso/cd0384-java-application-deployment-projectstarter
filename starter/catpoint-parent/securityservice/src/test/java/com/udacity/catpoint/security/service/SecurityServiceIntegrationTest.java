package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Class for integration testing the security service class and SecurityRepository
 */

@ExtendWith(MockitoExtension.class)
public class SecurityServiceIntegrationTest {
    private SecurityService securityService;

    private SecurityRepository fakeSecurityRepository;

    @Mock
    private ImageService imageService;

    @BeforeEach
    void init() {
        fakeSecurityRepository = new FakeSecurityRepository();
        securityService = new SecurityService(fakeSecurityRepository, imageService);
    }

    /**
     * 1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void testAlarmStatus_withArmedAlarm_andActivatedSensor(ArmingStatus armingStatus) {
        // Arm the alarm
        securityService.setArmingStatus(armingStatus);

        // Set alarm status to no alarm
        securityService.setAlarmStatus(AlarmStatus.NO_ALARM);

        // Make one sensor active
        Sensor sensor = securityService.getSensors().stream().collect(Collectors.toList()).get(0);
        securityService.changeSensorActivationStatus(sensor, true);

        // Verify alarm status is PENDING_ALARM
        assertEquals(AlarmStatus.PENDING_ALARM, securityService.getAlarmStatus());
    }

    /**
     * 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm,
     * set the alarm status to alarm.
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void testAlarmStatus_whenAlarmIsArmed_withSystemInPendingAlarm_andSensorBecomesActivated(ArmingStatus armingStatus) {
        // Set status to be armed
        securityService.setArmingStatus(armingStatus);

        // Set alarm status to pending alarm
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);

        // Activate one sensor
        Sensor sensor = securityService.getSensors().stream().collect(Collectors.toList()).get(0);
        securityService.changeSensorActivationStatus(sensor, true);

        // Assert alarm status is alarm
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    /**
     * 3. If pending alarm and all sensors are inactive, return to no alarm state.
     */
    @Test
    public void testAlarmStatus_whenSystemInPendingAlarm_andSensorBecomesInactive() {
        // Set system to pending alarm
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);

        // Set all sensors to be inactive
        securityService.getSensors().forEach(s -> securityService.changeSensorActivationStatus(s, false));

        // Assert system is in no alarm state
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    /**
     * 4. If alarm is active, change in sensor state should not affect the alarm state.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testAlarmState_whenAlarmIsActive_andSensorStateChanges(boolean sensorState) {
        // Set system to be active
        securityService.setAlarmStatus(AlarmStatus.ALARM);

        // Change sensor state
        securityService.getSensors().forEach(s -> securityService.changeSensorActivationStatus(s, sensorState));

        // Assert alarm remains unchanged
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    /**
     * 5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
     */
    @Test
    public void testAlarmState_whenSensorIsActive_andSensorGetsActivated_andSystemIsInPendingState() {
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        // Set system to be in pending state
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);

        // Set one sensor to be active
        Sensor newSensor = new Sensor("New sensor", SensorType.WINDOW, true);
        securityService.addSensor(newSensor);

        // Set sensor to be active through the securityService
        securityService.changeSensorActivationStatus(newSensor, true);


        // Verify that alarm status is ALARM
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    /**
     * 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
     */
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class)
    public void testAlarmState_remainsUnchanged_whenADeactivatedSensor_getsDeactivated(AlarmStatus alarmStatus) {
        // Make all sensors to be inactive
        securityService.getSensors().forEach(s -> s.setActive(false));

        // Set alarm status
        securityService.setAlarmStatus(alarmStatus);

        // Set sensors to be inactive through the securityService
        securityService.getSensors().forEach(s -> securityService.changeSensorActivationStatus(s, false));

        // Verify that alarm status didn't change
        assertEquals(alarmStatus, securityService.getAlarmStatus());
    }

    /**
     * 7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
     */
    @Test
    public void testAlarmStatus_isSetToAlarm_whenImageServiceDetectsCat_andArmingStatusIsArmedHome() {
        // Mock image service
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        // Set system to be armed home
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        securityService.processImage(null);

        // Verify alarm status
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    /**
     * 8. If the image service identifies an image that does not contain a cat,
     * change the status to no alarm as long as the sensors are not active.
     */
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class)
    public void testActiveStatus_setToNoAlarm_whenImageServiceDoesntSeeACat_andSensorsAreNotActive(AlarmStatus alarmStatus) {
        // Set alarm status
        securityService.setAlarmStatus(alarmStatus);

        // Mock image service
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

        // Set all sensors to be inactive
        securityService.getSensors().forEach(s -> s.setActive(false));

        // Trigger image detection
        securityService.processImage(null);

        // Verify status is No Alarm
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    /**
     * 9. If the system is disarmed, set the status to no alarm.
     */
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class)
    public void testAlarmStatus_setToNoAlarm_whenSystemIsDisarmed(AlarmStatus alarmStatus) {
        // Set alarm status
        securityService.setAlarmStatus(alarmStatus);

        // Disarm the system
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        // Verify that status is set to NO_ALARM
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    /**
     * 10. If the system is armed, reset all sensors to inactive.
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void testSensorsStatus_setToInactive_whenSystemIsArmed(ArmingStatus armingStatus) {
        // Set all sensors as active
        securityService.getSensors().forEach(s -> s.setActive(true));

        // Arm the system
        securityService.setArmingStatus(armingStatus);

        // Verify all sensors have moved to inactive
        securityService.getSensors().forEach(s -> assertFalse(s.getActive()));
    }

    /**
     * 11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
     */
    @Test
    public void testAlarmStatus_whenSystemIsArmedHome_andCameraShowsACat() {
        // Mock image service
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        // Arm the system
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // Trigger image detection
        securityService.processImage(null);

        // Verify alarm status is set to ALARM
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }
}
