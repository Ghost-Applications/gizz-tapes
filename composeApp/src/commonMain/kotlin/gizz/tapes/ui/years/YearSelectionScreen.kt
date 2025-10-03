package gizz.tapes.ui.years

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gizz.tapes.AppGraph
import gizz.tapes.api.GizzTapesApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


sealed interface YearSelectionUiState {
    data object Content : YearSelectionUiState
    data object Loading : YearSelectionUiState
    data object Error : YearSelectionUiState
}

class YearSelectionViewModel(
    private val apiClient: GizzTapesApiClient,
): ViewModel() {

//    val years = TODO()
//
    private fun loadYears(): Flow<YearSelectionUiState> {
        return flow {

        }
    }

    companion object {
        val API_CLIENT = object : CreationExtras.Key<GizzTapesApiClient> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val extras: CreationExtras = this
                val apiClient = extras[API_CLIENT] as GizzTapesApiClient
                YearSelectionViewModel(apiClient)
            }
        }
    }
}

@Composable
fun YearSelectionScreen(
    appGraph: AppGraph
) {
    val extras = MutableCreationExtras().apply {
        this[YearSelectionViewModel.API_CLIENT] = appGraph.gizzClientApi
    }

    val vm: YearSelectionViewModel = viewModel(
        factory = YearSelectionViewModel.Factory,
        extras = extras
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Cyan)) {
        Text("It works!")
    }
}
