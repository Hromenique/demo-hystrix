package br.com.hrom.demohystrix.service

class ExternalOrderService {
    fun createOrder(products: List<String>) = Order(products = products)
}