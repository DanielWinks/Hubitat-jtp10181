/*
 *  Zooz ZEN16 MultiRelay Advanced
 *    - Model: ZEN16 - MINIMUM FIRMWARE 1.02
 *    - Model: ZEN16 v2 - MINIMUM FIRMWARE 2.00
 *
 *  For Support, Information, and Updates:
 *  https://community.hubitat.com/t/zooz-relays-advanced/98194
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz
 *

Changelog:

## [0.2.0] - 2023-11-09 (@jtp10181)
  - Fixed issue with settings if both v1 and v2 in use on same hub
  - Added subModel state

## [0.1.0] - 2023-11-03 (@jtp10181)
  - Initial alpha release

 *  Copyright 2023 Jeff Page
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

@Field static final String VERSION = "0.2.0"
@Field static final String DRIVER = "Zooz-ZEN16"
@Field static final String COMM_LINK = "https://community.hubitat.com/t/zooz-relays-advanced/98194"
@Field static final Map deviceModelNames = ["A000:A00A":"ZEN16"]

metadata {
	definition (
		name: "Zooz ZEN16 MultiRelay Advanced",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/Hubitat/main/Drivers/zooz/zooz-zen16-multirelay.groovy"
	) {
		capability "Actuator"
		capability "Switch"
		capability "Configuration"
		capability "Refresh"

		//command "refreshParams"

		command "setParameter",[[name:"parameterNumber*",type:"NUMBER", description:"Parameter Number"],
			[name:"value*",type:"NUMBER", description:"Parameter Value"],
			[name:"size",type:"NUMBER", description:"Parameter Size"]]

		//DEBUGGING
		//command "debugShowVars"

		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"A000", deviceId:"A00A", inClusters:"0x00,0x00" //Zooz ZEN16 MultiRelay
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

		// for(int i in 2..maxAssocGroups) {
		// 	input "assocDNI$i", "string",
		// 		title: fmtTitle("Device Associations - Group $i"),
		// 		description: fmtDesc("Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. Check device documentation for more info. Save as blank or 0 to clear."),
		// 		required: false
		// }

		if (firmwareVersion >= 2) {
			input "childCleanup", "bool",
				title: fmtTitle("Clean Up Sensor Child Devices"),
				description: fmtDesc("WARNING: This will remove any sensor child devices that are unnecessary or do not have the capabilities for the selected input type."),
				defaultValue: false
		}
	}
}

void debugShowVars() {
	log.warn "settings ${settings.hashCode()} ${settings}"
	log.warn "paramsList ${paramsList.hashCode()} ${paramsList}"
	log.warn "paramsMap ${paramsMap.hashCode()} ${paramsMap}"
}

//Association Settings
@Field static final int maxAssocGroups = 1
@Field static final int maxAssocNodes = 1

/*** Static Lists and Settings ***/
@Field static final Map inputTypes = [			
	0:"Momentary (for lights only)",
	1:"Toggle Switch On/Off",
	2:"Toggle Switch State Change",
	3:"Garage Door Momentary (Z-Wave control)",
	4:"Water Sensor",
	5:"Heat Sensor",
	6:"Motion Sensor",
	7:"Contact Sensor",
	8:"Carbon Monoxide (CO) Sensor",
	9:"Carbon Dioxide (CO₂) Sensor",
	10:"Dry Contact Switch/Sensor",
	11:"R: Garage Door / Sw: Contact Sensor"
]
@Field static final Map inputCapabilities = [			
	4:"WaterSensor",
	5:"Switch",
	6:"MotionSensor",
	7:"ContactSensor",
	8:"CarbonMonoxideDetector ",
	9:"CarbonDioxideMeasurement",
	10:"Switch",
	11:"ContactSensor"
]

