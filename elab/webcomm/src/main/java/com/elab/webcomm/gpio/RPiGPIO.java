package com.elab.webcomm.gpio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elab.webcomm.pfeiffer.ReceiverPfeifferPressure;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class RPiGPIO {

    private static final Logger logger = LoggerFactory.getLogger(ReceiverPfeifferPressure.class);
    
    public static void GPIOTest() {
        logger.debug("<--Pi4J--> GPIO Control Example ... started.");

        // create gpio controller
        final GpioController gpio = GpioFactory.getInstance();

        // provision gpio pin #00 as an output pin and turn on
        final GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "MyLED", PinState.HIGH);

        // set shutdown state for this pin
        pin.setShutdownOptions(true, PinState.LOW);

        logger.debug("--> GPIO state should be: ON");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // turn off gpio pin #01
        pin.low();
        logger.debug("--> GPIO state should be: OFF");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // toggle the current state of gpio pin #01 (should turn on)
        pin.toggle();
        logger.debug("--> GPIO state should be: ON");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // toggle the current state of gpio pin #01  (should turn off)
        pin.toggle();
        logger.debug("--> GPIO state should be: OFF");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // turn on gpio pin #01 for 1 second and then off
        logger.debug("--> GPIO state should be: ON for only 1 second");
        pin.pulse(1000, true); // set second argument to 'true' use a blocking call

        // stop all GPIO activity/threads by shutting down the GPIO controller
        // (this method will forcefully shutdown all GPIO monitoring threads and scheduled tasks)
        gpio.shutdown();

        logger.debug("Exiting ControlGpioExample");

    }
}
