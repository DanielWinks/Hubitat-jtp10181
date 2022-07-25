/*  
 *  Zooz ZSE44 Temperature Humidity XS Sensor
 *    - Model: ZSE44 - MINIMUM FIRMWARE 1.10
 *
 *  For Support: https://community.hubitat.com/t/zooz-sensors/81074
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz
 *

Changelog:

## [1.0.1] - 2022-07-25 (@jtp10181)
  ### Added
  - Set deviceModel in device data (press refresh)
  ### Changed
  - Description text loging enabled by default
  - Removed getParam.value and replaced with separate function
  - Adding HTML styling to the Preferences
  - Cleaned up some logging functions
  - Other minor function updates synced from other drivers
  
## [0.1.0] - 2021-05-23 (@jtp10181)
  ### Added
  - Initial Release, from ZSE40 1.0.0 codebase
  - Supports all known settings and features except associations (and related settings)

NOTICE: This file has been created by *Jeff Page* with some code used 
	from the original work of *Zooz* and *Kevin LaFramboise* under compliance with the Apache 2.0 License.

 *  Copyright 2022 Jeff Page
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
*/

import groovy.transform.Field

@Field static final String VERSION = "1.0.1" 
@Field static final Map deviceModelNames = ["7000:E004":"ZSE44"]

metadata {
	definition (
		name: "Zooz ZSE44 Temperature Humidity XS Sensor",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/hubitat/master/Drivers/zooz/zooz-zse44-temp-humidity.groovy"
	) {
		capability "Sensor"
		capability "Relative Humidity Measurement"
		capability "Temperature Measurement"
		capability "Battery"
		capability "Tamper Alert"

		command "fullConfigure"
		command "forceRefresh"

		//DEBUGGING
		//command "debugShowVars"

		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"7000", deviceId:"E004", inClusters:"0x5E,0x55,0x9F,0x6C", deviceJoinName:"Zooz ZSE44 Temperature Humidity XS Sensor"
	}

	preferences {

		configParams.each { param ->
			if (!param.hidden) {
				Integer paramVal = getParamValue(param)
				if (param.options) {
					input "configParam${param.num}", "enum",
						title: fmtTitle("${param.title}"),
						description: fmtDesc("• Parameter #${param.num}, Selected: ${paramVal}" + (param?.description ? "<br>• ${param?.description}" : '')),
						defaultValue: paramVal,
						options: param.options,
						required: false
				}
				else if (param.range) {
					input "configParam${param.num}", "number",
						title: fmtTitle("${param.title}"),
						description: fmtDesc("• Parameter #${param.num}, Range: ${(param.range).toString()}, DEFAULT: ${param.defaultVal}" + (param?.description ? "<br>• ${param?.description}" : '')),
						defaultValue: paramVal,
						range: param.range,
						required: false
				}
			}
		}

		// input "supervisionGetEncap", "bool",
		// 	title: fmtTitle("Supervision Encapsulation") + "<em> (Experimental)</em>",
		// 	description: fmtDesc("This can increase reliability when the device is paired with security, but may not work correctly on all models."),
		// 	defaultValue: false

		//Logging options similar to other Hubitat drivers
		input "txtEnable", "bool", title: fmtTitle("Enable Description Text Logging?"), defaultValue: true
		input "debugEnable", "bool", title: fmtTitle("Enable Debug Logging?"), defaultValue: true
	}
}

//Preference Helpers
String fmtDesc(String str) {
	return "<div style='font-size: 85%; font-style: italic; padding: 1px 0px 4px 2px;'>${str}</div>"
}
String fmtTitle(String str) {
	return "<strong>${str}</strong>"
}

void debugShowVars() {
	log.warn "paramsList ${paramsList.hashCode()} ${paramsList}"
	log.warn "paramsMap ${paramsMap.hashCode()} ${paramsMap}"
	log.warn "settings ${settings.hashCode()} ${settings}"
}

