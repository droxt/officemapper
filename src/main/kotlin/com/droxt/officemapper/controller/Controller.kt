package com.droxt.officemapper.controller

import com.droxt.officemapper.database.Database

import com.droxt.officemapper.models.LoginRequest
import com.droxt.officemapper.models.Map
import com.droxt.officemapper.models.Office
import com.droxt.officemapper.models.RegisterRequest
import com.droxt.officemapper.models.User
import com.droxt.officemapper.models.enums.Privileges
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpHeaders

object Controller {

    fun login(login: LoginRequest): ResponseEntity<String> {
        val responseHeaders = HttpHeaders()
        if (Database.findCredentials(login.mail, login.password)) {
            val id = Database.getId(login.mail)
            val username = Database.getUsername(id)
            val privileges = Database.getPrivileges(id)
            return if (privileges != null) {
                val token = TokenHandler.generateTokenJwt(id, username, login.mail, privileges)
                responseHeaders.set("Authorization", token)
                ResponseEntity.ok()
                        .headers(responseHeaders)
                        .body(null)
            } else {
                ResponseEntity.status(500).header("Message", "Core Error").body(null)
            }
        } else {
            return ResponseEntity.status(401).header("Message", "Mail or Password incorrect").body(null)
        }
    }

    fun getUser(authToken: String, id: Int): ResponseEntity<User> {
        if (TokenHandler.validateToken(authToken)) {
            val privilege = TokenHandler.getPrivileges(authToken)
            val tokenId = TokenHandler.getId(authToken)
            when (privilege) {
                Privileges.GOD -> {
                    val response: User? = Database.getUser(id)
                    return when {
                        response == null -> ResponseEntity.status(404).header("Message", "User not found").body(null)
                        Database.isInCompany(tokenId, id) -> ResponseEntity.ok().body(response)
                        else -> ResponseEntity.status(403).header("Message", "Access to requested user forbidden").body(null)
                    }
                }
                Privileges.LILGOD -> {
                    val response: User? = Database.getUser(id)
                    return when {
                        response == null -> ResponseEntity.status(404).header("Message", "User not found").body(null)
                        Database.isInOffice(tokenId, id) -> ResponseEntity.ok().body(response)
                        else -> ResponseEntity.status(403).header("Message", "Access to requested user forbidden").body(null)
                    }
                }
                else -> {
                    if (tokenId == id) {
                        val response: User? = Database.getUser(id)
                        return if (response != null)
                            ResponseEntity.ok().body(response)
                        else
                            ResponseEntity.status(404).header("Message", "User not found").body(null)
                    } else {
                        return ResponseEntity.status(403).header("Message", "Access to requested user forbidden").body(null)
                    }
                }
            }
        } else {
            return ResponseEntity.status(401).header("Message", "Invalid Session Token").body(null)
        }
    }

    fun getUsers(authToken: String): ResponseEntity<ArrayList<User?>>? {
        return if (TokenHandler.validateToken(authToken)) {
            val privilege = TokenHandler.getPrivileges(authToken)
            when (privilege) {
                Privileges.GOD -> {
                    val response: ArrayList<User?>? = Database.getUsersFromCompany(TokenHandler.getId(authToken))
                    return if (response != null)
                        ResponseEntity.ok().body(response)
                    else
                        ResponseEntity.status(404).header("Message", "Users not found").body(null)
                }
                Privileges.LILGOD -> {
                    val response: ArrayList<User?>? = Database.getUsersFromOffice(TokenHandler.getId(authToken))
                    return if (response != null)
                        ResponseEntity.ok().body(response)
                    else
                        ResponseEntity.status(404).header("Message", "Users not found").body(null)
                }
                else -> {
                    return ResponseEntity.status(403).header("Message", "Access to requested users forbidden").body(null)
                }
            }
        } else {
            ResponseEntity.status(401).header("Message", "Invalid Session Token").body(null)
        }
    }

