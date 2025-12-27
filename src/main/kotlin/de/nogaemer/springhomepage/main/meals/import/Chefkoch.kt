package de.nogaemer.springhomepage.main.meals.import

import de.nogaemer.springhomepage.exceptions.UnitNotFoundException
import de.nogaemer.springhomepage.main.images.Image
import de.nogaemer.springhomepage.main.meals.models.MealIngredient
import de.nogaemer.springhomepage.main.meals.models.Meal
import de.nogaemer.springhomepage.main.meals.tags.Tag
import de.nogaemer.springhomepage.main.meals.tags.TagService
import de.nogaemer.springhomepage.main.meals.units.IngredientUnit
import de.nogaemer.springhomepage.main.meals.units.UnitService
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import kotlin.time.Duration
import kotlin.time.DurationUnit

@Component
class Chefkoch(
    val tagService: TagService,
    val unitService: UnitService,
) {
    private var page: Document? = null
    private var jsonData: JSONObject? = null


    fun getMealFromUrl(url: String): Meal {
        fetchDataFromUrl(url)

        return Meal(
            name = getName(),
            ingredients = getIngredients(),
            instructions = getInstructions(),
            tags = getTags(),
            images = getImageUrls(),
            difficulty = getDifficulty(),
            time = getTime(),
            portions = getPotions(),
            calories = getCals(),
            url = url
        )
    }


    private fun fetchDataFromUrl(url: String) {
        val client = OkHttpClient().newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()

        val stringResponse = response.body?.string()

        //Parsing to JSON
        page = Jsoup.parse(stringResponse!!)
        response.close()

        val jsonHeaders = page!!.selectXpath("/html/head/script")
        jsonHeaders.forEach {

            if (it.data().contains("\"@type\":\"Recipe\"")) {
                jsonData = JSONObject(it.data().toString())
                return@forEach
            }
        }
    }

    private fun getName(): String {
        return page!!.selectXpath("/html/body/main/article[1]/div/h1").text()
    }

    private fun getIngredients(): List<MealIngredient> {
        val mealIngredients = mutableListOf<MealIngredient>()

        jsonData!!.getJSONArray("recipeIngredient").forEach { ingredient ->
            ingredient as String

            var name = ""
            var unitString = ""
            var amount = ""

            when {
                ingredient.trimStart() != ingredient -> {
                    name = ingredient.trimStart()
                }

                ingredient.count { it == ' ' } == 1 -> {
                    name = ingredient.split(" ", limit = 2)[1]
                    amount = ingredient.split(" ")[0]
                }

                ingredient.count { it == ' ' } >= 2 -> {
                    name = ingredient.split(" ", limit = 3)[2]
                    unitString = ingredient.split(" ")[1]
                    amount = ingredient.split(" ")[0]
                }
            }


            // Find the best matching unit
            var unit: IngredientUnit?
            val candidates = unitService.getUnits(10, 0, unitString)
            val query = unitString.trim()
            unit = candidates.find {
                it.abbreviation.equals(query, true) ||
                        it.abbreviationPlural.equals(query, true) ||
                        it.fullName.equals(query, true) ||
                        it.fullNamePlural.equals(query, true)
            }
            if (unit == null && query.isNotEmpty()) {
                unit = candidates.find {
                    it.abbreviation.startsWith(query, true) ||
                            it.fullName.startsWith(query, true)
                }
            }
            if (unit == null && query.isNotEmpty()) {
                unit = candidates.find {
                    it.abbreviation.contains(query, true) ||
                            it.fullName.contains(query, true)
                }
            }
            if (unit == null) {
                unit = candidates.firstOrNull()
            }

            if (unit == null) throw UnitNotFoundException("Could not find unit for ingredient '$ingredient' (parsed unit: '$unitString')")

            mealIngredients.add(
                MealIngredient(
                    name = name,
                    amount = amount,
                    unit = unit
                )
            )

        }

        return mealIngredients
    }

    private fun getInstructions(): List<String> {
        val instructions = mutableListOf<String>()

        jsonData!!.getJSONArray("recipeInstructions")
            .getJSONObject(0)
            .getJSONArray("itemListElement")
            .forEach {
                instructions.add(
                    (it as JSONObject).getString("text")
                )
            }

        return instructions
    }

    private fun getTags(): MutableList<Tag> {
        val tags = mutableListOf<Tag>()
        val keywordsString = jsonData!!.getString("keywords")
        keywordsString.split(",").forEach { tag ->
            tags.add(
                tagService.saveTag(
                    Tag(
                        name = tag.trim(),
                        type = "chefkoch",
                        description = "",
                        color = "#e06c75"
                    )
                )
            )
        }
        return tags
    }


    private fun getImageUrls(): List<Image> {
        val imageList = mutableListOf<Image>()

        page!!.select(".ds-mb-left > #recipe-image-carousel > .recipe-image-carousel-slide").forEach { image ->
            val imageUrl = image.select("a > amp-img").attr("srcset")

            if (imageUrl.isEmpty()) {
                return@forEach
            }

            val imageUrls = ArrayList<String>()

            imageUrl.split(",").forEach {
                if (!imageUrls.contains(it.trim())){
                    imageUrls.add(it.trim())
                }
            }

            imageList.add(
                Image(
                    thumbnail = imageUrls[0],
                    srcSetArray = imageUrls,
                    srcSetString = imageUrl
                )
            )
        }

        return imageList
    }

    @Deprecated("Use getImageUrls() instead")
    private fun getImageUrl(): List<String> {
        val imageUrls = mutableListOf<String>()

        page!!.select(".ds-mb-left > #recipe-image-carousel > .recipe-image-carousel-slide").forEach { image ->
            val imageUrl = image.select("a > amp-img").attr("src")

            if (imageUrl.isEmpty() || imageUrls.contains(imageUrl)) {
                return@forEach
            }

            imageUrls.add(imageUrl)
        }

        return imageUrls
    }

    private fun getDifficulty(): String {
        return page!!.select(".recipe-difficulty")
            .textNodes()[0]
            .toString()
            .trim()
    }

    private fun getTime(): Long {
        return Duration.parseIsoString(jsonData!!.getString("totalTime"))
            .toLong(DurationUnit.MINUTES)
    }

    private fun getPotions(): Int {
        return jsonData!!.getInt("recipeYield")
    }

    private fun getCals(): Int {
        return try {
            jsonData!!.getJSONObject("nutrition")
                .getString("calories")
                .replace(" kcal", "")
                .toInt()
        } catch (e: JSONException) {
            0
        }


    }

}