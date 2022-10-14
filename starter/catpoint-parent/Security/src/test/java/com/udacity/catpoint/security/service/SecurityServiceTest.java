package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.IImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
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

    // As demoed in class by the instructor
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = { "ARMED_HOME", "ARMED_AWAY"})
    void ifAlarmIsArmedAndSensorActivated_systemIsPutInPendingAlarmStatus(ArmingStatus armingStatus){

        // alarm is armed
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        // status starts out as no alarm
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        // activate a sensor
        securityService.changeSensorActivationStatus(sensor, true);

        // verify that setAlarmStatus to PENDING_ALARM was invoked on the security repo
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);

    }

    // 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    // As demoed in class by the instructor
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = { "ARMED_HOME", "ARMED_AWAY"})
    void ifAlarmIsArmedAndSystemIsPendingAlarmAndSystemIsActivated_SystemIsPutInAlarmStatus(ArmingStatus armingStatus){

        // alarm is armed
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        // status starts out as pending alarm
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // activate a sensor
        securityService.changeSensorActivationStatus(sensor, true);

        // verify that setAlarmStatus to ALARM was invoked on the security repo
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);

    }

    // 3. If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    void ifPendingAlarmAndAllSensorsInactive_SystemIsPutInNoAlarmState(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
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
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
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

    // 5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.

    @Test
    public void ifASensorIsActivatedWhileAlreadyActiveAndTheSystemIsInThePendingState_changeItToAlarmState(){
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // call the security service to reactivate the active sensor
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);

    }

    // 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
    // As demoed in class by the instructor
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    public void ifASensorIsDeactivatedWhileAlreadyInactive_MakeNoChangesToAlarmState(AlarmStatus expectedAlarmStatus) {
        // start a sensor as false
        sensor.setActive(false);

        // mock the various alarm Statuses
        when(securityRepository.getAlarmStatus()).thenReturn(expectedAlarmStatus);

        // set the sensor to false, again
        securityService.changeSensorActivationStatus(sensor, false);

        // get the alarm status after changeSensorActivationStatus has been called
        AlarmStatus actualAlarmStatus = securityRepository.getAlarmStatus();

        // assert that the alarm status hasn't changed
        assertEquals(expectedAlarmStatus, actualAlarmStatus);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
    @Test
    public void ifImageServiceIDsCatWhileArmedHome_PutTheSystemIntoAlarmState() {
        // make a cat image
        BufferedImage cat = new BufferedImage(4, 4, 4);
        // set the system to ARMED_HOME
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        // make it detect a cat
        when(imageService.imageContainsCat(cat, 50F)).thenReturn(true);

        // call the method
        securityService.processImage(cat);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 8. If the image service identifies an image that does not contain a cat,
    // change the status to no alarm as long as the sensors are not active.
    @Test
    public void ifImageWithNoCatDetected_ifNoSensorIsActiveChangeStatusToNoAlarm(){
        sensor.setActive(false);
        // make a dog image
        BufferedImage dog = new BufferedImage(4, 4, 4);
        // set the alarm
        //when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // make it detect a non-cat
        when(imageService.imageContainsCat(dog, 50F)).thenReturn(false);

        // call the method
        securityService.processImage(dog);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    // 9. If the system is disarmed, set the status to no alarm.
    @Test
    public void ifSystemDisarmed_setStatusToNoAlarm() {
        // call the method
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 10. If the system is armed, reset all sensors to inactive.
    @ParameterizedTest //tests 10
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    public void ifSystemIsArmed_resetAllSensorsToInactive(ArmingStatus armingStatus){

        // make some active sensors
        Set<Sensor> sensors = new HashSet<>();
        Sensor windowSensor = new Sensor("Window", SensorType.WINDOW);
        windowSensor.setActive(true);
        Sensor doorSensor =  new Sensor("Door", SensorType.DOOR);
        doorSensor.setActive(true);
        sensors.add(windowSensor);
        sensors.add(doorSensor);

        // add the sensors
        when(securityRepository.getSensors()).thenReturn(sensors);

        // call the method
        securityService.setArmingStatus(armingStatus);

        assertEquals(false, windowSensor.getActive());
        assertEquals(false, doorSensor.getActive());

    }

    // 11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @ParameterizedTest //tests 10
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "DISARMED"})
    public void ifTheSystemIsArmedHomeWhileCameraShowsCat_setAlarmToALARM(ArmingStatus armingStatus){

        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        // make a cat image
        BufferedImage cat = new BufferedImage(4, 4, 4);
        // make it detect a cat
        when(imageService.imageContainsCat(cat, 50F)).thenReturn(true);
        securityService.processImage(cat);

        // set arming status to ARMED_HOME
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);

    }

}