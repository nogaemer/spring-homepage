package de.nogaemer.springhomepage.meals.filters

import de.nogaemer.springhomepage.meals.MealRepository
import de.nogaemer.springhomepage.user.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class FilterService(
    val userRepository: UserRepository,
) {

    fun getFilters() {

    }
}
