package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.*;
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
import java.util.stream.Stream;

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
     * If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
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
}
