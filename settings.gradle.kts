pluginManagement {
    includeBuild("../Bot Parent")
}

plugins {
    id("ksuto.settings")
}

rootProject.name = "clockwork"

includeBuild("../Commons")
includeBuild("../Logger")
includeBuild("../TBoTools")
includeBuild("../Bot Peripherals")
includeBuild("../Bot Generator")