//Main Parameters Listing
@Field static Map<String, Map> paramsMap =
[
	batteryLow: [ num:2, 
		title: "Low Battery Report (%)",
		size: 1, defaultVal: 10,
		range: "10..50"
	],
	tempTrigger: [ num:3,
		title: "Temperature Change Report Trigger (1 = 0.1° / 10 = 1°)",
		size: 1, defaultVal: 20,
		range: "10..100"
	],
	humidityTrigger: [ num:4,
		title: "Humidity Change Report Trigger (%)",
		size: 1, defaultVal: 5,
		range: "1..50"
	],
	//TEMP ALERTS
	//HUMIDITY ALERTS
	tempOffset: [ num:14,
		title: "Temperature Offset (°)",
		size: 1, defaultVal: 0,
		range: "-10.0..10.0"
	],
	humidOffset: [ num:15,
		title: "Humidity Offset (%)",
		size: 1, defaultVal: 0,
		range: "-10.0..10.0"
	],
	tempInterval: [ num:16,
		title: "Temperature Reporting Interval (mins)",
		description: "Set to 0 to Disable",
		size: 2, defaultVal: 240,
		range: "0..480"
	],
	humidInterval: [ num:17,
		title: "Humidity Reporting Interval (mins)",
		description: "Set to 0 to Disable",
		size: 2, defaultVal: 240,
		range: "0..480"
	],
	tempUnits: [ num:13,
		title: "Temperature Units:",
		size: 1, defaultVal: 1,
		options: [0:"Celsius (°C)", 1:"Fahrenheit (°F)"]
	],
]

//Command Classes Supported
@Field static final Map commandClassVersions = [
	0x31: 5,	// Sensor Multilevel (sensormultilevelv5) (11)
	0x59: 1,	// Association Grp Info (associationgrpinfov1)
	0x5A: 1,	// Device Reset Locally	(deviceresetlocallyv1)
	0x5E: 2,	// ZWave Plus Info (zwaveplusinfov2)
	0x6C: 1,	// Supervision (supervisionv1)
	0x70: 2,	// Configuration (configurationv2) (4)
	0x71: 8,	// Notification (notificationv8) (8)
	0x72: 2,	// Manufacturer Specific (manufacturerspecificv2)
	0x73: 1,	// Power Level (powerlevelv1)
	0x7A: 2,	// Firmware Update Md (firmwareupdatemdv2)
	0x80: 1,	// Battery (batteryv1)
	0x84: 2,	// Wakeup (wakeupv2)
	0x85: 2,	// Association (associationv2) (3)
	0x86: 2,	// Version (versionv2) (3)
	0x87: 3,	// Indicator (indicatorv3)
	0x8E: 3,	// Multi Channel Association (multichannelassociationv3)
	0x9F: 1		// Security S2
]

//Sensor Types
@Field static Short SENSOR_TYPE_TEMPERATURE = hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport.SENSOR_TYPE_TEMPERATURE_VERSION_1
@Field static Short SENSOR_TYPE_LUMINANCE = hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport.SENSOR_TYPE_LUMINANCE_VERSION_1 
@Field static Short SENSOR_TYPE_HUMIDITY = hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport.SENSOR_TYPE_RELATIVE_HUMIDITY_VERSION_2
//Notification Types
@Field static Short NOTIFICATION_TYPE_SECURITY = hubitat.zwave.commands.notificationv8.NotificationReport.NOTIFICATION_TYPE_BURGLAR
@Field static Short NOTIFICATION_TYPE_TEMP = hubitat.zwave.commands.notificationv8.NotificationReport.NOTIFICATION_TYPE_HEAT
@Field static Short NOTIFICATION_TYPE_WEATHER = hubitat.zwave.commands.notificationv8.NotificationReport.NOTIFICATION_TYPE_WEATHER_ALARM


/*******************************************************************
 ***** Core Functions
********************************************************************/
void installed() {
	logWarn "installed..."
}

List<String> fullConfigure() {
	logWarn "configure..."
	if (debugEnable) runIn(1800, debugLogsOff)

	if (!pendingChanges || state.resyncAll == null) {
		logForceWakeupMessage "Full Re-Configure"
		state.resyncAll = true
	} else {
		logForceWakeupMessage "Pending Configuration Changes"
	}

	updateSyncingStatus(1)
	return []
}

