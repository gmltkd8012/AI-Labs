package org.example.model

import kotlinx.serialization.Serializable

@Serializable
data class UserRequirement(
    val description: String,
    val features: List<String> = emptyList()
)

@Serializable
data class AgentSpec(
    val agentName: String,
    val role: String,
    val systemPrompt: String,
    val requiredTools: List<String>,
    val recommendedModel: String,
    val architectureNotes: String
)

@Serializable
data class GeneratedCode(
    val spec: AgentSpec,
    val files: List<CodeFile>
)

@Serializable
data class CodeFile(
    val path: String,
    val content: String
)

@Serializable
data class ReviewResult(
    val approved: Boolean,
    val score: Int,
    val issues: List<ReviewIssue>,
    val suggestions: List<String>
)

@Serializable
data class ReviewIssue(
    val severity: IssueSeverity,
    val location: String,
    val description: String,
    val suggestion: String
)

@Serializable
enum class IssueSeverity { CRITICAL, MAJOR, MINOR }
