import org.gradle.api.Project

fun Project.prop(name: String): String =
    requireNotNull(findProperty(name)) { "No property named '$name'" } as String

fun Project.mod(name: String): String = prop("mod.$name")
fun Project.libs(name: String): String = prop("libs.$name")