//Main Parameters Listing
@Field static Map<String, Map> paramsMap =
[
	powerFailure: [ num:1,
		title: "On / Off Status After Power Failure",
		size: 1, defaultVal: 1,
		options: [
			0:"All relays turned OFF",
			1:"All relays restores last state",
			2:"All relays turned ON",
			3:"R1/R2 restores last, R3 turns OFF",
			4:"R1/R2 restores last, R3 turns ON",
		]
	],
	ledIndicator: [ num:5,
		title: "LED Indicator Control",
		size: 1, defaultVal: 0,
		options: [
			0:"LED on when ALL relays off",
			1:"LED on when ANY relays on",
			2:"LED Indicator always off",
			3:"LED Indicator always on",
		]
	],
	inputSw1: [ num:2,
		title: "Input Type for Sw1",
		description: "Power Cycle the device after changing this setting",
		size: 1, defaultVal: 2,
		options: [:]  //inputTypes
	],
	controlSw1: [ num:12,
		title: "Input Control Sw1",
		description: "Should Sw input automatically activate the Relay",
		size: 1, defaultVal: 1,
		options: [1:"Activate Relay and Send Status", 0:"Send Input Sensor Status Only", 2:"Send All Status, Do Not Activate Relay"]
	],
	reverseSw1: [ num:25,
		title: "Reverse Sensor Values on Sw1",
		description: "See online device docs for which triggers allow this",
		size: 1, defaultVal: 0,
		options: [0:"Normal", 1:"Reversed"],
		firmVer: 2.0
	],
	inputSw2: [ num:3,
		title: "Input Type for Sw2",
		description: "Power Cycle the device after changing this setting",
		size: 1, defaultVal: 2,
		options: [:]  //inputTypes
	],
	controlSw2: [ num:13,
		title: "Input Control Sw2",
		description: "Should Sw input automatically activate the Relay",
		size: 1, defaultVal: 1,
		options: [1:"Activate Relay and Send Status", 0:"Send Input Sensor Status Only", 2:"Send All Status, Do Not Activate Relay"]
	],
	reverseSw2: [ num:26,
		title: "Reverse Sensor Values on Sw2",
		description: "See online device docs for which triggers allow this",
		size: 1, defaultVal: 0,
		options: [0:"Normal", 1:"Reversed"],
		firmVer: 2.0
	],
	inputSw3: [ num:4,
		title: "Input Type for Sw3",
		description: "Power Cycle the device after changing this setting",
		size: 1, defaultVal: 2,
		options: [:]  //inputTypes
	],
	controlSw3: [ num:14,
		title: "Input Control Sw3",
		description: "Should Sw input automatically activate the Relay",
		size: 1, defaultVal: 1,
		options: [1:"Activate Relay and Send Status", 0:"Send Input Sensor Status Only", 2:"Send All Status, Do Not Activate Relay"]
	],
	reverseSw3: [ num:27,
		title: "Reverse Sensor Values on Sw3",
		description: "See online device docs for which triggers allow this",
		size: 1, defaultVal: 0,
		options: [0:"Normal", 1:"Reversed"],
		firmVer: 2.0
	],
	//R1 Timers
	timerOffTime1: [ num:6,
		title: "Auto Turn-Off R1: TIME",
		size: 4, defaultVal: 0,
		range: "0..65535"
	],
	timerOffUnits1: [ num:15,
		title: "Auto Turn-Off R1: UNITS",
		size: 1, defaultVal: 0,
		options: [0:"minutes",1:"seconds",2:"hours"]
	],
	timerOnTime1: [ num:7,
		title: "Auto Turn-On R1: TIME",
		size: 4, defaultVal: 0,
		range: "0..65535"
	],
	timerOnUnits1: [ num:16,
		title: "Auto Turn-On R1: UNITS",
		size: 1, defaultVal: 0,
		options: [0:"minutes",1:"seconds",2:"hours"]
	],
	//R2 Timers
	timerOffTime2: [ num:8,
		title: "Auto Turn-Off R2: TIME",
		size: 4, defaultVal: 0,
		range: "0..65535"
	],
	timerOffUnits2: [ num:17,
		title: "Auto Turn-Off R2: UNITS",
		size: 1, defaultVal: 0,
		options: [0:"minutes",1:"seconds",2:"hours"]
	],
	timerOnTime2: [ num:9,
		title: "Auto Turn-On R2: TIME",
		size: 4, defaultVal: 0,
		range: "0..65535"
	],
	timerOnUnits2: [ num:18,
		title: "Auto Turn-On R2: UNITS",
		size: 1, defaultVal: 0,
		options: [0:"minutes",1:"seconds",2:"hours"]
	],
	//R3 Timers
	timerOffTime3: [ num:10,
		title: "Auto Turn-Off R3: TIME",
		size: 4, defaultVal: 0,
		range: "0..65535"
	],
	timerOffUnits3: [ num:19,
		title: "Auto Turn-Off R3: UNITS",
		size: 1, defaultVal: 0,
		options: [0:"minutes",1:"seconds",2:"hours"]
	],
	timerOnTime3: [ num:11,
		title: "Auto Turn-On R3: TIME",
		size: 4, defaultVal: 0,
		range: "0..65535"
	],
	timerOnUnits3: [ num:20,
		title: "Auto Turn-On R3: UNITS",
		size: 1, defaultVal: 0,
		options: [0:"minutes",1:"seconds",2:"hours"]
	],
	//End of Timers
	relay1Default: [ num:21,
		title: "Default Relay State R1",
		size: 1, defaultVal: 0,
		options: [0:"Normally Open (Reports off, Sw off = R off)", 1:"Normally Closed ON (Reports on, Sw off = R on)", 2:"Normally Closed OFF (Reports off, Sw off = R on)"],
		firmVer: 1.03
	],
	relay2Default: [ num:22,
		title: "Default Relay State R2",
		size: 1, defaultVal: 0,
		options: [0:"Normally Open (Reports off, Sw off = R off)", 1:"Normally Closed ON (Reports on, Sw off = R on)", 2:"Normally Closed OFF (Reports off, Sw off = R on)"],
		firmVer: 1.03
	],
	relay3Default: [ num:23,
		title: "Default Relay State R3",
		size: 1, defaultVal: 0,
		options: [0:"Normally Open (Reports off, Sw off = R off)", 1:"Normally Closed ON (Reports on, Sw off = R on)", 2:"Normally Closed OFF (Reports off, Sw off = R on)"],
		firmVer: 1.03
	],
	dcMotorMode: [ num:24,
		title: "DC Motor Mode",
		description: "Sync R1 and R2 so they cannot be activated at the same time",
		size: 1, defaultVal: 0,
		options: [0:"Disabled",1:"Enabled"],
		firmVer: 1.03
	],
]

/* ZEN16 v2.00
CommandClassReport - class:0x20, version:2   (Basic)
CommandClassReport - class:0x25, version:2   (Binary Switch)
CommandClassReport - class:0x55, version:2   (Transport Service)
CommandClassReport - class:0x59, version:3   (Association Group Information (AGI))
CommandClassReport - class:0x5A, version:1   (Device Reset Locally)
CommandClassReport - class:0x5E, version:2   (Z-Wave Plus Info)
CommandClassReport - class:0x60, version:4   (Multi Channel)
CommandClassReport - class:0x6C, version:1   (Supervision)
CommandClassReport - class:0x70, version:4   (Configuration)
CommandClassReport - class:0x72, version:2   (Manufacturer Specific)
CommandClassReport - class:0x73, version:1   (Powerlevel)
CommandClassReport - class:0x7A, version:5   (Firmware Update Meta Data)
CommandClassReport - class:0x85, version:3   (Association)
CommandClassReport - class:0x86, version:3   (Version)
CommandClassReport - class:0x8E, version:4   (Multi Channel Association)
CommandClassReport - class:0x9F, version:1   (Security 2)
*/

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x25: 1,	// switchBinary
	0x60: 3,	// multiChannel
	0x6C: 1,	// supervision
	0x70: 2,	// configuration
	0x85: 2,	// association
	0x86: 2,	// version
	0x8E: 3,	// multiChannelAssociation
]


/*******************************************************************
 ***** Core Functions
********************************************************************/
void installed() {
	logWarn "installed..."
	initialize()
}

void initialize() {
	logWarn "initialize..."
	refresh()
}

void configure() {
	logWarn "configure..."

	if (!pendingChanges || state.resyncAll == null) {
		logDebug "Enabling Full Re-Sync"
		clearVariables()
		state.resyncAll = true
	}

	updateSyncingStatus(6)
	runIn(1, executeProbeCmds)
	runIn(2, executeRefreshCmds)
	runIn(5, executeConfigureCmds)
}

