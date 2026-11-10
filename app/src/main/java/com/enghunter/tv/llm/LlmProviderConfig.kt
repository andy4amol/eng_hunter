package com.enghunter.tv.llm

import android.content.Context
import android.content.Intent
import org.json.JSONObject

/**
 * 【里程碑 M4】大模型供應商與模型靈活配置中心（LlmProviderConfig）
 *
 * 針對電視端無實體鍵盤的痛點，提供三種極致便捷的配置方式：
 * 1. **電視遙控器一鍵切換預置供應商與熱門模型**：
 *    內置 7 大主流供應商（DeepSeek 官方、阿里雲百煉 Qwen、硅基流動 SiliconFlow、字節火山引擎豆包、OpenAI、局域網 Ollama、離線 AI 魔法生題器），
 *    每個供應商預置 3~4 個推薦模型，遙控器左右鍵即可一鍵切換，無需手敲 BaseURL 或模型名！
 * 2. **ADB 命令行 / 手機局域網一鍵注入配置**：
 *    在開發電腦終端一行命令即可把 API Key、供應商、模型或局域網 Ollama 地址推送到電視並持久化保存：
 *    `adb shell am broadcast -a com.enghunter.tv.CONFIG_LLM --es provider DEEPSEEK --es model deepseek-chat --es api_key sk-xxx`
 * 3. **電視端可視化設置面板 + 本地 SharedPreferences 持久化**：
 *    配置一次永久生效，未填寫 API Key 或斷網時自動回退到「離線 AI 句式裂變引擎」，確保孩子點擊「✨ AI 魔法挑戰」100% 秒出新題！
 */
enum class LlmVendorPreset(
    val id: String,
    val displayName: String,
    val badge: String,
    val defaultBaseUrl: String,
    val candidateModels: List<String>,
    val requiresApiKey: Boolean,
    val description: String
) {
    DEEPSEEK(
        id = "DEEPSEEK",
        displayName = "DeepSeek 官方 (深度求索)",
        badge = "DeepSeek",
        defaultBaseUrl = "https://api.deepseek.com/v1",
        candidateModels = listOf("deepseek-chat", "deepseek-reasoner"),
        requiresApiKey = true,
        description = "性价比最高的中英双语大模型，生成小学生拓展句准确自然"
    ),
    SILICONFLOW(
        id = "SILICONFLOW",
        displayName = "SiliconFlow 硅基流动 (含免费模型)",
        badge = "硅基流动",
        defaultBaseUrl = "https://api.siliconflow.cn/v1",
        candidateModels = listOf(
            "Qwen/Qwen2.5-7B-Instruct",
            "deepseek-ai/DeepSeek-V3",
            "THUDM/glm-4-9b-chat"
        ),
        requiresApiKey = true,
        description = "支持 Qwen2.5-7B 等免费/低价开源模型高速推理"
    ),
    ALIYUN_QWEN(
        id = "ALIYUN_QWEN",
        displayName = "阿里云百炼 (通义千问 Qwen)",
        badge = "通义千问",
        defaultBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        candidateModels = listOf("qwen-plus", "qwen-turbo", "qwen-max"),
        requiresApiKey = true,
        description = "国内访问极快，兼容 OpenAI 接口协议"
    ),
    VOLC_DOUBAO(
        id = "VOLC_DOUBAO",
        displayName = "火山引擎 (字节豆包 Doubao)",
        badge = "字节豆包",
        defaultBaseUrl = "https://ark.cn-beijing.volces.com/api/v3",
        candidateModels = listOf("doubao-1-5-pro-32k-250115", "doubao-1-5-lite-32k-250115"),
        requiresApiKey = true,
        description = "字节跳动豆包大模型，适合少儿口语情景互动"
    ),
    OPENAI(
        id = "OPENAI",
        displayName = "OpenAI / 兼容中转网关",
        badge = "OpenAI兼容",
        defaultBaseUrl = "https://api.openai.com/v1",
        candidateModels = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini"),
        requiresApiKey = true,
        description = "标准 OpenAI /v1/chat/completions 协议或任意自定义中转站"
    ),
    LOCAL_OLLAMA(
        id = "LOCAL_OLLAMA",
        displayName = "局域网 Ollama 本地模型 (免Key)",
        badge = "本地Ollama",
        defaultBaseUrl = "http://192.168.1.100:11434/v1",
        candidateModels = listOf("qwen2.5:7b", "llama3.2:3b", "deepseek-r1:7b", "gemma2:9b"),
        requiresApiKey = false,
        description = "连接家里电脑/NAS 上的 Ollama 服务，完全免费且无需外网 API Key"
    ),
    OFFLINE_SMART_TEMPLATE(
        id = "OFFLINE_SMART_TEMPLATE",
        displayName = "内置离线 AI 魔法裂变引擎 (零配置)",
        badge = "离线AI魔法",
        defaultBaseUrl = "offline://local-template-generator",
        candidateModels = listOf("enghunter-kid-expander-v1"),
        requiresApiKey = false,
        description = "零网络、零 Key 开箱即用！基于小学词汇图谱智能裂变新句式"
    );

    companion object {
        fun fromId(id: String?): LlmVendorPreset {
            if (id.isNullOrBlank()) return DEEPSEEK
            return entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: DEEPSEEK
        }
    }
}

