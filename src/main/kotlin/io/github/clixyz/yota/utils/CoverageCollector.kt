package io.github.clixyz.yota.utils

import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.xml
import org.robolectric.shadows.ShadowLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

val collectLock = ReentrantLock()

val coverageData = mutableMapOf<String, Int>()
val coverageIndex = mutableMapOf<String, MutableSet<Int>>()

var coverageStorePath = "${System.getProperty("user.dir")}/results/coverage/${System.getProperty("running_app_pkg")}"

fun dumpMonkeyCycles(counter: Long) {
    if (!File(coverageStorePath).exists()) {
        File(coverageStorePath).mkdirs()
    }

    val counterFile = File("$coverageStorePath/cycles.txt")
    FileOutputStream(counterFile, true).bufferedWriter().use { writer ->
        writer.write("Monkey Events Injected ~~ $counter")
        writer.newLine()
    }
}

fun transToString(time: Long):String{
    return SimpleDateFormat("HH-mm-ss").format(time)
}

fun dump(timeStamp: Long) {
    if (!File(coverageStorePath).exists()) {
        File(coverageStorePath).mkdirs()
    }

//    println("========== Dump Coverage Data at $timeStamp ============")
    val tmpData = coverageData.toMap()
    val tmpIndex = coverageIndex.toMap()
    val dataFile = File("$coverageStorePath/CoverageData-${transToString(timeStamp)}")
    dataFile.bufferedWriter().use { writer ->
        tmpData.forEach { entry ->
            writer.write("${entry.key}~~${entry.value}")
            writer.newLine()
        }
    }
    val indexFile = File("$coverageStorePath/CoverageIndex-${transToString(timeStamp)}")
    indexFile.bufferedWriter().use { writer ->
        tmpIndex.forEach { entry ->
            writer.write(entry.key)
            entry.value.forEach {
                writer.write("~~$it")
            }
            writer.newLine()
        }
    }
//    println("===================== DUMP FINISHED ====================")
}


class CoverageListener : ShadowLog.Listener() {
    override fun receivedMessage(msg: String?) {
        if (msg != null) {
            collectLock.withLock {
                val cov = msg.trim().run {
                    listOf(substringBefore("> ") + ">", substringAfter("> "))
                }

                val count = cov[1].split("/")[0].toInt()
                val index = cov[1].split("/")[1].toInt()
                val set = coverageIndex[cov[0]]
                if (set == null || !set.contains(index)) {
                    coverageData[cov[0]] = (coverageData[cov[0]] ?: 0) + count
                    coverageIndex[cov[0]] = (set ?: mutableSetOf()).also { it.add(index) }
                }
            }
        }
    }
}

sealed class CoverageData
data class ClazzCoverageData(val name: String, var totalLine: Int, var covered: Int, val methodTotalLines: MutableMap<String, Triple<Int, Int, Double>>) : CoverageData()
data class PackageCoverageData(val name: String, var totalLine: Int, var covered: Int, val list: MutableList<CoverageData>) : CoverageData()
var totalCovered = 0
var totalLine = 0

fun calculateCoverage() {
    println("start calculate coverage")
    val topList = mutableListOf<CoverageData>()
    File("$coverageStorePath/$CoverageLabelFile").bufferedReader().useLines { lines ->
        lines.forEach { line ->
            val cov = line.trim().run {
                listOf(substringBefore("> ") + ">", substringAfter("> "))
            }
            val fullSignature = cov[0].drop(1).dropLast(1)
            val clazzName = fullSignature.split(": ")[0]
            val levelNames = clazzName.split(".")
            val methodName = fullSignature.split(": ")[1]
            collectLock.withLock {
                appendCoverage(topList, levelNames, 0, methodName, cov)
            }
        }
    }
    println("Total Coverage: Covered $totalCovered Total: $totalLine Ratio: ${totalCovered.toDouble() / totalLine.toDouble()}")

    val coverageFile = File("$coverageStorePath/$CoverageOutPutFile")
    coverageFile.bufferedWriter().use { writer ->
        val xml = toXml(topList)
//        println(xml.toString())
        writer.write(xml.toString())
        writer.flush()
    }
}

