# pilight Binding
The pilight binding allows openHAB to communicate with a pilight instance running pilight version 7.0 or greater.

# What is pilight?
_pilight_ is a cheap way to control ‘Click On Click Off’ devices. It started as an application for the Raspberry Pi (using the GPIO interface) but it’s also possible now to connect it to any other PC using an Arduino Nano. You will need a cheap 433Mhz transceiver in both cases. See the [Pilight manual](https://manual.pilight.org/electronics/wiring.html) for more information.

The binding supports Switch, Dimmer, Contact, String, and Number items.

## TODO for this readme
 - Add hint to ensure, `"standalone": 0` is set in config (is default?) to support disco of bridge
 - Add Examples for config files

