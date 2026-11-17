package com.example.zevora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zevora.model.VirtualGift
import com.example.zevora.ui.theme.ZevoraPrimary
import com.example.zevora.ui.theme.ZevoraSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveGiftModal(
  userCoins: Int,
  virtualGifts: List<VirtualGift>,
  onDismiss: () -> Unit,
  onSendGift: (VirtualGift) -> Unit,
  onBuyCoins: () -> Unit
) {
  var selectedGift by remember { mutableStateOf<VirtualGift?>(virtualGifts.firstOrNull()) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF131B2E),
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    modifier = Modifier.testTag("live_gift_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp)
    ) {
      // Header: Coins Balance & Buy Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Send Live Gift 🎁",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
          ) {
            Icon(
              imageVector = Icons.Default.MonetizationOn,
              contentDescription = "Coins",
              tint = Color(0xFFFBBF24),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "$userCoins Coins Available",
              fontSize = 13.sp,
              color = Color(0xFFFBBF24),
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        // Buy Coins Pill Button
        Button(
          onClick = onBuyCoins,
          shape = RoundedCornerShape(20.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          modifier = Modifier.testTag("get_coins_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = ZevoraSecondary, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Get Coins", color = ZevoraSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Gifts Grid (5 gifts)
      LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(virtualGifts) { gift ->
          val isSelected = selectedGift?.id == gift.id
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) Color(0xFF312E81) else Color(0xFF1E293B)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ZevoraPrimary else Color(0xFF334155),
                shape = RoundedCornerShape(16.dp)
              )
              .clickable { selectedGift = gift }
              .testTag("gift_card_${gift.id}")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = gift.icon,
                fontSize = 32.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = gift.name.split(" ").firstOrNull() ?: gift.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = Color.White
              )
              Spacer(modifier = Modifier.height(2.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.MonetizationOn,
                  contentDescription = null,
                  tint = Color(0xFFFBBF24),
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                  text = "${gift.coinPrice}",
                  fontSize = 11.sp,
                  color = Color(0xFFFBBF24),
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Selected Gift Description & Send Button
      selectedGift?.let { gift ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = gift.name,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = Color.White
            )
            Text(
              text = gift.description,
              fontSize = 11.sp,
              color = Color.LightGray
            )
          }

          val hasEnoughCoins = userCoins >= gift.coinPrice
          Button(
            onClick = {
              if (hasEnoughCoins) {
                onSendGift(gift)
              } else {
                onBuyCoins()
              }
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (hasEnoughCoins) Color(0xFFEC4899) else Color(0xFF475569)
            ),
            modifier = Modifier.testTag("send_gift_button")
          ) {
            Text(
              text = if (hasEnoughCoins) "Send (${gift.coinPrice} 🪙)" else "Recharge Coins",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = Color.White
            )
          }
        }
      }
    }
  }
}
