package de.nogaemer.springhomepage.meals.import

import de.nogaemer.springhomepage.meals.models.Ingredient
import de.nogaemer.springhomepage.meals.models.Meal
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.time.Duration
import kotlin.time.DurationUnit


internal class Chefkoch {
    private var page: Document? = null
    private var jsonData: JSONObject? = null


    fun getMealFromUrl(url: String): Meal {
        fetchDataFromUrl(url)

        return Meal(
            name = getName(),
            ingredients = getIngredients(),
            instructions = getInstructions(),
            tags = getTags(),
            imageSrc = getImageUrl(),
            imageSrcSet = getImageUrls(),
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
            if (it.data().contains("\"@type\": \"Recipe\"")) {
                jsonData = JSONObject(it.data().toString())
            }
        }
    }

    private fun getName(): String {
        return page!!.selectXpath("/html/body/main/article[1]/div/h1").text()
    }

    private fun getIngredients(): List<Ingredient> {
        val ingredients = mutableListOf<Ingredient>()

        jsonData!!.getJSONArray("recipeIngredient").forEach { ingredient ->
            ingredient as String

            var name = ""
            var unit = ""
            var amount =""

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
                    unit = ingredient.split(" ")[1]
                    amount = ingredient.split(" ")[0]
                }
            }

            ingredients.add(
                Ingredient(
                    name = name,
                    amount = amount,
                    unit = unit
                )
            )

        }

        return ingredients
    }

    private fun getInstructions(): List<String> {
        val instructions = mutableListOf<String>()

        jsonData!!.getString("recipeInstructions")
            .split("\n\n")
            .forEach {
                instructions.add(it)
            }

        return instructions
    }

    private fun getTags(): List<String> {
        val tags = mutableListOf<String>()

        jsonData!!.getJSONArray("keywords").forEach { tag ->
            tag as String
            tags.add(tag)
        }

        return tags
    }

    private fun getImageUrls(): List<String> {
        val imageUrls = mutableListOf<String>()

        page!!.select(".ds-mb-left > #recipe-image-carousel > .recipe-image-carousel-slide").forEach { image ->
            val imageUrl = image.select("a > amp-img").attr("srcset")

            if (imageUrl.isEmpty()) {
                return@forEach
            }

            if (!imageUrls.contains(imageUrl)) {
                imageUrls.add(imageUrl)
            }
        }

        return imageUrls
    }

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