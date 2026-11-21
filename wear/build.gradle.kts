plugins {
    id("rikkahub.android.library.compose")
}

android {
    namespace = "me.rerere.rikkahub.wear"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.material3)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.wear.compose.material3)
    api(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.huge.icons)

    testImplementation(libs.junit)
}
