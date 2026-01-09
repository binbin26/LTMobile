package smart.study.planner.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import smart.study.planner.presentation.viewmodel.DebugViewModel
import smart.study.planner.util.debug.DebugLogger

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel()
) {
    var logs by remember { mutableStateOf(DebugLogger.exportLogs()) }
    val testResult by viewModel.testResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Smart Study Planner", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { viewModel.testFirebaseConnection() }) {
                    Text("Test Firebase")
                }
                Button(onClick = { viewModel.testRoomOperations() }) {
                    Text("Test Room")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { viewModel.clearDatabase() }) {
                    Text("Clear DB")
                }
                Button(onClick = { logs = DebugLogger.exportLogs() }) {
                    Text("Export Logs")
                }
            }
            
            Button(onClick = { logs = DebugLogger.exportLogs() }) {
                Text("Refresh Logs")
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (testResult.isNotEmpty()) {
                Text(testResult)
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(logs.split("\n")) { log ->
                    Text(log)
                }
            }
        }
    }
}
