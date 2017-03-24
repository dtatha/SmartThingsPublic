/**
 *  Smart Lock / Unlock
 *
 *  Copyright 2014 Arnaud
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
    name: "Auto Lock Deadbolt",
    namespace: "dtatha",
    author: "Arnaud",
    description: "Automatically locks door X minutes after being unlocked",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("When a door unlocks...") {
		input "lock1", "capability.lock"
	}
	section("Lock it how many minutes later?") {
		input "minutesLater", "number", title: "When?"
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
	subscribe(lock1, "lock", doorUnlockedHandler, [filterEvents: false])
    state.currentState = "off"
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	subscribe(lock1, "lock", doorUnlockedHandler, [filterEvents: false])
}

def lockDoor() {                                                // This process locks the door.
               lock1.lock()                                     // Don't need delay because the process is scheduled to run later
               log.debug "in the lockDoor routine Switch is ${state.currentState}"
               if (switches && "off" == state.currentState) {
               
						switches.off()
	   } 
        if (location.contactBookEnabled) {
                    sendNotificationToContacts("Front door locked", recipients)
        }
        else {
            if (phone) {
                        sendSms phone, "Front door locked"
                    } else {
                        sendPush "Front door locked"
                    }

                }
               

}

def doorUnlockedHandler(evt) {
		
        if (switches) {
        	
        	log.debug "Debug statement - Lock ${lock1} was: ${evt.value}"
		}
        if (evt.value == "lock") {                      // If the human locks the door then...
                        unschedule(lockDoor)                	// ...we don't need to lock it later. 	
        }                
		
        if (evt.value == "unlocked") {                  		// If the human (or computer) unlocks the door then...
        	        log.debug "Locking in ${minutesLater} minutes"
                if (switches) {
                		state.currentState = switches.currentSwitch
                        log.debug "Switch was ${state.currentState}"
						switches.on()
	   			}
                 if (location.contactBookEnabled) {
                    sendNotificationToContacts("Front door unlocked", recipients)
        }
        else {
            if (phone) {
                        sendSms phone, "Front door unlocked"
                    } else {
                        sendPush "Front door unlocked"
                    }

                }
		        def delay = minutesLater * 60          		   // runIn uses seconds so we don't need to multiple by 1000
                        runIn(delay, lockDoor)                 // ...schedule the door lock procedure to run x minutes later.
                }

       
                



}