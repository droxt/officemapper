package com.droxt.officemapper.models

import com.droxt.officemapper.models.enums.Privileges

class RegisterRequest {
    var name: String? = null
    var surname: String? = null
    var mail: String? = null
    var password: String? = null
    var privileges: Privileges? = null
    var office: Int? = null
}