void updated() {
	logDebug "updated..."

	//Check Child Capabilities
	childDevices.each { child ->
		String ep = child.getDataValue("endPoint")
		if (ep && ep[-1] == "S") {
			String epNum = ep.substring(0, ep.length() - 1)
			Integer inputType = getParamValue("inputSw${epNum}" as String)
			String logMsg = null
			logDebug "Sensor endPoint ${ep} found, selected Input Type ${inputType}:${inputTypes[inputType]}"
			switch (inputType) {
				case 0..3:
					logMsg = "Sensor Child ${ep} is not needed with Input Type ${inputType}:${inputTypes[inputType]}."
					break
				case 4..11:
					if (!child.hasCapability(inputCapabilities[inputType])) {
						logMsg = "Sensor Child ${ep} is missing ${inputCapabilities[inputType]} capability."
					}
					break
				default:
					logWarn "Input Sw${epNum} has unknown Input Type of ${inputType}"
			}
			if (logMsg) {
				if (childCleanup) {
					logWarn "${logMsg} <b>*REMOVING Child Device*</b>"
					deleteChildDevice(child.deviceNetworkId)
				} else {
					logWarn "${logMsg} <b>*Change the Driver/Type or run Child Cleanup*</b>"
				}
			}
			if (childCleanup) {
				logWarn "Cleaning up Sensor Child ${ep} Current States"
				child.getCurrentStates()?.each { child.deleteCurrentState(it.name) }
				runIn(5, executeRefreshCmds)
			}
		}
	}
	device.updateSetting("childCleanup",[value:"false",type:"bool"])

	setSubModel()
	executeProbeCmds()
	runIn(1, executeConfigureCmds)
	runIn(3, createChildDevices)
}

void refresh() {
	logDebug "refresh..."
	setSubModel()
	executeRefreshCmds()
}


/*******************************************************************
 ***** Driver Commands
********************************************************************/
/*** Capabilities ***/
def on() {
	logDebug "on..."
	return getOnOffCmds(0xFF)
}

def off() {
	logDebug "off..."
	return getOnOffCmds(0x00)
}


/*** Custom Commands ***/
void refreshParams() {
	List<String> cmds = []

	//Refresh Only Out-of-Sync
	configParams.each { param ->
		Integer paramVal = getParamValueAdj(param)
		Integer storedVal = getParamStoredValue(param.num)

		if (paramVal != null && storedVal != paramVal) {
			logDebug "Refreshing ${param.name} (#${param.num}), currently: ${storedVal}"
			cmds += configGetCmd(param)
		}
	}

	if (cmds) sendCommands(cmds)
}

String setParameter(paramNum, value, size = null) {
	Map param = getParam(paramNum)
	if (param && !size) { size = param.size	}

	if (paramNum == null || value == null || size == null) {
		logWarn "Incomplete parameter list supplied..."
		logWarn "Syntax: setParameter(paramNum, value, size)"
		return
	}
	logDebug "setParameter ( number: $paramNum, value: $value, size: $size )" + (param ? " [${param.name}]" : "")
	return secureCmd(configSetCmd([num: paramNum, size: size], value as Integer))
}

/*** Child Capabilities ***/
def componentOn(cd) {
	logDebug "componentOn from ${cd.displayName} (${cd.deviceNetworkId})"
	sendCommands(getOnOffCmds(0xFF, getChildEP(cd)))
}

def componentOff(cd) {
	logDebug "componentOff from ${cd.displayName} (${cd.deviceNetworkId})"
	sendCommands(getOnOffCmds(0x00, getChildEP(cd)))
}

def componentRefresh(cd) {
	logDebug "componentRefresh from ${cd.displayName} (${cd.deviceNetworkId})"
	sendCommands(getChildRefreshCmds(getChildEP(cd)))
}

/*******************************************************************
 ***** Z-Wave Reports
********************************************************************/
void parse(String description) {
	zwaveParse(description)
}
void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	zwaveMultiChannel(cmd)
}
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
	zwaveSupervision(cmd,ep)
}

void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	Map param = getParam(cmd.parameterNumber)
	Integer val = cmd.scaledConfigurationValue

	if (param) {
		//Convert scaled signed integer to unsigned
		Long sizeFactor = Math.pow(256,param.size).round()
		if (val < 0) { val += sizeFactor }

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
		if (!state.endPoints) {
			logDebug "Lifeline Association: ${cmd.nodeId}"
			state.group1Assoc = (cmd.nodeId == [zwaveHubNodeId]) ? true : false
		}
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.multichannelassociationv3.MultiChannelAssociationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	List mcNodes = []
	cmd.multiChannelNodeIds.each {mcNodes += "${it.nodeId}:${it.endPointId}"}

	if (cmd.groupingIdentifier == 1) {
		if (state.endPoints) {
			logDebug "Lifeline Association: ${cmd.nodeId} | MC: ${mcNodes}"
			state.group1Assoc = (mcNodes == ["${zwaveHubNodeId}:0"] ? true : false)
		}
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendSwitchEvents(cmd.value, "physical", ep)
}

//All Switch Reports coming here on ZEN16 v2
void zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"

	String type = (state."isDigital$ep" ? "digital" : "physical")
	state.remove("isDigital$ep" as String)

	sendSwitchEvents(cmd.value, type, ep)
}

void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"

	if (cmd.endPoints > 0) {
		logDebug "Endpoints (${cmd.endPoints}) Detected and Enabled"
		state.endPoints = cmd.endPoints
		runIn(1,createChildDevices)
	}
}

void zwaveEvent(hubitat.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	
	//SensorBinary is depreciated so using NotificationReport
	/*String sensorEp = "${ep}S"
	switch (cmd.sensorType as Integer) {
		case 0x01:  //General Purpose (ZEN16 Dry Contact)
			sendEventLog(name:"switch", value:(cmd.sensorValue ? "on" : "off"), sensorEp)
			break
		case 0x03:  //CO
			sendEventLog(name:"carbonMonoxide", value:(cmd.sensorValue ? "detected" : "clear"), sensorEp)
			break
		case 0x04:  //CO2
			sendEventLog(name:"carbonDioxide", value:(cmd.sensorValue ? "detected" : "clear"), sensorEp)
			break
		case 0x05:  //Heat
			sendEventLog(name:"switch", value:(cmd.sensorValue ? "on" : "off"), sensorEp)
			break
		case 0x06:  //Water
			sendEventLog(name:"water", value:(cmd.sensorValue ? "wet" : "dry"), sensorEp)
			break
		case 0x0A:  //Door/Window
			sendEventLog(name:"contact", value:(cmd.sensorValue ? "open" : "closed"), sensorEp)
			break
		case 0x0C:  //Motion
			sendEventLog(name:"motion", value:(cmd.sensorValue ? "active" : "inactive"), sensorEp)
			break
		default:
			logDebug "Unhandled: ${cmd} (ep ${ep})"
	}*/
}