List<String> updated() {
	logDebug "updated..."
	logDebug "Debug logging is: ${debugEnable == true}"
	logDebug "Description logging is: ${txtEnable == true}"

	if (debugEnable) runIn(1800, debugLogsOff)

	if (!firmwareVersion || !state.deviceModel) {
		state.resyncAll = true
		state.pendingRefresh = true
		setDevModel(firmwareVersion)
		logForceWakeupMessage "Full Re-Configure and Refresh"
	}

	if (pendingChanges) {
		logForceWakeupMessage "Pending Configuration Changes"
	}
	else if (!state.resyncAll && !state.pendingRefresh) {
		state.remove("INFO")
	}

	updateSyncingStatus(1)
	return []
}

List<String> forceRefresh() {
	logDebug "refresh..."
	state.pendingRefresh = true
	logForceWakeupMessage "Sensor Info Refresh"
	return []
}

/*******************************************************************
 ***** Z-Wave Reports
********************************************************************/
void parse(String description) {
	hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)
	
	if (cmd) {
		logTrace "parse: ${description} --PARSED-- ${cmd}"
		zwaveEvent(cmd)
	} else {
		logWarn "Unable to parse: ${description}"
	}

	//Update Last Activity
	updateLastCheckIn()
}

void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd)
	} else {
		logWarn "Unable to extract encapsulated cmd from $cmd"
	}
}

//Decodes Multichannel Encapsulated Commands
void zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap cmd) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, cmd.sourceEndPoint as Integer)
	} else {
		logWarn "Unable to extract encapsulated cmd from $cmd"
	}
}

//Decodes Supervision Encapsulated Commands (and replies to device)
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, ep)
	} else {
		logWarn "Unable to extract encapsulated cmd from $cmd"
	}

	sendCommands(secureCmd(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0), ep))
}

//Handles reports back from Supervision Encapsulated Commands
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionReport cmd, ep=0 ) {
	logDebug "Supervision Report - SessionID: ${cmd.sessionID}, Status: ${cmd.status}"
	if (supervisedPackets["${device.id}"] == null) { supervisedPackets["${device.id}"] = [:] }

	switch (cmd.status as Integer) {
		case 0x00: // "No Support"
		case 0x01: // "Working"
		case 0x02: // "Failed"
			logWarn "Supervision NOT Successful - SessionID: ${cmd.sessionID}, Status: ${cmd.status}"
			break
		case 0xFF: // "Success"
			supervisedPackets["${device.id}"].remove(cmd.sessionID)
			break
	}
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
	logTrace "${cmd}"

	String subVersion = String.format("%02d", cmd.firmware0SubVersion)
	String fullVersion = "${cmd.firmware0Version}.${subVersion}"
	device.updateDataValue("firmwareVersion", fullVersion)

	logDebug "Received Version Report - Firmware: ${fullVersion}"
	setDevModel(new BigDecimal(fullVersion))
}

void zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	Map param = getParam(cmd.parameterNumber)
	Integer val = cmd.scaledConfigurationValue

	if (param) {
		if (val < 0 && param.size == 1) { val += 256 } //Convert scaled signed integer to unsigned
		logDebug "${param.name} (#${param.num}) = ${val.toString()}"
		setParamStoredValue(param.num, val)
	}
	else {
		logDebug "Parameter #${cmd.parameterNumber} = ${val.toString()}"
	}
}

void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	Integer grp = cmd.groupingIdentifier

	if (grp == 1) {
		logDebug "Lifeline Association: ${cmd.nodeId}"
		state.group1Assoc = (cmd.nodeId == [zwaveHubNodeId]) ? true : false
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"

	Integer lvl = cmd.batteryLevel
	if (lvl == 0xFF) {
		lvl = 1
		logWarn "LOW BATTERY WARNING"
	}

	lvl = validateRange(lvl, 100, 1, 100)

	String descText = "battery level is ${lvl}%"
	logInfo(descText)
	sendEvent(name:"battery", value: lvl, unit:"%", descriptionText: descText, isStateChange: true)
}

void zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpIntervalReport cmd) {
   logDebug "WakeUpIntervalReport: $cmd"
   device.updateDataValue("zwWakeupInterval", "${cmd.seconds}")
}

void zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpNotification cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	logDebug "WakeUpNotification"

	List<String> cmds = []
	cmds << batteryGetCmd()

	//Refresh all if requested
	if (state.pendingRefresh) { cmds += getRefreshCmds() }
	//Any configuration needed
	cmds += getConfigureCmds()

	//This needs a longer delay
	cmds << "delay 1000" << wakeUpNoMoreInfoCmd()

	//Clear pending status
	state.resyncAll = false
	state.pendingRefresh = false
	state.remove("INFO")
	
	sendCommands(cmds, 400)
}

void zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"	
	switch (cmd.sensorType) {
		case SENSOR_TYPE_TEMPERATURE: //0x01
			def temp = convertTemperatureIfNeeded(cmd.scaledSensorValue, (cmd.scale ? "F" : "C"), cmd.precision)
			sendEventIfNew("temperature", safeToDec(temp,0,Math.min(cmd.precision,1)), true, "", "°${temperatureScale}")
			break
		case SENSOR_TYPE_HUMIDITY:  //0x05
			def humid = cmd.scaledSensorValue
			sendEventIfNew("humidity", safeToDec(humid,0,Math.min(cmd.precision,1)), true, "", "%")
			break
		default:
			logDebug "Unhandled SensorMultilevelReport sensorType: ${cmd}"
	}
}

void zwaveEvent(hubitat.zwave.commands.notificationv8.NotificationReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	switch (cmd.notificationType) {
		case NOTIFICATION_TYPE_TEMP: //Temp Alarm 0x04
			logDebug "NOTIFICATION_TYPE_HEAT ${cmd.event}, ${cmd.eventParameter[0]}"
			//sendSecurityEvent(cmd.event, cmd.eventParameter[0])
			break
		case NOTIFICATION_TYPE_WEATHER: //Humidity Alarm 0x16
			logDebug "NOTIFICATION_TYPE_WEATHER_ALARM ${cmd.event}, ${cmd.eventParameter[0]}"
			//sendSecurityEvent(cmd.event, cmd.eventParameter[0])
			break
		default:
			logDebug "Unhandled NotificationReport notificationType: ${cmd}"
	}
}

void zwaveEvent(hubitat.zwave.Command cmd, ep=0) {
	logDebug "Unhandled zwaveEvent: $cmd (ep ${ep})"
}


/*******************************************************************
 ***** Z-Wave Command Shortcuts
********************************************************************/
//These send commands to the device either a list or a single command
void sendCommands(List<String> cmds, Long delay=200) {
	//Calculate supervisionCheck delay based on how many commands
	Integer packetsCount = supervisedPackets?."${device.id}"?.size()
	if (packetsCount > 0) {
		Integer delayTotal = (cmds.size() * delay) + 2000
		logDebug "Setting supervisionCheck to ${delayTotal}ms | ${packetsCount} | ${cmds.size()} | ${delay}"
		runInMillis(delayTotal, supervisionCheck, [data:1])
	}

	//Send the commands
	sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds, delay), hubitat.device.Protocol.ZWAVE))
}

//Single Command
void sendCommands(String cmd) {
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.ZWAVE))
}

//Consolidated zwave command functions so other code is easier to read
String associationSetCmd(Integer group, List<Integer> nodes) {
	return supervisionEncap(zwave.associationV2.associationSet(groupingIdentifier: group, nodeId: nodes))
}

String associationRemoveCmd(Integer group, List<Integer> nodes) {
	return supervisionEncap(zwave.associationV2.associationRemove(groupingIdentifier: group, nodeId: nodes))
}

String associationGetCmd(Integer group) {
	return secureCmd(zwave.associationV2.associationGet(groupingIdentifier: group))
}

String versionGetCmd() {
	return secureCmd(zwave.versionV2.versionGet())
}

String wakeUpIntervalGetCmd() {
	return secureCmd(zwave.wakeUpV2.wakeUpIntervalGet())
}

String wakeUpIntervalSetCmd(val) {
	return secureCmd(zwave.wakeUpV2.wakeUpIntervalSet(seconds:val, nodeid:zwaveHubNodeId))
}

String wakeUpNoMoreInfoCmd() {
	return secureCmd(zwave.wakeUpV2.wakeUpNoMoreInformation())
}

String batteryGetCmd() {
	return secureCmd(zwave.batteryV1.batteryGet())
}

String sensorBinaryGetCmd() {
	return secureCmd(zwave.sensorBinaryV1.sensorBinaryGet())
}

String sensorMultilevelGetCmd(sensorType) {
	return secureCmd(zwave.sensorMultilevelV5.sensorMultilevelGet(scale: 0x00, sensorType: sensorType))
}

