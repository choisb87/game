package com.velvetlift.game.game

import androidx.compose.ui.graphics.Color
import kotlin.math.max

enum class GuestArchetype(
    val label: String,
    val shortCode: String,
    val accent: Color,
    val baseFare: Int,
    val tipCeiling: Int,
    val patienceDrain: Float,
    val moodPenalty: Float,
    val serviceNote: String
) {
    REGULAR(
        label = "Suite Guest",
        shortCode = "SU",
        accent = Color(0xFFF5E6C8),
        baseFare = 85,
        tipCeiling = 35,
        patienceDrain = 0.07f,
        moodPenalty = 0.05f,
        serviceNote = "Reliable volume."
    ),
    VIP(
        label = "Patron",
        shortCode = "VIP",
        accent = Color(0xFFD8B16B),
        baseFare = 120,
        tipCeiling = 60,
        patienceDrain = 0.08f,
        moodPenalty = 0.08f,
        serviceNote = "Private ride bonus."
    ),
    COURIER(
        label = "Courier",
        shortCode = "RUN",
        accent = Color(0xFFEF8B6B),
        baseFare = 105,
        tipCeiling = 35,
        patienceDrain = 0.09f,
        moodPenalty = 0.06f,
        serviceNote = "Adds time on fast delivery."
    ),
    CRITIC(
        label = "Critic",
        shortCode = "CRT",
        accent = Color(0xFFB996F4),
        baseFare = 140,
        tipCeiling = 50,
        patienceDrain = 0.10f,
        moodPenalty = 0.14f,
        serviceNote = "Huge mood swing if lost."
    ),
    SOCIALITE(
        label = "Salon",
        shortCode = "SAL",
        accent = Color(0xFF63C6C1),
        baseFare = 95,
        tipCeiling = 40,
        patienceDrain = 0.075f,
        moodPenalty = 0.07f,
        serviceNote = "Bonus on shared arrivals."
    )
}

data class Guest(
    val id: Int,
    val archetype: GuestArchetype,
    val origin: Int,
    val destination: Int,
    val patience: Float = 1f,
    val waitTime: Float = 0f,
    val rideTime: Float = 0f
)

data class FloatingText(
    val id: Int,
    val text: String,
    val floor: Int,
    val age: Float = 0f,
    val color: Color
)

data class ContractConfig(
    val id: Int,
    val title: String,
    val subtitle: String,
    val briefing: String,
    val totalFloors: Int,
    val durationSeconds: Int,
    val targetScore: Int,
    val guestCap: Int,
    val baseSpawnSeconds: Float,
    val vipWeight: Int,
    val courierWeight: Int,
    val criticWeight: Int,
    val socialiteWeight: Int,
    val lobbyOriginBias: Float,
    val rooftopDemandBias: Float
)

data class ContractResult(
    val success: Boolean,
    val score: Int,
    val stars: Int,
    val delivered: Int,
    val lost: Int,
    val moodRemaining: Float
)

data class CareerProfile(
    val unlockedContractId: Int = 1,
    val bestStars: Map<Int, Int> = emptyMap(),
    val bestScores: Map<Int, Int> = emptyMap(),
    val highScore: Int = 0,
    val lifetimeGuests: Int = 0
) {
    fun record(contract: ContractConfig, result: ContractResult): CareerProfile {
        val nextUnlocked = if (result.success) {
            max(unlockedContractId, (contract.id + 1).coerceAtMost(HOTEL_CONTRACTS.size))
        } else {
            unlockedContractId
        }
        val updatedStars = bestStars + (contract.id to max(bestStars[contract.id] ?: 0, result.stars))
        val updatedScores = bestScores + (contract.id to max(bestScores[contract.id] ?: 0, result.score))
        return copy(
            unlockedContractId = nextUnlocked,
            bestStars = updatedStars,
            bestScores = updatedScores,
            highScore = max(highScore, result.score),
            lifetimeGuests = lifetimeGuests + result.delivered
        )
    }
}

data class CareerPerks(
    val speedMultiplier: Float = 1f,
    val patienceMultiplier: Float = 1f,
    val cabinCapacity: Int = 3
)

data class PermanentPerk(
    val title: String,
    val description: String
)

enum class ServicePhase {
    PLAYING,
    RESULT
}

data class HotelState(
    val contract: ContractConfig,
    val carFloor: Float = 0f,
    val activeStop: Int? = null,
    val queuedStops: List<Int> = emptyList(),
    val doorTimer: Float = 0f,
    val passengers: List<Guest> = emptyList(),
    val waitingByFloor: Map<Int, List<Guest>>,
    val remainingTime: Float = contract.durationSeconds.toFloat(),
    val spawnCooldown: Float = 0.9f,
    val score: Int = 0,
    val mood: Float = 1f,
    val deliveredGuests: Int = 0,
    val lostGuests: Int = 0,
    val combo: Int = 0,
    val comboTimer: Float = 0f,
    val floatingTexts: List<FloatingText> = emptyList(),
    val nextGuestId: Int = 1,
    val nextTextId: Int = 1,
    val arrivalPulse: Float = 0f,
    val selectedFloor: Int? = null,
    val selectionPulse: Float = 0f,
    val phase: ServicePhase = ServicePhase.PLAYING,
    val result: ContractResult? = null
) {
    val totalWaiting: Int
        get() = waitingByFloor.values.sumOf { it.size }
}

