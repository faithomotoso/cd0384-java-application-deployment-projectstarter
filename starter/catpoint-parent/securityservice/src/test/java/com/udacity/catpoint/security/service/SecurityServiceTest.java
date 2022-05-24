package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
                new Sensor("Fire Sensor", SensorType.DOOR, true),
                new Sensor("Water Sensor", SensorType.MOTION),
                new Sensor("Light Sensor", SensorType.WINDOW)
        );

        return sensors.stream().map(Arguments::of);
    }

    /**
     * 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm,
     * set the alarm status to alarm.
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
    @MethodSource(value = "activeSensorStream")
    public void testAlarmState_remainsUnchanged_whenADeactivatedSensor_getsDeactivated(Sensor sensor) {

        AlarmStatus alarmStatus = new Random().nextBoolean() ? AlarmStatus.PENDING_ALARM : AlarmStatus.ALARM;

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

    private List<Sensor> getSensors() {
        return activeSensorStream().map(a -> (Sensor) a.get()[0]).collect(Collectors.toList());
    }

    /**
     * 8. If the image service identifies an image that does not contain a cat,
     *    change the status to no alarm as long as the sensors are not active.
     */
    @Test
    public void testActiveStatus_setToNoAlarm_whenImageServiceDoesntSeeACat_andSensorsAreNotActive() {
        // Set all Sensors to be inactive
        List<Sensor> sensors = getSensors();
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
        List<Sensor> sensors = getSensors();
        sensors.get(0).setActive(true);

        // Mock securityRepository to return these sensors
        when(securityRepository.getSensors()).thenReturn(new HashSet<>(sensors));

        // Mock imageService to never see a cat
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

        // Call securityService#processImage
        securityService.processImage(null);

        // Verify that alarm status was never updated
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }
}
