package de.nogaemer.springhomepage.updatedb

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service

@Service
class LinkInMeal_11_16_2024(private val mongoTemplate: MongoTemplate) {

    fun updateAll() {
//        val criteria = Criteria().andOperator(
//            Criteria.where("imageSrc").exists(true),
//            Criteria.where("imageSrc").type(JsonSchemaObject.Type.stringType())
//        )
//        val query = Query(criteria)
//
//
//        mongoTemplate.find(query, Meal::class.java).forEach { meal ->
//            val oldLink = removeClones(meal.imageSrc as ArrayList<String>)
//            val oldLinkSet = removeClones(meal.imageSrcSet as ArrayList<String>)
//
//            val newLinkSet = ArrayList<ImgLink>()
//
//            combineLinks(oldLink, oldLinkSet).forEach {
//                val newLink = ImgLink(
//                    it[0],
//                    it,
//                    null)
//
//                newLinkSet.add(newLink)
//            }
//
//            val update = Update()
//                .set("images", newLinkSet)
//                .unset("imageSrcSet")
//                .unset("imageSrc")
//
//            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").`is`(meal.id)), update, Meal::class.java)
//        }
    }

    private fun combineLinks(imageSrc: ArrayList<String>, imageSrcSet: ArrayList<String>): ArrayList<ArrayList<String>> {
        val result = ArrayList<ArrayList<String>>()

        imageSrc.forEach {
            result.add(ArrayList(listOf(it)))
        }

        for ((index, s) in imageSrcSet.withIndex()) {
            val it = imageSrcSet[index]

            it.split(",").forEach { src ->
                if (!result[index].contains(src)) {
                    result[index].add(src)
                }
            }
        }

        return result;
    }

    private fun removeClones(imageSrcSet: ArrayList<String>): ArrayList<String> {
        val set = HashSet<String>()
        val result = ArrayList<String>()

        val trimmedImageSrcSet = imageSrcSet.map { it.replace("\\s".toRegex(), "") }.toSet()
        trimmedImageSrcSet.forEach { s -> set.add(s) }

        result.addAll(set)
        return result
    }


}
