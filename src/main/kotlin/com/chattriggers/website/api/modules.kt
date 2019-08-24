package com.chattriggers.website.api

import com.chattriggers.website.data.Module
import com.chattriggers.website.data.Modules
import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

const val METADATA_NAME = "metadata.json"
const val SCRIPTS_NAME = "scripts.zip"

fun moduleRoutes() {
    crud("modules/:module-id", ModuleController())

    // Essentially stuff to be used by the mod. The mod has no knowledge of module-id's,
    // releases, etc.
    // Instead, it gets to pass the module's name and its current mod version,
    // and the server handles all of the hard work finding the correct release version.
    get("modules/:module-name/metadata", ::getMetadata)
    get("module/:module-name/scripts", ::getScripts)
}

fun getMetadata(ctx: Context) {
    val releaseFolder = getReleaseFolder(ctx) ?: throw NotFoundResponse("No release applicable for specified mod version.")
    val file = File(releaseFolder, METADATA_NAME)

    ctx.status(200).contentType("application/json").result(file.inputStream())
}

fun getScripts(ctx: Context) {
    val releaseFolder = getReleaseFolder(ctx) ?: throw NotFoundResponse("No release applicable for specified mod version.")
    val file = File(releaseFolder, SCRIPTS_NAME)

    ctx.status(200).contentType("application/zip").result(file.inputStream())
}

fun getReleaseFolder(ctx: Context) = transaction {
    val moduleName = ctx.pathParam("module-name").toLowerCase()

    val module = Module.find { Modules.name.lowerCase() eq moduleName }
        .firstOrNull() ?: throw NotFoundResponse("No module with that module-name")

    val modVersion = ctx.queryParam("modVersion") ?: throw BadRequestResponse("Missing 'modVersion' query parameter.")

    try {
        val release = getReleaseForModVersion(module, modVersion) ?: return@transaction null

        File("storage/$moduleName/${release.id.value}")
    } catch (e: Exception) {
        throw BadRequestResponse("Invalid query.")
    }
}