    fun getOffices(authToken: String): ResponseEntity<ArrayList<Office?>> {
        if (TokenHandler.validateToken(authToken)) {
            val privilege = TokenHandler.getPrivileges(authToken)
            val updaterId = TokenHandler.getId(authToken)
            when (privilege) {
                Privileges.GOD -> {
                    val response: ArrayList<Office?>? = Database.getCompanyOffices(updaterId)
                    return if (response != null)
                        ResponseEntity.ok().body(response)
                    else
                        ResponseEntity.status(404).header("Message", "Offices not found").body(null)
                }
                Privileges.LILGOD -> {
                    val response: ArrayList<Office?>? = arrayListOf(Database.getOffice(updaterId))
                    return if (response != null)
                        ResponseEntity.ok().body(response)
                    else
                        ResponseEntity.status(404).header("Message", "Office not found").body(null)
                }
                else -> return ResponseEntity.status(403).header("Message", "Access to requested function forbidden").body(null)
            }
        } else {
            return ResponseEntity.status(401).header("Message", "Invalid Session Token").body(null)
        }
    }

    fun newUser (credentials :RegisterRequest, authToken: String) :ResponseEntity<User> {
        if (TokenHandler.validateToken(authToken)) {
            val privilege = TokenHandler.getPrivileges(authToken)
            val updaterId = TokenHandler.getId(authToken)
            if (credentials.name != null && credentials.surname != null && credentials.mail != null && credentials.password != null && credentials.privileges != null && credentials.office != null) {
                when (privilege) {
                    Privileges.GOD -> {
                        if (credentials.privileges != Privileges.GOD && !(credentials.office == Database.getOffice(updaterId)?.id && credentials.privileges == Privileges.LILGOD))
                            return if (Database.addUser(credentials, updaterId))
                                ResponseEntity.ok().body(Database.getUser(Database.getId(credentials.mail)))
                            else
                                ResponseEntity.status(400).header("Message", "Requested user could not be created").body(null)
                        else
                            return ResponseEntity.status(403).header("Message", "Requested privileges could not be assigned").body(null)
                    }
                    Privileges.LILGOD -> {
                        credentials.office = null
                        if (credentials.privileges != Privileges.GOD && credentials.privileges != Privileges.LILGOD)
                            return if (Database.addUser(credentials, updaterId))
                                ResponseEntity.ok().body(Database.getUser(Database.getId(credentials.mail)))
                            else
                                ResponseEntity.status(400).header("Message", "Requested user could not be created").body(null)
                        else
                            return ResponseEntity.status(403).header("Message", "Requested privileges could not be assigned").body(null)
                    }
                    else -> return ResponseEntity.status(403).header("Message", "Access to requested function forbidden").body(null)
                }
            } else
                return ResponseEntity.status(400).header("Message", "Some of the input fields are empty").body(null)
        } else {
            return ResponseEntity.status(401).header("Message", "Invalid Session Token").body(null)
        }
    }

