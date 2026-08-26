package dev.pschmitt.syncwich.ui.foods

import dev.pschmitt.syncwich.data.db.entity.FoodEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class FoodSearchTest {

    private fun food(name: String, pluralName: String? = null) =
        FoodEntity(id = name, name = name, pluralName = pluralName, description = "")

    @Test
    fun `blank query returns every food unchanged`() {
        val foods = listOf(food("Flour"), food("Sugar"))

        assertEquals(foods, filterFoodsByQuery(foods, "  "))
    }

    @Test
    fun `query matches food name case insensitively`() {
        val foods = listOf(food("Flour"), food("Sugar"))

        assertEquals(listOf(foods[0]), filterFoodsByQuery(foods, "FLO"))
    }

    @Test
    fun `query matches plural name`() {
        val foods = listOf(food("Tomato", pluralName = "Tomatoes"), food("Onion"))

        assertEquals(listOf(foods[0]), filterFoodsByQuery(foods, "tomatoes"))
    }

    @Test
    fun `query matching nothing returns an empty list`() {
        assertEquals(emptyList<FoodEntity>(), filterFoodsByQuery(listOf(food("Flour")), "soup"))
    }
}
