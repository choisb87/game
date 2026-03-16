package com.velvetlift.game.game

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.random.Random

object GameEngine {
    private const val MOVE_SPEED_FLOORS_PER_SECOND = 1.85f
    private const val DOOR_OPEN_SECONDS = 1.25f
    private const val MAX_QUEUE_SIZE = 4

    fun newRun(contract: ContractConfig): HotelState {
        var state = HotelState(
            contract = contract,
            waitingByFloor = emptyWaiting(contract.totalFloors),
            remainingTime = contract.durationSeconds.toFloat(),
            spawnCooldown = 0.95f
        )
        repeat(3) {
            state = spawnGuest(state)
        }
        return state
    }

    fun queueFloor(state: HotelState, floor: Int): HotelState {
        if (state.phase != ServicePhase.PLAYING) return state
        if (floor == state.activeStop) {
            return state.copy(selectedFloor = floor, selectionPulse = 1f)
        }
        val currentRoundedFloor = state.carFloor.roundToInt()
        if (state.doorTimer > 0f && floor == currentRoundedFloor) {
            return state.copy(selectedFloor = floor, selectionPulse = 1f)
        }
        return when {
            floor in state.queuedStops -> {
                state.copy(
                    queuedStops = state.queuedStops.filterNot { it == floor },
                    selectedFloor = floor,
                    selectionPulse = 1f
                )
            }

            state.queuedStops.size >= MAX_QUEUE_SIZE -> {
                state.copy(selectedFloor = floor, selectionPulse = 1f)
            }

            else -> {
                state.copy(
                    queuedStops = state.queuedStops + floor,
                    selectedFloor = floor,
                    selectionPulse = 1f
                )
            }
        }
    }

    fun step(state: HotelState, dt: Float, perks: CareerPerks): HotelState {
        var current = ageVisuals(state, dt)
        if (current.phase != ServicePhase.PLAYING) {
            return current
        }

        current = current.copy(
            remainingTime = (current.remainingTime - dt).coerceAtLeast(0f),
            comboTimer = (current.comboTimer - dt).coerceAtLeast(0f)
        )
        if (current.comboTimer <= 0f && current.combo != 0) {
            current = current.copy(combo = 0)
        }

        current = decayWaiting(current, dt, perks)
        if (current.phase == ServicePhase.RESULT) return current

        current = decayPassengers(current, dt, perks)
        current = advanceElevator(current, dt, perks)
        if (current.phase == ServicePhase.RESULT) return current

        current = maybeSpawn(current, dt)

        if (current.mood <= 0f || current.remainingTime <= 0f) {
            current = finishRun(current)
        }
        return current
    }

    private fun ageVisuals(state: HotelState, dt: Float): HotelState {
        val texts = state.floatingTexts.mapNotNull { text ->
            val age = text.age + dt
            if (age >= 1.3f) null else text.copy(age = age)
        }
        return state.copy(
            floatingTexts = texts,
            arrivalPulse = (state.arrivalPulse - dt * 1.5f).coerceAtLeast(0f),
            selectionPulse = (state.selectionPulse - dt * 1.6f).coerceAtLeast(0f)
        )
    }

    private fun decayWaiting(state: HotelState, dt: Float, perks: CareerPerks): HotelState {
        var mood = state.mood
        var lost = state.lostGuests
        var nextTextId = state.nextTextId
        val addedTexts = mutableListOf<FloatingText>()

        val updatedWaiting = buildMap {
            for (floor in 0 until state.contract.totalFloors) {
                val remaining = mutableListOf<Guest>()
                for (guest in state.waitingByFloor[floor].orEmpty()) {
                    val patience = guest.patience - (guest.archetype.patienceDrain * dt / perks.patienceMultiplier)
                    if (patience <= 0f) {
                        mood -= guest.archetype.moodPenalty
                        lost += 1
                        addedTexts += FloatingText(
                            id = nextTextId++,
                            text = "-REP",
                            floor = floor,
                            color = Color(0xFFE57373)
                        )
                    } else {
                        remaining += guest.copy(
                            patience = patience.coerceAtMost(1f),
                            waitTime = guest.waitTime + dt
                        )
                    }
                }
                put(floor, remaining)
            }
        }

        val updated = state.copy(
            waitingByFloor = updatedWaiting,
            mood = mood.coerceIn(0f, 1f),
            lostGuests = lost,
            floatingTexts = state.floatingTexts + addedTexts,
            nextTextId = nextTextId
        )
        return if (updated.mood <= 0f) finishRun(updated) else updated
    }

