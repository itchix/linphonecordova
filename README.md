# Linphone Plugin for Cordova

This plugin use Linphone library to activate VoIP communication. It was written for Android and iOS.

**_This plugin is still in development._**

## Supported platforms

* Android
* iOS


# Installing

Install with Cordova cli

    $ cordova plugin add cordova.scrachx.linphone
    
# API

## Methods

- [linphone.saveAuthInfo](#saveAuthInfo)
- [linphone.saveSipInfo](#saveSipInfo)
- [linphone.saveProxyInfo](#saveProxyInfo)
- [linphone.login](#login)
- [linphone.logout](#logout)
- [linphone.call](#call)
- [linphone.answerCall](#answerCall)
- [linphone.hangup](#hangup)
- [linphone.toggleSpeaker](#toggleSpeaker)
- [linphone.toggleMute](#toggleMute)
- [linphone.registerCallback](#registerCallback)
- [linphone.authInfoReqCallback](#authInfoReqCallback)
- [linphone.globalCallback](#globalCallback)
- [linphone.callStateCallback](#callStateCallback)
- [linphone.callStatsCallback](#callStatsCallback)
- [linphone.messageReceivedCallback](#messageReceivedCallback)
- [linphone.sendMessage](#sendMessage)
- [linphone.videoCall](#videoCall)
- [linphone.sendDtmf](#sendDtmf)

## saveAuthInfo

Save SIP credentials 

    linphone.saveAuthInfo(username, userid, domain, password, callbackSuccess, callbackFailure);

### Description

Function `saveAuthInfo` save SIP credentials.  The callback is long running.  Success will be called when credentials are saved.  Failure is called if it can't save credentials. An error message is passed to the failure callback.

## saveSipInfo

Save SIP proxy info 

    linphone.saveSipInfo(port, ipv6, incTimeout, keepAlive, callbackSuccess, callbackFailure);

### Description

Function `saveSipInfo` save SIP info.  The callback is long running.  Success will be called when SIP info are saved.  Failure is called if it can't save SIP info. An error message is passed to the failure callback.

## saveProxyInfo

Save SIP proxy info 

    linphone.saveProxyInfo(regProxy, regIdentity, regExpire, regSendRegister, publish, dialEscapePlus, callbackSuccess, callbackFailure);

### Description

Function `saveProxyInfo` save SIP proxy info.  The callback is long running.  Success will be called when proxy info are saved.  Failure is called if it can't save proxy info. An error message is passed to the failure callback.

## login

Log into the SIP server

    linphone.login(callbackSuccess, callbackFailure);

### Description

Function `login` log into the SIP server.  The callback is long running.  Success will be called when it's logged.  Failure is called if it can't connect to the server. An error message is passed to the failure callback.

## logout

Logout from the server

    linphone.logout(callbackSuccess, callbackFailure);

### Description

Function `logout` log into the SIP server.  The callback is long running.  Success will be called when it's disconnected.  Failure is called if it can't disconnect. An error message is passed to the failure callback.

## call

Call a number

    linphone.call(address, displayName, callbackSuccess, callbackFailure);

### Description

Function `call` make call.  The callback is long running.  Success will be called when it's calling.  Failure is called if it can't call. An error message is passed to the failure callback.

## answerCall

Call a number

    linphone.answerCall(address, displayName, callbackSuccess, callbackFailure);

### Description

Function `answerCall` answer call.  The callback is long running.  Success will be called when it answer call.  Failure is called if it can't answer call. An error message is passed to the failure callback.

## hangup

Hangup call

    linphone.hangup(callbackSuccess, callbackFailure);

### Description

Function `hangup` hangup call.  The callback is long running.  Success will be called when it hangup call.  Failure is called if it can't hangup call. An error message is passed to the failure callback.

## toggleSpeaker

Toggle speaker

    linphone.toggleSpeaker(callbackSuccess, callbackFailure);

### Description

Function `toggleSpeaker` toggle speaker.  The callback is long running.  Success will be called when it toggle speaker.  Failure is called if it can't toggle speaker. An error message is passed to the failure callback.

## toggleMute

Toggle mute

    linphone.toggleMute(callbackSuccess, callbackFailure);

### Description

Function `toggleMute` toggle mute.  The callback is long running.  Success will be called when it toggle mute.  Failure is called if it can't toggle mute. An error message is passed to the failure callback.

## registerCallback

Register callback

    linphone.registerCallback(callbackSuccess, callbackFailure);

### Description

Function `registerCallback` is a listener for register.  The callback is long running.  Success will be called to show each state of registration.

## authInfoReqCallback

Authentication callback

    linphone.authInfoReqCallback(callbackSuccess, callbackFailure);

### Description

Function `authInfoReqCallback` is a listener for authentication.  The callback is long running.  Success will be called to show info of the connection.

## globalCallback

Global callback

    linphone.globalCallback(callbackSuccess, callbackFailure);

### Description

Function `globalCallback` is a listener for global state.  The callback is long running.  Success will be called to show global state.  

## callStateCallback

Call state callback

    linphone.callStateCallback(callbackSuccess, callbackFailure);

### Description

Function `callStateCallback` is a listener for call state.  The callback is long running.  Success will be called to show call state.  

## callStatsCallback

Call statistics callback

    linphone.callStatsCallback(callbackSuccess, callbackFailure);

### Description

Function `callStatsCallback` is a listener for call statistics state.  The callback is long running.  Success will be called to show call statistics state.

## messageReceivedCallback

Message received callback

    linphone.messageReceivedCallback(callbackSuccess, callbackFailure);

### Description

Function `messageReceivedCallback` is a listener for message received.  The callback is long running.  Success will be called to show message received.

## sendMessage

Send message

    linphone.sendMessage(message, sipAddress, newChatRoom, successCallback, errorCallback);

### Description

Function `sendMessage` send text message. Success will be called when message is sent.  Failure is called if it can't send message. An error message is passed to the failure callback.

## videoCall

Video call

    linphone.videoCall(address, displayName, successCallback, errorCallback);

### Description

Function `videoCall` make a video call. Success will be called when video call has started.  Failure is called if it can't make a call. An error message is passed to the failure callback.

## sendDtmf

Send DTMF

    linphone.sendDtmf(character, successCallback, errorCallback);

### Description

Function `sendDtmf` send DTMF. Success will be called when dtmf is sent.  Failure is called if it can't be sent. An error message is passed to the failure callback.



# IOS configuration

Please add in "Header Search Path" : "$(SRCROOT)/HelloCordova/Plugins/cordova.scrachx.linphone"
