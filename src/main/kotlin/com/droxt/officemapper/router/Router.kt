package com.droxt.officemapper.router

import com.droxt.officemapper.controller.Controller
import com.droxt.officemapper.models.LoginRequest
import com.droxt.officemapper.models.Map
import com.droxt.officemapper.models.Office
import com.droxt.officemapper.models.RegisterRequest
import com.droxt.officemapper.models.User
import com.droxt.officemapper.models.enums.Privileges
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class Router {
    @GetMapping("/")
    fun homepage(@RequestHeader("Authorization") authToken :String) {}

    @PostMapping("/login", "Content-Type=application/json")
    fun login(@RequestBody credentials: LoginRequest) :ResponseEntity<String> = Controller.login(credentials)

    @GetMapping("/users")
    fun users(@RequestHeader("Authorization") authToken :String) :ResponseEntity<ArrayList<User?>>? {
        return Controller.getUsers(authToken)
    }

    @GetMapping ("/users/new", "/maps")
    fun getOffices(@RequestHeader("Authorization") authToken :String) :ResponseEntity<ArrayList<Office?>> {
        return Controller.getOffices(authToken)
    }

    @PostMapping ("/users/new")
    fun newUser(@RequestHeader("Authorization") authToken :String,
                @RequestBody credentials: RegisterRequest) :ResponseEntity<User> {
        return Controller.newUser(credentials, authToken)
    }

    @GetMapping("/users/{id}")
    fun showUser(@RequestHeader("Authorization") authToken : String,
                 @PathVariable("id") id :Int) :ResponseEntity<User>{
        return Controller.getUser(authToken, id)
    }

    @PutMapping("/users/{id}")
    fun updateUser(@RequestHeader("Authorization") authToken : String,
                   @PathVariable("id") id :Int,
                   @RequestBody credentials: RegisterRequest): ResponseEntity<User>{
        return Controller.updateUser(authToken, id, credentials)
    }

    @DeleteMapping("/users/{id}")
    fun deleteUser(@RequestHeader("Authorization") authToken : String,
                   @PathVariable("id") id :Int): ResponseEntity<Any>{
        return Controller.deleteUser(authToken, id)
    }

    @GetMapping("/maps/{name}")
    fun showFloors(@RequestHeader("Authorization") authToken : String,
                   @PathVariable("name") name :String) :ResponseEntity<ArrayList<Int>>?{
        return Controller.getFloors(authToken, name)
    }

    @GetMapping("/maps/{name}/{floor}")
    fun showFloors(@RequestHeader("Authorization") authToken : String,
                   @PathVariable("name") name :String,
                   @PathVariable("floor") floor :Int) :ResponseEntity<Map>?{
        return Controller.getMaps(authToken, name, floor)
    }
}