//Unplug and restart ZEN16 after making changes or these are not accurate
void zwaveEvent(hubitat.zwave.commands.notificationv8.NotificationReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	String sensorEp = "${ep}S"

	switch (cmd.notificationType as Integer) {
		case 0x05:  //Water
			logDebug "${cmd} (ep ${ep}) -- Water"
			sendEventLog(name:"water", value:(cmd.event ? "wet" : "dry"), sensorEp)
			break
		case 0x04:  //Heat
			logDebug "${cmd} (ep ${ep}) -- Heat"
			sendEventLog(name:"switch", value:(cmd.event ? "on" : "off"), sensorEp)
			break
		case 0x07:  //Home Security
			logDebug "${cmd} (ep ${ep}) -- Motion"
			sendEventLog(name:"motion", value:(cmd.event ? "active" : "inactive"), sensorEp)
			break
		case 0x06:  //Access Control - Door/Window
			logDebug "${cmd} (ep ${ep}) -- Contact"
			if      (cmd.event == 0x16) sendEventLog(name:"contact", value:"open", sensorEp)
			else if (cmd.event == 0x17) sendEventLog(name:"contact", value:"closed", sensorEp)
			break
		case 0x02:  //CO
			logDebug "${cmd} (ep ${ep}) -- CO"
			sendEventLog(name:"carbonMonoxide", value:(cmd.event ? "detected" : "clear"), sensorEp)
			break
		case 0x03:  //CO2
			logDebug "${cmd} (ep ${ep}) -- CO2"
			sendEventLog(name:"carbonDioxide", value:(cmd.event ? "detected" : "clear"), sensorEp)
			break
		case 0x00:  //Generic (Switch)
			logDebug "${cmd} (ep ${ep}) -- Dry Contact Switch"
			sendEventLog(name:"switch", value:(cmd.event ? "on" : "off"), sensorEp)
			break
		default:
			logDebug "Unhandled: ${cmd} (ep ${ep})"
	}
}


/*******************************************************************
 ***** Event Senders
********************************************************************/
//evt = [name, value, type, unit, desc, isStateChange]
void sendEventLog(Map evt, ep=0) {
	//Set description if not passed in
	evt.descriptionText = evt.desc ?: "${evt.name} set to ${evt.value} ${evt.unit ?: ''}".trim()

	//Endpoint Events
	if (ep) {
		def childDev = getChildByEP(ep)
	
		if (childDev) {
			if (childDev.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange) {
				evt.descriptionText = "${childDev}: ${evt.descriptionText}"
				childDev.parse([evt])
			} else {
				String epName = "Relay ${ep}"
				logDebug "(${epName}) ${evt.descriptionText} [NOT CHANGED]"
				childDev.sendEvent(evt)
			}
		}
		else {
			logErr "No device for endpoint (${ep}). Press Configure to create child devices."
		}
		return
	}

	//Main Device Events
	if (device.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange) {
		logInfo "${evt.descriptionText}"
	} else {
		logDebug "${evt.descriptionText} [NOT CHANGED]"
	}
	//Always send event to update last activity
	sendEvent(evt)
}

void sendSwitchEvents(rawVal, String type, Integer ep=0) {
	String value = (rawVal ? "on" : "off")
	String desc = "switch is turned ${value}" + (type ? " (${type})" : "")
	sendEventLog(name:"switch", value:value, type:type, desc:desc, ep)
}
void removeDigital() {
	state.remove("isDigital0")
	endPointList.each { state.remove("isDigital$it" as String) }
}


/*******************************************************************
 ***** Execute / Build Commands
********************************************************************/
void executeConfigureCmds() {
	logDebug "executeConfigureCmds..."

	//Checks and sets scheduled turn off
	checkLogLevel()

	List<String> cmds = []

	//Make sure its paying attention
	cmds << configSetCmd([num:0, size:1], 0)

	if (!firmwareVersion || !state.deviceModel) {
		cmds << versionGetCmd()
	}

	cmds += getConfigureAssocsCmds()

	configParams.each { param ->
		Integer paramVal = getParamValueAdj(param)
		Integer storedVal = getParamStoredValue(param.num)

		if ((paramVal != null) && (state.resyncAll || (storedVal != paramVal))) {
			logDebug "Changing ${param.name} (#${param.num}) from ${storedVal} to ${paramVal}"
			cmds += configSetGetCmd(param, paramVal)
		}
	}

	state.resyncAll = false

	if (cmds) runIn(5, refreshParams)
	if (cmds) sendCommands(cmds,300)
}

void executeProbeCmds() {
	logTrace "executeProbeCmds..."

	List<String> cmds = []

	//End Points Check
	if (state.endPoints == null) {
		logDebug "Probing for Multiple End Points"
		cmds << secureCmd(zwave.multiChannelV3.multiChannelEndPointGet())
		state.endPoints = 0
	}

	if (cmds) sendCommands(cmds)
}

void executeRefreshCmds() {
	List<String> cmds = []

	if (state.resyncAll || !firmwareVersion || !state.deviceModel) {
		cmds << versionGetCmd()
	}

	//Refresh Switch
	cmds << switchBinaryGetCmd()

	//Refresh Children
	endPointList.each { endPoint ->
		cmds += getChildRefreshCmds(endPoint)
	}

	sendCommands(cmds,300)
}

List getConfigureAssocsCmds() {
	List<String> cmds = []

	if (!state.group1Assoc || state.resyncAll) {
		if (state.group1Assoc == false) {
			logDebug "Need to reset lifeline association..."
			cmds << associationRemoveCmd(1,[])
			cmds << secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationRemove(groupingIdentifier: 1, nodeId:[], multiChannelNodeIds:[]))
		}
		logTrace "getConfigureAssocsCmds endPoints: ${state.endPoints}"
		if (state.endPoints > 0) {
			cmds << secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationSet(groupingIdentifier: 1, multiChannelNodeIds: [[nodeId: zwaveHubNodeId, bitAddress:0, endPointId: 0]]))
			cmds << mcAssociationGetCmd(1)
		}
		else {
			cmds << associationSetCmd(1, [zwaveHubNodeId])
			cmds << associationGetCmd(1)
		}
	}

	return cmds
}

String getOnOffCmds(val, Integer endPoint=0) {
	List epList = (endPoint ? [endPoint] : endPointList)
	epList.each { state."isDigital$it" = true }
	state."isDigital0" = true
	runIn(3, removeDigital)

	return switchBinarySetCmd(val ? 0xFF : 0x00, endPoint)
}

List getChildRefreshCmds(Integer endPoint) {
	List<String> cmds = []
	cmds << switchBinaryGetCmd(endPoint)
	cmds << notificationGetCmd(0xFF, 0x00, endPoint)
	//cmds << secureCmd(zwave.sensorBinaryV2.sensorBinaryGet(sensorType:0xFF), endPoint)
	return cmds
}

