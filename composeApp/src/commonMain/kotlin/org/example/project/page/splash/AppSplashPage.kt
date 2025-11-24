package org.example.project.page.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.pickcat_login_bg
import org.example.project.navigation.LocalNavController
import org.example.project.routes.RouterMainHome
import org.example.project.statusbar.StatusBarConfig
import org.example.project.statusbar.StatusBarStyle
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppSplashPage(modifier: Modifier = Modifier) {
    var countdown by remember { mutableIntStateOf(2) }
    val navController = LocalNavController.current
    
    StatusBarConfig(StatusBarStyle.LIGHT_CONTENT)

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        navController.navigate(RouterMainHome)
    }

    MaterialTheme {
        Scaffold {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(Res.drawable.pickcat_login_bg),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )

                if (countdown > 0) {
                    Text(
                        color = Color.White,
                        text = countdown.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopEnd)
                            .padding(32.dp)
                    )
                }
            }
        }
    }
}