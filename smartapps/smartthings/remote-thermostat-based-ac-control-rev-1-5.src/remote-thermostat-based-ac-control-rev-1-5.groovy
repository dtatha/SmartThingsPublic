/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  RemoStat
 *
 *  Author: SmartThings
 */

definition(
    name: "Remote Thermostat Based AC Control Rev 1.5",
    namespace: "smartthings",
    author: "Tats De",
    description: "Thermostat control based on remote sensor - Maintains an absolute minimum (for heating) and maximum (for cooling) temperature",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences() {
	section("Choose thermostat... ") {
		input "thermostat", "capability.thermostat"
	}
	section("Heat setting..." ) {
		input "heatingSetpoint", "decimal", title: "Degrees F"
	}
	section("Air conditioning setting...") {
		input "coolingSetpoint", "decimal", title: "Degrees F"
	}
	section("Optionally choose temperature sensor to use instead of the thermostat's... ") {
		input "sensor", "capability.temperatureMeasurement", title: "Temp Sensors", required: false
	}
    section("Send Push Notification?") {
        input "sendPush", "bool", required: true,
              title: "Send Push Notification when mode changed?"
    }
}

def installed()
{
	log.debug "enter installed, state: $state"
	subscribeToEvents()
}

def updated()
{
	log.debug "enter updated, state: $state"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents()
{
	subscribe(location, changedLocationMode)
	if (sensor) {
		subscribe(sensor, "temperature", temperatureHandler)
		subscribe(thermostat, "temperature", temperatureHandler)
		subscribe(thermostat, "thermostatMode", temperatureHandler)
	}
	evaluate()
}

def changedLocationMode(evt)
{
	log.debug "changedLocationMode mode: $evt.value, heat: $heat, cool: $cool"
	evaluate()
}

def temperatureHandler(evt)
{
	evaluate()
}

private evaluate()
{
	if (sensor) {
		def threshold = 1.0
		def tm = thermostat.currentThermostatMode
		def ct = thermostat.currentTemperature
		def currentTemp = sensor.currentTemperature
        
        ct = 72
		log.trace("evaluate:, mode: $tm -- temp: $ct, heat: $thermostat.currentHeatingSetpoint, cool: $thermostat.currentCoolingSetpoint -- "  +
			"sensor: $currentTemp, heat: $heatingSetpoint, cool: $coolingSetpoint, Push?: $sendPush")
        if (tm in ["cool","auto"]) {
        	// send 68F as setpoint if the temperature is above setpoint+deadband AND the system is not "cooling" already
        	if (currentTemp - coolingSetpoint >= threshold) {
        		thermostat.setCoolingSetpoint(68)
            	log.trace("AC turned ON - Current Temperature $currentTemp °F")
                //if 	the thermostat was not in cooling mode, i.e. it is entering the cooling mode 
                //		and
                //		push notification is enabled
                //then
                //		send the notification
                if ((!(thermostat.thermostatOperatingState in ["cooling"])) && sendPush){
                	sendNotificationEvent("RemoStat - AC turned ON . Current Temperature $currentTemp °F")
                }
               
        	}
        	else if( coolingSetpoint - currentTemp >= threshold){
        		thermostat.setCoolingSetpoint(84)
            	log.trace("AC turned OFF - Current Temperature $currentTemp °F")
                if (thermostat.thermostatOperatingState in ["cooling"]){
                	if(sendPush){
                    	sendNotificationEvent("RemoStat - AC turned OFF . Current Temperature $currentTemp  °F")
                    }
                }
                else{
                	// do not send notification
                }
        	}
        }
        if (tm in ["heat","emergency heat","auto"]) {
       	 if (currentTemp - heatingSetpoint >= threshold) {
        	thermostat.setHeatingSetpoint(62)
            log.trace("Heat turned OFF - Current Temperature $currentTemp, F")
            if(sendPush){
                    	sendNotificationEvent("RemoStat - Heat turned OFF . Current Temperature $currentTemp, F")
            }
       	 }
       	 else {
        	thermostat.setHeatingSetpoint(72)
            log.trace("Heat turned ON - Current Temperature $currentTemp, F")
            if(sendPush){
                    	sendNotificationEvent("RemoStat - Heat turned ON . Current Temperature $currentTemp, F")
            }
       	 }
        }
		/*if (tm in ["cool","auto"]) {
			// air conditioner
			if (currentTemp - coolingSetpoint >= threshold) {
				thermostat.setCoolingSetpoint(ct - 2)
				log.debug "thermostat.setCoolingSetpoint(${ct - 2}), ON"
                sendPush("AC turned ON")
			}
			else if (coolingSetpoint - currentTemp >= threshold && ct - thermostat.currentCoolingSetpoint >= threshold) {
				thermostat.setCoolingSetpoint(ct + 2)
				log.debug "thermostat.setCoolingSetpoint(${ct + 10}), OFF"
                sendPush("AC turned OFF")
			}
		}
		if (tm in ["heat","emergency heat","auto"]) {
			// heater
			if (heatingSetpoint - currentTemp >= threshold) {
				thermostat.setHeatingSetpoint(ct + 2)
				log.debug "thermostat.setHeatingSetpoint(${ct + 2}), ON"
			}
			else if (currentTemp - heatingSetpoint >= threshold && thermostat.currentHeatingSetpoint - ct >= threshold) {
				thermostat.setHeatingSetpoint(ct - 2)
				log.debug "thermostat.setHeatingSetpoint(${ct - 2}), OFF"
			}
		}*/
	}
	else {
		thermostat.setHeatingSetpoint(heatingSetpoint)
		thermostat.setCoolingSetpoint(coolingSetpoint)
		thermostat.poll()
	}
    
}

// for backward compatibility with existing subscriptions
def coolingSetpointHandler(evt) {
	log.debug "coolingSetpointHandler()"
}
def heatingSetpointHandler (evt) {
	log.debug "heatingSetpointHandler ()"
}