package online.taxcore.pos.models

class StatusResponse {
    var IsPinRequired = false
    var AuditRequired = false
    var DT = ""
    var LastInvoiceNumber = ""
    var ProtocolVersion = ""
    var SecureElementVersion = ""
    var HardwareVersion = ""
    var SoftwareVersion = ""
    var DeviceSerialNumber = ""
    var Make = ""
    var Model = ""
    var MSSC = arrayListOf<String>()
    var GSC = arrayListOf<String>()
}
