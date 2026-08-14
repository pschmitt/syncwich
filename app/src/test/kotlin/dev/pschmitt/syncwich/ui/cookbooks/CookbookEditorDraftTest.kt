package dev.pschmitt.syncwich.ui.cookbooks

import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CookbookEditorDraftTest {

    @Test
    fun `blank name is rejected before a save request can be built`() {
        val draft = CookbookEditorDraft(description = "A saved collection")

        assertEquals("Enter a cookbook name", draft.validationError())
    }

    @Test
    fun `valid draft trims editable text and preserves mutation fields`() {
        val draft =
            CookbookEditorDraft(
                name = "  Weeknight meals  ",
                description = "  Fast dinners  ",
                queryFilterString = "  recipe_category.id IN [\"quick\"]  ",
                position = 4,
                public = true,
                existingSlug = "weeknight-meals",
            )

        assertNull(draft.validationError())
        assertEquals(
            dev.pschmitt.syncwich.data.api.dto.CreateCookbookDto(
                name = "Weeknight meals",
                description = "Fast dinners",
                position = 4,
                public = true,
                queryFilterString = "recipe_category.id IN [\"quick\"]",
                slug = "weeknight-meals",
            ),
            draft.toRequest(),
        )
    }

    @Test
    fun `an edit draft loads all cached cookbook fields`() {
        val draft =
            CookbookEditorDraft.from(
                CookbookEntity(
                    id = "cookbook-1",
                    name = "Saved meals",
                    slug = "saved-meals",
                    description = "Keep these around",
                    position = 2,
                    public = true,
                    queryFilterString = "tags.id IN [\"keep\"]",
                )
            )

        assertEquals("Saved meals", draft.name)
        assertEquals("Saved meals", draft.toRequest().name)
        assertEquals("tags.id IN [\"keep\"]", draft.toRequest().queryFilterString)
        assertEquals("saved-meals", draft.toRequest().slug)
        assertEquals(true, draft.toRequest().public)
    }
}
