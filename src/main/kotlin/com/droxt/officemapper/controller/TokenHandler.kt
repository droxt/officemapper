package com.droxt.officemapper.controller

import com.droxt.officemapper.models.enums.Privileges
import com.droxt.officemapper.utils.Utils
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import javax.crypto.SecretKey

object TokenHandler {
    private var secret: SecretKey? = null

    fun generateTokenJwt (id: Int?, username: String?, mail: String?, privileges: String?) : String {
        secret = Keys.secretKeyFor(SignatureAlgorithm.HS256)
        return Jwts.builder()
                .setIssuer("offmap")
                .setSubject("auth")
                .claim("id", id)
                .claim("username", username)
                .claim("mail", mail)
                .claim("privileges", privileges)
                .signWith(secret)
                .compact()
    }

    fun validateToken (token: String?) : Boolean {
        return try {
            Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token!!.substringAfter(" "))
            true
        } catch (ex: JwtException) {
            false
        }
    }

    fun getId (token: String?): Int? {
        return try {
            val claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token!!.substringAfter(" "))
            claims.body["id"] as Int?

        } catch (ex: JwtException) {
            null
        }
    }

    fun getPrivileges (token: String?): Privileges? {
        return try {
            val claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token!!.substringAfter(" "))
            Utils.getPrivileges(claims.body["privileges"] as String?)

        } catch (ex: JwtException) {
            null
        }
    }
}