String notificationGetCmd(notificationType, eventType) {
	return secureCmd(zwave.notificationV8.notificationGet(notificationType: notificationType, v1AlarmType:0, event: eventType))
}

String configSetCmd(Map param, Integer value) {
	if (value > 127 && param.size == 1) { value -= 256 }
	return supervisionEncap(zwave.configurationV2.configurationSet(parameterNumber: param.num, size: param.size, scaledConfigurationValue: value))
}

String configGetCmd(Map param) {
	return secureCmd(zwave.configurationV2.configurationGet(parameterNumber: param.num))
}

List configSetGetCmd(Map param, Integer value) {
	List<String> cmds = []
	cmds << configSetCmd(param, value)
	cmds << configGetCmd(param)
	return cmds
}


/*******************************************************************
 ***** Z-Wave Encapsulation
********************************************************************/
//Secure and MultiChannel Encapsulate
String secureCmd(String cmd) {
	return zwaveSecureEncap(cmd)
}
String secureCmd(hubitat.zwave.Command cmd, ep=0) {
	return zwaveSecureEncap(multiChannelEncap(cmd, ep))
}

//MultiChannel Encapsulate if needed
//This is called from secureCmd or supervisionEncap, do not call directly
String multiChannelEncap(hubitat.zwave.Command cmd, ep) {
	//logTrace "multiChannelEncap: ${cmd} (ep ${ep})"
	if (ep > 0) {
		cmd = zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint:ep).encapsulate(cmd)
	}
	return cmd.format()
}

//====== Supervision Encapsulate START ======\\
@Field static Map<String, Map<Short, String>> supervisedPackets = new java.util.concurrent.ConcurrentHashMap()
@Field static Map<String, Short> sessionIDs = new java.util.concurrent.ConcurrentHashMap()

String supervisionEncap(hubitat.zwave.Command cmd, ep=0) {
	//logTrace "supervisionEncap: ${cmd} (ep ${ep})"

	if (settings.supervisionGetEncap) {
		//Encap with SupervisionGet
		Short sessId = getSessionId()
		def cmdEncap = zwave.supervisionV1.supervisionGet(sessionID: sessId).encapsulate(cmd)

		//Encap with MultiChannel now so it is cached that way below
		cmdEncap = multiChannelEncap(cmdEncap, ep)

		logDebug "New Supervised Packet for Session: ${sessId}"
		if (supervisedPackets["${device.id}"] == null) { supervisedPackets["${device.id}"] = [:] }
		supervisedPackets["${device.id}"][sessId] = cmdEncap

		//Calculate supervisionCheck delay based on how many cached packets
		Integer packetsCount = supervisedPackets?."${device.id}"?.size()
		Integer delayTotal = (packetsCount * 500) + 2000
		runInMillis(delayTotal, supervisionCheck, [data:1])

		//Send back secured command
		return secureCmd(cmdEncap)
	}
	else {
		//If supervision disabled just multichannel and secure
		return secureCmd(cmd, ep)
	}
}

Short getSessionId() {
	Short sessId = sessionIDs["${device.id}"] ?: state.lastSupervision ?: 0
	sessId = (sessId + 1) % 64  // Will always will return between 0-63
	state.lastSupervision = sessId
	sessionIDs["${device.id}"] = sessId

	return sessId
}

void supervisionCheck(Integer num) {
	Integer packetsCount = supervisedPackets?."${device.id}"?.size()
	logDebug "Supervision Check #${num} - Packet Count: ${packetsCount}"

	if (packetsCount > 0 ) {
		List<String> cmds = []
		supervisedPackets["${device.id}"].each { sid, cmd ->
			logWarn "Re-Sending Supervised Session: ${sid} (Retry #${num})"
			cmds << secureCmd(cmd)
		}
		sendCommands(cmds)

		if (num >= 3) { //Clear after this many attempts
			logWarn "Supervision MAX RETIES (${num}) Reached"
			supervisedPackets["${device.id}"].clear()
		}
		else { //Otherwise keep trying
			Integer delayTotal = (packetsCount * 500) + 2000
			runInMillis(delayTotal, supervisionCheck, [data:num+1])
		}
	}
}
//====== Supervision Encapsulate END ======\\


