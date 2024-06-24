package com.example.algorythm

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale

@Composable
fun SongItem(
    bitmap: Bitmap?,
    title: String,
    author: String,
    views: String,
    likes: String,
    playlistId: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RectangleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongClick()
                    },
                    onTap = {
                        onClick()
                    }
                )
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .size(70.dp)
                    .shadow(
                        elevation = 15.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)

            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.logo_placeholder),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .size(70.dp)
                    .shadow(
                        elevation = 15.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                fontSize = 20.sp,
                color = Color.White
            )
            Text(
                text = author,
                fontSize = 14.sp,
                color = Color.White
            )
        }
    }
}
