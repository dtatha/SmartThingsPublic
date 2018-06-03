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
    name: "Remote Temperature Based AC/Heating Control",
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
		input "sensor", "capability.temperatureMeasurement", title: "Temp Sensors", required: true
	}
    section("Outdoor Temperature... ") {
		input "sensorOAT", "capability.temperatureMeasurement", title: "Outdoor Air Temperature", required: true
	}
    section("Select the ceiling fan control hardware..."){
			input "fanDimmer", "capability.switchLevel", 
	    	multiple:false, title: "Fan Control device", required: false
	}
    /*section("Enable Adaptive Fan Speed Control?") {
        input "enFanSpeed", "bool", required: false,
              title: "Enable Adaptive Fan Speed Control?"
    }*/
    section("Enable Adaptive Fan Control"){
			input "adapFanSwitch", "capability.switch", 
	    	multiple:false, title: "Enable Adaptive Fan Speed Control?", required: false
	}
    section("Disable Away mode offset between") {
        input "fromTime", "time", title: "From", required: true
        input "toTime", "time", title: "To", required: true
    }
    section("Enable Away mode offset?") {
        input "enAway", "bool", required: true,
              title: "Enable Away mode offset?"
    }
    section("Send Push Notification?") {
        input "sendPush", "bool", required: true,
              title: "Send Push Notification when mode changed?"
    }
/*    section("Sleep Cycle on between what times?") {
        input "fromTime", "time", title: "From", required: true
        input "toTime", "time", title: "To", required: true
    }
 */
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
        def OAT = sensorOAT.currentTemperature
        def offset=0
        def controlPoint=0
        def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
    	def adaptiveFan = adapFanSwitch.currentSwitch
        def sleepBetween = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
            
        if (enAway && location.currentMode=="Away" && !between){
         	offset=(10)
         }
        ct = 72
        if (tm in ["cool","auto"]) {
        	// send 68F as setpoint if the temperature is above setpoint+deadband AND the system is not "cooling" already
        	//if (currentTemp - coolingSetpoint >= threshold) {
           /* 
           if (sleepBetween){
            	offset= (-4)*TimeCategory.minus(new Date(),fromTime)/TimeCategory.minus(toTime,fromTime)
            	log.trace("Offset $offset °F")
            }
           */
           controlPoint=coolingSetpoint+0.4*max(0,min(82,OAT)-72)
            if (currentTemp - (coolingSetpoint+offset) >= threshold) {
        		thermostat.setCoolingSetpoint(68)
            	log.trace("AC turned ON - Control Point $controlPoint °F Current Temperature  $currentTemp °F, Setpoint $heatingSetpoint °F, Offset $offset °F")
                if ((!(thermostat.thermostatOperatingState in ["cooling"])) && sendPush){
                	sendNotificationEvent("RemoStat - AC turned ON . Current Temperature $currentTemp °F")
                }
               
        	}
        	else {
        		thermostat.setCoolingSetpoint(84)
            	log.trace("AC turned OFF - Control Point $controlPoint °F Current Temperature  $currentTemp °F, Setpoint $heatingSetpoint °F, Offset $offset °F")
                if (thermostat.thermostatOperatingState in ["cooling"] && sendPush){
                	sendNotificationEvent("RemoStat - AC turned OFF . Current Temperature $currentTemp  °F")
             	}
        	}
         }
        if (tm in ["heat","emergency heat"]) {
         if (currentTemp - (heatingSetpoint-offset) >= threshold) {
        	thermostat.setHeatingSetpoint(62)
            if(fanDimmer){
            	//fanDimmer.on()
                log.trace("Entering Adaptive Fan Control decision loop- Adaptive Fan Control Setting $enFanSpeed, Adaptive Fan Control Switch $adaptiveFan")
            	if(adaptiveFan=="on"){
                			log.trace("Entering Adaptive Fan Control - Adaptive Fan Control Setting $enFanSpeed, Adaptive Fan Control Switch $adaptiveFan")
                	    	fanDimmer.setLevel(0)
                            fanDimmer.off()
                }
            }
            log.trace("Heat turned OFF - Current Temperature $currentTemp °F, Setpoint $heatingSetpoint °F, Offset $offset °F")
            if(sendPush){
                    	sendNotificationEvent("RemoStat - Heat turned OFF . Current Temperature $currentTemp, °F")
            }
       	 }
       	 else {
        	thermostat.setHeatingSetpoint(76)
            if(fanDimmer){
            	//fanDimmer.on()
                log.trace("Entering Adaptive Fan Control decision loop- Adaptive Fan Control Setting $enFanSpeed, Adaptive Fan Control Switch $adaptiveFan")
            	if (adaptiveFan=="on"){
                	log.trace("Entering Adaptive Fan Control - Adaptive Fan Control Setting $enFanSpeed, Adaptive Fan Control Switch $adaptiveFan")
                	fanDimmer.on()
                    fanDimmer.setLevel(33)
                }
            }
            log.trace("Heat turned ON - Current Temperature $currentTemp °F, Setpoint $heatingSetpoint °F, Offset $offset °F")
            if(sendPush){
                    	sendNotificationEvent("RemoStat - Heat turned ON . Current Temperature $currentTemp, °F")
            }
       	 }
        }
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