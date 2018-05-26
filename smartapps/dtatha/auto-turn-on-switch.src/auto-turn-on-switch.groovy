/**
 *  Turn on a switch after x minutes
 *
 *  Copyright 
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
 */

// Automatically generated. Make future change here.
definition(
    name: "Auto turn on switch",
    namespace: "dtatha",
    author: "Tats De",
    description: "Automatically turns on a switch X seconds after being turned off",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("When a switch turns off...") {
		input "switch1", "capability.switch"
	}
	section("Lock it how many seconds later?") {
		input "secondsLater", "number", title: "When?"
	}
    section("Via this number (optional, sends push notification if not specified)"){
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Phone Number", required: false
        }
   
	}
 section("Indicator Light (optional)") {
		input "switches", "capability.switch", required: false, multiple: false, title: "Which lights?"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(switch1, "switch.off", switchOffHandler, [filterEvents: false])
    state.currentState = "on"
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	subscribe(switch1, "switch.off", switchOffHandler, [filterEvents: false])
}

def lockDoor() {                                                // This process locks the door.
               switch1.on()                                     // Don't need delay because the process is scheduled to run later
               
        if (location.contactBookEnabled) {
                    sendNotificationToContacts("Router power restored", recipients)
        }
        else {
            if (phone) {
                        sendSms phone, "Router power restored"
                    } else {
                        sendPush "Router power restored"
                    }

                }
               

}

def switchOffHandler(evt) {
		
       if (evt.value == "on") {                      // If the human locks the door then...
                        unschedule(lockDoor)                	// ...we don't need to lock it later. 	
        }                
		
        if (evt.value == "off") {                  		// If the human (or computer) unlocks the door then...
        	        log.debug "Turning on in ${secondsLater} seconds"
               
                 if (location.contactBookEnabled) {
                    sendNotificationToContacts("Router switch turned off", recipients)
        }
        else {
            if (phone) {
                        sendSms phone, "Router switch turned off"
                    } else {
                        sendPush "Router switch turned off"
                    }

                }
		        def delay = secondsLater         		   // runIn uses seconds so we don't need to multiple by 1000
                        runIn(delay, lockDoor)                 // ...schedule the door lock procedure to run x seconds later.
                }

       
                



}