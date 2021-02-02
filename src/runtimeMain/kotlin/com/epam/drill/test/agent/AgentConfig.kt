package com.epam.drill.test.agent

import com.epam.drill.kni.*

@Kni
actual object AgentConfig {
    actual external fun proxyUrl(): String?

    actual external fun dispatcherUrl(): String?

    actual external fun adminAddress(): String?

    actual external fun agentId(): String?

    actual external fun groupId(): String?

    actual external fun agentUrl(): String?

    actual external fun extensionUrl(): String?
    actual external fun extensionVersion(): String?
}
