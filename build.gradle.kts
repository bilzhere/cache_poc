plugins {
	java
	id("org.springframework.boot") version "3.4.0"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com.chamberlain.cache"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
	maven {
			url = uri("https://redisson.pro/repo/")
		}
}

dependencies {
	implementation("pro.redisson:redisson:3.39.0")
	compileOnly("org.projectlombok:lombok")
	implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
	//implementation("org.redisson:redisson:3.39.0")
	implementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
	implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
	implementation("io.lettuce:lettuce-core:6.5.1.RELEASE")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
	annotationProcessor("org.projectlombok:lombok")
	implementation("org.springframework.boot:spring-boot-starter-web")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
