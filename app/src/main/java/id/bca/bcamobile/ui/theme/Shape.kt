package id.bca.bcamobile.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Corner radius — #none..#7 plus #full. */
object AppShape {
    val None = RoundedCornerShape(0.dp)
    val R1 = RoundedCornerShape(2.dp)
    val R2 = RoundedCornerShape(4.dp)
    val R3 = RoundedCornerShape(6.dp)
    val R4 = RoundedCornerShape(8.dp)
    val R5 = RoundedCornerShape(10.dp)
    val R6 = RoundedCornerShape(12.dp)
    val R7 = RoundedCornerShape(16.dp)
    val Full = CircleShape
}