/*******************************************************************
 ***** Execute / Build Commands
********************************************************************/
List<String> getConfigureCmds() {
	logDebug "getConfigureCmds..."

	List<String> cmds = []

	if (state.resyncAll || !firmwareVersion || !state.deviceModel) {
		cmds << versionGetCmd()
		cmds << wakeUpIntervalSetCmd(43200)
		cmds << wakeUpIntervalGetCmd()
	}

	cmds += getConfigureAssocsCmds()

	configParams.each { param ->
		Integer paramVal = getParamValue(param, true)
		Integer storedVal = getParamStoredValue(param.num)

		if ((paramVal != null) && (state.resyncAll || (storedVal != paramVal))) {
			logDebug "Changing ${param.name} (#${param.num}) from ${storedVal} to ${paramVal}"
			cmds += configSetGetCmd(param, paramVal)
		}
	}

	if (state.resyncAll) clearVariables()
	state.resyncAll = false

	if (cmds) updateSyncingStatus(6)

	return cmds ?: []
}

List<String> getRefreshCmds() {
	List<String> cmds = []
	cmds << versionGetCmd()
	cmds << wakeUpIntervalGetCmd()
	cmds << sensorMultilevelGetCmd(SENSOR_TYPE_TEMPERATURE)
	cmds << sensorMultilevelGetCmd(SENSOR_TYPE_HUMIDITY)

	return cmds ?: []
}

void clearVariables() {
	logWarn "Clearing state variables and data..."

	//Backup
	String devModel = state.deviceModel 

	//Clears State Variables
	state.clear()

	//Clear Data from other Drivers
	device.removeDataValue("configVals")
	device.removeDataValue("configHide")
	device.removeDataValue("protocolVersion")
	device.removeDataValue("hardwareVersion")
	device.removeDataValue("serialNumber")
	device.removeDataValue("zwaveAssociationG1")
	device.removeDataValue("zwaveAssociationG2")
	device.removeDataValue("zwaveAssociationG3")

	//Restore
	if (devModel) state.deviceModel = devModel
}

List getConfigureAssocsCmds() {
	List<String> cmds = []

	if (!state.group1Assoc || state.resyncAll) {
		if (state.group1Assoc == false) {
			logDebug "Adding missing lifeline association..."
		}
		cmds << associationSetCmd(1, [zwaveHubNodeId])
		cmds << associationGetCmd(1)
	}

	return cmds
}

Integer getPendingChanges() {
	Integer configChanges = configParams.count { param ->
		Integer paramVal = getParamValue(param, true)
		((paramVal != null) && (paramVal != getParamStoredValue(param.num)))
	}
	Integer pendingAssocs = Math.ceil(getConfigureAssocsCmds()?.size()/2) ?: 0
	return (!state.resyncAll ? (configChanges + pendingAssocs) : configChanges)
}


/*******************************************************************
 ***** Event Senders
********************************************************************/
void sendEventIfNew(String name, value, boolean displayed=true, String type=null, String unit="", String desc=null, Integer ep=0) {
	if (desc == null) desc = "${name} set to ${value}${unit}"

	Map evt = [name: name, value: value, descriptionText: desc, displayed: displayed]
	if (type) evt.type = type
	if (unit) evt.unit = unit

	//Main Device Events
	if (name != "syncStatus") {
		if (device.currentValue(name).toString() != value.toString()) {
			logInfo(desc)
		} else {
			logDebug "${desc} [NOT CHANGED]"
		}
	}

	//Always send event to update last activity
	sendEvent(evt)
}


/*******************************************************************
 ***** Common Functions
********************************************************************/
/*** Parameter Store Map Functions ***/
Integer getParamStoredValue(Integer paramNum) {
	//Using Data (Map) instead of State Variables
	TreeMap configsMap = getParamStoredMap()
	return safeToInt(configsMap[paramNum], null)
}

void setParamStoredValue(Integer paramNum, Integer value) {
	//Using Data (Map) instead of State Variables
	TreeMap configsMap = getParamStoredMap()
	configsMap[paramNum] = value
	device.updateDataValue("configVals", configsMap.inspect())
}

Map getParamStoredMap() {
	Map configsMap = [:]
	String configsStr = device.getDataValue("configVals")

	if (configsStr) {
		try {
			configsMap = evaluate(configsStr)
		}
		catch(Exception e) {
			logWarn("Clearing Invalid configVals: ${e}")
			device.removeDataValue("configVals")
		}
	}
	return configsMap
}

