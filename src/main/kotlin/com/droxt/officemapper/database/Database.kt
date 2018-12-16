package com.droxt.officemapper.database

import com.droxt.officemapper.models.Map
import com.droxt.officemapper.models.Office
import com.droxt.officemapper.models.RegisterRequest
import com.droxt.officemapper.models.User
import com.droxt.officemapper.utils.Utils
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

object Database {

    private val connection :Connection = DriverManager.getConnection("jdbc:postgresql://ec2-54-197-249-140.compute-1.amazonaws.com:5432/d6cf6lsv30urto?user=myctxtipzlneej&password=a5532aa45c2597a2de6f25c05f0894177c784dffde2d60edf485f974aee8e56a&sslmode=require")

    fun findCredentials(mail :String?, pass :String?) :Boolean {
        try {
            val stmt = connection.createStatement()
            var rs: ResultSet = stmt.executeQuery("SELECT \"ID\" FROM \"USERS\" WHERE \"MAIL\" = '$mail'")
            rs.next()
            val id= rs.getInt("ID")
            if (id != 0) {
                rs = stmt.executeQuery("SELECT \"PASS\" FROM \"USERS\" WHERE \"ID\" = '$id'")
                rs.next()
                val retrievedpass= rs.getString("PASS")
                rs = stmt.executeQuery("SELECT \"SALT\" FROM \"USERS\" WHERE \"ID\" = '$id'")
                rs.next()
                return Utils.encodeToMd5(pass + rs.getString("SALT")) == retrievedpass
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun getId(mail: String?) :Int? {
        return try {
            val stmt = connection.createStatement()
            val rs: ResultSet = stmt.executeQuery("SELECT \"ID\" FROM \"USERS\" WHERE \"MAIL\" = '$mail'")
            rs.next()
            rs.getInt("ID")
        } catch (e: Exception) {
            null
        }
    }

    fun getUsername(id: Int?) :String? {
        return try {
            val stmt = connection.createStatement()
            val rs: ResultSet = stmt.executeQuery("SELECT \"NAME\" FROM \"USERS\" WHERE \"ID\" = '$id'")
            rs.next()
            rs.getString("NAME")
        } catch (e: Exception) {
            null
        }
    }

    fun getPrivileges(id: Int?) :String? {
        return try {
            val stmt = connection.createStatement()
            val rs: ResultSet = stmt.executeQuery("SELECT \"PRIVILEGES\" FROM \"USERS\" WHERE \"ID\" = '$id'")
            rs.next()
            rs.getString("PRIVILEGES")
        } catch (e: Exception) {
            null
        }
    }

    fun getOffice(id: Int?) :Office? {
        return try {
            val row = Office()
            val stmt = connection.createStatement()
            val rs: ResultSet = stmt.executeQuery(
                    "SELECT \"OFFICES\".\"ID\", \"OFFICES\".\"NAME\", \"DIR\" FROM \"USERS\", \"OFFICES\" WHERE " +
                            "(\"OFFICES\".\"ID\" = \"OFFICES_FK\") AND \"USERS\".\"ID\" = '$id'")
            rs.next()
            row.id = rs.getInt("ID")
            row.name = rs.getString("NAME")
            row.dir = rs.getString("DIR")
            row
        } catch (e: Exception) {
            null
        }
    }

    fun getOfficeFromName(userId: Int?, name: String) :Office? {
        return try {
            val row = Office()
            val stmt = connection.createStatement()
            val rs: ResultSet = stmt.executeQuery(
                    "SELECT \"OFFICES\".\"ID\", \"OFFICES\".\"NAME\", \"DIR\" FROM \"USERS\", \"OFFICES\" WHERE " +
                            "(\"OFFICES\".\"ID\" = \"OFFICES_FK\") AND \"OFFICES\".\"NAME\" = '$name' AND " +
                            "(\"USERS\".\"ID\" = '$userId' OR \"COMPANIES_FK\" = (SELECT \"COMPANIES_FK\" FROM \"USERS\", \"OFFICES\" WHERE " +
                            "(\"OFFICES\".\"ID\" = \"OFFICES_FK\") AND \"USERS\".\"ID\" = '$userId'))")
            rs.next()
            row.id = rs.getInt("ID")
            row.name = rs.getString("NAME")
            row.dir = rs.getString("DIR")
            row
        } catch (e: Exception) {
            null
        }
    }

    fun getFloors(officesId: Int?): ArrayList<Int>? {
        val rows: ArrayList<Int> = ArrayList()
        return try {
            val stmt = connection.createStatement()
            val rs: ResultSet = stmt.executeQuery(
                    "SELECT \"FLOOR\" FROM \"FLOORS\" WHERE \"OFFICES_FK\" = '$officesId'")
            while (rs.next()) {
                rows.add(rs.getInt("FLOOR"))
            }
            rows
        } catch (e: Exception) {
            null
        }
    }

    fun getMaps(officesId: Int, floor: Int): Map? {
        val row = Map()
        return try {
            val stmt = connection.createStatement()
            val rs: ResultSet = stmt.executeQuery(
                    "SELECT \"ID\", \"NAME\", \"FLOOR\", \"BASEMAP\", \"PLACESMAP\", \"EMERGENCYMAP\"  FROM \"OFFICES\", \"FLOORS\" WHERE " +
                            "\"ID\" = '$officesId' AND \"FLOOR\" = '$floor'")
            rs.next()
                row.office = rs.getInt("ID")
                row.officeName = rs.getString("NAME")
                row.floor = rs.getInt("FLOOR")
                row.baseMap = rs.getString("BASEMAP")
                row.placesMap = rs.getString("PLACESMAP")
                row.emergencyMap = rs.getString("EMERGENCYMAP")
                row
        } catch (e: Exception) {
            null
        }
    }

    fun getUser (id: Int?) :User? {
        return try {
            val user = User()
            val stmt = connection.createStatement()
            val rs = stmt.executeQuery("SELECT \"USERS\".\"ID\",\"USERS\".\"NAME\",\"SURNAME\",\"MAIL\",\"PRIVILEGES\" FROM \"USERS\" WHERE \"ID\" = '$id'")
            rs.next()
            user.id = rs.getInt("ID")
            user.name = rs.getString("NAME")
            user.surname = rs.getString("SURNAME")
            user.mail = rs.getString("MAIL")
            user.privileges = Utils.getPrivileges(rs.getString("PRIVILEGES"))
            val office = getOffice(user.id)
            user.office = office?.id
            user.officeName = office?.name
            user
        } catch (e: Exception) {
            null
        }
    }

    fun getUsersFromCompany(id: Int?) :ArrayList<User?>? {
        return try {
            val rows = ArrayList<User?>()
            val stmt = connection.createStatement()
            val rs = stmt.executeQuery(
                    "SELECT \"USERS\".\"ID\",\"USERS\".\"NAME\",\"SURNAME\",\"MAIL\",\"PRIVILEGES\" FROM \"USERS\", \"OFFICES\", \"COMPANIES\"" +
                    "WHERE (\"COMPANIES\".\"ID\" = \"COMPANIES_FK\" AND \"OFFICES\".\"ID\" = \"OFFICES_FK\") AND \"COMPANIES\".\"ID\" =" +
                    "(SELECT \"COMPANIES_FK\" FROM \"OFFICES\", \"USERS\" WHERE (\"OFFICES\".\"ID\" = \"OFFICES_FK\") AND \"USERS\".\"ID\" = '$id');")
            while (rs.next()) {
                val row = User()
                row.id = rs.getInt("ID")
                row.name = rs.getString("NAME")
                row.surname = rs.getString("SURNAME")
                row.mail = rs.getString("MAIL")
                row.privileges = Utils.getPrivileges(rs.getString("PRIVILEGES"))
                val office = getOffice(row.id)
                row.office = office?.id
                row.officeName = office?.name
                rows.add(row)
            }
            rows
        } catch (e: Exception) {
            null
        }
    }

    fun getUsersFromOffice(id: Int?) :ArrayList<User?>? {
        return try {
            val rows = ArrayList<User?>()
            val stmt = connection.createStatement()
            val rs = stmt.executeQuery(
                    "SELECT \"USERS\".\"ID\",\"USERS\".\"NAME\",\"SURNAME\",\"MAIL\",\"PRIVILEGES\" FROM \"USERS\", \"OFFICES\"" +
                            "WHERE (\"OFFICES\".\"ID\" = \"OFFICES_FK\") AND \"OFFICES\".\"ID\" =" +
                            "(SELECT \"OFFICES_FK\" FROM \"USERS\" WHERE \"USERS\".\"ID\" = '$id');")
            while (rs.next()) {
                val row = User()
                row.id = rs.getInt("ID")
                row.name = rs.getString("NAME")
                row.surname = rs.getString("SURNAME")
                row.mail = rs.getString("MAIL")
                row.privileges = Utils.getPrivileges(rs.getString("PRIVILEGES"))
                val office = getOffice(row.id)
                row.office = office?.id
                row.officeName = office?.name
                rows.add(row)
            }
            rows
        } catch (e: Exception) {
            null
        }
    }

    fun getCompanyOffices(id: Int?) :ArrayList<Office?>? {
        return try {
            val rows = ArrayList<Office?>()
            val stmt = connection.createStatement()
            val rs = stmt.executeQuery(
                    "SELECT \"OFFICES\".\"ID\",\"OFFICES\".\"NAME\",\"DIR\" FROM \"OFFICES\" WHERE" +
                            "\"COMPANIES_FK\" = (SELECT \"COMPANIES_FK\" FROM \"OFFICES\", \"USERS\" WHERE " +
                            "(\"OFFICES\".\"ID\" = \"OFFICES_FK\") AND \"USERS\".\"ID\" = '$id');")
            while (rs.next()) {
                val row = Office()
                row.id = rs.getInt("ID")
                row.name = rs.getString("NAME")
                row.dir = rs.getString("DIR")
                rows.add(row)
            }
            rows
        } catch (e: Exception) {
            null
        }
    }

    fun addUser (credentials :RegisterRequest, managerId: Int?) :Boolean {
        credentials.office = if(credentials.office == null) getOffice(managerId)?.id else credentials.office
        return try {
            val salt = Utils.generateSalt()
            val encodedPass = Utils.encodeToMd5(credentials.password + salt)
            val stmt = connection.prepareStatement(
                    "INSERT INTO \"USERS\" (\"NAME\",\"SURNAME\",\"MAIL\",\"PASS\",\"SALT\",\"PRIVILEGES\",\"OFFICES_FK\") VALUES" +
                    "('${credentials.name}', '${credentials.surname}', '${credentials.mail}', '$encodedPass', '$salt'," +
                    "'${credentials.privileges}', '${credentials.office}');")
            stmt.execute()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun updateUser ( id: Int?, credentials :RegisterRequest) :Boolean {
        return try  {
            var lineSet = false
            var encodedPass: String? = null
            var salt: String? = null
            if (credentials.password != null) {
                salt = Utils.generateSalt()
                encodedPass = Utils.encodeToMd5(credentials.password + salt)
            }
            val stmt = connection.prepareStatement(
                    "UPDATE \"USERS\" SET " +
                            (if (credentials.name != null) {lineSet = true; "\"NAME\" = '${credentials.name}'"} else{""}) +
                            (if (credentials.surname != null) (if (lineSet) { ", " }else{lineSet = true; ""}) + "\"SURNAME\" = '${credentials.surname}'" else "") +
                            (if (credentials.mail != null) (if (lineSet) { ", " }else{lineSet = true; ""}) + "\"MAIL\" = '${credentials.mail}'" else "") +
                            (if (credentials.password != null) (if (lineSet) { ", " }else{lineSet = true; ""}) + "\"PASS\" = '$encodedPass', \"SALT\" = '$salt'" else "") +
                            (if (credentials.privileges != null) (if (lineSet) { ", " }else{lineSet = true; ""}) + "\"PRIVILEGES\" = '${credentials.privileges}'" else "") +
                            (if (credentials.office != null) (if (lineSet) ", " else "") + "\"OFFICES_FK\" = '${credentials.office}'" else "") +
                            " WHERE \"ID\" = '$id';")
            stmt.execute()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteUser ( id: Int?) :Boolean {
        return try  {
            val stmt = connection.prepareStatement("DELETE FROM \"USERS\" WHERE \"ID\" = '$id';")
            stmt.execute()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isInOffice (idRequester: Int?, id :Int) :Boolean {
        val stmt = connection.createStatement()
        val rs = stmt.executeQuery(
                "SELECT \"USERS\".\"ID\" FROM \"USERS\", \"OFFICES\" WHERE (\"OFFICES\".\"ID\" = \"OFFICES_FK\") AND " +
                        "\"OFFICES\".\"ID\" = (SELECT \"OFFICES\".\"ID\" FROM \"OFFICES\", \"USERS\" WHERE" +
                        "(\"OFFICES\".\"ID\" = \"OFFICES_FK\") AND \"USERS\".\"ID\" = $idRequester);")
        while (rs.next()) {
            if (rs.getInt("ID") == id) return true
        }
        return false
    }

    fun isInCompany (idRequester: Int?, id :Int) :Boolean {
        val stmt = connection.createStatement()
        val rs = stmt.executeQuery(
                "SELECT \"USERS\".\"ID\" FROM \"USERS\", \"OFFICES\", \"COMPANIES\" WHERE" +
                        "(\"COMPANIES\".\"ID\" = \"COMPANIES_FK\" AND \"OFFICES\".\"ID\" = \"OFFICES_FK\")" +
                        "AND \"COMPANIES\".\"ID\" = (SELECT \"COMPANIES_FK\" FROM \"OFFICES\", \"USERS\" WHERE" +
                        "(\"OFFICES\".\"ID\" = \"OFFICES_FK\") AND \"USERS\".\"ID\" = $idRequester);")
        while (rs.next()) {
            if (rs.getInt("ID") == id) return true
        }
        return false
    }
}