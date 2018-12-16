package com.droxt.officemapper.models

import com.droxt.officemapper.models.enums.Privileges

class User {
    var id: Int? = null
    var name: String? = null
    var surname: String? = null
    var mail: String? = null
    var privileges: Privileges? = null
    var office: Int? = null
    var officeName : String? = null
}