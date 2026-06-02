package capsule

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.util.regex.Pattern

/**
 * Loads capsule configuration from YAML files.
 *
 * Supports environment variable substitution using `${VAR_NAME}` syntax.
 * Pattern aligné sur plantuml-gradle ConfigLoader.
 */
object CapsuleConfigLoader {

    private val ENV_VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}")
    private val MAPPER: ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    /**
     * Loads configuration from a YAML file with environment variable resolution.
     * Returns default CapsuleConfig if file does not exist or is empty.
     *
     * @param configFile The YAML configuration file
     * @return The loaded CapsuleConfig with resolved environment variables
     */
    fun load(configFile: File): CapsuleConfig {
        if (!configFile.exists() || configFile.length() == 0L) {
            return CapsuleConfig()
        }

        val yamlContent = configFile.readText()
        val resolvedYaml = resolveEnvironmentVariables(yamlContent)
        return try {
            MAPPER.readValue(resolvedYaml, CapsuleConfig::class.java)
        } catch (e: Exception) {
            println("[capsule] WARNING: Failed to parse YAML ${configFile.absolutePath}: ${e.message}")
            CapsuleConfig()
        }
    }

    /**
     * Resolves environment variables in YAML content.
     *
     * Replaces all occurrences of `${VAR_NAME}` with the corresponding
     * environment variable value. If the variable is not found, the
     * original `${VAR_NAME}` syntax is preserved.
     *
     * @param yamlContent The raw YAML content
     * @return The YAML content with resolved environment variables
     */
    fun resolveEnvironmentVariables(yamlContent: String): String {
        val matcher = ENV_VAR_PATTERN.matcher(yamlContent)
        val result = StringBuffer()

        while (matcher.find()) {
            val varName = matcher.group(1)
            val envValue = System.getenv(varName)
            val replacement = envValue ?: "\${$varName}"
            matcher.appendReplacement(result, Regex.escapeReplacement(replacement))
        }
        matcher.appendTail(result)

        return result.toString()
    }
}