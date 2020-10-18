package br.com.hrom.demohystrix.service

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey
import com.netflix.hystrix.HystrixCommandProperties
import com.netflix.hystrix.exception.HystrixRuntimeException
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.concurrent.TimeoutException

internal class CreateOrderCommandTest {

    private val externalOrderService = mockk<ExternalOrderService>()
    private val products = listOf("Playstation 4", "TV Phillips", "Teclado Microsoft")


    private fun assertOrder(order: Order) {
        assertThat(order.id).isNotNull()
        assertThat(order.products).isEqualTo(products)
        assertThat(order.createdAt).isEqualToIgnoringSeconds(LocalDateTime.now())
    }

    @Test
    fun executeTest() {
        // given
        every { externalOrderService.createOrder(products) } returns Order(products = products)

        // when
        val order = CreateOrderCommand(externalOrderService, products).execute()

        // then
        assertOrder(order)
    }

    @Test
    fun observeTest() {
        // given
        val order = Order(products = products)
        every { externalOrderService.createOrder(products) } answers { Thread.sleep(1000).run { order } }

        // when
        CreateOrderCommand(externalOrderService, products)
                .observe()
                .toBlocking()
                .first()
                .apply {
                    // then
                    assertOrder(this)
                }
    }

    @Test
    fun `given timeout greater than service execution time, then should work`() {
        // given
        every {
            externalOrderService.createOrder(products)
        } answers { Thread.sleep(1000).run { Order(products = products) } }

        // when
        val config = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("createOrderCommand"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(2000))
        val order = CreateOrderCommand(externalOrderService, products, config).execute()

        // then
        assertOrder(order)
    }

    @Test
    fun `given timeout less than service execution time, then should fail`() {
        // given
        every {
            externalOrderService.createOrder(products)
        } answers { Thread.sleep(10000).run { Order(products = products) } }

        // when
        val config = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("createOrderCommand"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(2000))

        // then
        assertThatThrownBy { CreateOrderCommand(externalOrderService, products, config).execute() }
                .isInstanceOf(HystrixRuntimeException::class.java)
                .hasCauseInstanceOf(TimeoutException::class.java)
    }

    @Test
    fun `execute - given error on call service, when does not exist a fallback, then fail`() {
        // given
        every { externalOrderService.createOrder(products) } throws IllegalStateException("The externalOrderService is not working")

        // when, then
        assertThatThrownBy { CreateOrderCommand(externalOrderService, products).execute() }
                .isInstanceOf(HystrixRuntimeException::class.java)
                .hasCauseInstanceOf(java.lang.IllegalStateException::class.java)
                .hasRootCauseMessage("The externalOrderService is not working")
    }


    @Test
    fun `execute - given error on call service, when there is a fallback, then return value from fallback`() {
        // given
        every { externalOrderService.createOrder(products) } throws IllegalStateException("The externalOrderService is not working")

        // when
        val order = CreateOrderCommandWitFallback(externalOrderService, products).execute()

        // then
        assertOrder(order)
    }
}