    fun updateUser (authToken: String, id: Int, credentials: RegisterRequest) :ResponseEntity<User> {
        return if (TokenHandler.validateToken(authToken)) {
            val privilege = TokenHandler.getPrivileges(authToken)
            val updaterId = TokenHandler.getId(authToken)
            when (privilege) {
                Privileges.GOD -> {
                    val response: User? = Database.getUser(id)
                    return when {
                        (response == null) -> ResponseEntity.status(404).header("Message", "User not found").body(null)

                        (!(Database.isInCompany(updaterId, id))) -> ResponseEntity.status(403).header("Message", "Access to requested user forbidden").body(null)

                        (id == updaterId) ->  ResponseEntity.status(403).header("Message", "Update of the requested user forbidden").body(null)

                        (credentials.privileges != null && (credentials.privileges == Privileges.GOD ||
                                (credentials.office == null || credentials.office == Database.getOffice(updaterId)?.id) &&
                                credentials.privileges == Privileges.LILGOD)) -> ResponseEntity.status(403).header("Message", "Requested privileges could not be assigned").body(null)

                        (!Database.updateUser(id, credentials)) -> ResponseEntity.status(400).header("Message", "Requested user could not be updated").body(null)

                        else -> ResponseEntity.ok().body(Database.getUser(id))
                    }
                }
                Privileges.LILGOD -> {
                    if (credentials.office != null) credentials.office = null
                    val response: User? = Database.getUser(id)
                    return when {
                        (response == null) -> ResponseEntity.status(404).header("Message", "User not found").body(null)

                        (!(Database.isInOffice(updaterId, id))) -> ResponseEntity.status(403).header("Message", "Access to requested user forbidden").body(null)

                        (id == updaterId) ->  ResponseEntity.status(403).header("Message", "Update of the requested user forbidden").body(null)

                        (credentials.privileges != null &&
                                (credentials.privileges == Privileges.GOD || credentials.privileges == Privileges.LILGOD)) -> ResponseEntity.status(403).header("Message", "Requested privileges could not be assigned").body(null)

                        (!Database.updateUser(id, credentials)) -> ResponseEntity.status(400).header("Message", "Requested user could not be updated").body(null)

                        else -> ResponseEntity.ok().body(Database.getUser(id))
                    }
                } else -> return ResponseEntity.status(403).header("Message", "Access to requested function forbidden").body(null)
            }
        } else {
            ResponseEntity.status(401).header("Message", "Invalid Session Token").body(null)
        }
    }

    fun deleteUser (authToken: String, id: Int) :ResponseEntity<Any> {
        return if (TokenHandler.validateToken(authToken)) {
            val privilege = TokenHandler.getPrivileges(authToken)
            val updaterId = TokenHandler.getId(authToken)
            when (privilege) {
                Privileges.GOD -> {
                    val response: User? = Database.getUser(id)
                    return when {
                        (response == null) -> ResponseEntity.status(404).header("Message", "User not found").body(null)

                        (!(Database.isInCompany(updaterId, id))) -> ResponseEntity.status(403).header("Message", "Access to requested user forbidden").body(null)

                        (id == updaterId) -> ResponseEntity.status(403).header("Message", "Requested user could not be erased").body(null)

                        (!Database.deleteUser(id)) -> ResponseEntity.status(400).header("Message", "Requested user could not be updated").body(null)

                        else -> ResponseEntity.ok().body(null)
                    }
                }
                Privileges.LILGOD -> {
                    val response: User? = Database.getUser(id)
                    return when {
                        (response == null) -> ResponseEntity.status(404).header("Message", "User not found").body(null)

                        (!(Database.isInOffice(updaterId, id))) -> ResponseEntity.status(403).header("Message", "Access to requested user forbidden").body(null)

                        (id == updaterId) -> ResponseEntity.status(403).header("Message", "Requested user could not be erased").body(null)

                        (!Database.deleteUser(id)) -> ResponseEntity.status(400).header("Message", "Requested user could not be updated").body(null)

                        else -> ResponseEntity.ok().body(null)
                    }
                }
                else -> return ResponseEntity.status(403).header("Message", "Access to requested function forbidden").body(null)
            }
        } else {
            ResponseEntity.status(401).header("Message", "Invalid Session Token").body(null)
        }
    }