private setSubModel() {
	String devModel = state.deviceModel
	if (!state.subModel) {
		if (devModel == "ZEN16" && firmwareVersion >= 2.0) {
			state.subModel = "v2"
		}
	}
}

/*******************************************************************
 ***** Required for Library
********************************************************************/
//These have to be added in after the fact or groovy complains
void fixParamsMap() {
	paramsMap.inputSw1.options = inputTypes.findAll { it.key <= 3 }
	paramsMap.inputSw2.options = inputTypes.findAll { it.key <= 3 }
	paramsMap.inputSw3.options = inputTypes.findAll { it.key <= 3 }
	paramsMap.inputSw1.changesFR = [(2..99):[options: inputTypes]]
	paramsMap.inputSw2.changesFR = [(2..99):[options: inputTypes]]
	paramsMap.inputSw3.changesFR = [(2..99):[options: inputTypes]]
	paramsMap['settings'] = [fixed: true]
}

Integer getParamValueAdj(Map param) {
	Integer paramVal = getParamValue(param)

	//Check and Set the reverse Sw settings
	def matches = (param.name =~ /reverse(Sw\d)/)
	//logDebug "getParamValueAdj: ${param.name} - ${matches.size()} - ${matches}"
	if (matches.size() == 1 && paramVal > 0) {
		String swNum = matches[0][1]
		Map trigParam = getParam("input${swNum}")
		Integer trigVal = getParamValue(trigParam)
		if (trigVal >=4 && trigVal <=10) {
			paramVal = trigVal
		}
		else { //Cannot Enable
			logWarn "Cannot Reverse when ${trigParam.title} = ${trigVal}"
			device.updateSetting("configParam${param.num}", [value:"0",type:"enum"])
			paramVal = 0
		}
	}

	return paramVal
}


/*******************************************************************
 ***** Child/Other Functions
********************************************************************/
/*** Child Creation Functions ***/
void createChildDevices() {
	endPointList.each { endPoint ->
		if (!getChildByEP(endPoint)) {
			logDebug "Creating new child device for endPoint ${endPoint}, did not find existing"
			addChild(endPoint)
		}

		Integer inputType = getParamValue("inputSw${endPoint}" as String)
		if (inputType >= 4) { //Need Sensor Child
			String sensorEp = "${endPoint}S"
			if (!getChildByEP(sensorEp)) {
				logDebug "Creating new child device for endPoint ${endPoint} (Sensor ${sensorEp}), did not find existing"
				addChild(sensorEp, inputType)
			}
		}
	}
}

void addChild(endPoint, inputType=null) {
	//Driver Settings
	Map deviceType = [namespace:"hubitat", typeName:"Generic Component Switch"]
	Map deviceTypeBak = [:]
	Map properties = [name:"${device.name}", isComponent:false, endPoint:"${endPoint}"]

	String dni = getChildDNI(endPoint)
	String epName = "Relay ${endPoint}"
	// properties.type = "R"

	//Handle Sensor Child Devices
	if (inputType != null) {
		epName = "Sensor ${endPoint}"
		// properties.type = "S"
		switch (inputType) {
			case 4: deviceType.typeName = "Generic Component Water Sensor"; break
			case 5: deviceType.typeName = "Generic Component Switch"; break
			case 6: deviceType.typeName = "Generic Component Motion Sensor"; break
			case 7: deviceType.typeName = "Generic Component Contact Sensor"; break
			case 8: deviceType.typeName = "Generic Component Carbon Monoxide Detector"; break
			case 9: deviceType.typeName = "Generic Component Carbon Dioxide Detector"; break
			case 10: deviceType.typeName = "Generic Component Switch"; break
			case 11: deviceType.typeName = "Generic Component Contact Sensor"; break
			default: deviceType.typeName = "Generic Component Switch"
		}
	}

	properties.name = "${device.name} - ${epName}"
	logDebug "Creating '${epName}' Child Device"

	def childDev
	try {
		childDev = addChildDevice(deviceType.namespace, deviceType.typeName, dni, properties)
	}
	catch (e) {
		logWarn "The '${deviceType}' driver failed"
		if (deviceTypeBak) {
			logWarn "Defaulting to '${deviceTypeBak}' instead"
			childDev = addChildDevice(deviceTypeBak.namespace, deviceTypeBak.typeName, dni, properties)
		}
	}
}

/*** Child Common Functions ***/
private getChildByEP(endPoint) {
	String devModel = state.deviceModel
	endPoint = endPoint.toString()
	//Searching using endPoint data value
	def childDev = childDevices?.find { it.getDataValue("endPoint") == endPoint }
	if (childDev) logTrace "Found Child for endPoint ${endPoint} using data.endPoint: ${childDev.displayName} (${childDev.deviceNetworkId})"
	//If not found try deeper search using the child DNIs
	else {
		childDev = childDevices?.find  { ch ->
			String ep = null
			List<String> dni = ch.deviceNetworkId.split('-')
			if (dni.size() <= 1) return false
			String dniEp = dni[1]

			//logWarn "getChildByEP dni.size ${dni.size()} -- ${dni}"
			if (dni[2] == "0" || !dni[2])  ep = dniEp  //Default Format DNI-<EP>
			else if (dni[2] == "1")  ep = "${dniEp}S"  //Format DNI-<EP>-1 (Sensor Child)

			//Return true if match found to save child device
			return (ep == endPoint)
		}
		if (childDev) {
			logDebug "Found Child for endPoint ${endPoint} parsing DNI: ${childDev.displayName} (${childDev.deviceNetworkId})"
			//Save the EP on the device so we can find it easily next time
			childDev.updateDataValue("endPoint","$endPoint")
		}
	}
	return childDev
}

private getChildEP(childDev) {
	Integer endPoint = safeToInt(childDev.getDataValue("endPoint")?.replaceAll("[^0-9]+",""))
	if (!endPoint) logWarn "Cannot determine endPoint number for $childDev (defaulting to 0), run Configure to detect existing endPoints"
	return endPoint
}

String getChildDNI(epName) {
	return "${device.deviceId}-${epName}".toUpperCase()
}

List getEndPointList() {
	return (state.endPoints>0 ? 1..(state.endPoints) : [])
}


//#include jtp10181.zwaveDriverLibrary
/*******************************************************************
 *******************************************************************
 ***** Z-Wave Driver Library by Jeff Page (@jtp10181)
 *******************************************************************
********************************************************************

Changelog:
2023-05-10 - First version used in drivers
2023-05-12 - Adjustments to community links
2023-05-14 - Updates for power metering
2023-05-18 - Adding requirement for getParamValueAdj in driver
2023-05-24 - Fix for possible RuntimeException error due to bad cron string
2023-10-25 - Less savings to the configVals data, and some new functions
2023-10-26 - Added some battery shortcut functions
2023-11-08 - Added ability to adjust settings on firmware range

********************************************************************/