    private fun decayPassengers(state: HotelState, dt: Float, perks: CareerPerks): HotelState {
        val updatedPassengers = state.passengers.map { guest ->
            guest.copy(
                patience = (guest.patience - (guest.archetype.patienceDrain * 0.35f * dt / perks.patienceMultiplier))
                    .coerceAtLeast(0f),
                rideTime = guest.rideTime + dt
            )
        }
        return state.copy(passengers = updatedPassengers)
    }

    private fun advanceElevator(state: HotelState, dt: Float, perks: CareerPerks): HotelState {
        if (state.doorTimer > 0f) {
            val nextDoor = state.doorTimer - dt
            return if (nextDoor <= 0f) {
                state.copy(doorTimer = 0f)
            } else {
                state.copy(doorTimer = nextDoor)
            }
        }

        val target = state.activeStop ?: state.queuedStops.firstOrNull()
        if (target == null) {
            return state
        }
        var current = state
        if (state.activeStop == null) {
            current = state.copy(
                activeStop = target,
                queuedStops = state.queuedStops.drop(1)
            )
        }

        val distance = target - current.carFloor
        val movement = MOVE_SPEED_FLOORS_PER_SECOND * perks.speedMultiplier * dt
        return if (abs(distance) <= movement) {
            handleArrival(current.copy(carFloor = target.toFloat(), activeStop = null), target, perks)
        } else {
            current.copy(carFloor = current.carFloor + sign(distance) * movement)
        }
    }

    private fun handleArrival(state: HotelState, floor: Int, perks: CareerPerks): HotelState {
        val unloading = state.passengers.filter { it.destination == floor }
        var stayingPassengers = state.passengers.filterNot { it.destination == floor }
        val sharedArrivalCount = unloading.size
        var score = state.score
        var mood = state.mood
        var remainingTime = state.remainingTime
        var delivered = state.deliveredGuests
        var combo = if (state.comboTimer > 0f && unloading.isNotEmpty()) state.combo + 1 else if (unloading.isNotEmpty()) 1 else state.combo
        var comboTimer = if (unloading.isNotEmpty()) 4.6f else state.comboTimer
        var nextTextId = state.nextTextId
        val texts = mutableListOf<FloatingText>()

        for (guest in unloading) {
            val privateRideBonus = if (guest.archetype == GuestArchetype.VIP && state.passengers.size == 1) 55 else 0
            val socialBonus = if (guest.archetype == GuestArchetype.SOCIALITE && sharedArrivalCount >= 2) 35 else 0
            val patienceTip = (guest.patience.coerceIn(0f, 1f) * guest.archetype.tipCeiling).roundToInt()
            val comboMultiplier = 1f + ((combo - 1).coerceAtLeast(0) * 0.12f)
            val fare = ((guest.archetype.baseFare + patienceTip + privateRideBonus + socialBonus) * comboMultiplier).roundToInt()
            score += fare
            delivered += 1
            mood = (mood + 0.01f).coerceAtMost(1f)
            texts += FloatingText(
                id = nextTextId++,
                text = "+$fare",
                floor = floor,
                color = guest.archetype.accent
            )
            if (guest.archetype == GuestArchetype.COURIER && guest.patience > 0.42f) {
                remainingTime += 5f
                texts += FloatingText(
                    id = nextTextId++,
                    text = "+5s",
                    floor = floor,
                    color = Color(0xFF63C6C1)
                )
            }
            if (guest.archetype == GuestArchetype.CRITIC && guest.patience > 0.55f) {
                mood = (mood + 0.05f).coerceAtMost(1f)
            }
        }

        val waitingHere = state.waitingByFloor[floor].orEmpty()
        val availableSeats = max(0, perks.cabinCapacity - stayingPassengers.size)
        val boarding = waitingHere
            .sortedWith(compareBy<Guest> { boardingPriority(it) }.thenBy { it.patience })
            .take(availableSeats)
        stayingPassengers = stayingPassengers + boarding

        val updatedWaiting = state.waitingByFloor.toMutableMap()
        updatedWaiting[floor] = waitingHere.filterNot { guest -> boarding.any { it.id == guest.id } }

        return state.copy(
            carFloor = floor.toFloat(),
            doorTimer = DOOR_OPEN_SECONDS,
            passengers = stayingPassengers,
            waitingByFloor = updatedWaiting,
            score = score,
            mood = mood.coerceIn(0f, 1f),
            remainingTime = remainingTime,
            deliveredGuests = delivered,
            combo = combo,
            comboTimer = comboTimer,
            floatingTexts = state.floatingTexts + texts,
            nextTextId = nextTextId,
            arrivalPulse = 1f,
            selectedFloor = floor,
            selectionPulse = 1f
        )
    }

