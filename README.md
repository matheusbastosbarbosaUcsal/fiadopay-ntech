# FiadoPay Simulator (Spring Boot + H2)

Gateway de pagamento **FiadoPay** para a AVI/POOA.
Substitui PSPs reais com um backend em memória (H2).

## Rodar
```bash
./mvnw spring-boot:run
# ou
mvn spring-boot:run
```

H2 console: http://localhost:8080/h2  
Swagger UI: http://localhost:8080/swagger-ui.html

## Contexto
Foi utilizado a opção 1, refatorando o código com foco em Engenharia. 

## Padrões
O foco inicial foi no SRP, fazendo separação de responsabilidade no service do PaymentService, dessa forma foram criadas a classe WebhookDeliveryService, MerchantService e PaymentProcessor. Além disso, foi separado criado um método na PaymentService para o calculo de juros.

## Threads
O processo assíncrono foi mudado para utilizar ExecutorService. Essa mudança permite controlar a quantidade de threads disponíveis e, dessa forma, protege a aplicação de gargalos, evitando a rejeitando ou enfileirando tasks.

## Anotações
Foi criada a anotação PaymentMethod para ter um controle maior dos tipos de pagamento e identificar de forma mais clara. Permite que o calculo de juros não fique preso ao tipo "CARD", pois se em algum outro momento venha a ter juros/taxa para outros pagamentos, não precisa mudar o método.

## Fluxo

1) **Cadastrar merchant**
```bash
curl -X POST http://localhost:8080/fiadopay/admin/merchants   -H "Content-Type: application/json"   -d '{"name":"MinhaLoja ADS","webhookUrl":"http://localhost:8081/webhooks/payments"}'
```

2) **Obter token**
```bash
curl -X POST http://localhost:8080/fiadopay/auth/token   -H "Content-Type: application/json"   -d '{"client_id":"<clientId>","client_secret":"<clientSecret>"}'
```

3) **Criar pagamento**
```bash
curl -X POST http://localhost:8080/fiadopay/gateway/payments   -H "Authorization: Bearer FAKE-<merchantId>"   -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000"   -H "Content-Type: application/json"   -d '{"method":"CARD","currency":"BRL","amount":250.50,"installments":12,"metadataOrderId":"ORD-123"}'
```

4) **Consultar pagamento**
```bash
curl http://localhost:8080/fiadopay/gateway/payments/<paymentId>
```
