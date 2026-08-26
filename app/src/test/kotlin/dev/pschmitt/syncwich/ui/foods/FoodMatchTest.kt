package dev.pschmitt.syncwich.ui.foods

import dev.pschmitt.syncwich.data.db.entity.FoodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodMatchTest {

    private fun food(id: String, name: String, pluralName: String? = null) =
        FoodEntity(id = id, name = name, pluralName = pluralName, description = "")

    @Test
    fun `matches a food name embedded in a full ingredient line`() {
        val foods = listOf(food("f1", "Flour"), food("f2", "Sugar"))

        assertEquals(foods[0], findFoodMatch(foods, "2 cups all-purpose flour"))
    }

    @Test
    fun `matches by plural name`() {
        val foods = listOf(food("f1", "Tomato", pluralName = "Tomatoes"))

        assertEquals(foods[0], findFoodMatch(foods, "3 tomatoes, diced"))
    }

    @Test
    fun `does not match a partial word`() {
        val foods = listOf(food("f1", "Salt"))

        assertNull(findFoodMatch(foods, "1 cup unsalted butter"))
    }

    @Test
    fun `prefers the most specific of two overlapping matches`() {
        val foods = listOf(food("f1", "Pepper"), food("f2", "Bell pepper"))

        assertEquals(foods[1], findFoodMatch(foods, "1 red bell pepper, diced"))
    }

    @Test
    fun `no match returns null`() {
        assertNull(findFoodMatch(listOf(food("f1", "Flour")), "2 eggs"))
    }
}