    private fun boardingPriority(guest: Guest): Int = when (guest.archetype) {
        GuestArchetype.CRITIC -> 0
        GuestArchetype.VIP -> 1
        GuestArchetype.COURIER -> 2
        GuestArchetype.SOCIALITE -> 3
        GuestArchetype.REGULAR -> 4
    }

    private fun maybeSpawn(state: HotelState, dt: Float): HotelState {
        if (state.remainingTime <= 0f) return state
        val cooldown = state.spawnCooldown - dt
        return if (cooldown > 0f) {
            state.copy(spawnCooldown = cooldown)
        } else {
            spawnGuest(state.copy(spawnCooldown = nextSpawnSeconds(state.contract)))
        }
    }

    private fun spawnGuest(state: HotelState): HotelState {
        if (state.totalWaiting >= state.contract.guestCap) {
            return state.copy(spawnCooldown = 0.7f)
        }

        var origin = chooseOrigin(state.contract)
        for (attempt in 0 until 8) {
            val candidate = chooseOrigin(state.contract)
            if (state.waitingByFloor[candidate].orEmpty().size < 4) {
                origin = candidate
                break
            }
        }
        val destination = chooseDestination(origin, state.contract)
        val guest = Guest(
            id = state.nextGuestId,
            archetype = chooseArchetype(state.contract),
            origin = origin,
            destination = destination
        )
        val updatedWaiting = state.waitingByFloor.toMutableMap()
        updatedWaiting[origin] = updatedWaiting[origin].orEmpty() + guest
        return state.copy(
            waitingByFloor = updatedWaiting,
            nextGuestId = state.nextGuestId + 1
        )
    }

    private fun chooseOrigin(contract: ContractConfig): Int {
        if (Random.nextFloat() < contract.lobbyOriginBias) {
            return 0
        }
        return Random.nextInt(1, contract.totalFloors)
    }

    private fun chooseDestination(origin: Int, contract: ContractConfig): Int {
        val topFloor = contract.totalFloors - 1
        if (origin == 0) {
            return if (Random.nextFloat() < contract.rooftopDemandBias) {
                topFloor
            } else {
                Random.nextInt(1, contract.totalFloors)
            }
        }

        if (origin == topFloor) {
            return if (Random.nextFloat() < 0.65f) 0 else Random.nextInt(1, topFloor)
        }

        return if (Random.nextFloat() < 0.58f) {
            0
        } else {
            var destination = Random.nextInt(1, contract.totalFloors)
            while (destination == origin) {
                destination = Random.nextInt(1, contract.totalFloors)
            }
            destination
        }
    }

    private fun chooseArchetype(contract: ContractConfig): GuestArchetype {
        val regularWeight = 60
        val total = regularWeight +
            contract.vipWeight +
            contract.courierWeight +
            contract.criticWeight +
            contract.socialiteWeight
        val roll = Random.nextInt(total)
        var cursor = regularWeight
        if (roll < cursor) return GuestArchetype.REGULAR
        cursor += contract.vipWeight
        if (roll < cursor) return GuestArchetype.VIP
        cursor += contract.courierWeight
        if (roll < cursor) return GuestArchetype.COURIER
        cursor += contract.criticWeight
        if (roll < cursor) return GuestArchetype.CRITIC
        return GuestArchetype.SOCIALITE
    }

    private fun nextSpawnSeconds(contract: ContractConfig): Float {
        val jitter = Random.nextFloat() * 0.9f
        return contract.baseSpawnSeconds + jitter - 0.25f
    }

    private fun finishRun(state: HotelState): HotelState {
        if (state.phase == ServicePhase.RESULT) return state
        val success = state.mood > 0f && state.score >= state.contract.targetScore
        val stars = when {
            !success -> 0
            state.score >= (state.contract.targetScore * 1.35f).roundToInt() && state.lostGuests <= 1 && state.mood > 0.6f -> 3
            state.score >= (state.contract.targetScore * 1.12f).roundToInt() && state.mood > 0.35f -> 2
            else -> 1
        }
        val result = ContractResult(
            success = success,
            score = state.score,
            stars = stars,
            delivered = state.deliveredGuests,
            lost = state.lostGuests,
            moodRemaining = state.mood
        )
        return state.copy(
            phase = ServicePhase.RESULT,
            result = result,
            activeStop = null,
            queuedStops = emptyList(),
            doorTimer = 0f
        )
    }

    private fun emptyWaiting(totalFloors: Int): Map<Int, List<Guest>> =
        (0 until totalFloors).associateWith { emptyList<Guest>() }
}
