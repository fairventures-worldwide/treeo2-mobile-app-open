package org.treeo.treeo.network.models

data class PageUploadDTO(
    var header: Map<String, String>,
    var answers: List<Map<String, String>>
)