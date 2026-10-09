package com.driverapp.dados

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Banco local (SQLite via Room).
 *
 * IMPORTANTE para futuras alterações: ao mudar uma tabela, aumente `version`
 * e escreva uma migração em [Migracoes] — nunca apague o banco do motorista.
 */
@Database(
    entities = [
        ConfiguracaoEntity::class, DespesaEntity::class, JornadaEntity::class, CorridaEntity::class,
        LimiteEntity::class, OfertaEntity::class, SaldoEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class BancoDados : RoomDatabase() {
    abstract fun configuracao(): ConfiguracaoDao
    abstract fun despesas(): DespesaDao
    abstract fun jornadas(): JornadaDao
    abstract fun corridas(): CorridaDao
    abstract fun limites(): LimiteDao
    abstract fun ofertas(): OfertaDao
    abstract fun saldos(): SaldoDao

    companion object {
        fun criar(context: Context): BancoDados =
            Room.databaseBuilder(context, BancoDados::class.java, "driverapp.db")
                .addMigrations(*Migracoes.todas)
                .build()
    }
}

object Migracoes {
    /**
     * Versão 1 → 2: só ACRESCENTA colunas (todas opcionais) e tabelas novas.
     * Nenhum dado existente é apagado ou alterado.
     */
    val DE_1_PARA_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `configuracao` ADD COLUMN `tema` TEXT")
            db.execSQL("ALTER TABLE `configuracao` ADD COLUMN `horaInicio` TEXT")
            db.execSQL("ALTER TABLE `configuracao` ADD COLUMN `horaFim` TEXT")
            db.execSQL("ALTER TABLE `configuracao` ADD COLUMN `kmPorDia` REAL")
            db.execSQL("ALTER TABLE `despesa` ADD COLUMN `prazoMeses` INTEGER")
            db.execSQL("ALTER TABLE `despesa` ADD COLUMN `observacao` TEXT")
            db.execSQL("ALTER TABLE `corrida` ADD COLUMN `categoria` TEXT")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `limite` (" +
                    "`chave` TEXT NOT NULL, `kmMinimo` REAL NOT NULL, `kmBom` REAL NOT NULL, " +
                    "`horaMinimo` REAL NOT NULL, `horaBom` REAL NOT NULL, PRIMARY KEY(`chave`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `oferta` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `plataforma` TEXT NOT NULL, `valor` REAL, " +
                    "`minColeta` REAL, `kmColeta` REAL, `minViagem` REAL, `kmViagem` REAL, `categoria` TEXT, " +
                    "`nota` REAL, `confianca` TEXT NOT NULL, `alertas` TEXT NOT NULL, `textoAnonimo` TEXT NOT NULL, " +
                    "`vistaEm` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_oferta_vistaEm` ON `oferta` (`vistaEm`)")
        }
    }

    /** Versão 2 → 3: só ACRESCENTA a tabela de histórico de saldos. */
    val DE_2_PARA_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `saldo` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `plataforma` TEXT NOT NULL, `semanal` INTEGER NOT NULL, " +
                    "`periodoInicio` INTEGER NOT NULL, `valor` REAL, `corridas` INTEGER, `ultimaCorrida` REAL, " +
                    "`lidoEm` INTEGER NOT NULL, `estado` TEXT NOT NULL, `origem` TEXT NOT NULL, `motivo` TEXT)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_saldo_plataforma_periodoInicio` ON `saldo` (`plataforma`, `periodoInicio`)")
        }
    }

    val todas: Array<Migration> = arrayOf(DE_1_PARA_2, DE_2_PARA_3)
}
