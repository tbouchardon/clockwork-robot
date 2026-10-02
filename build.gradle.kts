plugins {
    id("ksuto.java-application")
    id("ksuto.picture-enums")
}

group = "fr.ksuto"
version = "1.0"

dependencies {
    implementation(libs.ksuto.peripherals)
    implementation(libs.ksuto.commons)
    implementation(libs.commons.lang3)
}

application {
    mainClass = "fr.ksuto.clockwork.Runner"
}
