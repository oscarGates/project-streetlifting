package com.streetlifting.app.core

/** Discos por lado para cargar la barra (reparto voraz con los discos estándar). */
object Discos {

    val KG = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    val LBS = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)

    /** Discos de un lado, de mayor a menor. Vacío si la carga es la barra o menos. */
    fun porLado(total: Double, barra: Double, unit: String): List<Double> {
        var resto = (total - barra) / 2.0
        val out = ArrayList<Double>()
        for (d in if (unit == "kg") KG else LBS) {
            while (resto >= d - 1e-9) {
                out.add(d)
                resto -= d
            }
        }
        return out
    }
}
