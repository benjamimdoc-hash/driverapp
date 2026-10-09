package com.driverapp.dados

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Banco local (SQLite via Room).
 *
 * IMPORTANTE para futuras alterações: ao mudar uma tabela, aumente `version`
 * e escreva uma migração em [Migracoes] — nunca apague o banco do motorista.
 */
@Database(
    entities = [ConfiguracaoEntity::class, DespesaEntity::class, JornadaEntity::class, CorridaEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class BancoDados : RoomDatabase() {
    abstract fun configuracao(): ConfiguracaoDao
    abstract fun despesas(): DespesaDao
    abstract fun jornadas(): JornadaDao
    abstract fun corridas(): CorridaDao

    companion object {
        fun criar(context: Context): BancoDados =
            Room.databaseBuilder(context, BancoDados::class.java, "driverapp.db")
                .addMigrations(*Migracoes.todas)
                .build()
    }
}

/** Migrações entre versões do banco. Vazio na versão 1. */
object Migracoes {
    val todas: Array<androidx.room.migration.Migration> = emptyArray()
}
