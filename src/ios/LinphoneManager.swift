import Foundation
import AVFoundation
import AudioToolbox

class LinphoneManager {

    var lc: OpaquePointer!
    var isLaunched: Bool = false
    var isRinging: Bool = false
    var lct: LinphoneCoreVTable = LinphoneCoreVTable()
    var mAudioPlayer: AVAudioPlayer?
    var mCommandRegisterState: CDVInvokedUrlCommand
    var mCommandCallState: CDVInvokedUrlCommand
    var mCommandAuthInfo: CDVInvokedUrlCommand
    var mCommandCallStats: CDVInvokedUrlCommand
    var mCommandGlobalState: CDVInvokedUrlCommand
    var mCommandMessage: CDVInvokedUrlCommand
    var mPlugin: CDVPlugin
    var timer = Timer()

    static let sharedInstance : LinphoneManager = {
        let instance = LinphoneManager()
        return instance
    }()
    
    func getLinphoneCore() -> OpaquePointer {
        if isLaunched {
            return lc
        } else {
           createAndStart()
            return lc
        }
    }

    init() {
        // Enable debug log to stdout
        linphone_core_set_log_file(nil)
        linphone_core_set_log_level(ORTP_DEBUG)
        mCommandRegisterState = CDVInvokedUrlCommand()
        mCommandCallState = CDVInvokedUrlCommand()
        mCommandAuthInfo = CDVInvokedUrlCommand()
        mCommandCallStats = CDVInvokedUrlCommand()
        mCommandGlobalState = CDVInvokedUrlCommand()
        mCommandMessage = CDVInvokedUrlCommand()
        mPlugin = CDVPlugin()
        createAndStart()
    }

    func createAndStart() {
        if !isLaunched {
            // Set Callback
            lct.registration_state_changed = registrationStateChanged
            lct.call_state_changed = callStateChanged
            lct.auth_info_requested = authInfoReqChanged
            lct.call_stats_updated = callStatsChanged
            lct.global_state_changed = globalCallback
            lct.message_received = messageCallback
        
            lc = linphone_core_new(&lct, nil, nil, nil)
            linphone_core_iterate(lc)
            DispatchQueue.main.async {
                self.timer = Timer.scheduledTimer(timeInterval: 0.02, target: self, selector: #selector(self.iterateCore), userInfo: nil, repeats: true)
            }
            isLaunched = true
        }
    }
    
    @objc func iterateCore() {
        linphone_core_iterate(lc)
    }
    
    func startRinging() {
        let source = Bundle.main.path(forResource: "ringback", ofType: "wav")
        do {
            mAudioPlayer = try AVAudioPlayer(contentsOf: URL(fileURLWithPath: source!))
            mAudioPlayer?.numberOfLoops = -1
            mAudioPlayer?.volume = 1.0
            mAudioPlayer?.play()
            if #available(iOS 9.0, *) {
                startVibrate()
            }
            isRinging = true
        } catch {
            NSLog("Error audio")
        }
    }
    
    @available(iOS 9.0, *)
    func startVibrate() {
        AudioServicesPlayAlertSoundWithCompletion(SystemSoundID(kSystemSoundID_Vibrate), handlePhone)
    }
    
    func stopRinging() {
        if isRinging {
            mAudioPlayer?.stop()
            isRinging = false
        }
    }
    
    @available(iOS 9.0, *)
    func handlePhone() -> Void {
        if isRinging {
            AudioServicesPlayAlertSoundWithCompletion(SystemSoundID(kSystemSoundID_Vibrate), handlePhone)
        }
    }
    
    /**
     * MESSAGE RECEIVED CALLBACK
     */
    func setCallbackMessageReceived(command: CDVInvokedUrlCommand, plugin: CDVPlugin) {
        let nc = NotificationCenter.default
        nc.addObserver(forName: Notification.Name(rawValue: "MessageNotification"), object: nil, queue: nil, using: catchMessageNotification)
        mPlugin = plugin
        mCommandMessage = command
    }
    
