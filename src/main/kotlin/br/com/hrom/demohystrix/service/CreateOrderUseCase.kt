package br.com.hrom.demohystrix.service

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey


class CreateOrderService(private val orderService: ExternalOrderService) {
    fun create(products: List<String>) = orderService.createOrder(products)
}

open class CreateOrderCommand(
        private val orderService: ExternalOrderService,
        val products: List<String>,
        config: Setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("createOrderCommand"))
) : HystrixCommand<Order>(config) {
    override fun run(): Order = orderService.createOrder(products)
}

class CreateOrderCommandWitFallback : CreateOrderCommand {
    constructor(orderService: ExternalOrderService, products: List<String>, config: Setter) : super(orderService, products, config)
    constructor(orderService: ExternalOrderService, products: List<String>) : super(orderService = orderService, products = products)

    override fun getFallback(): Order {
        val order = Order(products = products)
        println("create a static order and send by queue do be processed async. order=$order")
        return order
    }
}