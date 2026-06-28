package com.streetlifting.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

/** Atajo uniforme para recolectar un StateFlow en Compose. */
@Composable
fun <T> StateFlow<T>.collectAsStateSafe(): State<T> = collectAsState()
