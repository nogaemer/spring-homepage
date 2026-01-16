package de.nogaemer.springhomepage.main.filters

import de.nogaemer.springhomepage.user.UserResponse

/**
 * Response containing all available filter options for the meal browsing UI.
 *
 * Provides metadata needed to populate filter controls including user lists
 * for creator filtering and sort parameter options.
 *
 * ## Structure
 * - **users**: All registered users for "created by" filtering
 * - **sortParameters**: Available sorting options with display names
 *
 * ## Usage
 * Typically fetched once when loading the meal browsing interface to
 * initialize all filter dropdowns and controls with available options.
 *
 * @property users List of all users in the system (id and name only)
 * @property sortParameters Available sort options with metadata
 *
 * @see FilterController.getFilters
 * @see SortParameter
 */
data class FilterResponse (
    val users: List<UserResponse>,
    val sortParameters: List<SortParameter>,
)

/**
 * Represents a single sort option for meal ordering.
 *
 * Contains both internal identifier and user-friendly display name,
 * along with selection state for UI initialization.
 *
 * ## Fields
 * - **id**: Internal enum name (e.g., "RELEVANCE", "NAME")
 * - **name**: User-friendly label (e.g., "Most Relevant", "Name")
 * - **selected**: True if this is the default/currently selected option
 *
 * ## Usage
 * Used to populate sort dropdown menus in the frontend with
 * appropriate default selection.
 *
 * @property id Internal identifier (typically enum name)
 * @property name Display name for UI
 * @property selected Whether this option is currently selected
 *
 * @see de.nogaemer.springhomepage.main.meals.dto.UnifiedMealSearchRequest.SortBy
 */
data class SortParameter(
    val id: String,
    val name: String,
    val selected: Boolean
)