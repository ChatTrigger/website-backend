package com.chattriggers.website.api

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.http.Context
import java.io.File

fun tagRoutes() {
    get("tags", ::getTags)
}

const val TAGS_FILE = "tags.txt"
const val TIMEOUT = 1000 * 60 * 30
var allowedTags = File(TAGS_FILE).readText().split("\n").map { it.trim() }
var lastCheckTime = System.currentTimeMillis()

fun getTags(ctx: Context) {
    if (System.currentTimeMillis() - lastCheckTime > TIMEOUT) {
        allowedTags = File(TAGS_FILE).readText().split("\n").map { it.trim() }

        lastCheckTime = System.currentTimeMillis()
    }

    ctx.status(200).json(allowedTags)
}