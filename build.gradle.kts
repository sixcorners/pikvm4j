import net.ltgt.gradle.errorprone.errorprone

plugins {
  `java-library`
  `maven-publish`
  id("com.diffplug.spotless") version "8.8.0"
  id("net.ltgt.errorprone") version "5.1.0"
  id("io.freefair.lombok") version "9.5.0"
  id("pl.allegro.tech.build.axion-release") version "1.21.2"
  id("org.kordamp.gradle.jandex") version "2.3.0"
  id("org.openapi.generator") version "7.23.0"
}

group = "com.github.sixcorners"

version = scmVersion.version

repositories { mavenCentral() }

dependencies {
  compileOnly(platform("org.eclipse.microprofile:microprofile:7.1"))
  compileOnly("org.eclipse.microprofile.rest.client:microprofile-rest-client-api")
  compileOnly("jakarta.json:jakarta.json-api:2.1.3")
  compileOnly("jakarta.json.bind:jakarta.json.bind-api:3.0.2")
  compileOnly("jakarta.validation:jakarta.validation-api:3.1.1")
  testImplementation("org.jboss.resteasy.microprofile:microprofile-rest-client:3.0.1.Final")
  testImplementation("org.jboss.resteasy:resteasy-json-binding-provider:7.0.2.Final")
  errorprone("com.google.errorprone:error_prone_core:2.50.0")
}

tasks {
  withType<JavaCompile>().configureEach {
    mustRunAfter("openApiGenerate")
    options.errorprone.disableWarningsInGeneratedCode = true
    options.errorprone.disable("MissingSummary", "ParameterName")
  }
  wrapper { gradleVersion = "latest" }
  matching { it.name == "spotlessJava" }.configureEach { mustRunAfter("openApiGenerate") }
}

testing { suites { named<JvmTestSuite>("test") { useJUnitJupiter() } } }

spotless {
  java { googleJavaFormat() }
  kotlinGradle { ktfmt() }
}

openApiGenerate {
  setInputSpec("https://api.openapi-generator.tech/api-docs")
  skipValidateSpec = true
  invokerPackage = "${project.group}.${project.name}"
  apiPackage = "${invokerPackage.get()}.api"
  modelPackage = "${invokerPackage.get()}.model"
  generatorName = "java"
  library = "microprofile"
  configOptions.put("configKey", project.name)
  configOptions.put("dateLibrary", "java8")
  configOptions.put("microprofileRestClientVersion", "3.0")
  configOptions.put("useBeanValidation", "true")
  configOptions.put("useRuntimeException", "true")
}

sourceSets {
  main { java { srcDir(layout.buildDirectory.dir("generate-resources/main/src/main/java")) } }
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
    }
  }
}