    func catchMessageNotification(notification: Notification) -> Void {
        guard let userInfo = notification.userInfo,
            let message = userInfo["message"] as? String else {
                NSLog("No userInfo found in notification message")
                return
        }
        NSLog(message)
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        pluginResult = CDVPluginResult(
            status: CDVCommandStatus_OK,
            messageAs: message
        )
        pluginResult?.setKeepCallbackAs(true)
        mPlugin.commandDelegate!.send(
            pluginResult,
            callbackId: mCommandMessage.callbackId
        )
        
    }
    
    let messageCallback: LinphoneCoreMessageReceivedCb  = {
        (lc, room, message) in
        let nc = NotificationCenter.default
        let addr = String(cString: linphone_address_as_string(linphone_chat_message_get_from_address(message!)), encoding: String.Encoding.utf8)!
        let body = String(cString: linphone_chat_message_get_text(message!), encoding: String.Encoding.utf8)!
        nc.post(name: Notification.Name(rawValue: "MessageNotification"), object: nil, userInfo: ["message": "\(addr) : '\(body)'"])
    }
    
    /**
     * REGISTER CALLBACK
     */
    func setCallbackRegister(command: CDVInvokedUrlCommand, plugin: CDVPlugin) {
        let nc = NotificationCenter.default
        nc.addObserver(forName: Notification.Name(rawValue: "RegisterNotification"), object: nil, queue: nil, using: catchRegisterNotification)
        mPlugin = plugin
        mCommandRegisterState = command
    }
    
    func catchRegisterNotification(notification: Notification) -> Void {
        guard let userInfo = notification.userInfo,
            let message = userInfo["message"] as? String else {
                NSLog("No userInfo found in notification register")
                return
        }
        NSLog(message)
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        pluginResult = CDVPluginResult(
            status: CDVCommandStatus_OK,
            messageAs: message
        )
        pluginResult?.setKeepCallbackAs(true)
        mPlugin.commandDelegate!.send(
            pluginResult,
            callbackId: mCommandRegisterState.callbackId
        )
        
    }
    
    let registrationStateChanged: LinphoneCoreRegistrationStateChangedCb  = {
        (lc, proxyConfig, state, message) in
        
        let nc = NotificationCenter.default
        nc.post(name: Notification.Name(rawValue: "RegisterNotification"), object: nil, userInfo: ["message": String(cString: linphone_registration_state_to_string(state))])
    }
    
    /**
     * CALL STATE CALLBACK
     */
    func setCallbackCallState(command: CDVInvokedUrlCommand, plugin: CDVPlugin) {
        let nc = NotificationCenter.default
        nc.addObserver(forName: Notification.Name(rawValue: "CallStateNotification"), object: nil, queue: nil, using: catchCallStateNotification)
        mPlugin = plugin
        mCommandCallState = command
    }
    
    func catchCallStateNotification(notification: Notification) -> Void {
        guard let userInfo = notification.userInfo,
            let message = userInfo["message"] as? String else {
                NSLog("No userInfo found in notification call")
                return
        }
        NSLog(message)
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        pluginResult = CDVPluginResult(
            status: CDVCommandStatus_OK,
            messageAs: message
        )
        pluginResult?.setKeepCallbackAs(true)
        mPlugin.commandDelegate!.send(
            pluginResult,
            callbackId: mCommandCallState.callbackId
        )
        
    }
    
    let callStateChanged: LinphoneCoreCallStateChangedCb = {
        (lc, call, callSate, message) in
        
        let nc = NotificationCenter.default
        var messageToSend: String = "";
        let uri = String(cString: linphone_call_get_remote_address_as_string(call))
        if callSate == LinphoneCallIncomingReceived {
            messageToSend = String(cString: linphone_call_state_to_string(callSate))
                + " " + String(cString: linphone_call_get_remote_contact(call)) + " " +
                String(cString: linphone_call_get_remote_address_as_string(call))
        } else {
            messageToSend = String(cString: linphone_call_state_to_string(callSate))
                + " \"" + String(cString: linphone_call_get_remote_address_as_string(call)) + "\" <" +
                String(cString: linphone_call_get_remote_address_as_string(call)) + ">"
        }
        nc.post(name: Notification.Name(rawValue: "CallStateNotification"), object: nil, userInfo: ["message": messageToSend])
        
        if callSate == LinphoneCallEnd || callSate == LinphoneCallError {
            linphone_core_start_dtmf_stream(lc)
        }
        
        if callSate == LinphoneCallIncomingReceived && call != linphone_core_get_current_call(lc) {
            if linphone_call_get_replaced_call(call) != nil {
                return
            }
        }
        
        if callSate == LinphoneCallIncomingReceived {
            LinphoneManager.sharedInstance.startRinging()
        }
        
        
        if callSate == LinphoneCallConnected {
            if LinphoneManager.sharedInstance.isRinging {
                LinphoneManager.sharedInstance.stopRinging()
            }
            if linphone_core_get_calls_nb(lc) > 0 {
                
            }
        }
        
        if callSate == LinphoneCallReleased  {
            if LinphoneManager.sharedInstance.isRinging {
                LinphoneManager.sharedInstance.stopRinging()
            }
        }
    }
    
