package online.taxcore.pos.enums

enum class InvoiceOption(val value: Int) {
    TRANSACTION(0),
    INVOICE(1),
    PAYMENT(2)
}

enum class TransactionType(val value: String) {
    SALE("sale"),
    REFUND("refund"),
}

enum class PaymentType(val value: String) {
    CASH("cash"),
    CARD("card"),
    OTHER("other"),
    CHECK("check"),
    WIRE_TRANSFER("wiretransfer"),
    VOUCHER("voucher"),
    MOBILE_MONEY("mobilemoney"),
}

enum class InvoiceType(val value: String) {
    NORMAL("normal"),
    PROFORMA("proforma"),
    COPY("copy"),
    TRAINING("training")
}

enum class InvoiceActivityType(val value: String) {
    NORMAL("normal"),
    COPY("copy"),
    REFUND("refund")
}