library (
  author: "Jeff Page (@jtp10181)",
  category: "zwave",
  description: "Z-Wave Driver Library",
  name: "zwaveDriverLibrary",
  namespace: "jtp10181",
  documentationLink: ""
)

/*******************************************************************
 ***** Z-Wave Reports (COMMON)
********************************************************************/
//Include these in Driver
//void parse(String description) {zwaveParse(description)}
//void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {zwaveMultiChannel(cmd)}
//void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {zwaveSupervision(cmd,ep)}

void zwaveParse(String description) {
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

//Decodes Multichannel Encapsulated Commands
void zwaveMultiChannel(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	hubitat.zwave.Command encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"

	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, cmd.sourceEndPoint as Integer)
	} else {
		logWarn "Unable to extract encapsulated cmd from $cmd"
	}
}

//Decodes Supervision Encapsulated Commands (and replies to device)
void zwaveSupervision(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
	hubitat.zwave.Command encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"

	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, ep)
	} else {
		logWarn "Unable to extract encapsulated cmd from $cmd"
	}

	sendCommands(secureCmd(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0), ep))
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
	logTrace "${cmd}"

	String fullVersion = String.format("%d.%02d",cmd.firmware0Version,cmd.firmware0SubVersion)
	String zwaveVersion = String.format("%d.%02d",cmd.zWaveProtocolVersion,cmd.zWaveProtocolSubVersion)
	device.updateDataValue("firmwareVersion", fullVersion)
	device.updateDataValue("protocolVersion", zwaveVersion)
	device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")

	logDebug "Received Version Report - Firmware: ${fullVersion}"
	setDevModel(new BigDecimal(fullVersion))
}

void zwaveEvent(hubitat.zwave.Command cmd, ep=0) {
	logDebug "Unhandled zwaveEvent: $cmd (ep ${ep})"
}


/*******************************************************************
 ***** Z-Wave Command Shortcuts
********************************************************************/
//These send commands to the device either a list or a single command
void sendCommands(List<String> cmds, Long delay=200) {
	sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds, delay), hubitat.device.Protocol.ZWAVE))
}

//Single Command
void sendCommands(String cmd) {
	sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.ZWAVE))
}

//Consolidated zwave command functions so other code is easier to read
String associationSetCmd(Integer group, List<Integer> nodes) {
	return secureCmd(zwave.associationV2.associationSet(groupingIdentifier: group, nodeId: nodes))
}

String associationRemoveCmd(Integer group, List<Integer> nodes) {
	return secureCmd(zwave.associationV2.associationRemove(groupingIdentifier: group, nodeId: nodes))
}

String associationGetCmd(Integer group) {
	return secureCmd(zwave.associationV2.associationGet(groupingIdentifier: group))
}

String mcAssociationGetCmd(Integer group) {
	return secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationGet(groupingIdentifier: group))
}

String versionGetCmd() {
	return secureCmd(zwave.versionV2.versionGet())
}

String switchBinarySetCmd(Integer value, Integer ep=0) {
	return secureCmd(zwave.switchBinaryV1.switchBinarySet(switchValue: value), ep)
}

String switchBinaryGetCmd(Integer ep=0) {
	return secureCmd(zwave.switchBinaryV1.switchBinaryGet(), ep)
}

String switchMultilevelSetCmd(Integer value, Integer duration, Integer ep=0) {
	return secureCmd(zwave.switchMultilevelV4.switchMultilevelSet(dimmingDuration: duration, value: value), ep)
}

String switchMultilevelGetCmd(Integer ep=0) {
	return secureCmd(zwave.switchMultilevelV4.switchMultilevelGet(), ep)
}

String switchMultilevelStartLvChCmd(Boolean upDown, Integer duration, Integer ep=0) {
	//upDown: false=up, true=down
	return secureCmd(zwave.switchMultilevelV4.switchMultilevelStartLevelChange(upDown: upDown, ignoreStartLevel:1, dimmingDuration: duration), ep)
}

String switchMultilevelStopLvChCmd(Integer ep=0) {
	return secureCmd(zwave.switchMultilevelV4.switchMultilevelStopLevelChange(), ep)
}

String meterGetCmd(meter, Integer ep=0) {
	return secureCmd(zwave.meterV3.meterGet(scale: meter.scale), ep)
}

String meterResetCmd(Integer ep=0) {
	return secureCmd(zwave.meterV3.meterReset(), ep)
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

String sensorMultilevelGetCmd(sensorType) {
	Integer scale = (temperatureScale == "F" ? 1 : 0)
	return secureCmd(zwave.sensorMultilevelV11.sensorMultilevelGet(scale: scale, sensorType: sensorType))
}

String notificationGetCmd(notificationType, eventType, Integer ep=0) {
	return secureCmd(zwave.notificationV3.notificationGet(notificationType: notificationType, v1AlarmType:0, event: eventType), ep)
}

String configSetCmd(Map param, Integer value) {
	//Convert from unsigned to signed for scaledConfigurationValue
	Long sizeFactor = Math.pow(256,param.size).round()
	if (value >= sizeFactor/2) { value -= sizeFactor }

	return secureCmd(zwave.configurationV1.configurationSet(parameterNumber: param.num, size: param.size, scaledConfigurationValue: value))
}

String configGetCmd(Map param) {
	return secureCmd(zwave.configurationV1.configurationGet(parameterNumber: param.num))
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
		cmd = zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:ep).encapsulate(cmd)
	}
	return cmd.format()
}


/*******************************************************************
 ***** Common Functions
********************************************************************/
/*** Parameter Store Map Functions ***/
@Field static Map<String, Map> configsList = new java.util.concurrent.ConcurrentHashMap()
Integer getParamStoredValue(Integer paramNum) {
	//Using Data (Map) instead of State Variables
	Map configsMap = getParamStoredMap()
	return safeToInt(configsMap[paramNum], null)
}

void setParamStoredValue(Integer paramNum, Integer value) {
	//Using Data (Map) instead of State Variables
	TreeMap configsMap = getParamStoredMap()
	configsMap[paramNum] = value
	configsList[device.id][paramNum] = value
	//device.updateDataValue("configVals", configsMap.inspect())
}