    /**
     * CALL STATS CALLBACK
     */
    let callStatsChanged: LinphoneCoreCallStatsUpdatedCb = {
        (lc, call, callStats) in
        // TODO
    }
    
    /**
     * AUTH INFO CALLBACK
     */
    func setCallbackAuthInfo(command: CDVInvokedUrlCommand, plugin: CDVPlugin) {
        let nc = NotificationCenter.default
        nc.addObserver(forName: Notification.Name(rawValue: "AuthInfoNotification"), object: nil, queue: nil, using: catchAuthInfoNotification)
        mPlugin = plugin
        mCommandAuthInfo = command
    }
    
    func catchAuthInfoNotification(notification: Notification) -> Void {
        guard let userInfo = notification.userInfo,
            let message = userInfo["message"] as? String else {
                NSLog("No userInfo found in notification")
                return
        }
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        pluginResult = CDVPluginResult(
            status: CDVCommandStatus_OK,
            messageAs: message
        )
        pluginResult?.setKeepCallbackAs(true)
        mPlugin.commandDelegate!.send(
            pluginResult,
            callbackId: mCommandAuthInfo.callbackId
        )
        
    }
    
    let authInfoReqChanged: LinphoneCoreAuthInfoRequestedCb = {
        (lc, realm, username, domain) in
        
        let nc = NotificationCenter.default
        nc.post(name: Notification.Name(rawValue: "AuthInfoNotification"), object: nil, userInfo: ["message": "Auth Info Requested : \(realm)  \(username) \(domain)"])
    }
    
    /**
     * GLOBAL CALLBACK
     */
    func setCallbackGlobal(command: CDVInvokedUrlCommand, plugin: CDVPlugin) {
        let nc = NotificationCenter.default
        nc.addObserver(forName: Notification.Name(rawValue: "GlobalNotification"), object: nil, queue: nil, using: catchGlobalNotification)
        mPlugin = plugin
        mCommandGlobalState = command
    }
    
    func catchGlobalNotification(notification: Notification) -> Void {
        if(!mCommandGlobalState.callbackId.isEmpty) {
            guard let userInfo = notification.userInfo,
                let message = userInfo["message"] as? String else {
                    NSLog("No userInfo found in notification")
                    return
            }
            var pluginResult = CDVPluginResult(
                status: CDVCommandStatus_ERROR
            )
            pluginResult = CDVPluginResult(
                status: CDVCommandStatus_OK,
                messageAs: message
            )
            pluginResult?.setKeepCallbackAs(true)
            mPlugin.commandDelegate!.send(
                pluginResult,
                callbackId: mCommandGlobalState.callbackId
            )
        }
    }
    
    let globalCallback: LinphoneCoreGlobalStateChangedCb = {
        (lc, state, message) in
        
        let nc = NotificationCenter.default
        nc.post(name: Notification.Name(rawValue: "GlobalNotification"), object: nil, userInfo: ["message": state])
    }

    
    func shutdown(){
        NSLog("Shutdown..")

        self.timer.invalidate()
        let proxy_cfg = linphone_core_get_default_proxy_config(lc); /* get default proxy config*/
        linphone_proxy_config_edit(proxy_cfg); /*start editing proxy configuration*/
        linphone_proxy_config_enable_register(proxy_cfg, 0); /*de-activate registration for this proxy config*/
        linphone_proxy_config_done(proxy_cfg); /*initiate REGISTER with expire = 0*/

        linphone_core_destroy(lc);
    }
    
}
