plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.opencsv:opencsv:5.9")
    implementation("com.vk.api:sdk:1.0.16")
    implementation("org.apache.commons:commons-lang3:3.6")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.11.2")
    implementation("org.apache.logging.log4j:log4j-api:2.11.2")
    implementation("org.apache.logging.log4j:log4j-core:2.11.2")
    implementation("org.apache.httpcomponents:httpclient:4.5.8")
    implementation("commons-io:commons-io:2.6")
    implementation("org.apache.commons:commons-collections4:4.3")
    implementation("com.google.code.gson:gson:2.8.5")
    //implementation("org.slf4j:slf4j-api:1.7.26")
}

tasks.test {
    useJUnitPlatform()
}