//Parameter List Functions
//This will rebuild the list for the current model and firmware only as needed
//paramsList Structure: MODEL:[FIRMWARE:PARAM_MAPS]
//PARAM_MAPS [num, name, title, description, size, defaultVal, options, firmVer]
@Field static Map<String, Map<String, List>> paramsList = new java.util.concurrent.ConcurrentHashMap()
void updateParamsList() {
	logDebug "Update Params List"
	String devModel = state.deviceModel
	Short modelNum = deviceModelShort
	Short modelSeries = Math.floor(modelNum/10)
	BigDecimal firmware = firmwareVersion

	List<Map> tmpList = []
	paramsMap.each { name, pMap ->
		Map tmpMap = pMap.clone()
		tmpMap.options = tmpMap.options?.clone()

		//Save the name
		tmpMap.name = name

		//Apply custom adjustments
		tmpMap.changes.each { m, changes ->
			if (m == devModel || m == modelNum || m ==~ /${modelSeries}X/) {
				tmpMap.putAll(changes)
				if (changes.options) { tmpMap.options = changes.options.clone() }
			}
		}
		//Don't need this anymore
		tmpMap.remove("changes")

		//Set DEFAULT tag on the default
		tmpMap.options.each { k, val ->
			if (k == tmpMap.defaultVal) {
				tmpMap.options[(k)] = "${val} [DEFAULT]"
			}
		}

		//Save to the temp list
		tmpList << tmpMap
	}

	//Remove invalid or not supported by firmware
	tmpList.removeAll { it.num == null }
	tmpList.removeAll { firmware < (it.firmVer ?: 0) }
	tmpList.removeAll { 
		if (it.firmVerM) {
			(firmware-(int)firmware)*100 < it.firmVerM[(int)firmware]
		}
	}

	//Save it to the static list
	if (paramsList[devModel] == null) paramsList[devModel] = [:]
	paramsList[devModel][firmware] = tmpList
}

//Verify the list and build if its not populated
void verifyParamsList() {
	String devModel = state.deviceModel
	BigDecimal firmware = firmwareVersion
	if (paramsList[devModel] == null) updateParamsList()
	if (paramsList[devModel][firmware] == null) updateParamsList()
}

//Gets full list of params
List<Map> getConfigParams() {
	//logDebug "Get Config Params"
	String devModel = state.deviceModel
	BigDecimal firmware = firmwareVersion
	if (!devModel || devModel == "UNK00") return []

	verifyParamsList()

	return paramsList[devModel][firmware]
}

//Get a single param by name or number
Map getParam(def search) {
	//logDebug "Get Param (${search} | ${search.class})"
	Map param = [:]

	verifyParamsList()
	if (search instanceof String) {
		param = configParams.find{ it.name == search }
	} else {
		param = configParams.find{ it.num == search }
	}

	return param
}

//Convert Param Value if Needed
Integer getParamValue(String paramName) {
	return getParamValue(getParam(paramName))
}
Number getParamValue(Map param, Boolean adjust=false) {
	Number paramVal = safeToInt(settings."configParam${param.num}", param.defaultVal)
	if (!adjust) return paramVal

	//Reset hidden parameters to default
	if (param.hidden && settings."configParam${param.num}" != null) {
		logWarn "Resetting hidden parameter ${param.name} (${param.num}) to default ${param.defaultVal}"
		device.removeSetting("configParam${param.num}")
		paramVal = param.defaultVal
	}

	switch(param.name) {
		case "tempOffset":
		case "humidOffset":
			//Convert -10.0..10.0 range to 0..200
			paramVal = (paramVal * 10) + 100
			paramVal = validateRange(paramVal, 100, 0, 200)
			break
	}

	return paramVal
}

/*** Other Helper Functions ***/
void updateSyncingStatus(Integer delay=2) {
	runIn(delay, refreshSyncStatus)
	sendEventIfNew("syncStatus", "Syncing...", false)
}

void refreshSyncStatus() {
	Integer changes = pendingChanges
	sendEventIfNew("syncStatus", (changes ?  "${changes} Pending Changes" : "Synced"), false)
}

