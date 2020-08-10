package com.example.test3.contest_watch

import com.example.test3.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CodeforcesContestWatcher(val handle: String, val contestID: Int, val scope: CoroutineScope): CodeforcesContestWatchListener(){

    private var job: Job? = null

    fun start(){
        job = scope.launch {
            val contestName = ChangingValue("")
            val contestType = ChangingValue(CodeforcesContestType.UNDEFINED)
            val phaseCodeforces = ChangingValue(CodeforcesContestPhase.UNDEFINED)
            val rank = ChangingValue(-1)
            val pointsTotal = ChangingValue(0.0)
            val durationSeconds = ChangingValue(-1L)
            val startTimeSeconds = ChangingValue(-1L)
            var problemNames: ArrayList<String>? = null
            val participationType = ChangingValue(CodeforcesParticipationType.NOT_PARTICIPATED)
            val sysTestPercentage = ChangingValue(-1)
            var problemsResults: List<ChangingValue<CodeforcesProblemResult>> = emptyList()
            val testedSubmissions = mutableSetOf<Long>()
            var ratingChangeWaitingStartTimeMillis = 0L

            while (true) {
                var timeSecondsFromStart: Long? = null
                CodeforcesAPI.getContestStandings(contestID, handle, participationType.value!=CodeforcesParticipationType.CONTESTANT)?.run {
                    if(status == CodeforcesAPIStatus.FAILED){
                        if(comment == "contestId: Contest with id $contestID has not started")
                            phaseCodeforces.value = CodeforcesContestPhase.BEFORE
                        return@run
                    }
                    with(result ?: return@run){
                        if(problemNames == null){
                            val tmp = problems.mapTo(ArrayList()) { it.index }
                            problemNames = tmp
                            onSetProblemNames(tmp.toTypedArray())
                        }

                        phaseCodeforces.value = contest.phase
                        timeSecondsFromStart = contest.relativeTimeSeconds
                        durationSeconds.value = contest.durationSeconds
                        startTimeSeconds.value = contest.startTimeSeconds
                        contestName.value = contest.name
                        contestType.value = contest.type

                        rows.find { row ->
                            row.party.participantType.participatedInContest()
                        }?.let { row ->
                            participationType.value = row.party.participantType
                            rank.value = row.rank
                            pointsTotal.value = row.points
                            with(row.problemResults){
                                if (this.size != problemsResults.size) problemsResults = this.map { ChangingValue(it, true) }
                                else this.forEachIndexed { index, result -> problemsResults[index].value = result }
                            }
                        }
                    }
                }


                if(contestName.isChanged() || contestType.isChanged()) onSetContestNameAndType(contestName.value, contestType.value)
                if(phaseCodeforces.isChanged()) onSetContestPhase(phaseCodeforces.value)

                if(phaseCodeforces.value == CodeforcesContestPhase.CODING && (durationSeconds.isChanged() || startTimeSeconds.isChanged())) timeSecondsFromStart?.let{
                    val remainingTime = durationSeconds.value - it
                    onSetRemainingTime(remainingTime)
                }

                if(phaseCodeforces.value == CodeforcesContestPhase.SYSTEM_TEST){
                    //get progress of testing (0% ... 100%)
                    CodeforcesAPI.getPageSource("contest/$contestID", "en")?.let { page ->
                        var i = page.indexOf("<span class=\"contest-state-regular\">")
                        if (i != -1) {
                            i = page.indexOf(">", i + 1)
                            val progress = page.substring(i + 1, page.indexOf("</", i + 1))
                            if (progress.endsWith('%')) {
                                sysTestPercentage.value = progress.substring(0, progress.length - 1).toInt()
                                if(sysTestPercentage.isChanged()) onSetSysTestProgress(sysTestPercentage.value)
                            }
                        }
                    }
                }


                if(participationType.isChanged()){
                    onSetParticipationType(participationType.value)
                    if(participationType.value == CodeforcesParticipationType.CONTESTANT){
                        pointsTotal.value = 0.0
                        rank.value = -1
                        problemsResults = emptyList()
                        continue
                    }
                }

                if(rank.value != -1){
                    if(rank.isChanged()) onSetContestantRank(rank.value)
                    if(pointsTotal.isChanged()) onSetContestantPoints(pointsTotal.value)

                    problemsResults.forEachIndexed { index, changingValue ->
                        if(changingValue.isChanged()){
                            val result = changingValue.value
                            val problemName = problemNames!![index]
                            onSetProblemResult(problemName, result)
                        }

                    }
                }

                if(phaseCodeforces.value == CodeforcesContestPhase.SYSTEM_TEST
                    && participationType.value.participatedInContest()
                    && contestType.value == CodeforcesContestType.CF
                    && problemsResults.count { it.value.type == CodeforcesProblemStatus.PRELIMINARY } > 0
                ){
                    CodeforcesAPI.getContestSubmissions(contestID, handle)?.result
                        ?.filterNot { submission -> testedSubmissions.contains(submission.id) }
                        ?.asSequence()
                        ?.filter { submission -> submission.author.participantType.participatedInContest() }
                        ?.filter { submission -> submission.testset == "TESTS" }
                        ?.filter { submission -> submission.verdict != CodeforcesProblemVerdict.WAITING }
                        ?.filter { submission -> submission.verdict != CodeforcesProblemVerdict.TESTING }
                        ?.filter { submission -> submission.verdict != CodeforcesProblemVerdict.SKIPPED }
                        ?.forEach { submission ->
                            testedSubmissions.add(submission.id)
                            onSetProblemSystestResult(submission)
                        }
                }


                //------------------------
                commit()
                when(phaseCodeforces.value){
                    CodeforcesContestPhase.CODING -> delay(3_000)
                    CodeforcesContestPhase.SYSTEM_TEST -> delay(5_000)
                    CodeforcesContestPhase.PENDING_SYSTEM_TEST -> delay(15_000)
                    CodeforcesContestPhase.FINISHED -> {
                        if(participationType.value != CodeforcesParticipationType.CONTESTANT) return@launch

                        val currentTimeMillis = System.currentTimeMillis()
                        if(ratingChangeWaitingStartTimeMillis == 0L) ratingChangeWaitingStartTimeMillis = currentTimeMillis

                        CodeforcesAPI.getContestRatingChanges(contestID)?.let { response ->
                            if(response.status == CodeforcesAPIStatus.FAILED){
                                if(response.comment == "contestId: Rating changes are unavailable for this contest") return@launch
                                return@let
                            }
                            with(response.result ?: return@let) {
                                val change = findLast { it.handle == handle } ?: return@let
                                onRatingChange(change)
                                return@launch
                            }
                        }

                        val hoursWaiting = TimeUnit.MILLISECONDS.toHours(currentTimeMillis - ratingChangeWaitingStartTimeMillis)
                        when {
                            hoursWaiting<=1 -> delay(10_000)
                            hoursWaiting<=2 -> delay(30_000)
                            hoursWaiting<=4 -> delay(60_000)
                            else -> return@launch
                        }
                    }
                    else -> delay(30_000)
                }
            }
        }
    }

    fun stop(){
        job?.cancel()
    }




    private val listeners = mutableListOf<CodeforcesContestWatchListener>()

    fun addCodeforcesContestWatchListener(listener: CodeforcesContestWatchListener) = listeners.add(listener)

    override fun onSetContestNameAndType(contestName: String, contestType: CodeforcesContestType) {
        listeners.forEach { l -> l.onSetContestNameAndType(contestName, contestType) }
    }

    override fun onSetProblemNames(problemNames: Array<String>) {
        listeners.forEach { l -> l.onSetProblemNames(problemNames) }
    }

    override fun onSetContestPhase(phase: CodeforcesContestPhase) {
        listeners.forEach { l -> l.onSetContestPhase(phase) }
    }

    override fun onSetRemainingTime(timeSeconds: Long) {
        listeners.forEach { l -> l.onSetRemainingTime(timeSeconds) }
    }

    override fun onSetSysTestProgress(percents: Int) {
        listeners.forEach { l -> l.onSetSysTestProgress(percents) }
    }

    override fun onSetContestantRank(rank: Int) {
        listeners.forEach { l -> l.onSetContestantRank(rank) }
    }

    override fun onSetContestantPoints(points: Double) {
        listeners.forEach { l -> l.onSetContestantPoints(points) }
    }

    override fun onSetProblemResult(problemName: String, result: CodeforcesProblemResult) {
        listeners.forEach { l -> l.onSetProblemResult(problemName, result) }
    }

    override fun onSetProblemSystestResult(submission: CodeforcesSubmission) {
        listeners.forEach { l -> l.onSetProblemSystestResult(submission) }
    }

    override fun onSetParticipationType(type: CodeforcesParticipationType) {
        listeners.forEach { l -> l.onSetParticipationType(type) }
    }

    override fun onRatingChange(ratingChange: CodeforcesRatingChange) {
        listeners.forEach { l -> l.onRatingChange(ratingChange) }
    }

    override fun commit() {
        listeners.forEach { l -> l.commit() }
    }
}


abstract class CodeforcesContestWatchListener{
    abstract fun onSetContestNameAndType(contestName: String, contestType: CodeforcesContestType)
    abstract fun onSetProblemNames(problemNames: Array<String>)
    abstract fun onSetContestPhase(phase: CodeforcesContestPhase)
    abstract fun onSetRemainingTime(timeSeconds: Long)
    abstract fun onSetSysTestProgress(percents: Int)
    abstract fun onSetContestantRank(rank: Int)
    abstract fun onSetContestantPoints(points: Double)
    abstract fun onSetProblemResult(problemName: String, result: CodeforcesProblemResult)
    abstract fun onSetProblemSystestResult(submission: CodeforcesSubmission)
    abstract fun onSetParticipationType(type: CodeforcesParticipationType)
    abstract fun onRatingChange(ratingChange: CodeforcesRatingChange)
    abstract fun commit()
}


class ChangingValue<T>(private var x: T, private var changingFlag: Boolean = false){

    fun isChanged(): Boolean = changingFlag

    var value: T
        get() = x
        set(newValue) {
            if(newValue != x){
                changingFlag = true
                x = newValue
            }else{
                changingFlag = false
            }
        }
}