Map getParamStoredMap() {
	TreeMap configsMap = configsList[device.id]
	if (configsMap == null) {
		configsMap = [:]
		if (device.getDataValue("configVals")) {
			try {
				configsMap = evaluate(device.getDataValue("configVals"))
			}
			catch(Exception e) {
				logWarn("Clearing Invalid configVals: ${e}")
				device.removeDataValue("configVals")
			}
		}
		configsList[device.id] = configsMap
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
		tmpMap.changesFR.each { m, changes ->
			if (firmware >= m.getFrom() && firmware <= m.getTo()) {
				tmpMap.putAll(changes)
				if (changes.options) { tmpMap.options = changes.options.clone() }
			}
		}
		//Don't need this anymore
		tmpMap.remove("changes")
		tmpMap.remove("changesFR")

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
	if (!paramsMap.settings?.fixed) fixParamsMap()
	if (paramsList[devModel] == null) updateParamsList()
	if (paramsList[devModel][firmware] == null) updateParamsList()
}

//Gets full list of params
List<Map> getConfigParams() {
	//logDebug "Get Config Params"
	if (!device) return []
	String devModel = state.deviceModel
	BigDecimal firmware = firmwareVersion

	//Try to get device model if not set
	if (devModel) { verifyParamsList() }
	else          { runInMillis(200, setDevModel) }
	//Bail out if unknown device
	if (!devModel || devModel == "UNK00") return []

	return paramsList[devModel][firmware]
}

//Get a single param by name or number
Map getParam(String search) {
	verifyParamsList()
	return configParams.find{ it.name == search }
}
Map getParam(Integer search) {
	verifyParamsList()
	return configParams.find{ it.num == search }
}

//Convert Param Value if Needed
BigDecimal getParamValue(String paramName) {
	return getParamValue(getParam(paramName))
}
BigDecimal getParamValue(Map param) {
	if (param == null) return
	BigDecimal paramVal = safeToDec(settings."configParam${param.num}", param.defaultVal)

	//Reset hidden parameters to default
	if (param.hidden && settings."configParam${param.num}" != null) {
		logWarn "Resetting hidden parameter ${param.name} (${param.num}) to default ${param.defaultVal}"
		device.removeSetting("configParam${param.num}")
		paramVal = param.defaultVal
	}

	return paramVal
}

/*** Preference Helpers ***/
String fmtTitle(String str) {
	return "<strong>${str}</strong>"
}
String fmtDesc(String str) {
	return "<div style='font-size: 85%; font-style: italic; padding: 1px 0px 4px 2px;'>${str}</div>"
}
String fmtHelpInfo(String str) {
	String info = "${DRIVER} v${VERSION}"
	String prefLink = "<a href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 70%;'>${info}</div></a>"
	String topStyle = "style='font-size: 18px; padding: 1px 12px; border: 2px solid Crimson; border-radius: 6px;'" //SlateGray
	String topLink = "<a ${topStyle} href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 14px;'>${info}</div></a>"

	return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>" +
		"<div style='text-align: center; position: absolute; top: 46px; right: 60px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"
}

private getTimeOptionsRange(String name, Integer multiplier, List range) {
	return range.collectEntries{ [(it*multiplier): "${it} ${name}${it == 1 ? '' : 's'}"] }
}

/*** Other Helper Functions ***/
void updateSyncingStatus(Integer delay=2) {
	runIn(delay, refreshSyncStatus)
	sendEvent(name:"syncStatus", value:"Syncing...")
}

void refreshSyncStatus() {
	Integer changes = pendingChanges
	sendEvent(name:"syncStatus", value:(changes ? "${changes} Pending Changes" : "Synced"))
	device.updateDataValue("configVals", getParamStoredMap()?.inspect())
}

void updateLastCheckIn() {
	def nowDate = new Date()
	state.lastCheckInDate = convertToLocalTimeString(nowDate)

	Long lastExecuted = state.lastCheckInTime ?: 0
	Long allowedMil = 24 * 60 * 60 * 1000   //24 Hours
	if (lastExecuted + allowedMil <= nowDate.time) {
		state.lastCheckInTime = nowDate.time
		if (lastExecuted) runIn(4, doCheckIn)
		scheduleCheckIn()
	}
}

void scheduleCheckIn() {
	def cal = Calendar.getInstance()
	cal.add(Calendar.MINUTE, -1)
	Integer hour = cal[Calendar.HOUR_OF_DAY]
	Integer minute = cal[Calendar.MINUTE]
	schedule( "0 ${minute} ${hour} * * ?", doCheckIn)
}

void doCheckIn() {
	String devModel = (state.deviceModel ?: "NA") + (state.subModel ? ".${state.subModel}" : "")
	String checkUri = "http://jtp10181.gateway.scarf.sh/${DRIVER}/chk-${devModel}-v${VERSION}"

	try {
		httpGet(uri:checkUri, timeout:4) { logDebug "Driver ${DRIVER} ${devModel} v${VERSION}" }
		state.lastCheckInTime = (new Date()).time
	} catch (Exception e) { }
}

Integer getPendingChanges() {
	Integer configChanges = configParams.count { param ->
		Integer paramVal = getParamValueAdj(param)
		((paramVal != null) && (paramVal != getParamStoredValue(param.num)))
	}
	Integer pendingAssocs = Math.ceil(getConfigureAssocsCmds()?.size()/2) ?: 0
	return (!state.resyncAll ? (configChanges + pendingAssocs) : configChanges)
}

//iOS app has no way of clearing string input so workaround is to have users enter 0.
String getAssocDNIsSetting(grp) {
	String val = settings."assocDNI$grp"
	return ((val && (val.trim() != "0")) ? val : "")
}

List getAssocDNIsSettingNodeIds(grp) {
	String dni = getAssocDNIsSetting(grp)
	List nodeIds = convertHexListToIntList(dni.split(","))

	if (dni && !nodeIds) {
		logWarn "'${dni}' is not a valid value for the 'Device Associations - Group ${grp}' setting.  All z-wave devices have a 2 character Device Network ID and if you're entering more than 1, use commas to separate them."
	}
	else if (nodeIds.size() > maxAssocNodes) {
		logWarn "The 'Device Associations - Group ${grp}' setting contains more than ${maxAssocNodes} IDs so some (or all) may not get associated."
	}

	return nodeIds
}

//Used with configure to reset variables
void clearVariables() {
	logWarn "Clearing state variables and data..."

	//Backup
	String devModel = state.deviceModel
	def engTime = state.energyTime

	//Clears State Variables
	state.clear()

	//Clear Config Data
	configsList["${device.id}"] = [:]
	device.removeDataValue("configVals")
	//Clear Data from other Drivers
	device.removeDataValue("zwaveAssociationG1")
	device.removeDataValue("zwaveAssociationG2")
	device.removeDataValue("zwaveAssociationG3")

	//Restore
	if (devModel) state.deviceModel = devModel
	if (engTime) state.energyTime = engTime
	//setDevModel()
}

//Stash the model in a state variable
String setDevModel(BigDecimal firmware) {
	if (!device) return
	def devTypeId = convertIntListToHexList([safeToInt(device.getDataValue("deviceType")),safeToInt(device.getDataValue("deviceId"))],4)
	String devModel = deviceModelNames[devTypeId.join(":")] ?: "UNK00"
	if (!firmware) { firmware = firmwareVersion }

	state.deviceModel = devModel
	device.updateDataValue("deviceModel", devModel)
	logDebug "Set Device Info - Model: ${devModel} | Firmware: ${firmware}"

	if (devModel == "UNK00") {
		logWarn "Unsupported Device USE AT YOUR OWN RISK: ${devTypeId}"
		state.WARNING = "Unsupported Device Model - USE AT YOUR OWN RISK!"
	}
	else state.remove("WARNING")

	//Setup parameters if not set
	verifyParamsList()

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

Integer convertLevel(level, userLevel=false) {
	if (levelCorrection) {
		Integer brightmax = getParamValue("maximumBrightness")
		Integer brightmin = getParamValue("minimumBrightness")
		brightmax = (brightmax == 99) ? 100 : brightmax
		brightmin = (brightmin == 1) ? 0 : brightmin

		if (userLevel) {
			//This converts what the user selected into a physical level within the min/max range
			level = ((brightmax-brightmin) * (level/100)) + brightmin
			state.levelActual = level
			level = validateRange(Math.round(level), brightmax, brightmin, brightmax)
		}
		else {
			//This takes the true physical level and converts to what we want to show to the user
			if (Math.round(state.levelActual ?: 0) == level) level = state.levelActual
			else state.levelActual = level

			level = ((level - brightmin) / (brightmax - brightmin)) * 100
			level = validateRange(Math.round(level), 100, 1, 100)
		}
	}
	else if (state.levelActual) {
		state.remove("levelActual")
	}

	return level
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

Integer safeToInt(val, defaultVal=0) {
	if ("${val}"?.isInteger())		{ return "${val}".toInteger() }
	else if ("${val}"?.isNumber())	{ return "${val}".toDouble()?.round() }
	else { return defaultVal }
}

BigDecimal safeToDec(val, defaultVal=0, roundTo=-1) {
	BigDecimal decVal = "${val}"?.isNumber() ? "${val}".toBigDecimal() : defaultVal
	if (roundTo == 0)		{ decVal = Math.round(decVal) }
	else if (roundTo > 0)	{ decVal = decVal.setScale(roundTo, BigDecimal.ROUND_HALF_UP).stripTrailingZeros() }
	if (decVal.scale()<0)	{ decVal = decVal.setScale(0) }
	return decVal
}

Boolean isDuplicateCommand(Long lastExecuted, Long allowedMil) {
	!lastExecuted ? false : (lastExecuted + allowedMil > new Date().time)
}


/*******************************************************************
 ***** Logging Functions
********************************************************************/
//Logging Level Options
@Field static final Map LOG_LEVELS = [0:"Error", 1:"Warn", 2:"Info", 3:"Debug", 4:"Trace"]
@Field static final Map LOG_TIMES = [0:"Indefinitely", 30:"30 Minutes", 60:"1 Hour", 120:"2 Hours", 180:"3 Hours", 360:"6 Hours", 720:"12 Hours", 1440:"24 Hours"]

/*//Command to set log level, OPTIONAL. Can be copied to driver or uncommented here
command "setLogLevel", [ [name:"Select Level*", description:"Log this type of message and above", type: "ENUM", constraints: LOG_LEVELS],
	[name:"Debug/Trace Time", description:"Timer for Debug/Trace logging", type: "ENUM", constraints: LOG_TIMES] ]
*/

//Additional Preferences
preferences {
	//Logging Options
	input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
		description: fmtDesc("Logs selected level and above"), defaultValue: 3, options: LOG_LEVELS
	input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
		description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 30, options: LOG_TIMES
	//Help Link
	input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
}

//Call this function from within updated() and configure() with no parameters: checkLogLevel()
void checkLogLevel(Map levelInfo = [level:null, time:null]) {
	unschedule(logsOff)
	//Set Defaults
	if (settings.logLevel == null) device.updateSetting("logLevel",[value:"3", type:"enum"])
	if (settings.logLevelTime == null) device.updateSetting("logLevelTime",[value:"30", type:"enum"])
	//Schedule turn off and log as needed
	if (levelInfo.level == null) levelInfo = getLogLevelInfo()
	String logMsg = "Logging Level is: ${LOG_LEVELS[levelInfo.level]} (${levelInfo.level})"
	if (levelInfo.level >= 3 && levelInfo.time > 0) {
		logMsg += " for ${LOG_TIMES[levelInfo.time]}"
		runIn(60*levelInfo.time, logsOff)
	}
	logInfo(logMsg)
}

//Function for optional command
void setLogLevel(String levelName, String timeName=null) {
	Integer level = LOG_LEVELS.find{ levelName.equalsIgnoreCase(it.value) }.key
	Integer time = LOG_TIMES.find{ timeName.equalsIgnoreCase(it.value) }.key
	device.updateSetting("logLevel",[value:"${level}", type:"enum"])
	checkLogLevel(level: level, time: time)
}

Map getLogLevelInfo() {
	Integer level = settings.logLevel as Integer ?: 3
	Integer time = settings.logLevelTime as Integer ?: 0
	return [level: level, time: time]
}

//Legacy Support
void debugLogsOff() {
	logWarn "Debug logging toggle disabled..."
	device.removeSetting("logEnable")
	device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

//Current Support
void logsOff() {
	logWarn "Debug and Trace logging disabled..."
	if (logLevelInfo.level >= 3) {
		device.updateSetting("logLevel",[value:"2", type:"enum"])
	}
}

//Logging Functions
void logErr(String msg) {
	log.error "${device.displayName}: ${msg}"
}
void logWarn(String msg) {
	if (logLevelInfo.level>=1) log.warn "${device.displayName}: ${msg}"
}
void logInfo(String msg) {
	if (logLevelInfo.level>=2) log.info "${device.displayName}: ${msg}"
}
void logDebug(String msg) {
	if (logLevelInfo.level>=3) log.debug "${device.displayName}: ${msg}"
}
void logTrace(String msg) {
	if (logLevelInfo.level>=4) log.trace "${device.displayName}: ${msg}"
}