val HOTEL_CONTRACTS = listOf(
    ContractConfig(
        id = 1,
        title = "After Hours Check-In",
        subtitle = "Calm arrivals, small tips, learn the queue.",
        briefing = "The Meridian opens its velvet doors. Build rhythm with clean pickup routes and avoid empty lifts.",
        totalFloors = 7,
        durationSeconds = 95,
        targetScore = 1850,
        guestCap = 11,
        baseSpawnSeconds = 2.9f,
        vipWeight = 12,
        courierWeight = 4,
        criticWeight = 0,
        socialiteWeight = 8,
        lobbyOriginBias = 0.72f,
        rooftopDemandBias = 0.12f
    ),
    ContractConfig(
        id = 2,
        title = "Gallery Preview",
        subtitle = "Social traffic spikes between lounge and suites.",
        briefing = "Salon guests arrive in clusters. Plan shared drop-offs to multiply value instead of chasing single fares.",
        totalFloors = 7,
        durationSeconds = 100,
        targetScore = 2350,
        guestCap = 12,
        baseSpawnSeconds = 2.65f,
        vipWeight = 16,
        courierWeight = 4,
        criticWeight = 6,
        socialiteWeight = 18,
        lobbyOriginBias = 0.58f,
        rooftopDemandBias = 0.18f
    ),
    ContractConfig(
        id = 3,
        title = "Blue Hour Dispatch",
        subtitle = "Couriers reward sharp route timing.",
        briefing = "Messenger traffic accelerates the clock. Use couriers to extend the contract and keep the lobby flowing.",
        totalFloors = 8,
        durationSeconds = 108,
        targetScore = 2950,
        guestCap = 13,
        baseSpawnSeconds = 2.45f,
        vipWeight = 14,
        courierWeight = 18,
        criticWeight = 8,
        socialiteWeight = 12,
        lobbyOriginBias = 0.54f,
        rooftopDemandBias = 0.22f
    ),
    ContractConfig(
        id = 4,
        title = "Critics' Table",
        subtitle = "Mood management matters more than speed alone.",
        briefing = "Food critics now roam the tower. Miss too many and the house reputation collapses before closing time.",
        totalFloors = 8,
        durationSeconds = 112,
        targetScore = 3500,
        guestCap = 14,
        baseSpawnSeconds = 2.28f,
        vipWeight = 20,
        courierWeight = 12,
        criticWeight = 18,
        socialiteWeight = 14,
        lobbyOriginBias = 0.49f,
        rooftopDemandBias = 0.26f
    ),
    ContractConfig(
        id = 5,
        title = "Moonlit Gala",
        subtitle = "The penthouse becomes the night's anchor route.",
        briefing = "Penthouse demand surges. Private VIP lifts and tightly packed salon arrivals define your margin.",
        totalFloors = 9,
        durationSeconds = 118,
        targetScore = 4150,
        guestCap = 15,
        baseSpawnSeconds = 2.15f,
        vipWeight = 24,
        courierWeight = 14,
        criticWeight = 15,
        socialiteWeight = 22,
        lobbyOriginBias = 0.46f,
        rooftopDemandBias = 0.36f
    ),
    ContractConfig(
        id = 6,
        title = "Last Call at Meridian",
        subtitle = "Everything the tower can throw at a concierge.",
        briefing = "The full guest mix arrives with almost no dead air. Queue discipline decides whether the season ends in style.",
        totalFloors = 9,
        durationSeconds = 124,
        targetScore = 4850,
        guestCap = 16,
        baseSpawnSeconds = 2.02f,
        vipWeight = 26,
        courierWeight = 20,
        criticWeight = 18,
        socialiteWeight = 22,
        lobbyOriginBias = 0.44f,
        rooftopDemandBias = 0.32f
    )
)

fun floorLabel(floor: Int, totalFloors: Int): String = when {
    floor == 0 -> "L"
    floor == totalFloors - 1 -> "PH"
    else -> floor.toString()
}

fun perksForProfile(profile: CareerProfile): CareerPerks {
    var speed = 1f
    var patience = 1f
    var capacity = 3
    if (profile.unlockedContractId >= 2) {
        speed += 0.12f
    }
    if (profile.unlockedContractId >= 4) {
        capacity += 1
    }
    if (profile.unlockedContractId >= 5) {
        patience += 0.18f
    }
    return CareerPerks(
        speedMultiplier = speed,
        patienceMultiplier = patience,
        cabinCapacity = capacity
    )
}

fun perkCatalog(profile: CareerProfile): List<PermanentPerk> {
    val perks = mutableListOf<PermanentPerk>()
    if (profile.unlockedContractId >= 2) {
        perks += PermanentPerk("Brass Servo", "Elevator travel speed +12%.")
    }
    if (profile.unlockedContractId >= 4) {
        perks += PermanentPerk("Suite Ledger", "Cabin capacity +1.")
    }
    if (profile.unlockedContractId >= 5) {
        perks += PermanentPerk("Soft Lighting", "Guest patience lasts 18% longer.")
    }
    return perks
}
