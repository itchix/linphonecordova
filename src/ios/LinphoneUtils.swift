import Foundation
import SystemConfiguration

class LinphoneUtils {
    
    static func getDisplayableUsernameFromAddress(sipAddress: String) -> String {
        var username : String = sipAddress
        let lc : OpaquePointer = LinphoneManager.sharedInstance.getLinphoneCore()
 as OpaquePointer
        
        if username.hasPrefix("sip:") {
            let index = username.index(username.startIndex, offsetBy: 4)
            username = username.substring(to: index)
        }
        
        if username.contains("@") {
            let domain : String = username.components(separatedBy: "@")[1]
            let proxyConfig = linphone_core_get_default_proxy_config(lc)
            if proxyConfig != nil {
                let domainFromProxy = String(cString: linphone_proxy_config_get_domain(proxyConfig))
                if domain == domainFromProxy {
                    return username.components(separatedBy: "@")[0]
                }
            } else {
                if domain == "sip.linphone.org" {
                    return username.components(separatedBy: "@")[1]
                }
            }
        }
        linphone_ringtoneplayer_new()
        return username
    }
    
    static func getAddressDisplayName(uri: String) -> String {
        return String(cString: linphone_address_get_display_name(linphone_address_new(uri)))
    }
    
    static func isNetworkReachable() -> Bool {
        var zeroAddress = sockaddr_in()
        zeroAddress.sin_len = UInt8(MemoryLayout.size(ofValue: zeroAddress))
        zeroAddress.sin_family = sa_family_t(AF_INET)
        
        let defaultRouteReachability = withUnsafePointer(to: &zeroAddress) {
            $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {zeroSockAddress in
                SCNetworkReachabilityCreateWithAddress(nil, zeroSockAddress)
            }
        }
        
        var flags = SCNetworkReachabilityFlags()
        if !SCNetworkReachabilityGetFlags(defaultRouteReachability!, &flags) {
            return false
        }
        let isReachable = (flags.rawValue & UInt32(kSCNetworkFlagsReachable)) != 0
        let needsConnection = (flags.rawValue & UInt32(kSCNetworkFlagsConnectionRequired)) != 0
        return (isReachable && !needsConnection)
    }
    
}
