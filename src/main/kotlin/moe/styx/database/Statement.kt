package moe.styx.database

import kotlinx.serialization.Serializable
import moe.styx.dbConfig
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

fun openConnection(database: String = "Styx2"): Connection {
    Class.forName("com.mysql.cj.jdbc.Driver").newInstance()
    return DriverManager.getConnection(
        "jdbc:mysql://${dbConfig.ip}/$database?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Europe/Berlin",
        dbConfig.user, dbConfig.pass
    )
}

fun openStatement(query: String, database: String = "Styx2"): Pair<Connection, PreparedStatement> {
    val connection = openConnection(database)
    return Pair(connection, connection.prepareStatement(query))
}

fun objectExists(GUID: String, table: String, identifier: String = "GUID"): Boolean {
    val (con, stat) = openStatement("SELECT $identifier FROM $table WHERE $identifier=?;")
    stat.setString(1, GUID)
    val results = stat.executeQuery()
    val exists = results.next()
    stat.close()
    con.close()
    return exists
}

@Serializable
data class DbConfig(val ip: String, val user: String, val pass: String)