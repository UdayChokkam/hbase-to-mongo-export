package app.configuration

import app.services.KeyService
import org.apache.hadoop.hbase.client.Connection
import org.apache.http.client.HttpClient
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.security.SecureRandom

@Configuration
class TestContextConfiguration {

    @Bean
    @Profile("unitTest")
    fun secureRandom() = SecureRandom.getInstance("SHA1PRNG")

    @Bean
    @Profile("unitTest")
    fun connection()= Mockito.mock(Connection::class.java)

    @Bean
    @Profile("unitTest")
    fun httpClient()= Mockito.mock(HttpClient::class.java)

    @Bean
    @Profile("decryptionTest")
    fun dataKeyService(): KeyService {
        return Mockito.mock(KeyService::class.java)
    }
}