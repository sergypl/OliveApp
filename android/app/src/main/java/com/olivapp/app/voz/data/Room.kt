package com.olivapp.app.voz.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "notas_voz")
data class NotaVozFila(
    @PrimaryKey val id: String,
    val fichero: String,
    @ColumnInfo(name = "creada_en") val creadaEn: String,
    val segundos: Int,
    val estado: String,
    val texto: String,
    /** Borrador IA en JSON (borrador-ia-desde-transcripcion); vacío si no hay. */
    @ColumnInfo(defaultValue = "") val borrador: String = "",
    /**
     * Cuenta dueña de la nota (aislamiento de la cola por socio). Vacío en las
     * notas grabadas antes de que existiera el dueño: se conservan en el
     * teléfono, pero no se enseñan ni se envían con la cuenta de nadie.
     */
    @ColumnInfo(name = "dueno", defaultValue = "") val dueno: String = ""
)

/** Acceso a la cola de notas de voz en Room. */
@Dao
interface NotasVozDao {
    @Query("SELECT * FROM notas_voz WHERE dueno = :dueno ORDER BY creada_en DESC")
    fun notas(dueno: String): List<NotaVozFila>

    @Query(
        "SELECT * FROM notas_voz WHERE estado = 'pendiente' AND dueno = :dueno " +
            "ORDER BY creada_en ASC"
    )
    fun pendientes(dueno: String): List<NotaVozFila>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertar(nota: NotaVozFila)

    @Query(
        "UPDATE notas_voz SET estado = :estado, texto = :texto, " +
            "borrador = :borrador WHERE id = :id"
    )
    fun marcar(id: String, estado: String, texto: String, borrador: String)

    @Query("UPDATE notas_voz SET borrador = :borrador WHERE id = :id")
    fun actualizarBorrador(id: String, borrador: String)

    /**
     * Notas que se quedaron a medias en una transcripción (el proceso murió
     * mientras tanto): vuelven a la cola como 'pendiente'.
     */
    @Query("UPDATE notas_voz SET estado = 'pendiente' WHERE estado = 'transcribiendo'")
    fun reanudarTranscripciones()
}

/** Base Room de la cola de notas de voz. */
@Database(entities = [NotaVozFila::class], version = 3, exportSchema = false)
abstract class BaseNotasVoz : RoomDatabase() {
    abstract fun dao(): NotasVozDao

    companion object {
        @Volatile
        private var instancia: BaseNotasVoz? = null

        /** v2 (borrador-ia-desde-transcripcion): cada nota guarda su borrador IA. */
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notas_voz ADD COLUMN borrador TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /** v3 (aislamiento-de-cola-por-socio): cada nota pertenece a una cuenta. */
        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notas_voz ADD COLUMN dueno TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun de(contexto: Context): BaseNotasVoz =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    BaseNotasVoz::class.java,
                    "notas-voz.db"
                )
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                    // La cola es pequeña y las consultas son de pocos milisegundos:
                    // el almacén mantiene la interfaz síncrona del resto de la app.
                    .allowMainThreadQueries()
                    .build()
                    .also { instancia = it }
            }
    }
}
