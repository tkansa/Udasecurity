package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.FakeImageService;
import com.udacity.catpoint.image.service.IImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.thirdparty.jackson.core.JsonEncoding;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {


    @Mock
    SecurityRepository securityRepository;

    @Mock
    IImageService imageService;

    //private final FakeImageService imageService = new FakeImageService();
    private final Sensor sensor = new Sensor("Back door", SensorType.DOOR);
    // class we are testing
    private SecurityService securityService;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
    }

    // 1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = { "ARMED_HOME", "ARMED_AWAY"})
    void ifAlarmIsArmedAndSensorActivated_systemIsPutInPendingAlarmStatus(ArmingStatus armingStatus){

        // alarm is armed
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        // status starts out as no alarm
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        // activate a sensor
        securityService.changeSensorActivationStatus(sensor, true);

        // verify that setAlarmStatus to PENDING_ALARM was invoked on the security repo
        // shouldn't calling getAlarmStatus after activating a sensor return PENDING_ALARM?
        // but this test fails
        // assertEquals(AlarmStatus.PENDING_ALARM, securityService.getAlarmStatus());
        // and this one passes. The fact that setAlarmStatus to pending was invoked
        // shouldn't the above test pass also then?
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);

    }

    // 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = { "ARMED_HOME", "ARMED_AWAY"})
    void ifAlarmIsArmedAndSystemIsPendingAlarmAndSystemIsActivated_SystemIsPutInAlarmStatus(ArmingStatus armingStatus){

        // alarm is armed
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        // status starts out as pending alarm
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // activate a sensor
        securityService.changeSensorActivationStatus(sensor, true);

        // verify that setAlarmStatus to ALARM was invoked on the security repo
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);

    }

    // 3. If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    void ifPendingAlarmAndAllSensorsInactive_SystemIsPutInNoAlarmState(){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Set<Sensor> sensors = new HashSet<>();
        Sensor inactiveSensor = new Sensor("Window", SensorType.DOOR);
        inactiveSensor.setActive(false);
        Sensor activeSensor =  new Sensor("Window", SensorType.DOOR);
        activeSensor.setActive(true);
        sensors.add(inactiveSensor);
        sensors.add(activeSensor);
        // Mockito doesn't like this : )
        // when(securityService.getSensors()).thenReturn(sensors);

        // call the security service to deactivate the active sensor
        securityService.changeSensorActivationStatus(activeSensor, false);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    // 4. If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    public void ifAlarmIsActiveChangeInSensorStateShouldNotAffectAlarmState() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Set<Sensor> sensors = new HashSet<>();
        Sensor inactiveSensor = new Sensor("Window", SensorType.DOOR);
        inactiveSensor.setActive(false);
        Sensor activeSensor =  new Sensor("Window", SensorType.DOOR);
        activeSensor.setActive(true);
        sensors.add(inactiveSensor);
        sensors.add(activeSensor);

        // call the security service to deactivate the active sensor
        securityService.changeSensorActivationStatus(activeSensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

}
