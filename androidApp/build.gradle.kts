import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

val localSupabaseProperties =
    Properties().apply {
        val propertiesFile = rootProject.file("supabase.properties")
        if (propertiesFile.isFile) {
            propertiesFile.inputStream().use(::load)
        }
    }

fun supabaseConfigurationValue(name: String): String =
    providers
        .environmentVariable(name)
        .orNull
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: localSupabaseProperties.getProperty(name)?.trim()?.takeIf(String::isNotEmpty)
        ?: ""

fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val configuredSupabaseUrl = supabaseConfigurationValue("SUPABASE_URL")
val configuredSupabasePublishableKey = supabaseConfigurationValue("SUPABASE_PUBLISHABLE_KEY")

abstract class ValidateSupabaseConfigurationTask : DefaultTask() {
    @get:Input
    abstract val supabaseUrl: Property<String>

    @get:Input
    abstract val supabasePublishableKey: Property<String>

    @TaskAction
    fun validateConfiguration() {
        val url = supabaseUrl.get()
        val publishableKey = supabasePublishableKey.get()
        val missingValues =
            buildList {
                if (url.isBlank()) add("SUPABASE_URL")
                if (publishableKey.isBlank()) add("SUPABASE_PUBLISHABLE_KEY")
            }

        if (missingValues.isNotEmpty()) {
            throw GradleException(
                "Missing Supabase configuration: ${missingValues.joinToString()}. " +
                    "Set these environment variables or create supabase.properties from " +
                    "supabase.properties.example. See 'Supabase development setup' in README.md.",
            )
        }

        val parsedUrl = runCatching { URI(url) }.getOrNull()
        if (
            parsedUrl == null ||
            parsedUrl.scheme !in setOf("http", "https") ||
            parsedUrl.host.isNullOrBlank()
        ) {
            throw GradleException(
                "Invalid SUPABASE_URL. Use a complete http:// or https:// URL. " +
                    "See 'Supabase development setup' in README.md.",
            )
        }

        if (
            publishableKey.isDocumentedPlaceholder() ||
            publishableKey.startsWith("sb_secret_", ignoreCase = true) ||
            publishableKey.isLegacyServiceRoleKey()
        ) {
            throw GradleException(
                "SUPABASE_PUBLISHABLE_KEY must be a real publishable client key, never a " +
                    "placeholder, secret, or service-role key. " +
                    "See 'Supabase development setup' in README.md.",
            )
        }
    }

    private fun String.isDocumentedPlaceholder(): Boolean =
        this in
            setOf(
                "replace_with_supabase_publishable_key",
                "sb_publishable_replace_me",
            )

    private fun String.isLegacyServiceRoleKey(): Boolean {
        val payload = split('.').getOrNull(1) ?: return false
        val decodedPayload =
            runCatching {
                String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8)
            }.getOrNull() ?: return false

        return Regex("\"role\"\\s*:\\s*\"service_role\"").containsMatchIn(decodedPayload)
    }
}

val validateSupabaseConfiguration =
    tasks.register<ValidateSupabaseConfigurationTask>("validateSupabaseConfiguration") {
        group = "verification"
        description = "Validates the external Supabase client configuration."
        supabaseUrl.set(configuredSupabaseUrl)
        supabasePublishableKey.set(configuredSupabasePublishableKey)
    }

android {
    namespace = "com.mediax.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mediax.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", configuredSupabaseUrl.asBuildConfigString())
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            configuredSupabasePublishableKey.asBuildConfigString(),
        )
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
    }
}

tasks.named("preBuild").configure {
    dependsOn(validateSupabaseConfiguration)
}

dependencies {
    implementation(project(":sharedLogic"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
