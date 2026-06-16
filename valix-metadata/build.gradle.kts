plugins {
    kotlin("jvm")
}

configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    pom {
        withXml {
            val root = asNode()
            val distributionManagement = root.appendNode("distributionManagement")
            val relocation = distributionManagement.appendNode("relocation")
            relocation.appendNode("groupId", "com.developersyndicate.valix")
            relocation.appendNode("artifactId", "valix-core")
            relocation.appendNode("version", "1.0.1")
            relocation.appendNode("message", "valix-metadata has been merged into valix-core. Please update your dependencies to com.developersyndicate.valix:valix-core.")
        }
    }
}
