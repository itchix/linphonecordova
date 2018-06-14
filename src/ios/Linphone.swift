import Foundation

@objc(Linphone) class Linphone : CDVPlugin {
    
    var mDefaultDomain : String = "sip.linphone.org"
    
    override init() {
    }
    
    override func pluginInitialize() {
        
    }

    @objc(saveAuthInfo:) func saveAuthInfo(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        
        let pUsername = command.arguments[0] as? String ?? ""
        let pUserId = command.arguments[1] as? String ?? ""
        let pRealme = command.arguments[2] as? String ?? ""
        let pPassword = command.arguments[3] as? String ?? ""
        
        self.commandDelegate.run(inBackground: {
            if !pUsername.isEmpty && !pUserId.isEmpty && !pPassword.isEmpty {
                UserDefaults.standard.set(pUsername, forKey: "authUsername")
                UserDefaults.standard.set(pUserId, forKey: "authUserId")
                UserDefaults.standard.set(pPassword, forKey: "authPassword")
                if !pRealme.isEmpty {
                    UserDefaults.standard.set(pRealme, forKey: "authRealm")
                } else {
                    UserDefaults.standard.set(self.mDefaultDomain, forKey: "authRealm")
                }
                UserDefaults.standard.synchronize()
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_OK,
                    messageAs: "Authentication parameters saved/updated"
                )
            } else {
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: "Authentication parameters are empty"
                )
            }
        
            self.commandDelegate!.send(
                pluginResult,
                callbackId: command.callbackId
            )
        })
    }

    @objc(saveSipInfo:) func saveSipInfo(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        
        let pPort = command.arguments[0] as? Int ?? 0
        let pIpv6 = command.arguments[1] as? Bool ?? false
        let pIncTimeout = command.arguments[2] as? Int ?? 0
        let pKeepAlive = command.arguments[3] as? Int ?? 0
        
        self.commandDelegate.run(inBackground: {
            if pPort != 0 && pIpv6 != false && pIncTimeout != 0 && pKeepAlive != 0 {
                UserDefaults.standard.set(pPort, forKey: "sipPort")
                UserDefaults.standard.set(pIpv6, forKey: "supIpv6")
                UserDefaults.standard.set(pIncTimeout, forKey: "sipIncTimeOut")
                UserDefaults.standard.set(pKeepAlive, forKey: "sipKeepAlive")
                UserDefaults.standard.synchronize()
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_OK,
                    messageAs: "Sip parameters saved/updated"
                )
            } else {
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: "Sip parameters are empty"
                )
            }
        
            self.commandDelegate!.send(
                pluginResult,
                callbackId: command.callbackId
            )
        })
    }

    @objc(saveProxyInfo:) func saveProxyInfo(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        
        let pRegProxy = command.arguments[0] as? String ?? ""
        let pRegIdentity = command.arguments[1] as? String ?? ""
        let pRegExpires = command.arguments[2] as? String ?? ""
        let pRegSendRegister = command.arguments[3] as? String ?? ""
        let pPublish = command.arguments[4] as? Bool ?? true
        let pDialEscapePlus = command.arguments[5] as? Bool ?? false
        
        self.commandDelegate.run(inBackground: {
            if !pRegProxy.isEmpty && !pRegIdentity.isEmpty && pRegExpires.isEmpty && pRegSendRegister.isEmpty  {
                UserDefaults.standard.set(pRegProxy, forKey: "regProxy")
                UserDefaults.standard.set(pRegIdentity, forKey: "regIdentity")
                UserDefaults.standard.set(pRegExpires, forKey: "regExpires")
                UserDefaults.standard.set(pRegSendRegister, forKey: "regSendRegister")
                UserDefaults.standard.set(pPublish, forKey: "publish")
                UserDefaults.standard.set(pDialEscapePlus, forKey: "dialEscapePlus")
                UserDefaults.standard.synchronize()
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_OK,
                    messageAs: "Proxy parameters saved/updated"
                )
            } else {
                 pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: "Proxy parameters are empty"
                )
            }
            
            self.commandDelegate!.send(
                pluginResult,
                callbackId: command.callbackId
            )
        })
    }

    @objc(login:) func login(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        self.commandDelegate.run(inBackground: {
            let lc : OpaquePointer = LinphoneManager.sharedInstance.getLinphoneCore()
            if LinphoneUtils.isNetworkReachable() {
                let usernameSaved: String = UserDefaults.standard.object(forKey: "authUsername") as? String ?? String()
                let domainSaved: String = UserDefaults.standard.object(forKey: "authRealm") as? String ?? String()
                let passwordSaved: String = UserDefaults.standard.object(forKey: "authPassword") as? String ?? String()
                let username = LinphoneUtils.getDisplayableUsernameFromAddress(sipAddress: usernameSaved)
                //let domain = LinphoneUtils.getAddressDisplayName(uri: domainSaved)
                let identity : String = "sip:" + username + "@" + domainSaved
                let from = linphone_address_new(identity)
                
                let proxy_cfg = linphone_proxy_config_new()
                linphone_proxy_config_edit(proxy_cfg)
                let info = linphone_auth_info_new(linphone_address_get_username(from), nil, passwordSaved, nil, nil, linphone_address_get_domain(from))
                linphone_core_clear_all_auth_info(lc)
                linphone_core_add_auth_info(lc, info)
                
                linphone_proxy_config_set_identity_address(proxy_cfg, from)
                linphone_proxy_config_set_server_addr(proxy_cfg, linphone_address_get_domain(from))
                linphone_proxy_config_enable_register(proxy_cfg, 1)
                linphone_proxy_config_enable_publish(proxy_cfg, 1)
                linphone_core_add_proxy_config(lc, proxy_cfg)
                linphone_core_set_default_proxy_config(lc, proxy_cfg)
                
                linphone_proxy_config_done(proxy_cfg)
                linphone_address_destroy(from)
                
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_OK,
                    messageAs: "Logging..."
                )
            } else {
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: "Network unreachable"
                )
            }
            
            self.commandDelegate!.send(
                pluginResult,
                callbackId: command.callbackId
            )
        })
    }

    @objc(logout:) func logout(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        
        self.commandDelegate.run(inBackground: {
            let lc : OpaquePointer = LinphoneManager.sharedInstance.getLinphoneCore()
            let proxy_cfg = linphone_core_get_default_proxy_config(lc);
            if proxy_cfg != nil {
                linphone_proxy_config_edit(proxy_cfg);
                linphone_proxy_config_enable_register(proxy_cfg, 0);
                linphone_proxy_config_done(proxy_cfg);
                
                if linphone_proxy_config_get_state(proxy_cfg) !=  LinphoneRegistrationCleared {
                    pluginResult = CDVPluginResult(
                        status: CDVCommandStatus_OK,
                        messageAs: "Lougout..."
                    )
                }
            }

            self.commandDelegate!.send(
                pluginResult,
                callbackId: command.callbackId
            )
        })
    }

    @objc(call:) func call(command: CDVInvokedUrlCommand) {
        
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        
        let pAddress = command.arguments[0] as? String ?? ""
        //let pDisplayName = command.arguments[1] as? String ?? ""

        self.commandDelegate.run(inBackground: {
            let lc : OpaquePointer = LinphoneManager.sharedInstance.getLinphoneCore()
            var call: OpaquePointer;
            
            if !pAddress.isEmpty {
                call = linphone_core_invite(lc, pAddress)
                linphone_call_ref(call)
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_OK,
                    messageAs: "Calling..."
                )
                
            } else {
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: "Error calling..."
                )
            }
            
            self.commandDelegate!.send(
                pluginResult,
                callbackId: command.callbackId
            )
        })
    }

    @objc(answerCall:) func answerCall(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        
        self.commandDelegate.run(inBackground: {
            let lc : OpaquePointer = LinphoneManager.sharedInstance.getLinphoneCore()
            let currentCall: OpaquePointer = linphone_core_get_current_call(lc)
            if linphone_core_get_calls_nb(lc) > 0 && linphone_call_get_state(currentCall) == LinphoneCallIncomingReceived {
                linphone_core_accept_call(lc, currentCall)
                linphone_call_ref(currentCall)
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_OK,
                    messageAs: "Take call..."
                )
            } else {
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: "Not calling"
                )
            }
            
            self.commandDelegate!.send(
                pluginResult,
                callbackId: command.callbackId
            )
        })
    }

    @objc(hangup:) func hangup(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        
        self.commandDelegate.run(inBackground: {
            let lc : OpaquePointer = LinphoneManager.sharedInstance.getLinphoneCore()
            let currentCall: OpaquePointer = linphone_core_get_current_call(lc)
            if linphone_core_get_calls_nb(lc) > 0 && linphone_call_get_state(currentCall) != LinphoneCallEnd {
                if(LinphoneManager.sharedInstance.isRinging) {
                    LinphoneManager.sharedInstance.stopRinging()
                }
                linphone_core_terminate_call(lc, currentCall)
                linphone_call_unref(currentCall)
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_OK,
                    messageAs: "Terminate call..."
                )
            } else {
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: "Not calling"
                )
            }
            
            self.commandDelegate!.send(
                pluginResult,
                callbackId: command.callbackId
            )
        })
    }
    
    @objc(sendMessage:) func sendMessage(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        
        self.commandDelegate.run(inBackground: {
            let lc : OpaquePointer = LinphoneManager.sharedInstance.getLinphoneCore()
            let pMessage = command.arguments[0] as? String ?? ""
            let pSipUri = command.arguments[1] as? String ?? ""
            let pNewChatConversation = command.arguments[2] as? Bool ?? false
            
            let address : OpaquePointer = linphone_core_interpret_url(lc, pSipUri)
            let chatRoom : OpaquePointer = linphone_core_get_chat_room(lc, address)
            
            if !pMessage.isEmpty && linphone_core_is_network_reachable(lc) == 1 {
                let message : OpaquePointer = linphone_chat_room_create_message(chatRoom, pMessage)
                linphone_chat_room_send_chat_message(chatRoom, message)
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_OK,
                    messageAs: "Sending..."
                )
            } else {
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: "Error sending..."
                )
            }
            
            self.commandDelegate!.send(
                pluginResult,
                callbackId: command.callbackId
            )
        })
    }

    @objc(toggleSpeaker:) func toggleSpeaker(command: CDVInvokedUrlCommand) {

    }

    @objc(toggleMute:) func toggleMute(command: CDVInvokedUrlCommand) {

    }
    
    @objc(registerCallback:) func registerCallback(command: CDVInvokedUrlCommand) {
        self.commandDelegate.run(inBackground: {
            LinphoneManager.sharedInstance.setCallbackRegister(command: command, plugin: self)
        })
    }
    
    @objc(authInfoReqCallback:) func authInfoReqCallback(command: CDVInvokedUrlCommand) {
        self.commandDelegate.run(inBackground: {
            LinphoneManager.sharedInstance.setCallbackAuthInfo(command: command, plugin: self)
        })
    }
    
    @objc(callStateCallback:) func callStateCallback(command: CDVInvokedUrlCommand) {
        self.commandDelegate.run(inBackground: {
            LinphoneManager.sharedInstance.setCallbackCallState(command: command, plugin: self)
        })
    }
    
    @objc(callStatsCallback:) func callStatsCallback(command: CDVInvokedUrlCommand) {
        // TODO
    }
    
    @objc(globalCallback:) func globalCallback(command: CDVInvokedUrlCommand) {
        self.commandDelegate.run(inBackground: {
            LinphoneManager.sharedInstance.setCallbackGlobal(command: command, plugin: self)
        })
    }
    
    @objc(messageReceivedCallback:) func messageReceivedCallback(command: CDVInvokedUrlCommand) {
        self.commandDelegate.run(inBackground: {
            LinphoneManager.sharedInstance.setCallbackMessageReceived(command: command, plugin: self)
        })
    }

}