private fun appendCoverage(currentList: MutableList<CoverageData>, levelsName: List<String>, index: Int, methodName: String, cov: List<String>) : Pair<Int, Int> {
    val name = levelsName[index]
    if (index < levelsName.lastIndex) {
        val pack = (currentList.find {
            it is PackageCoverageData && it.name == name
        } ?: PackageCoverageData(name, 0, 0, mutableListOf())) as PackageCoverageData
        val (totalNew, coverNew) = appendCoverage(pack.list, levelsName, index+1, methodName, cov)
        pack.totalLine += totalNew
        pack.covered += coverNew
        currentList.remove(pack)
        currentList.add(pack)
        return totalNew to coverNew
    } else {
        val clazzCoverage = (currentList.find {
            it is ClazzCoverageData && it.name == name
        } ?: ClazzCoverageData(name, 0,0, mutableMapOf())) as ClazzCoverageData
        if (clazzCoverage.methodTotalLines[methodName] != null) {
            assert(false)
        }
        coverageData[cov[0]] = coverageData[cov[0]] ?: 0
        clazzCoverage.methodTotalLines[methodName] = Triple(cov[1].toInt(), coverageData[cov[0]]!!,  coverageData[cov[0]]!!.toDouble() / cov[1].toDouble())
        clazzCoverage.totalLine += cov[1].toInt()
        clazzCoverage.covered += coverageData[cov[0]]!!
        totalLine += cov[1].toInt()
        totalCovered += coverageData[cov[0]]!!
        currentList.remove(clazzCoverage)
        currentList.add(clazzCoverage)
//        println("Metroid: [${cov[0]}] Covered: ${coverageData[cov[0]]!!} Total: ${cov[1]}")
        return cov[1].toInt() to coverageData[cov[0]]!!
    }
}


private fun toXml(list: List<CoverageData>) =
    xml("Coverage") {
        xmlns = "http://www.ics.nju.edu.cn/weihe/metroid"
        attribute("Name", System.getProperty("running_app_pkg"))
        attribute("Total_Lines", totalLine)
        attribute("Covered_Lines", totalCovered)
        attribute("Coverage_Ratio", (totalCovered.toDouble() / totalLine.toDouble()).format(2))
        list.forEach {
            handleNode(it)
        }
    }

private fun Node.handleNode(coverageData: CoverageData)  {
    if (coverageData is PackageCoverageData) {
        "Package" {
            attribute("Name", coverageData.name)
            attribute("Total_Lines", coverageData.totalLine)
            attribute("Covered_Lines", coverageData.covered)
            attribute("Coverage_Ratio", (coverageData.covered.toDouble() / coverageData.totalLine.toDouble()).format(2))
            coverageData.list.forEach {
                handleNode(it)
            }
        }
    } else {
        val clazzCoverageData = coverageData as ClazzCoverageData
        "Class" {
            attribute("Name", clazzCoverageData.name)
            attribute("Total_Lines", clazzCoverageData.totalLine)
            attribute("Covered_Lines", clazzCoverageData.covered)
            attribute("Coverage_Ratio", (clazzCoverageData.covered.toDouble() / clazzCoverageData.totalLine.toDouble()).format(2))
            clazzCoverageData.methodTotalLines.forEach { (s, triple) ->
                val (t,c,r) = triple
                "Method" {
                    attribute("Name", s.replace("<","").replace(">",""))
                    attribute("Total_Lines", t)
                    attribute("Covered_Lines", c)
                    attribute("Coverage_Ratio", r.format(2))
                }
            }
        }
    }
}


fun Double.format(digits: Int) = "%.${digits}f".format(this)
