package com.droxt.officemapper.utils

import com.droxt.officemapper.models.enums.Privileges
import java.security.MessageDigest
import kotlin.random.Random

object Utils {
    fun generateSalt() :String {
        val random = Random
        val sb = StringBuilder()
        while (sb.length < 10) {
            sb.append(Integer.toHexString(random.nextInt()))
        }
        return sb.toString().toUpperCase()
    }

    fun encodeToMd5(strToEncode :String) :String {
        return MessageDigest
                .getInstance("MD5")
                .digest(strToEncode.toByteArray())
                .map { String.format("%02X", it) }
                .joinToString(separator = "")
    }

    fun getPrivileges(privilegesString :String?) :Privileges? {
        return when (privilegesString) {
            ("GOD") -> Privileges.GOD
            ("LILGOD") -> Privileges.LILGOD
            ("MAPPER") -> Privileges.MAPPER
            ("USHER") -> Privileges.USHER
                else -> null
        }
    }
}