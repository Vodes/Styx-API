package moe.styx.misc

import kotlinx.coroutines.runBlocking
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.data.MyAnimeListData
import moe.styx.common.data.User
import moe.styx.libs.mal.AbstractMALApiClient
import moe.styx.libs.mal.MALServerFunctions
import moe.styx.libs.mal.ext.fetching.fetchCurrentUser
import moe.styx.libs.mal.returnables.RefreshResult

class NoRefreshMALClient(malData: MyAnimeListData) : AbstractMALApiClient(malData) {
    override suspend fun refreshToken(): RefreshResult {
        return RefreshResult(-1, null)
    }

    companion object {
        fun refreshTokenForUser(user: User): MyAnimeListData? {
            val config = UnifiedConfig.current
            if (config.webConfig.malClientID.isBlank() || config.webConfig.malClientSecret.isBlank())
                return null
            val newData = runBlocking {
                MALServerFunctions.refreshToken(
                    user.malData!!.refreshToken,
                    config.webConfig.malClientID,
                    config.webConfig.malClientSecret
                )
            }
            var newMalData = newData.data
            if (newData.isSuccess && newData.data != null) {
                val client = NoRefreshMALClient(newData.data!!)
                val userResp = runBlocking { client.fetchCurrentUser() }
                if (userResp.isSuccess && userResp.data != null) {
                    newMalData = newMalData?.copy(userName = userResp.data!!.name, userID = userResp.data!!.id)
                }
            }
            return newMalData
        }
    }
}