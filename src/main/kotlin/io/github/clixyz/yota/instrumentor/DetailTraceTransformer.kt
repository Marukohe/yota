package io.github.clixyz.yota.instrumentor

import io.github.clixyz.yota.utils.SOOT_COV_TAG
import io.github.clixyz.yota.utils.pkgName
import org.robocli.soot.*
import org.robocli.soot.Unit
import org.robocli.soot.jimple.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

val outputLock = ReentrantLock()
val output = mutableListOf<String>()

class DetailTraceTransformer : BodyTransformer() {

    override fun internalTransform(b: Body?, phaseName: String?, options: MutableMap<String, String>?) {
        if (b == null) return

        if (b.method.declaringClass.shortName.startsWith("R$")) return
        if (!b.method.declaringClass.packageName.contains(pkgName)) return
        val units = b.units
        val label = b.method.signature
        var count = 0
        var total = 0
        var lastUnit: Unit? = null
        var insertCount = 0
        var index = 0

        val iterator = units.snapshotIterator()
        while (iterator.hasNext()) {
            val u = iterator.next()
            count++
            u.apply(object : AbstractStmtSwitch() {
                override fun caseBreakpointStmt(stmt: BreakpointStmt?) {
                    if (stmt == null) return
                    if (lastUnit == null) {
                        insertFirstLog(b, u, units, count, index, label)
                    } else {
                        insertLog(b, lastUnit!!, units, count, index, label)
                    }
                    insertCount++
                    lastUnit = u
                    total += count
                    count = 0
                    index++
                }

                override fun caseGotoStmt(stmt: GotoStmt?) {
                    if (stmt == null) return
                    if (lastUnit == null) {
                        insertFirstLog(b, u, units, count, index, label)
                    } else {
                        insertLog(b, lastUnit!!, units, count, index, label)
                    }
                    insertCount++
                    lastUnit = u
                    total += count
                    count = 0
                    index++
                }

                override fun caseRetStmt(stmt: RetStmt?) {
                    if (stmt == null) return
                    if (lastUnit == null) {
                        insertFirstLog(b, u, units, count, index, label)
                    } else {
                        insertLog(b, lastUnit!!, units, count, index, label)
                    }
                    insertCount++
                    lastUnit = u
                    total += count
                    count = 0
                    index++
                }

                override fun caseReturnVoidStmt(stmt: ReturnVoidStmt?) {
                    if (stmt == null) return
                    if (lastUnit == null) {
                        insertFirstLog(b, u, units, count, index, label)
                    } else {
                        insertLog(b, lastUnit!!, units, count, index, label)
                    }
                    insertCount++
                    lastUnit = u
                    total += count
                    count = 0
                    index++
                }

                override fun caseThrowStmt(stmt: ThrowStmt?) {
                    if (stmt == null) return
                    if (lastUnit == null) {
                        insertFirstLog(b, u, units, count, index, label)
                    } else {
                        insertLog(b, lastUnit!!, units, count, index, label)
                    }
                    insertCount++
                    lastUnit = u
                    total += count
                    count = 0
                    index++
                }
            })
        }

        if (count != 0) {
            if (lastUnit == null) {
                insertFirstLog(b, units.last, units, count, index, label)
            } else {
                insertLog(b, lastUnit!!, units, count, index, label)
            }
            insertCount++
            total += count
            index++
        }

        outputLock.withLock {
            output.add("$label $total")
        }

    }

    private fun insertFirstLog(b: Body, u: Unit, units: UnitPatchingChain, count: Int, total: Int, label: String) {
        val msg = "$label $count/$total"
        val tagRef = Jimple.v().newLocal("_tagLogStr", RefType.v("java.lang.String")).also { b.locals.add(it) }
        val logRef = Jimple.v().newLocal("_logLogStr", RefType.v("java.land.String")).also { b.locals.add(it) }

        val toCall = Scene.v().getSootClass("android.util.Log").getMethod("int i(java.lang.String,java.lang.String)")

        units.insertBefore(Jimple.v().newAssignStmt(tagRef, StringConstant.v(SOOT_COV_TAG)), u)
        units.insertBefore(Jimple.v().newAssignStmt(logRef, StringConstant.v(msg)), u)
        units.insertBefore(
            Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(toCall.makeRef(), tagRef, logRef)), u
        )
        b.validate()
    }

    private fun insertLog(b: Body, u: Unit, units: UnitPatchingChain, count: Int, total: Int, label: String) {
        val msg = "$label $count/$total"
        val tagRef = Jimple.v().newLocal("_tagLogStr", RefType.v("java.lang.String")).also { b.locals.add(it) }
        val logRef = Jimple.v().newLocal("_logLogStr", RefType.v("java.land.String")).also { b.locals.add(it) }

        val toCall = Scene.v().getSootClass("android.util.Log").getMethod("int i(java.lang.String,java.lang.String)")

        units.insertAfter(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(toCall.makeRef(), tagRef, logRef)), u)
        units.insertAfter(Jimple.v().newAssignStmt(tagRef, StringConstant.v(SOOT_COV_TAG)), u)
        units.insertAfter(Jimple.v().newAssignStmt(logRef, StringConstant.v(msg)), u)
        b.validate()
    }
}