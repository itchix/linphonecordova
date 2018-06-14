module.exports = {
	login: function(successCallback, errorCallback) {
	    cordova.exec(
		successCallback,
		errorCallback,
		"Linphone",
		"login",
		[]
	    );
	},
	logout: function(successCallback, errorCallback) {
	    cordova.exec(
		successCallback,
		errorCallback,
		"Linphone",
		"logout",
		[]
	    );
	},
	call: function(address, displayName, successCallback, errorCallback) {
	    cordova.exec(
		successCallback,
		errorCallback,
		"Linphone",
		"call",
		[address, displayName]
	    );
	},
	sendMessage: function(message, sipAddress, newChatRoom, successCallback, errorCallback) {
	    cordova.exec(
		successCallback,
		errorCallback,
		"Linphone",
		"sendMessage",
		[message, sipAddress, newChatRoom]
	    );
	},
	answerCall: function(address, displayName, successCallback, errorCallback) {
		cordova.exec(
		successCallback,
		errorCallback,
		"Linphone",
		"answerCall",
		[]
		);
	    },
	videoCall: function(address, displayName, successCallback, errorCallback) {
	    cordova.exec(
		successCallback,
		errorCallback,
		"Linphone",
		"videoCall",
		[address, displayName]
	    );
	},
	hangup: function(successCallback, errorCallback) {
	    cordova.exec(
		successCallback,
		errorCallback,
		"Linphone",
		"hangup",
		[]
	    );
	},
	toggleVideo: function(successCallback, errorCallback) {
	    cordova.exec(
		successCallback,
		errorCallback,
		"Linphone",
		"toggleVideo",
		[]
	    );
	},
	toggleSpeaker: function(successCallback, errorCallback) {
	    cordova.exec(
		successCallback,
		errorCallback,
		"Linphone",
		"toggleSpeaker",
		[]
	    );
	},
	toggleMute: function(successCallback, errorCallback) {
	    cordova.exec(
		successCallback,
		errorCallback,
		"Linphone",
		"toggleMute",
		[]
	    );
	},
	sendDtmf: function(character, successCallback, errorCallback) {
	    cordova.exec(
		successCallback,
		errorCallback,
		"Linphone",
		"sendDtmf",
		[character]
	    );
	},
	messageReceivedCallback: function(successCallback, errorCallback) {
	    cordova.exec(
		successCallback,
		errorCallback,
		"Linphone",
		"messageReceivedCallback",
		[]
	    );
	},
	registerCallback: function(successCallback, errorCallback) {
	    cordova.exec(
		successCallback,
		errorCallback,
		"Linphone",
		"registerCallback",
		[]
	    );
	},
	authInfoReqCallback: function(successCallback, errorCallback) {
	     cordova.exec(
		 successCallback,
		 errorCallback,
		 "Linphone",
		 "authInfoReqCallback",
		 []
	     );
	},
	globalCallback: function(successCallback, errorCallback) {
	     cordova.exec(
		 successCallback,
		 errorCallback,
		 "Linphone",
		 "globalCallback",
		 []
	     );
	},
	callStateCallback: function(successCallback, errorCallback) {
	     cordova.exec(
		 successCallback,
		 errorCallback,
		 "Linphone",
		 "callStateCallback",
		 []
	     );
	},
	callStatsCallback: function(successCallback, errorCallback) {
		 cordova.exec(
		 successCallback,
		 errorCallback,
		 "Linphone",
		 "callStatsCallback",
		 []
		);
	},
	saveAuthInfo: function(pUsername, pUserId, pRealm, pPaswd, successCallback, errorCallback) {
	     cordova.exec(
		 successCallback,
		 errorCallback,
		 "Linphone",
		 "saveAuthInfo",
		 [pUsername, pUserId, pRealm, pPaswd]
	     );
	},
	saveProxyInfo: function(pRegProxy, pRegIdentity, pRegExpires, pRegSendRegister, pPublish, pDialEscapePlus, successCallback, errorCallback) {
	     cordova.exec(
		 successCallback,
		 errorCallback,
		 "Linphone",
		 "saveProxyInfo",
		 [pRegProxy, pRegExpires, pRegIdentity, pRegSendRegister, pPublish, pDialEscapePlus]
	     );
	},
	saveSipInfo: function(pPort, pIpv6, pIncTimeOut, pKeepAlive, successCallback, errorCallback) {
	     cordova.exec(
		 successCallback,
		 errorCallback,
		 "Linphone",
		 "saveSipInfo",
		 [pPort, pIpv6, pIncTimeOut, pKeepAlive]
	     );
	}
};

