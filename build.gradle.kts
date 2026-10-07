plugins {
    id("ksuto.java-application")
}

group = "fr.ksuto"
version = "1.0"

dependencies {
    implementation(libs.ksuto.peripherals)
    implementation(libs.ksuto.commons)
    implementation(libs.ksuto.logger)
    implementation(libs.commons.lang3)
    implementation(libs.snakeyaml)
    implementation(libs.jexl)
}

application {
    mainClass = "fr.ksuto.clockwork.Runner"
    // FlatLaf charge sa bibliothèque native (barre de titre Windows)
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}