void updateLastCheckIn() {
	if (!isDuplicateCommand(state.lastCheckInTime, 60000)) {
		state.lastCheckInTime = new Date().time
		state.lastCheckInDate = convertToLocalTimeString(new Date())
	}
}

// iOS app has no way of clearing string input so workaround is to have users enter 0.
String getAssocDNIsSetting(grp) {
	def val = settings."assocDNI$grp"
	return ((val && (val.trim() != "0")) ? val : "") 
}

String setDevModel(BigDecimal firmware) {
	//Stash the model in a state variable
	def devTypeId = convertIntListToHexList([safeToInt(device.getDataValue("deviceType")),safeToInt(device.getDataValue("deviceId"))],4)
	String devModel = deviceModelNames[devTypeId.join(":")] ?: "UNK00"

	logDebug "Set Device Info - Model: ${devModel} | Firmware: ${firmware}"
	state.deviceModel = devModel
	device.updateDataValue("deviceModel", devModel)

	if (devModel == "UNK00") {
		logWarn "Unsupported Device USE AT YOUR OWN RISK: ${devTypeId}"
		state.WARNING = "Unsupported Device Model - USE AT YOUR OWN RISK!"
	}
	else state.remove("WARNING")

	return devModel
}

Integer getDeviceModelShort() {
	return safeToInt(state.deviceModel?.drop(3))
}

BigDecimal getFirmwareVersion() {
	String version = device?.getDataValue("firmwareVersion")
	return ((version != null) && version.isNumber()) ? version.toBigDecimal() : 0.0
}

String convertToLocalTimeString(dt) {
	def timeZoneId = location?.timeZone?.ID
	if (timeZoneId) {
		return dt.format("MM/dd/yyyy hh:mm:ss a", TimeZone.getTimeZone(timeZoneId))
	} else {
		return "$dt"
	}
}

List convertIntListToHexList(intList, pad=2) {
	def hexList = []
	intList?.each {
		hexList.add(Integer.toHexString(it).padLeft(pad, "0").toUpperCase())
	}
	return hexList
}

List convertHexListToIntList(String[] hexList) {
	def intList = []

	hexList?.each {
		try {
			it = it.trim()
			intList.add(Integer.parseInt(it, 16))
		}
		catch (e) { }
	}
	return intList
}

Integer validateRange(val, Integer defaultVal, Integer lowVal, Integer highVal) {
	Integer intVal = safeToInt(val, defaultVal)
	if (intVal > highVal) {
		return highVal
	} else if (intVal < lowVal) {
		return lowVal
	} else {
		return intVal
	}
}

private safeToInt(val, defaultVal=0) {
	if ("${val}"?.isInteger())		{ return "${val}".toInteger() } 
	else if ("${val}"?.isDouble())	{ return "${val}".toDouble()?.round() }
	else { return defaultVal }
}

private safeToDec(val, defaultVal=0, roundTo=-1) {
	def decVal = "${val}"?.isBigDecimal() ? "${val}".toBigDecimal() : defaultVal
    if (roundTo == 0)       { decVal = Math.round(decVal) }
    else if (roundTo > 0)   { decVal = decVal.doubleValue().round(roundTo) }
	return decVal
}

boolean isDuplicateCommand(lastExecuted, allowedMil) {
	!lastExecuted ? false : (lastExecuted + allowedMil > new Date().time)
}


/*******************************************************************
 ***** Logging Functions
********************************************************************/
private logForceWakeupMessage(msg) {
	String helpText = "You can force a wake up by pressing the Z-Wave button 4 times."
	logWarn "${msg} will execute the next time the device wakes up.  ${helpText}"
	state.INFO = "*** ${msg} *** Waiting for device to wake up.  ${helpText}"
}

void logsOff() {}
void debugLogsOff() {
	logWarn "Debug logging disabled..."
	device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

void logWarn(String msg) {
	log.warn "${device.displayName}: ${msg}"
}

void logInfo(String msg) {
	if (txtEnable) log.info "${device.displayName}: ${msg}"
}

void logDebug(String msg) {
	if (debugEnable) log.debug "${device.displayName}: ${msg}"
}

//For Extreme Code Debugging - tracing commands
void logTrace(String msg) {
	//Uncomment to Enable
	//log.trace "${device.displayName}: ${msg}"
}
