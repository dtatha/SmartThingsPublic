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
 *  Virtual Thermostat
 *
 *  Author: SmartThings
 */
definition(
    name: "Window Fan Economizer",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Turn on a window fan if ambient temperature is below room temperature",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Choose a outdoor temperature sensor... "){
		input "sensorOutdoor", "capability.temperatureMeasurement", title: "Outdoor (Ambient) Temperature Sensor"
	}
    section("Choose a room temperature sensor... "){
		input "sensorIndoor", "capability.temperatureMeasurement", title: "Indoor Temperature Sensor"
	}
	section("Select the fan outlet(s)... "){
		input "outlets", "capability.switch", title: "Fan Outlets", multiple: true
	}
	section("Send Push Notification?") {
        input "sendPush", "bool", required: false,
              title: "Send Push Notification when Opened?"
    }
}

def installed()
{
	subscribe(sensorIndoor, "temperature", temperatureHandler)	
    subscribe(sensorOutdoor, "temperature", temperatureHandler)	
}

def updated()
{
	unsubscribe()
	subscribe(sensorIndoor, "temperature", temperatureHandler)	
    subscribe(sensorOutdoor, "temperature", temperatureHandler)	

}

def temperatureHandler(evt)
{


		def lastTempIndoor = sensorIndoor.currentTemperature
        def lastTempOutdoor = sensorOutdoor.currentTemperature

		log.debug "EVALUATE(Indoor Temperature - $lastTempIndoor, Outdoor Temperature -  $lastTempOutdoor)"
	
		if (lastTempIndoor - lastTempOutdoor >= 1.0) {
			outlets.on()
            if (sendPush) {
        	sendPush("The temperature reached ${currentTemp} F. I turned on AC!")
    		}
		}
		else if (lastTempOutdoor - lastTempIndoor >= 1.0) {
			outlets.off()
            if (sendPush) {
        	sendPush("The temperature reached ${currentTemp} F. I turned off AC!")
    		}
		}
}	


