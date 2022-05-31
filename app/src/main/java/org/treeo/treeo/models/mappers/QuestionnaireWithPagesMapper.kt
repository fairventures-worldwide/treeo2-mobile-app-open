package org.treeo.treeo.models.mappers

import javax.inject.Inject

class QuestionnaireWithPagesMapper @Inject constructor() {
//    fun getPageList(obj: QuestionnaireWithPages): List<Page> {
//        val pageList = mutableListOf<Page>()
//        obj.pages.forEach {
//            pageList.add(
//                Page(
//                    it.pageId!!,
//                    it.questionnaireId!!,
//                    it.pageType!!,
//                    it.questionCode!!,
//                    getPageHeader(it.header!!),
//                    getPageDescription(it.description!!)
//                )
//            )
//        }
//        return pageList
//    }

//    private fun getPageHeader(header: Header): Map<String, String> {
//        return mapOf(
//            "en" to header.headerEn!!,
//            "in" to header.headerIn!!
//        )
//    }

//    private fun getPageDescription(description: Description): Map<String, String> {
//        return mapOf(
//            "en" to description.descEn!!,
//            "in" to description.descIn!!
//        )
//    }

//    fun getOptionList(list: List<OptionEntity>): List<Option> {
//        val optionList = mutableListOf<Option>()
//        list.forEach {
//            optionList.add(
//                Option(
//                    it.optionId!!,
//                    it.pageId!!,
//                    it.code!!,
//                    it.isSelected,
//                    getOptionTitle(it.title!!)
//                )
//            )
//        }
//        return optionList
//    }

//    fun getOptionTitle(title: Title): Map<String, String> {
//        return mapOf(
//            "en" to title.titleEn!!,
//            "in" to title.titleIn!!
//        )
//    }
}
