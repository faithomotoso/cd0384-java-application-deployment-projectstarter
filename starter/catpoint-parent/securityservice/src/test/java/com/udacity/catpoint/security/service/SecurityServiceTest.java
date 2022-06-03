package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private SecurityService securityService;

    @Mock
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
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void testAlarmStatus_withArmedAlarm_andActivatedSensor(ArmingStatus armingStatus) {
        // Mock arming status to be armed
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        // Set mock in repository for getAlarmStatus
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        Sensor sensor = generateSensors().get(0);
        securityService.changeSensorActivationStatus(sensor, true);

        // Verify that system was updated through the repository
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    private static List<Sensor> generateSensors() {
        return List.of(
                new Sensor("Fire Sensor", SensorType.DOOR, true),
                new Sensor("Water Sensor", SensorType.MOTION),
                new Sensor("Light Sensor", SensorType.WINDOW)
        );
    }

    private static Stream<Arguments> activeSensorStream() {
        return generateSensors().stream().map(Arguments::of);
    }

    /**
     * 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm,
     * set the alarm status to alarm.
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void testAlarmStatus_whenAlarmIsArmed_withSystemInPendingAlarm_andSensorBecomesActivated(ArmingStatus armingStatus) {
        // Mock security repository and set arming status
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        // Mock alarm status to pending alarm
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Sensor sensor = generateSensors().get(0);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * 3. If pending alarm and all sensors are inactive, return to no alarm state.
     */
    @Test
    public void testAlarmStatus_whenSystemInPendingAlarm_andSensorBecomesInactive() {

        // Mock alarm status to PendingAlarm
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // Set all sensors to active
        List<Sensor> activeSensors = generateSensors().stream().peek(s -> s.setActive(true)).collect(Collectors.toList());

        // Mock sensors list
        when(securityRepository.getSensors()).thenReturn(new HashSet<>(activeSensors));

        // Deactivate all sensors
        securityService.getSensors().forEach(s -> securityService.changeSensorActivationStatus(s, false));

        // Verify system change
        verify(securityRepository, atLeastOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
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
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    /**
     * 5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
     */
    @ParameterizedTest
    @MethodSource(value = "activeSensorStream")
    public void testAlarmState_whenSensorIsActive_andSensorGetsActivated_andSystemIsInPendingState(Sensor sensor) {
        // Set sensor to active
        sensor.setActive(true);

        // Mock pending state
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // Change sensor status through service
        securityService.changeSensorActivationStatus(sensor, true);

        // Assert alarm status was updated in securityRepository
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
     */
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class)
    public void testAlarmState_remainsUnchanged_whenADeactivatedSensor_getsDeactivated(AlarmStatus alarmStatus) {

        Sensor sensor = generateSensors().get(0);
        // Set sensor to be deactivated
        sensor.setActive(false);

        // Deactivate sensor in securityService
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * 7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
     */
    @Test
    public void testAlarmStatus_isSetToAlarm_whenImageServiceDetectsCat_andArmingStatusIsArmedHome() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        // Mock imageService to always detect a cat
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        // Call processImage in securityService
        securityService.processImage(null);

        // Verify system's AlarmStatus is updated to Alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    private Set<Sensor> getSensors() {
        return activeSensorStream().map(a -> (Sensor) a.get()[0]).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 8. If the image service identifies an image that does not contain a cat,
     * change the status to no alarm as long as the sensors are not active.
     */
    @Test
    public void testActiveStatus_setToNoAlarm_whenImageServiceDoesntSeeACat_andSensorsAreNotActive() {
        // Set all Sensors to be inactive
        Set<Sensor> sensors = getSensors();
        sensors.forEach(s -> s.setActive(false));

        // Mock securityRepository to return these sensors
        when(securityRepository.getSensors()).thenReturn(new HashSet<>(sensors));

        // Mock imageService to never see a cat
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

        // Call securityService#processImage
        securityService.processImage(null);

        // Verify that alarm status was updated
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * 8.1 Testing 8 but with an active sensor
     */
    @Test
    public void testActiveStatus_remainsUnchanged_whenImageServiceDoesntDetectACat_andASensorIsActive() {
        // Set all Sensors to be inactive
        Set<Sensor> sensors = getSensors();
        new ArrayList<>(sensors).get(0).setActive(true);

        // Mock securityRepository to return these sensors
        when(securityRepository.getSensors()).thenReturn(new HashSet<>(sensors));

        // Mock imageService to never see a cat
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

        // Call securityService#processImage
        securityService.processImage(null);

        // Verify that alarm status was never updated
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * 9. If the system is disarmed, set the status to no alarm.
     */
    @Test
    public void testAlarmStatus_setToNoAlarm_whenSystemIsDisarmed() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    /**
     * 10. If the system is armed, reset all sensors to inactive.
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void testSensorsStatus_setToInactive_whenSystemIsArmed(ArmingStatus armingStatus) {
        Set<Sensor> sensors = getSensors();

        // Make each sensor active
        sensors.forEach(s -> s.setActive(true));

        // Mock getSensors
        when(securityRepository.getSensors()).thenReturn(sensors);

        // Mock alarm status
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        // Set system to be armed
        securityService.setArmingStatus(armingStatus);

        // Verify securityRepository was called to update the sensor
        verify(securityRepository, atLeast(sensors.size())).updateSensor(any(Sensor.class));

        // Check that each sensor has been set to inactive
        sensors.forEach(s -> assertEquals(false, s.getActive()));
    }

    /**
     * 11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
     */
    @Test
    public void testAlarmStatus_whenSystemIsArmedHome_andCameraShowsACat() {
        // Mock image service to always detect a cat
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(Boolean.TRUE);

        // Mock system to be armed home
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        // Call process image - detects a cat
        securityService.processImage(null);

        // Verify method to change alarm status was called
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
}