data class LlmActiveConfig(
    val vendor: LlmVendorPreset = LlmVendorPreset.DEEPSEEK,
    val baseUrl: String = LlmVendorPreset.DEEPSEEK.defaultBaseUrl,
    val modelName: String = LlmVendorPreset.DEEPSEEK.candidateModels.first(),
    val apiKey: String = "",
    val temperature: Double = 0.7,
    val autoFallbackToOffline: Boolean = true
) {
    val isCloudReady: Boolean
        get() = when (vendor) {
            LlmVendorPreset.OFFLINE_SMART_TEMPLATE -> false
            LlmVendorPreset.LOCAL_OLLAMA -> baseUrl.startsWith("http")
            else -> baseUrl.startsWith("http") && apiKey.isNotBlank()
        }

    val maskedApiKey: String
        get() = when {
            apiKey.isBlank() -> "未设置 (将自动使用离线AI裂变)"
            apiKey.length <= 8 -> "****"
            else -> "${apiKey.take(5)}...${apiKey.takeLast(4)}"
        }

    fun toSummaryBadge(): String {
        return "${vendor.badge} · $modelName"
    }
}

/**
 * 持久化存儲與 ADB 廣播動態注入管理器
 */
class LlmConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): LlmActiveConfig {
        val vendor = LlmVendorPreset.fromId(prefs.getString(KEY_VENDOR, LlmVendorPreset.DEEPSEEK.id))
        val savedUrl = prefs.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() } ?: vendor.defaultBaseUrl
        val savedModel = prefs.getString(KEY_MODEL, null)?.takeIf { it.isNotBlank() } ?: vendor.candidateModels.first()
        val savedKey = prefs.getString(KEY_API_KEY, "") ?: ""
        val autoFallback = prefs.getBoolean(KEY_AUTO_FALLBACK, true)
        return LlmActiveConfig(
            vendor = vendor,
            baseUrl = savedUrl,
            modelName = savedModel,
            apiKey = savedKey,
            autoFallbackToOffline = autoFallback
        )
    }

    fun save(config: LlmActiveConfig) {
        prefs.edit()
            .putString(KEY_VENDOR, config.vendor.id)
            .putString(KEY_BASE_URL, config.baseUrl.trim())
            .putString(KEY_MODEL, config.modelName.trim())
            .putString(KEY_API_KEY, config.apiKey.trim())
            .putBoolean(KEY_AUTO_FALLBACK, config.autoFallbackToOffline)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "enghunter_llm_config"
        const val ACTION_ADB_CONFIG_LLM = "com.enghunter.tv.CONFIG_LLM"

        private const val KEY_VENDOR = "vendor_id"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model_name"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_AUTO_FALLBACK = "auto_fallback"

        /**
         * 解析來自 ADB 廣播或 Intent Extra 的快捷配置參數並合併到當前配置
         * 支持字段：
         * - `--es provider DEEPSEEK|SILICONFLOW|ALIYUN_QWEN|VOLC_DOUBAO|OPENAI|LOCAL_OLLAMA|OFFLINE_SMART_TEMPLATE`
         * - `--es model <模型名稱>`
         * - `--es base_url <自定義接口地址>`
         * - `--es api_key <API密鑰>`
         */
        fun mergeFromIntent(current: LlmActiveConfig, intent: Intent?): LlmActiveConfig? {
            if (intent == null) return null
            val providerArg = intent.getStringExtra("provider")
            val modelArg = intent.getStringExtra("model")
            val baseUrlArg = intent.getStringExtra("base_url")
            val apiKeyArg = intent.getStringExtra("api_key")

            if (providerArg == null && modelArg == null && baseUrlArg == null && apiKeyArg == null) {
                return null
            }

            val newVendor = if (!providerArg.isNullOrBlank()) {
                LlmVendorPreset.fromId(providerArg)
            } else {
                current.vendor
            }

            val vendorChanged = newVendor != current.vendor
            val newBaseUrl = when {
                !baseUrlArg.isNullOrBlank() -> baseUrlArg.trim()
                vendorChanged -> newVendor.defaultBaseUrl
                else -> current.baseUrl
            }
            val newModel = when {
                !modelArg.isNullOrBlank() -> modelArg.trim()
                vendorChanged -> newVendor.candidateModels.first()
                else -> current.modelName
            }
            val newApiKey = apiKeyArg?.trim() ?: current.apiKey

            return current.copy(
                vendor = newVendor,
                baseUrl = newBaseUrl,
                modelName = newModel,
                apiKey = newApiKey
            )
        }

        fun toExportJson(config: LlmActiveConfig): String {
            return JSONObject().apply {
                put("provider", config.vendor.id)
                put("baseUrl", config.baseUrl)
                put("model", config.modelName)
                put("hasApiKey", config.apiKey.isNotBlank())
                put("autoFallbackToOffline", config.autoFallbackToOffline)
            }.toString(2)
        }
    }
}
