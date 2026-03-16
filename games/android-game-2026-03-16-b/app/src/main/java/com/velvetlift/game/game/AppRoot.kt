package com.velvetlift.game.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private data class RunSummary(
    val contract: ContractConfig,
    val result: ContractResult
)

@Composable
fun VelvetLiftApp() {
    val context = LocalContext.current
    var profile by remember { mutableStateOf(ProfileStore.load(context)) }
    var activeContract by remember { mutableStateOf<ContractConfig?>(null) }
    var summary by remember { mutableStateOf<RunSummary?>(null) }

    val currentContract = activeContract
    val currentSummary = summary

    when {
        currentContract != null -> {
            GameScreen(
                contract = currentContract,
                perks = perksForProfile(profile),
                onExit = { activeContract = null },
                onComplete = { result ->
                    val updated = profile.record(currentContract, result)
                    profile = updated
                    ProfileStore.save(context, updated)
                    summary = RunSummary(currentContract, result)
                    activeContract = null
                }
            )
        }

        currentSummary != null -> {
            ResultScreen(
                contract = currentSummary.contract,
                result = currentSummary.result,
                profile = profile,
                onReplay = {
                    activeContract = currentSummary.contract
                    summary = null
                },
                onContinue = {
                    val nextContract = HOTEL_CONTRACTS.getOrNull(currentSummary.contract.id)
                    if (nextContract != null && nextContract.id <= profile.unlockedContractId) {
                        activeContract = nextContract
                        summary = null
                    } else {
                        summary = null
                    }
                },
                onMenu = { summary = null }
            )
        }

        else -> {
            MenuScreen(
                profile = profile,
                onStart = { contract ->
                    activeContract = contract
                    summary = null
                }
            )
        }
    }
}
