package de.nogaemer.springhomepage.main.meals.dto

import de.nogaemer.springhomepage.main.images.Image
import de.nogaemer.springhomepage.main.tags.Tag
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.jetbrains.annotations.NotNull
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Collections.emptyList

/**
 * Data Transfer Object for meal creation and update operations.
 *
 * This DTO represents the payload for POST and PUT requests to the meal endpoints.
 * It excludes system-managed fields (id, ratings, notes, calculated rating) that
 * should not be modified directly by clients.
 *
 * ## Mapping to Entity
 * - **MealDto → Meal**: Used in [MealService.create] and [MealService.update]
 * - Ingredients are resolved from [MealIngredientDto] to [MealIngredient] entities
 *   with full Ingredient and IngredientUnit references
 * - Tags are accepted as full Tag objects and must already exist in the system
 *
 * ## Validation
 * - [name] and [difficulty] are required (annotated with @NotNull)
 * - Ingredient validation occurs in [MealService.resolveIngredients]:
 *   - Unit IDs must be valid ObjectIds
 *   - Units must exist in the database
 * - Tag validation is implicit (tags must be pre-existing entities)
 *
 * ## Usage in Controllers
 * - **POST /api/v1/meals**: Creates new meal from MealDto
 * - **PUT /api/v1/meals/{id}**: Updates existing meal with MealDto
 *
 * @property name The display name of the meal
 * @property ingredients List of ingredient DTOs with amounts and unit references
 * @property instructions Step-by-step cooking instructions
 * @property images List of image metadata (URLs for display and deletion)
 * @property difficulty Difficulty level (e.g., "easy", "medium", "hard")
 * @property time Preparation and cooking time in minutes
 * @property portions Number of servings
 * @property calories Estimated calories per serving
 * @property tags Categorization tags (must be existing Tag entities)
 *
 * @see MealIngredientDto
 * @see de.nogaemer.springhomepage.main.meals.models.Meal
 * @see MealService.create
 * @see MealService.update
 */
@Document(collection = "meals")
@Data
@AllArgsConstructor
@NoArgsConstructor
data class MealDto(
    @NotNull
    val name: String,

    val ingredients: List<MealIngredientDto>,

    val instructions: List<String>,

    val images: List<Image>,

    @NotNull
    val difficulty: String,

    val time: Long,

    val portions: Int,

    val calories: Int,

    @NotNull
    var tags: MutableList<Tag> = emptyList()
)

