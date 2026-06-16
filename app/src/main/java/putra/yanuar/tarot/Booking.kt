package putra.yanuar.tarot

data class Booking(
    val id:            String,
    val customerId:    String,
    val customerName:  String,
    val readerId:      String,
    val readerName:    String,
    val packageId:     String,
    val packageName:   String,
    val paymentMethod: String,
    val totalPrice:    Int,
    val date:          String,
    val time:          String,
    val notes:         String,
    val status:        String,
    val answer:        String  = "",
    val qrContent:     String  = ""      // ← field QR
)