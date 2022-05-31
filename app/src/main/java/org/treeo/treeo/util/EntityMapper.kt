package org.treeo.treeo.util

interface EntityMapper<O, E> {
    fun toEntity(obj: O): E
    fun fromEntity(obj: E): O
}