    fun getFloors(authToken: String, officeName: String): ResponseEntity<ArrayList<Int>>? {
        if (TokenHandler.validateToken(authToken)) {
            val privilege = TokenHandler.getPrivileges(authToken)
            val tokenId = TokenHandler.getId(authToken)
            val officeId = Database.getOfficeFromName(tokenId, officeName)?.id
            if (officeId != null) {
                when (privilege) {
                    Privileges.GOD -> {
                        if (Database.getCompanyOffices(tokenId)?.any { office -> office?.id == officeId} as Boolean) {
                            val response: ArrayList<Int>? = Database.getFloors(officeId)
                            return if (response != null)
                                ResponseEntity.ok().body(response)
                            else
                                ResponseEntity.status(404).header("Message", "Floors not found").body(null)
                        } else {
                            return ResponseEntity.status(403).header("Message", "Access to requested office forbidden").body(null)
                        }
                    }
                    Privileges.LILGOD -> {
                        if (Database.getOffice(tokenId)?.id == officeId) {
                            val response: ArrayList<Int>? = Database.getFloors(officeId)
                            return if (response != null)
                                ResponseEntity.ok().body(response)
                            else
                                ResponseEntity.status(404).header("Message", "Floors not found").body(null)
                        } else {
                            return ResponseEntity.status(403).header("Message", "Access to requested office forbidden").body(null)
                        }
                    }
                    Privileges.USHER -> {
                        if (Database.getOffice(tokenId)?.id == officeId) {
                            val response: ArrayList<Int>? = Database.getFloors(officeId)
                            return if (response != null)
                                ResponseEntity.ok().body(response)
                            else
                                ResponseEntity.status(404).header("Message", "Floors not found").body(null)
                        } else {
                            return ResponseEntity.status(403).header("Message", "Access to requested office forbidden").body(null)
                        }
                    }
                    Privileges.MAPPER -> {
                        if (Database.getOffice(tokenId)?.id == officeId) {
                            val response: ArrayList<Int>? = Database.getFloors(officeId)
                            return if (response != null)
                                ResponseEntity.ok().body(response)
                            else
                                ResponseEntity.status(404).header("Message", "Floors not found").body(null)
                        } else {
                            return ResponseEntity.status(403).header("Message", "Access to requested office forbidden").body(null)
                        }
                    }
                    else -> {
                            return ResponseEntity.status(403).header("Message", "Access to requested office forbidden").body(null)
                    }
                }
            } else {
                return ResponseEntity.status(400).header("Message", "Access to requested office forbidden").body(null)
            }
        } else {
            return ResponseEntity.status(401).header("Message", "Invalid Session Token").body(null)
        }
    }

    fun getMaps(authToken: String, officeName: String, floor: Int): ResponseEntity<Map>? {
        if (TokenHandler.validateToken(authToken)) {
            val privilege = TokenHandler.getPrivileges(authToken)
            val tokenId = TokenHandler.getId(authToken)
            val officeId = Database.getOfficeFromName(tokenId, officeName)?.id
            if (officeId != null) {
                when (privilege) {
                    Privileges.GOD -> {
                        if (Database.getCompanyOffices(tokenId)?.any { office -> office?.id == officeId} as Boolean) {
                            val response: Map? = Database.getMaps(officeId, floor)
                            return if (response != null)
                                ResponseEntity.ok().body(response)
                            else
                                ResponseEntity.status(404).header("Message", "Maps not found").body(null)
                        } else {
                            return ResponseEntity.status(403).header("Message", "Access to requested maps forbidden").body(null)
                        }
                    }
                    Privileges.LILGOD -> {
                        if (Database.getOffice(tokenId)?.id == officeId) {
                            val response: Map? = Database.getMaps(officeId, floor)
                            return if (response != null)
                                ResponseEntity.ok().body(response)
                            else
                                ResponseEntity.status(404).header("Message", "Maps not found").body(null)
                        } else {
                            return ResponseEntity.status(403).header("Message", "Access to requested maps forbidden").body(null)
                        }
                    }
                    Privileges.MAPPER -> {
                        if (Database.getOffice(tokenId)?.id == officeId) {
                            val response: Map? = Database.getMaps(officeId, floor)
                            return if (response != null)
                                ResponseEntity.ok().body(response)
                            else
                                ResponseEntity.status(404).header("Message", "Maps not found").body(null)
                        } else {
                            return ResponseEntity.status(403).header("Message", "Access to requested maps forbidden").body(null)
                        }
                    }
                    else -> {
                        return ResponseEntity.status(403).header("Message", "Access to requested maps forbidden").body(null)
                    }
                }
            } else {
                return ResponseEntity.status(400).header("Message", "Access to requested maps forbidden").body(null)
            }
        } else {
            return ResponseEntity.status(401).header("Message", "Invalid Session Token").body(null)
        }
    }
}