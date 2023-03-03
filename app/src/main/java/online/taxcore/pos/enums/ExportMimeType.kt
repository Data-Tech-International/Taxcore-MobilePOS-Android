package online.taxcore.pos.enums

enum class ExportMimeType(val type: String) {
    JSON("application/json"), PDF("application/pdf"), CSV("text/csv")
}
