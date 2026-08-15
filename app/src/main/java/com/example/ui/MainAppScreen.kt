package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.DnsSyncScreen
import com.example.ui.screens.SavedIpScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.CfOrangePrimary
import com.example.ui.theme.DarkSlateSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.OffWhiteText

data class NavTabItem(
    val title: String,
    val icon: ImageVector,
    val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: MainViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        NavTabItem("扫描器", Icons.Default.Bolt, "tab_scanner"),
        NavTabItem("已存 IP", Icons.Default.Storage, "tab_saved"),
        NavTabItem("CF 同步", Icons.Default.CloudSync, "tab_dns")
    )

    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val scanConfig by viewModel.scanConfig.collectAsStateWithLifecycle()
    val savedIps by viewModel.savedIps.collectAsStateWithLifecycle()
    val dnsRules by viewModel.dnsRules.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CF IP 扫描与 DNS 同步",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = OffWhiteText
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSlateSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSlateSurface,
                contentColor = OffWhiteText
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(text = tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkSlateSurface,
                            selectedTextColor = CfOrangePrimary,
                            indicatorColor = CfOrangePrimary,
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ScannerScreen(
                    viewModel = viewModel,
                    scanProgress = scanProgress,
                    scanConfig = scanConfig
                )
                1 -> SavedIpScreen(
                    viewModel = viewModel,
                    savedIps = savedIps
                )
                2 -> DnsSyncScreen(
                    viewModel = viewModel,
                    dnsRules = dnsRules
                )
            }
        }
    }
}
